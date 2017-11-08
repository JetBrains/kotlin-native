import kotlinx.cinterop.*
import sdl.*
import ffmpeg.*

fun fillBufferFromQueue(packetQueue: CPointer<PacketQueue>, buffer: CPointer<Uint8Var>?, length: Int) {
    println("fill up to $length from packetQueue")
    SDL_LockMutex(packetQueue.pointed.mutex)
    while (packetQueue.pointed.tail == null) {
        SDL_CondWait(packetQueue.pointed.cond, packetQueue.pointed.mutex)
    }
    val next = packetQueue.pointed.tail
    if (next == null) {
        // TODO: use that as exit condition?
        platform.posix.memset(buffer, 0, length.signExtend())
    } else {
        println("get ${next.pointed.id}")
        val dataLength = next.pointed.length
        val copied = min(length, dataLength - next.pointed.position)
        platform.posix.memcpy(buffer, next.pointed.data + next.pointed.position, copied.signExtend())
        if (copied + next.pointed.position < dataLength) {
            next.pointed.position += copied
        } else {
            packetQueue.pointed.tail = next.pointed.prev
            if (packetQueue.pointed.tail != null)
                packetQueue.pointed.tail!!.pointed.next = null
            SDL_free(next.pointed.data)
            SDL_free(next)
        }
        println("Request $copied of $length buffer has $dataLength")
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
            packetQueue!!.pointed.cond = SDL_CreateCond()
        }
    }

    fun deinit() {
        if (packetQueue != null) {
            SDL_DestroyCond(packetQueue!!.pointed.cond)
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

    var id = 0

    fun nextFrame(buffer: CPointer<Uint8Var>?, length: Int) {
        // Put frame to audio thread's queue.
        println("Next audio: $length $id")
        val next = SDL_calloc(QueueElement.size, 1)!!.reinterpret<QueueElement>()
        next.pointed.data = SDL_calloc(length.signExtend(), 1)!!.reinterpret<ByteVar>()
        platform.posix.memcpy(next.pointed.data, buffer, length.signExtend())
        next.pointed.length = length
        next.pointed.id = id++

        val queue = packetQueue!!.pointed
        SDL_LockMutex(queue.mutex)
        if (queue.head != null) {
            queue.head!!.pointed.prev = next
        }
        next.pointed.next = queue.head
        queue.head = next
        if (queue.tail == null) {
            queue.tail = queue.head
        }
        SDL_UnlockMutex(queue.mutex)

        SDL_CondSignal(queue.cond)
    }

    fun stopPlayback() {
    }
}