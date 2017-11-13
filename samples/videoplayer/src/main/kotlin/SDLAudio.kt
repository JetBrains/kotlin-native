import kotlinx.cinterop.*
import sdl.*
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
            spec.freq = 44100 // sampleRate
            spec.format = AUDIO_S16SYS.narrow()
            spec.channels = 2.narrow()
            spec.silence = 0
            spec.samples = 4096
            spec.callback = staticCFunction {
                userdata, buffer, length ->
                // This handler will be invoked in the audio thread, so reinit runtime.
                konan.initRuntimeIfNeeded()

                if (decoder == null) {
                    val callbackData = userdata as? CPointer<IntVar>
                    decoder = DecodeWorker(callbackData!!.pointed.value)
                }
                var outPosition = 0
                while (outPosition < length) {
                    val frame = decoder!!.nextAudioFrame(length - outPosition)
                    if (frame != null) {
                       val toCopy = min(length - outPosition, frame.size - frame.position)
                       //println("got audio frame of ${frame.size} len=$length framePos=${frame.position} outPos=$outPosition tc=$toCopy")
                       platform.posix.memcpy(buffer + outPosition, frame.buffer.pointed.data + frame.position, toCopy.signExtend())
                       frame.unref()
                       outPosition += toCopy
                    } else {
                      println("got silence")
                      platform.posix.memset(buffer + outPosition, 0, (length - outPosition).signExtend())
                      break
                    }
                }
            }
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

// This global is only set in the audio thread.
var decoder: DecodeWorker? = null
