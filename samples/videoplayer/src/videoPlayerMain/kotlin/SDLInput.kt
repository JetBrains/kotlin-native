package sample.videoplayer

import kotlinx.cinterop.*
import sdl.*

class SDLInput(val player: VideoPlayer) : DisposableContainer() {
    private val event = arena.alloc<SDL_Event>().ptr

    fun check() {
        while (SDL_PollEvent(event.reinterpret()) != 0) {
            when (event.pointed.type) {
                SDL_QUIT -> player.stop()
                SDL_KEYDOWN -> {
                    val keyboardEvent = event.reinterpret<SDL_KeyboardEvent>().pointed
                    when (keyboardEvent.keysym.scancode) {
                        SDL_SCANCODE_ESCAPE -> player.stop()
                        SDL_SCANCODE_SPACE -> player.pause()
                    }
                }
            }
        }
    }
}