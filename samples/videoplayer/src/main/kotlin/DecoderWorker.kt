/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import ffmpeg.*
import kotlinx.cinterop.*
import konan.worker.*
import platform.posix.memcpy

// This global variable only set to != null value in the decoding worker.
private var decoder: Decoder? = null

data class OutputInfo(val windowSize: Vec2D, val pixelFormat: AVPixelFormat)
data class VideoInfo(val size: Vec2D, val fps: Double)
data class AudioInfo(val sampleRate: Int, val channels: Int)

data class CodecInfo(val video: VideoInfo?, val audio: AudioInfo?) {
    val hasVideo = video != null
    val hasAudio = audio != null
}

class VideoFrame(val buffer: CPointer<AVBufferRef>, val lineSize: Int, val timeStamp: Double) {
    fun unref() = av_buffer_unref2(buffer)
}

class AudioFrame(val buffer: CPointer<AVBufferRef>, var position: Int, val size: Int, val timeStamp: Double) {
    fun unref() = av_buffer_unref2(buffer)
}

private fun renderPixelFormat(pixelFormat: PixelFormat) = when (pixelFormat) {
    PixelFormat.RGB24 -> AV_PIX_FMT_RGB24
    PixelFormat.ARGB32 -> AV_PIX_FMT_RGB32
    PixelFormat.INVALID -> AV_PIX_FMT_NONE
}

private fun Int.checkAVError() {
    if (this != 0) {
        val buffer = ByteArray(1024)
        av_strerror(this, buffer.refTo(0), buffer.size.signExtend())
        throw Error("AVError: ${buffer.stringFromUtf8()}")
    }
}

private val AVFormatContext.codecs: List<AVCodecContext?>
    get() = List(nb_streams) { streams?.get(it)?.pointed?.codec?.pointed }

private fun AVFormatContext.streamAt(index: Int): AVStream? =
    if (index < 0) null else streams?.get(index)?.pointed

private fun AVStream.openCodec(tag: String): AVCodecContext {
    // Get codec context for the video stream.
    val codecContext = codec!!.pointed
    val codec = avcodec_find_decoder(codecContext.codec_id)?.pointed ?:
        throw Error("Unsupported $tag codec with id ${codecContext.codec_id}...")
    // Open codec.
    if (avcodec_open2(codecContext.ptr, codec.ptr, null) < 0)
        throw Error("Couldn't open $tag codec with id ${codecContext.codec_id}")
    return codecContext
}

class AVFile(private val fileName: String) : DisposableContainer() {
    private val contextPtrPtr = arena.alloc<CPointerVar<AVFormatContext>>().ptr
    private val contextPtr: CPointer<AVFormatContext>

    init {
        avformat_open_input(contextPtrPtr, fileName, null, null).checkAVError()
        contextPtr = contextPtrPtr.pointed.value ?: throw Error("Failed to open AV file")
        tryConstruct {
            if (avformat_find_stream_info(contextPtr, null) < 0)
                throw Error("Couldn't find stream information")
        }
    }

    override fun dispose() {
        avformat_close_input(contextPtrPtr)
        super.dispose()
    }

    fun dumpFormat() = av_dump_format(contextPtr, 0, fileName, 0)

    val context get() = contextPtr.pointed
}

class VideoDecoder(
    private val videoCodecContext: AVCodecContext,
    private val output: OutputInfo
) : DisposableContainer() {
    private val videoSize = Vec2D(videoCodecContext.width, videoCodecContext.height)
    private val windowSize = output.windowSize
    private val videoFrame: AVFrame =
        disposable("av_frame_alloc", ::av_frame_alloc, ::av_frame_unref).pointed
    private val scaledVideoFrame: AVFrame =
        disposable("av_frame_alloc", ::av_frame_alloc, ::av_frame_unref).pointed
    private val softwareScalingContext: CPointer<SwsContext> = disposable(
        message = "sws_getContext",
        create = {
            sws_getContext(
                videoSize.x, videoSize.y,
                videoCodecContext.pix_fmt,
                windowSize.x, windowSize.y, output.pixelFormat,
                SWS_BILINEAR, null, null, null)
        },
        dispose = ::sws_freeContext
    )
    private val scaledFrameSize = avpicture_get_size(output.pixelFormat, output.windowSize.x, output.windowSize.y)
    private val buffer: ByteArray = ByteArray(scaledFrameSize)

    private val videoQueue = Queue<VideoFrame>(100)

    private val minVideoFrames = 5

    init {
        avpicture_fill(scaledVideoFrame.ptr.reinterpret(), buffer.refTo(0),
            output.pixelFormat, output.windowSize.x, output.windowSize.y)
    }

    override fun dispose() {
        super.dispose()
        while (!videoQueue.isEmpty()) videoQueue.pop().unref()
    }

    fun isQueueEmpty() = videoQueue.isEmpty()
    fun isQueueAlmostFull() = videoQueue.size() > videoQueue.maxSize - 5
    fun needMoreFrames() = videoQueue.size() < minVideoFrames
    fun nextFrame() = videoQueue.popOrNull()

    fun decodeVideoPacket(packet: AVPacket, frameFinished: IntVar) {
        // Decode video frame.
        avcodec_decode_video2(videoCodecContext.ptr, videoFrame.ptr, frameFinished.ptr, packet.ptr)
        // Did we get a video frame?
        if (frameFinished.value != 0) {
            // Convert the frame from its movie format to window pixel format.
            sws_scale(softwareScalingContext, videoFrame.data,
                videoFrame.linesize, 0, videoSize.y,
                scaledVideoFrame.data, scaledVideoFrame.linesize)
            // TODO: reuse buffers!
            val buffer = av_buffer_alloc(scaledFrameSize)!!
            val ts = av_frame_get_best_effort_timestamp(videoFrame.ptr) *
                av_q2d(videoCodecContext.time_base.readValue())
            memcpy(buffer.pointed.data, scaledVideoFrame.data[0], scaledFrameSize.signExtend())
            videoQueue.push(VideoFrame(buffer, scaledVideoFrame.linesize[0], ts))
        }
    }

}

class AudioDecoder(
    private val audioCodecContext: AVCodecContext
): DisposableContainer() {
    private val audioFrame: AVFrame =
        disposable(create = ::av_frame_alloc, dispose = ::av_frame_unref).pointed
    private val resampledAudioFrame: AVFrame =
        disposable(create = ::av_frame_alloc, dispose = ::av_frame_unref).pointed
    private val resampleContext: CPointer<AVAudioResampleContext> =
        disposable(create = ::avresample_alloc_context, dispose = ::avresample_free2)

    private val audioQueue = Queue<AudioFrame>(100)

    private val minAudioFrames = 2
    private val maxAudioFrames = 5

    init {
        with (resampledAudioFrame) {
            format = AV_SAMPLE_FMT_S16
            channels = 2
            channel_layout = AV_CH_LAYOUT_STEREO.signExtend()
            sample_rate = 44100
        }
        with (audioCodecContext) {
            setResampleOpt("in_channel_layout", channel_layout.narrow())
            setResampleOpt("out_channel_layout", AV_CH_LAYOUT_STEREO)
            setResampleOpt("in_sample_rate", sample_rate)
            setResampleOpt("out_sample_rate", 44100)
            setResampleOpt("in_sample_fmt", sample_fmt)
            setResampleOpt("out_sample_fmt", AV_SAMPLE_FMT_S16)
        }
        avresample_open(resampleContext)
    }

    private fun setResampleOpt(name: String, value: Int) =
        av_opt_set_int(resampleContext, name, value.signExtend(), 0)

    override fun dispose() {
        super.dispose()
        while (!audioQueue.isEmpty()) audioQueue.pop().unref()
    }

    fun isSynced(): Boolean = audioQueue.size() < maxAudioFrames

    fun isQueueEmpty() = audioQueue.isEmpty()
    fun isQueueAlmostFull() = audioQueue.size() > audioQueue.maxSize - 20
    fun needMoreFrames() = audioQueue.size() < minAudioFrames

    fun nextFrame(size: Int): AudioFrame? {
        val frame = audioQueue.peek() ?: return null
        val realSize = if (frame.position + size > frame.size) frame.size - frame.position else size
        if (frame.position + realSize == frame.size) {
            return audioQueue.pop()
        } else {
            val result = AudioFrame(av_buffer_ref(frame.buffer)!!, frame.position, frame.size, frame.timeStamp)
            frame.position += realSize
            return result
        }
    }

    fun decodeAudioPacket(packet: AVPacket, frameFinished: IntVar) {
        while (packet.size > 0) {
            val size = avcodec_decode_audio4(audioCodecContext.ptr, audioFrame.ptr, frameFinished.ptr, packet.ptr)
            if (frameFinished.value != 0) {
                // Put audio frame to decoder's queue.
                avresample_convert_frame(resampleContext, resampledAudioFrame.ptr, audioFrame.ptr).checkAVError()
                with (resampledAudioFrame) {
                    val audioFrameSize = av_samples_get_buffer_size(null, channels, nb_samples, format, 1)
                    val buffer = av_buffer_alloc(audioFrameSize)!!
                    val ts = av_frame_get_best_effort_timestamp(audioFrame.ptr) *
                        av_q2d(audioCodecContext.time_base.readValue())
                    memcpy(buffer.pointed.data, data[0], audioFrameSize.signExtend())
                    audioQueue.push(AudioFrame(buffer, 0, audioFrameSize, ts))
                }
            }
            packet.size -= size
            packet.data += size
        }
    }
}

class Decoder(
    private val formatContext: CPointer<AVFormatContext>,
    private val videoStreamIndex: Int,
    private val audioStreamIndex: Int,
    private val videoCodecContext: AVCodecContext?,
    private val audioCodecContext: AVCodecContext?
) {
    private var video: VideoDecoder? = null
    private var audio: AudioDecoder? = null

    var noMoreFrames = false

    fun start(output: OutputInfo) {
        video = videoCodecContext?.let { VideoDecoder(it, output) }
        audio = audioCodecContext?.let { AudioDecoder(it) }
        noMoreFrames = false
        decodeIfNeeded()
    }

    fun done() = noMoreFrames && (video?.isQueueEmpty() ?: true) && (audio?.isQueueEmpty() ?: true)

    fun dispose() {
        video?.dispose()
        audio?.dispose()
    }

    private fun needMoreFrames(): Boolean =
        (video?.needMoreFrames() ?: false) || (audio?.needMoreFrames() ?: false)

    fun decodeIfNeeded() {
        if (!needMoreFrames()) return
        if (video?.isQueueAlmostFull() == true) return
        if (audio?.isQueueAlmostFull() == true) return
        memScoped {
            val packet = alloc<AVPacket>()
            val frameFinished = alloc<IntVar>()
            while (needMoreFrames() && av_read_frame(formatContext, packet.ptr) >= 0) {
                when (packet.stream_index) {
                    videoStreamIndex -> video?.decodeVideoPacket(packet, frameFinished)
                    audioStreamIndex -> audio?.decodeAudioPacket(packet, frameFinished)
                }
                av_packet_unref(packet.ptr)
            }
            if (needMoreFrames()) noMoreFrames = true
        }
    }

    fun nextVideoFrame(): VideoFrame? {
        decodeIfNeeded()
        return video?.nextFrame()
    }

    fun nextAudioFrame(size: Int): AudioFrame? {
        decodeIfNeeded()
        return audio?.nextFrame(size)
    }

    fun audioVideoSynced() = (audio?.isSynced() ?: true) || done()
}

class DecoderWorker : Disposable {
    // This class must have no other state, but this worker object.
    // All the real state must be stored on the worker's side.
    private val worker: Worker

    constructor() { worker = konan.worker.startWorker() }
    constructor(id: WorkerId) { worker = Worker(id) }

    override fun dispose() {
        worker.requestTermination().result()
    }
    
    val workerId get() = worker.id

    fun initDecode(context: AVFormatContext, useVideo: Boolean = true, useAudio: Boolean = true): CodecInfo {
        // Find the first video/audio streams.
        val videoStreamIndex =
            if (useVideo) context.codecs.indexOfFirst { it?.codec_type == AVMEDIA_TYPE_VIDEO } else -1
        val audioStreamIndex =
            if (useAudio) context.codecs.indexOfFirst { it?.codec_type == AVMEDIA_TYPE_AUDIO } else -1

        val videoStream = context.streamAt(videoStreamIndex)
        val audioStream = context.streamAt(audioStreamIndex)

        val videoContext = videoStream?.openCodec("video")
        val audioContext = audioStream?.openCodec("audio")

        // Extract video info.
        val video = videoContext?.run {
            VideoInfo(Vec2D(width, height), av_q2d(av_stream_get_r_frame_rate(videoStream.ptr)))
        }
        // Extract audio info.
        val audio = audioContext?.run {
            AudioInfo(sample_rate, channels)
        }

        // Pack all state and pass it to the worker.
        worker.schedule(TransferMode.CHECKED, {
                Decoder(context.ptr,
                    videoStreamIndex, audioStreamIndex,
                    videoContext, audioContext)
            }) { decoder = it }
        return CodecInfo(video, audio)
    }

    fun start(videoSize: Vec2D, pixelFormat: PixelFormat) {
        worker.schedule(TransferMode.CHECKED,
            { OutputInfo(videoSize.copy(), renderPixelFormat(pixelFormat)) }) { decoder?.start(it) }
    }

    fun stop() {
        worker.schedule(TransferMode.CHECKED, { null }) {
            decoder?.run {
                dispose()
                decoder = null
            }
        }.result()
    }

    fun done(): Boolean =
        worker.schedule(TransferMode.CHECKED, { null }) { decoder?.done() }.consume { it != false }

    fun requestDecodeChunk() {
        worker.schedule(TransferMode.CHECKED, { null }) { decoder?.decodeIfNeeded() }.result()
    }

    fun nextVideoFrame(): VideoFrame? =
        worker.schedule(TransferMode.CHECKED, { null }) { decoder?.nextVideoFrame() }.result()

    // TODO: we manually box returned primitive value,
    // fix by autoboxing schedule()'s result in the compiler.

    fun nextAudioFrame(size: Int): AudioFrame? =
        worker.schedule(TransferMode.CHECKED, { size as Int? }) { decoder?.nextAudioFrame(it!!) }.result()

    fun audioVideoSynced(): Boolean =
        worker.schedule(TransferMode.CHECKED, {null }) { decoder?.audioVideoSynced() }.consume { it != false }
}
