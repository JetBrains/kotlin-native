import kotlinx.cinterop.*
import sdl.*
import ffmpeg.*
import konan.worker.WorkerId

class SDLAudio(val player: VideoPlayer) {
    private fun get_SDL_Error() = SDL_GetError()!!.toKString()

    private var opaqueData: COpaquePointer? = null

    fun init() {}

    fun deinit() {}

    fun start(sampleRate: Int, channels: Int) {
        println("Audio: $channels channels, $sampleRate samples per second")

        memScoped {
            val spec = alloc<SDL_AudioSpec>()
            spec.freq = sampleRate
            spec.format = AUDIO_S16SYS.narrow()
            spec.channels = channels.narrow()
            spec.silence = 0
            spec.samples = 1024
            spec.callback = staticCFunction {
                userdata, buffer, length ->
                // This handler will be invoked in audio thread, so reinit runtime.
                konan.initRuntimeIfNeeded()
                println("fill up to $length from $userdata")
                if (decoder == null) {
                    val callbackData = userdata as? CPointer<IntVar>
                    decoder = DecodeWorker(callbackData!!.pointed.value)
                }
                println("id is ${(userdata as? CPointer<IntVar>)!!.pointed.value}")
                val frame = decoder!!.nextAudioFrame(length)
                if (frame != null) {
                    println("frame of ${frame.size} len=$length")
                    platform.posix.memcpy(buffer, frame.buffer.pointed.data + frame.position, frame.size.signExtend())
                    frame.unref()
                } else {
                    platform.posix.memset(buffer, 0, length.signExtend())
                }
            }
            // TODO: userData leaks this way!
            val userData = nativeHeap.alloc<IntVar>()
            userData.value = player.decoder.workerId()
            opaqueData = userData.ptr
            spec.userdata = opaqueData
            val realSpec = alloc<SDL_AudioSpec>()
            if (SDL_OpenAudio(spec.ptr, realSpec.ptr) < 0)
                throw Error("SDL_OpenAudio: ${get_SDL_Error()}")
            // TODO: ensure real spec matches what we asked for.

            resume()
        }
    }

    fun pause() {
        SDL_PauseAudio(1)
    }

    fun resume() {
        SDL_PauseAudio(0)
    }

    fun stop() {
        pause()
        SDL_CloseAudio()
        if (opaqueData != null) {
            nativeHeap.free(opaqueData!!)
            opaqueData = null
        }
    }
}

// This global is only set in audio thread.
var decoder: DecodeWorker? = null