import ffmpeg.*
import kotlin.system.*
import kotlinx.cinterop.*
import konan.worker.*

// This global variable only set to != null value in decoding worker.
var state: DecodeWorkerState? = null

data class OutputInfo(val width: Int, val height: Int, val pixelFormat: AVPixelFormat)
data class VideoInfo(val width: Int, val height: Int, val fps: Double,
                     val sampleRate: Int, val channels: Int)
data class AskNextAudioFrame(val size: Int)
data class VideoFrame(val buffer: CPointer<AVBufferRef>, val lineSize: Int) {
    fun unref() = av_buffer_unref2(buffer)
}
data class AudioFrame(val buffer: CPointer<AVBufferRef>, val position: Int, val size: Int) {
    fun unref() {
        if (position + size >= buffer.pointed.size)
            av_buffer_unref2(buffer)
    }
}

class DecodeWorkerState(val formatContext: CPointer<AVFormatContext>,
                        val videoStreamIndex: Int,
                        val audioStreamIndex: Int,
                        val videoCodecContext: CPointer<AVCodecContext>?,
                        val audioCodecContext: CPointer<AVCodecContext>?) {
    var videoFrame: CPointer<AVFrame>? = null
    var scaledVideoFrame: CPointer<AVFrame>? = null
    var audioFrame: CPointer<AVFrame>? = null
    var softwareScalingContext: CPointer<SwsContext>? = null
    val videoQueue = Queue<VideoFrame?>(100, null)
    val audioQueue = Queue<AudioFrame?>(1000, null)
    var buffer: ByteArray? = null
    var videoWidth = 0
    var videoHeight = 0
    var windowWidth = 0
    var windowHeight = 0
    var frameBytes = 0

    fun makeVideoFrame(): VideoFrame {
        //val buffer = av_buffer_ref(scaledVideoFrame!!.pointed.buf[0])
        // TODO: reuse buffers!
        val buffer = av_buffer_alloc(frameBytes)!!
        platform.posix.memcpy(buffer.pointed.data, scaledVideoFrame!!.pointed.data[0], frameBytes.signExtend())
        return VideoFrame(buffer, scaledVideoFrame!!.pointed.linesize[0])
    }

    fun makeAudioFrame(): AudioFrame {
        val dataSize = av_samples_get_buffer_size(
                null,
                audioFrame!!.pointed.channels,
                audioFrame!!.pointed.nb_samples,
                audioCodecContext!!.pointed.sample_fmt,
                1)
        val buffer = av_buffer_alloc(frameBytes)!!
        platform.posix.memcpy(buffer.pointed.data, audioFrame!!.pointed.data[0], dataSize.signExtend())
        return AudioFrame(buffer, 0, dataSize)
    }

    fun start(output: OutputInfo) {
        if (videoCodecContext != null) {
            videoWidth = videoCodecContext.pointed.width
            videoHeight = videoCodecContext.pointed.height
            windowWidth = output.width
            windowHeight = output.height
            videoFrame = av_frame_alloc()!!
            scaledVideoFrame = av_frame_alloc()!!
            softwareScalingContext = sws_getContext(
                    videoWidth,
                    videoHeight,
                    videoCodecContext.pointed.pix_fmt,
                    windowWidth, windowHeight, output.pixelFormat,
                    SWS_BILINEAR, null, null, null)!!

            frameBytes = avpicture_get_size(output.pixelFormat, output.width, output.height)
            buffer = ByteArray(frameBytes)

            avpicture_fill(scaledVideoFrame!!.reinterpret(), buffer!!.refTo(0),
                    output.pixelFormat, output.width, output.height)
        }

        if (audioCodecContext != null) {
            audioFrame = av_frame_alloc()!!
        }

        decodeIfNeeded()
    }

    fun stop() {
    }

    fun needMoreBuffers(): Boolean {
        return ((videoStreamIndex != -1) && (videoQueue.size() < 5)) ||
                ((audioStreamIndex != -1) && (audioQueue.size() < 50))
    }

    fun decodeIfNeeded() {
        println("decodeIfNeeded")
        if (!needMoreBuffers()) return

        memScoped {
            val packet = alloc<AVPacket>()
            val frameFinished = alloc<IntVar>()
            while (needMoreBuffers() && av_read_frame(formatContext, packet.ptr) >= 0) {
                when (packet.stream_index) {
                    videoStreamIndex -> {
                        // Decode video frame.
                        avcodec_decode_video2(videoCodecContext, videoFrame, frameFinished.ptr, packet.ptr)
                        // Did we get a video frame?
                        if (frameFinished.value != 0) {
                            // Convert the frame from its movie format to window pixel format.
                            sws_scale(softwareScalingContext, videoFrame!!.pointed.data,
                                    videoFrame!!.pointed.linesize, 0, videoHeight,
                                    scaledVideoFrame!!.pointed.data, scaledVideoFrame!!.pointed.linesize)
                            videoQueue.push(makeVideoFrame())
                        }
                    }
                    audioStreamIndex -> {
                        // Put audio frame to decoder's queue.
                        avcodec_decode_audio4(audioCodecContext, audioFrame, frameFinished.ptr, packet.ptr)
                        if (frameFinished.value != 0) {
                            audioQueue.push(makeAudioFrame())
                        }
                    }
                }
            }
        }
    }

    fun nextVideoFrame(): VideoFrame? {
        println("nextVideoFrame")
        decodeIfNeeded()

        if (videoQueue.isEmpty()) {
            return null
        }
        return videoQueue.pop()
    }

    fun nextAudioFrame(size: Int): AudioFrame? {
        println("nextAudioFrame $size")
        decodeIfNeeded()

        if (audioQueue.isEmpty()) {
            return null
        }
        return audioQueue.pop()
    }
}

class DecodeWorker {
    // This class must have no other state, but this worker object.
    // All the real state must be stored on the worker's side.
    private val decodeWorker: Worker

    constructor() {
        decodeWorker = konan.worker.startWorker()
    }

    constructor(id: WorkerId) {
        decodeWorker = Worker(id)
    }

    fun workerId() = decodeWorker.id

    fun init() {}

    fun renderPixelFormat(pixelFormat: PixelFormat) = when (pixelFormat) {
        PixelFormat.RGB24 -> AV_PIX_FMT_RGB24
        PixelFormat.ARGB32 -> AV_PIX_FMT_RGB32
        else -> {
            println("$pixelFormat unsupported!")
            TODO()
        }
    }

    private fun findStream(formatContext: CPointer<AVFormatContext>, streamIndex: Int, tag: String):
            Pair<CPointer<AVStream>?, CPointer<AVCodecContext>?> {
        if (streamIndex < 0) return null to null
        val stream = formatContext.pointed.streams!!.get(streamIndex)!!
        // Get codec context for the video stream.
        val codecContext = stream.pointed.codec!!
        val codec = avcodec_find_decoder(codecContext.pointed.codec_id)
        if (codec == null)
            throw Error("Unsupported $tag codec...")
        // Open codec.
        if (avcodec_open2(codecContext, codec, null) < 0)
            throw Error("Couldn't open $tag codec")
        return stream to codecContext
    }

    fun initDecode(file: String): VideoInfo {
        try {
            memScoped {
                val formatContextPtr = alloc<CPointerVar<AVFormatContext>>()
                if (avformat_open_input(formatContextPtr.ptr, file, null, null) != 0)
                    throw Error("Cannot open video file")
                val formatContext = formatContextPtr.value!!
                if (avformat_find_stream_info(formatContext, null) < 0)
                    throw Error("Couldn't find stream information")
                av_dump_format(formatContext, 0, file, 0)

                // Find the first video/audio streams.
                var videoStreamIndex = -1
                var audioStreamIndex = -1
                for (i in 0 until formatContext.pointed.nb_streams) {
                    val stream = formatContext.pointed.streams!!.get(i)
                    val codec = stream!!.pointed.codec!!.pointed
                    if (codec.codec_type == AVMEDIA_TYPE_VIDEO && videoStreamIndex == -1) {
                        videoStreamIndex = i
                    }
                    if (codec.codec_type == AVMEDIA_TYPE_AUDIO && audioStreamIndex == -1) {
                        audioStreamIndex = i
                    }
                }

                val (videoStream, videoCodecContext) = findStream(formatContext, videoStreamIndex, "video")
                val (_, audioCodecContext) = findStream(formatContext, audioStreamIndex, "audio")

                // Extract video info.
                val (videoWidth, videoHeight, fps) = if (videoCodecContext != null) {
                    Triple(videoCodecContext.pointed.width, videoCodecContext.pointed.height,
                            av_q2d(av_stream_get_r_frame_rate(videoStream)))
                } else {
                    Triple(-1, -1, 0.0)
                }
                val (sampleRate, channels) = if (audioCodecContext != null) {
                    Pair(audioCodecContext.pointed.sample_rate, audioCodecContext.pointed.channels)
                } else {
                    Pair(0, 0)
                }

                // Pack all inited state and pass it to the worker.
                decodeWorker.schedule(TransferMode.CHECKED, {
                    DecodeWorkerState(formatContext,
                            videoStreamIndex, audioStreamIndex,
                            videoCodecContext, audioCodecContext)
                }) { input ->
                    state = input
                    null
                }
                return VideoInfo(videoWidth, videoHeight, fps, sampleRate, channels)
            }
        } finally {
            // TODO: clean up whatever we allocated.
        }
    }

    fun start(width: Int, height: Int, pixelFormat: PixelFormat) {
        decodeWorker.schedule(TransferMode.CHECKED, {
            OutputInfo(width, height, renderPixelFormat(pixelFormat))
        }) { input ->

            state!!.start(input)

            null
        }
    }

    fun stop() {
        decodeWorker.schedule(TransferMode.CHECKED, {
            null
        }) { _ ->
            state!!.stop()
            null
        }
    }

    fun deinit() {
        decodeWorker.requestTermination().consume { _ -> }
    }

    fun nextVideoFrame(): VideoFrame? {
        var result: VideoFrame? = null
        decodeWorker.schedule(TransferMode.CHECKED, { null }) {
            _ -> state!!.nextVideoFrame()
        }.consume {
            it ->
            result = it
        }
        return result
    }

    fun nextAudioFrame(size: Int): AudioFrame? {
        var result: AudioFrame? = null
        decodeWorker.schedule(TransferMode.CHECKED, { AskNextAudioFrame(size) }) {
            input -> state!!.nextAudioFrame(input.size)
        }.consume {
            it ->
            result = it
        }
        return result
    }
}