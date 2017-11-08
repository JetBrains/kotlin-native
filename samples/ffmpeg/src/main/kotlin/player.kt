import ffmpeg.*
import kotlin.system.*
import kotlinx.cinterop.*

enum class State {
    PLAYING,
    STOPPED,
    PAUSED
}

enum class PixelFormat {
    INVALID,
    RGB24,
    ARGB32
}

class VideoPlayer {
    val video = SDLVideo(this)
    val audio = SDLAudio(this)

    var state: State = State.STOPPED

    fun stop() {
        state = State.STOPPED
    }

    fun pause() {
        when (state) {
            State.PAUSED -> state = State.PLAYING
            State.PLAYING -> state = State.PAUSED
            State.STOPPED -> throw Error("Cannot pause in stopped state")
        }
    }

    fun renderPixelFormat() = when (video.pixelFormat()) {
            PixelFormat.RGB24 -> AV_PIX_FMT_RGB24
            PixelFormat.ARGB32 -> AV_PIX_FMT_RGB32
            else -> TODO()
        }

    fun playFile(file: String) {
        println("playFile $file")
        video.init()
        audio.init()
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

                // Init playback.

                // Allocate frames.
                val videoFrame = av_frame_alloc()!!
                val audioFrame = av_frame_alloc()!!
                val frameRGB = av_frame_alloc()!!
                val videoWidth = videoCodecContext.pointed.width
                val videoHeight = videoCodecContext.pointed.height
                val fps = av_q2d(av_stream_get_r_frame_rate(videoStream))

                val packet = alloc<AVPacket>()
                val frameFinished = alloc<IntVar>()

                video.startPlayback(videoWidth, videoHeight, fps)

                // Allocate audio stream.
                val sampleRate = audioCodecContext.pointed.sample_rate
                val channels = audioCodecContext.pointed.channels
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
                    av_packet_unref(packet.ptr)
                }
                video.stopPlayback()
            }
        } finally {
            audio.deinit()
            video.deinit()
        }
    }
}

fun main(args: Array<String>) {
    if (args.size != 1) {
        println("usage: koplayer file.ext")
        exitProcess(1)
    }

    av_register_all()

    val player = VideoPlayer()
    player.playFile(args[0])
}