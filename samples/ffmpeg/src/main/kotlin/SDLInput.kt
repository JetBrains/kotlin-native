import kotlinx.cinterop.*
import sdl.*

class SDLInput(val player: VideoPlayer) {
    // TODO: leaks!
    val event = nativeHeap.alloc<SDL_Event>()
    fun check() {
        while (SDL_PollEvent(event.ptr.reinterpret()) != 0) {
            val eventType = event.type
            when (eventType) {
                SDL_QUIT -> player.stop()
                SDL_KEYDOWN -> {
                    val keyboardEvent = event.ptr.reinterpret<SDL_KeyboardEvent>().pointed
                    when (keyboardEvent.keysym.scancode) {
                        SDL_SCANCODE_ESCAPE -> player.stop()
                        SDL_SCANCODE_SPACE -> player.pause()
                    }
                }
            }
        }
    }
}