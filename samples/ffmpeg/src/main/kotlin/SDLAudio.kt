import kotlinx.cinterop.*
import sdl.*
import ffmpeg.*

fun fillBufferFromQueue(packetQueue: CPointer<PacketQueue>, buffer: CPointer<Uint8Var>?, length: Int) {
    println("fill up to $length in packetQueue")
    SDL_LockMutex(packetQueue.pointed.mutex)
    val next = packetQueue.pointed.tail
    if (next == null) {
        println("Underrun!")
    } else {
        val copied = min(length, next.pointed.length - next.pointed.position)
        platform.posix.memcpy(buffer, next.pointed.data + next.pointed.position, copied.signExtend())
        if (copied != next.pointed.length - next.pointed.position) {
            next.pointed.position += copied
        } else {
            packetQueue.pointed.tail = next.pointed.prev
        }
        platform.posix.memset(buffer, copied, (length - copied).signExtend())
    }
    SDL_UnlockMutex(packetQueue.pointed.mutex)
}

class SDLAudio(val player: VideoPlayer) {
    private fun get_SDL_Error() = SDL_GetError()!!.toKString()

    var packetQueue: CPointer<PacketQueue>? = null

    fun init() {
        if (packetQueue == null) {
            packetQueue = SDL_calloc(PacketQueue.size, 1)!!.reinterpret()
            packetQueue!!.pointed.mutex = SDL_CreateMutex()
        }
    }

    fun deinit() {
        if (packetQueue != null) {
            SDL_DestroyMutex(packetQueue!!.pointed.mutex)
            SDL_free(packetQueue)
            packetQueue = null
        }
    }

    fun startPlayback(sampleRate: Int, channels: Int) {
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
                val packetQueueInner = userdata!!.reinterpret<PacketQueue>()
                fillBufferFromQueue(packetQueueInner, buffer, length)
            }
            spec.userdata = packetQueue
            val realSpec = alloc<SDL_AudioSpec>()
            if (SDL_OpenAudio(spec.ptr, realSpec.ptr) < 0)
                throw Error("SDL_OpenAudio: ${get_SDL_Error()}")
            // TODO: ensure real spec matches what we asked for.
            SDL_PauseAudio(0)
        }
    }

    fun nextFrame(buffer: CPointer<Uint8Var>?, length: Int) {
        // Put frame to audio thread's queue.
        println("Next audio: $length")
        val next = SDL_calloc(QueueElement.size, 1)!!.reinterpret<QueueElement>()
        next.pointed.data = SDL_calloc(length.signExtend(), 1)!!.reinterpret<ByteVar>()
        platform.posix.memcpy(next.pointed.data, buffer, length.signExtend())
        next.pointed.length = length

        val queue = packetQueue!!.pointed
        SDL_LockMutex(queue.mutex)
        if (queue.head != null) {
            queue.head!!.pointed.prev = next
        }
        next.pointed.next = queue.head
        queue.head = next
        if (queue.tail == null) {
            queue.tail = next
        }
        SDL_UnlockMutex(queue.mutex)
    }

    fun stopPlayback() {
    }
}