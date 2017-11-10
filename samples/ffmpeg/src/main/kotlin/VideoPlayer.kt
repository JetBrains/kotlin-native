import ffmpeg.*
import kotlin.system.*
import kotlinx.cinterop.*
import platform.posix.*

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
    val input = SDLInput(this)

    var decoder = DecodeWorker()
    var state: State = State.STOPPED
    var hasAudio = false
    var hasVideo = false

    fun stop() {
        state = State.STOPPED
    }

    fun pause() {
        when (state) {
            State.PAUSED -> {
                state = State.PLAYING
                audio.resume()
            }
            State.PLAYING -> {
                state = State.PAUSED
                audio.pause()
            }
            State.STOPPED -> throw Error("Cannot pause in stopped state")
        }
    }


    private fun getTime(): Double =
        memScoped {
            val now = alloc<timespec>()
            clock_gettime(CLOCK_MONOTONIC, now.ptr)
            now.tv_sec + now.tv_nsec / 1_000_000_000.0
        }

    fun playFile(file: String) {
        println("playFile $file")

        decoder.init()
        video.init()
        audio.init()

        try {
            val info = decoder.initDecode(file)
            val windowWidth = info.width
            val windowHeight = info.height
            hasVideo = windowWidth > 0
            hasAudio = info.sampleRate != 0
            if (hasVideo)
                video.start(windowWidth, windowHeight)
            decoder.start(windowWidth, windowHeight, video.pixelFormat())
            if (hasAudio)
                audio.start(info.sampleRate, info.channels)
            var lastTimeStamp = getTime()
            state = State.PLAYING
            while (state != State.STOPPED) {
                if (hasVideo) {
                    val frame = decoder.nextVideoFrame()
                    if (frame == null) {
                        state = State.STOPPED
                        continue
                    }
                    println("next frame ${frame.buffer}")
                    video.nextFrame(frame.buffer.pointed.data!!, frame.lineSize)
                    frame.unref()
                }
                // Audio is being auto-fetched by audio thread.

                // Check if there are any input.
                input.check()

                // Pause support.
                while (state == State.PAUSED) {
                    input.check()
                    usleep(5 * 1000)
                }

                // Interframe pause, may lead to broken A/V sync, think of better approach.
                if (state == State.PLAYING) {
                    if (hasVideo) {
                        val now = getTime()
                        val delta = now - lastTimeStamp
                        if (delta < 1.0 / info.fps) {
                            usleep(1000 * 1000 * (1.0 / info.fps - delta).toInt())
                        }
                        lastTimeStamp = now
                    } else {
                        // For pure sound, playback is driven by demand.
                        usleep(10 * 1000)
                    }
                }
                println("state=$state")
            }
            if (hasAudio)
                audio.stop()
            if (hasVideo)
                video.stop()
            decoder.stop()
        } finally {
            audio.deinit()
            video.deinit()
            decoder.deinit()
        }

        /*

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
                    //av_packet_unref(packet.ptr)
                }
                video.stopPlayback()
                av_frame_free(videoFrame.ptr)
                av_frame_free(audioFrame.ptr)
            }
        } finally {
            audio.deinit()
            video.deinit()
        } */

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