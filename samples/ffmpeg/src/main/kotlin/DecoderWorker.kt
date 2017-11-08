import ffmpeg.*
import kotlin.system.*
import kotlinx.cinterop.*
import konan.worker.*

// This global variable only set to != null value in decoding worker.
var state: DecodeWorkerState? = null

class NextVideoFrame(val buffer: CPointer<AVBufferRef>?)
class NextAudioFrame(val buffer: CPointer<AVBufferRef>?)
class OutputInfo(val width: Int, val height: Int, val pixelFormat: AVPixelFormat)

data class VideoInfo(val width: Int, val height: Int, val fps: Double,
                     val sampleRate: Int, val channels: Int)

class DecodeWorkerState(val formatContext: CPointer<AVFormatContext>,
                        val videoStreamIndex: Int,
                        val audioStreamIndex: Int,
                        val videoCodecContext: CPointer<AVCodecContext>?,
                        val audioCodecContext: CPointer<AVCodecContext>?) {
    var videoFrame: CPointer<AVFrame>? = null
    var scaledVideoFrame: CPointer<AVFrame>? = null
    var audioFrame: CPointer<AVFrame>? = null
    var softwareScalingContext: CPointer<SwsContext>? = null
    val audioQueue = Queue<CPointer<AVBufferRef>?>(100, null)
    val videoQueue = Queue<CPointer<AVBufferRef>?>(1000, null)
    var buffer: ByteArray? = null
    var videoWidth = 0
    var videoHeight = 0
    var windowWidth = 0
    var windowHeight = 0

    fun makeVideoFrame(): CPointer<AVBufferRef>? {
        return null
    }

    fun makeAudioFrame(): CPointer<AVBufferRef>? {
        return null
    }

    fun startPlayback(output: OutputInfo) {
        if (videoStreamIndex != -1) {
            videoWidth = videoCodecContext!!.pointed.width
            videoHeight = videoCodecContext!!.pointed.height
            windowWidth = output.width
            windowHeight = output.height
            videoFrame = av_frame_alloc()!!
            scaledVideoFrame = av_frame_alloc()!!
            softwareScalingContext = sws_getContext(
                    videoWidth,
                    videoHeight,
                    videoCodecContext!!.pointed.pix_fmt,
                    windowWidth, windowHeight, output.pixelFormat,
                    SWS_BILINEAR, null, null, null)!!

            val frameBytes = avpicture_get_size(output.pixelFormat, output.width, output.height)
            buffer = ByteArray(frameBytes)

            avpicture_fill(scaledVideoFrame!!.reinterpret(), buffer!!.refTo(0),
                    output.pixelFormat, output.width, output.height)


        }

        if (audioStreamIndex != -1) {
            audioFrame = av_frame_alloc()!!
        }

        decodeIfNeeded()
    }

    fun needMoreBuffers(): Boolean {
        return ((videoStreamIndex != -1) && (videoQueue.size() < 30)) ||
                ((audioStreamIndex != -1) && (audioQueue.size() < 500))
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
                            val dataSize = av_samples_get_buffer_size(
                                    null,
                                    audioFrame!!.pointed.channels,
                                    audioFrame!!.pointed.nb_samples,
                                    audioCodecContext!!.pointed.sample_fmt,
                                    1)
                            audioQueue.push(makeAudioFrame())
                        }
                    }
                }
            }
        }
    }

    fun nextVideoFrame(): CPointer<AVBufferRef>? {
        println("nextVideoFrame")
        decodeIfNeeded()
        return null
    }

    fun nextAudioFrame(): CPointer<AVBufferRef>? {
        println("nextAudioFrame")
        decodeIfNeeded()
        return null
    }
}

class DecodeWorker {
    val decodeWorker = konan.worker.startWorker()

    fun init() {
        av_register_all()
    }

    fun deinit() {
        decodeWorker.requestTermination().consume { _ -> }
    }

    fun renderPixelFormat(pixelFormat: PixelFormat) = when (pixelFormat) {
        PixelFormat.RGB24 -> AV_PIX_FMT_RGB24
        PixelFormat.ARGB32 -> AV_PIX_FMT_RGB32
        else -> TODO()
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

                if (videoStreamIndex == -1)
                    throw Error("No video stream")

                if (audioStreamIndex == -1)
                    throw Error("No audio stream")

                // VIDEO part.
                val videoStream = formatContext.pointed.streams!!.get(videoStreamIndex)!!
                // Get codec context for the video stream.
                val videoCodecContext = videoStream.pointed.codec!!
                val videoCodec = avcodec_find_decoder(videoCodecContext.pointed.codec_id)
                if (videoCodec == null)
                    throw Error("Unsupported codec in video...")

                // Open video codec.
                if (avcodec_open2(videoCodecContext, videoCodec, null) < 0)
                    throw Error("Couldn't open video codec")

                // AUDIO part.
                val audioStream = formatContext.pointed.streams!!.get(audioStreamIndex)!!

                // Get codec context for the audio stream.
                val audioCodecContext = audioStream.pointed.codec!!
                val audioCodec = avcodec_find_decoder(audioCodecContext.pointed.codec_id)
                if (audioCodec == null)
                    throw Error("Unsupported audio codec...")

                // Open audio codec.
                if (avcodec_open2(audioCodecContext, audioCodec, null) < 0)
                    throw Error("Couldn't open audio codec")

                // Extract video info.
                val videoWidth = videoCodecContext.pointed.width
                val videoHeight = videoCodecContext.pointed.height
                val fps = av_q2d(av_stream_get_r_frame_rate(videoStream))

                // Extract audio info.
                val sampleRate = audioCodecContext.pointed.sample_rate
                val channels = audioCodecContext.pointed.channels

                // Pack all inited state and pass it to the worker.
                decodeWorker.schedule(TransferMode.CHECKED, {
                    DecodeWorkerState(formatContext, videoStreamIndex, audioStreamIndex,
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

    fun startPlayback(width: Int, height: Int, pixelFormat: PixelFormat) {
        decodeWorker.schedule(TransferMode.CHECKED, {
            OutputInfo(width, height, renderPixelFormat(pixelFormat))
        }) { input ->

            state!!.startPlayback(input)

            null
        }
    }


    fun nextVideoFrame(): CPointer<AVBufferRef>? {
        var result: CPointer<AVBufferRef>? = null
        decodeWorker.schedule(TransferMode.CHECKED, { null }) {
            input ->
            NextVideoFrame(state!!.nextVideoFrame())
        }.consume {
            it ->
            result = it.buffer
        }
        return result
    }

    fun nextAudioFrame(): CPointer<AVBufferRef>? {
        var result: CPointer<AVBufferRef>? = null
        decodeWorker.schedule(TransferMode.CHECKED, { null }) {
            input -> NextAudioFrame(state!!.nextAudioFrame())
        }.consume {
            it ->
            result = it.buffer
        }
        return result
    }

    /*
            // Allocate frames.
            val videoFrame = av_frame_alloc()!!
            val audioFrame = av_frame_alloc()!!
            val frameRGB = av_frame_alloc()!!


            val packet = alloc<AVPacket>()
            val frameFinished = alloc<IntVar>()

            video.startPlayback(videoWidth, videoHeight, fps)

            // Allocate audio stream.

            audio.startPlayback(sampleRate, channels)

            val pixelFormat = renderPixelFormat()

            // Initialize SWS context for software scaling.
            val softwareScalingContext = sws_getContext(
                    videoWidth, videoHeight, videoCodecContext.pointed.pix_fmt,
                    videoWidth, videoHeight, pixelFormat, SWS_BILINEAR,
                    null, null, null)

            val frameBytes = avpicture_get_size(pixelFormat, videoWidth, videoHeight)
            println("frame ${videoWidth}x${videoHeight}, size is $frameBytes bytes")
            // Or use av_malloc?
            val buffer = ByteArray(frameBytes)
            avpicture_fill(frameRGB.reinterpret(), buffer.refTo(0), pixelFormat, videoWidth, videoHeight)

            state = State.PLAYING

            while (state != State.STOPPED && av_read_frame(formatContext, packet.ptr) >= 0) {

                while (state == State.PAUSED) {
                    video.checkInput()
                    platform.posix.usleep(5 * 1000)
                }

                when (packet.stream_index) {
                    videoStreamIndex -> {
                        // Decode video frame.
                        avcodec_decode_video2(videoCodecContext, videoFrame, frameFinished.ptr, packet.ptr)

                        // Did we get a video frame?
                        if (frameFinished.value != 0) {
                            // Convert the image from its native format to window pixel format.
                            sws_scale(softwareScalingContext, videoFrame.pointed.data,
                                    videoFrame.pointed.linesize, 0, videoHeight,
                                    frameRGB.pointed.data, frameRGB.pointed.linesize)
                            // Render the frame.
                            video.nextFrame(
                                    frameRGB.pointed.data[0]!!, frameRGB.pointed.linesize[0], videoWidth, videoHeight)
                        }
                    }
                    audioStreamIndex -> {
                        // Put audio frame to decoder's queue.
                        avcodec_decode_audio4(audioCodecContext, audioFrame, frameFinished.ptr, packet.ptr)
                        if (frameFinished.value != 0) {
                            val dataSize = av_samples_get_buffer_size(
                                    null, channels, audioFrame.pointed.nb_samples,
                                    audioCodecContext.pointed.sample_fmt, 1)
                            audio.nextFrame(audioFrame.pointed.data[0], dataSize)
                        }
                    }
                }
                //av_packet_unref(packet.ptr)
            }
            video.stopPlayback()
            av_frame_free(videoFrame.ptr)
            av_frame_free(audioFrame.ptr)
        }
    } finally {
        audio.deinit()
        video.deinit()
    }*/
}