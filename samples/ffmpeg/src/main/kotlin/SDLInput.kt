import kotlinx.cinterop.*
import sdl.*

class SDLInput(val player: VideoPlayer) {
    fun check() = memScoped {
        val event = alloc<SDL_Event>()
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