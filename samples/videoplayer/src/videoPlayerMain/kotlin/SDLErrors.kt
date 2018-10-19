package sample.videoplayer

import sdl.SDL_GetError
import kotlinx.cinterop.*

fun throwSDLError(name: String): Nothing =
    throw Error("SDL_$name Error: ${SDL_GetError()!!.toKString()}")

fun checkSDLError(name: String, result: Int) {
    if (result != 0) throwSDLError(name)
}
