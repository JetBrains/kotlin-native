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

class VideoPlayer(val requestedWidth: Int, val requestedHeight: Int) {
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
            val now = alloc<platform.posix.timespec>()
            clock_gettime(platform.posix.CLOCK_MONOTONIC, now.ptr)
            now.tv_sec + now.tv_nsec / 1_000_000_000.0
        }

    fun playFile(file: String) {
        println("playFile $file")

        decoder.init()
        video.init()
        audio.init()

        try {
            val info = decoder.initDecode(file, true, true)
            val windowWidth = if (requestedWidth == 0) { if (info.width < 0) 400 else info.width } else requestedWidth
            val windowHeight = if (requestedHeight == 0) { if (info.height < 0) 200 else info.height } else requestedHeight
            hasVideo = info.width > 0
            hasAudio = info.sampleRate != 0
            if (hasVideo)
                video.start(windowWidth, windowHeight)
            decoder.start(windowWidth, windowHeight, video.pixelFormat())
            if (hasAudio)
                audio.start(info.sampleRate, info.channels)
            var lastTimeStamp = getTime()
            state = State.PLAYING
            decoder.decodeChunk().consume { _ -> }
            while (state != State.STOPPED) {
                if (hasVideo) {
                    val frame = decoder.nextVideoFrame()
                    if (frame == null) {
                        state = State.STOPPED
                        continue
                    }
                    video.nextFrame(frame.buffer.pointed.data!!, frame.lineSize)
                    frame.unref()
                }
                // Audio is being auto-fetched by audio thread.

                // Check if there are any input.
                input.check()

                // Pause support.
                while (state == State.PAUSED) {
                    if (hasAudio) audio.pause()
                    input.check()
                    usleep(1 * 1000)
                }
                if (hasAudio) audio.resume()

                // Interframe pause, may lead to broken A/V sync, think of better approach.
                if (state == State.PLAYING) {
                    if (hasVideo) {
                       // Use FPS for A/V sync.
                       val now = getTime()
                       val delta = now - lastTimeStamp
                       if (delta < 1.0 / info.fps) {
                           usleep((1000 * 1000 * (1.0 / info.fps - delta)).toInt())
                       }
                       while (hasAudio && !decoder.audioVideoSynced()) {
                           usleep(10)
                       }
                       lastTimeStamp = now
                    } else {
                        // For pure sound, playback is driven by demand.
                        usleep(10 * 1000)
                        if (decoder.done()) {
                           state = State.STOPPED
                        }
                    }
                }
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
    }
}

fun main(args: Array<String>) {
    if (args.size < 1) {
        println("usage: koplayer file.ext <width> <height>")
        exitProcess(1)
    }

    av_register_all()

    val width = if (args.size < 3) 0 else args[1].toInt()
    val height = if (args.size < 3) 0 else args[2].toInt()
    val player = VideoPlayer(width, height)
    player.playFile(args[0])
}
