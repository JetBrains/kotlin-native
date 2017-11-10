import kotlinx.cinterop.*
import sdl.*

class SDLVideo(val player: VideoPlayer) {
    var displayWidth = 0
    var displayHeight = 0
    var fps: Double = 0.0
    private var window: CPointer<SDL_Window>? = null
    private var renderer: CPointer<SDL_Renderer>? = null
    private var surface: CPointer<SDL_Surface>? = null
    private var texture: CPointer<SDL_Texture>? = null

    fun init() {
        if (SDL_Init(SDL_INIT_EVERYTHING) != 0) {
            println("SDL_Init Error: ${get_SDL_Error()}")
            throw Error()
        }

        memScoped {
            val displayMode = alloc<SDL_DisplayMode>()
            if (SDL_GetCurrentDisplayMode(0, displayMode.ptr.reinterpret()) != 0) {
                println("SDL_GetCurrentDisplayMode Error: ${get_SDL_Error()}")
                SDL_Quit()
                throw Error()
            }
            displayWidth = displayMode.w
            displayHeight = displayMode.h
        }
    }

    fun deinit() {
        stop()
        SDL_Quit()
    }

    private fun get_SDL_Error() = SDL_GetError()!!.toKString()

    var windowWidth = 0
    var windowHeight = 0
    var rect: CPointer<SDL_Rect>? = null

    fun start(videoWidth: Int, videoHeight: Int) {
        // To free resources from previous playbacks.
        stop()

        windowWidth = videoWidth
        windowHeight = videoHeight

        // Use arena instead!
        val rect = SDL_calloc(1, SDL_Rect.size)!!.reinterpret<SDL_Rect>()
        rect.pointed.let {
            it.x = 0
            it.y = 0
            it.w = windowWidth
            it.h = windowHeight
        }
        this.rect = rect

        val windowX = (displayWidth - windowWidth) / 2
        val windowY = (displayHeight - windowHeight) / 2

        val window = SDL_CreateWindow("KoPlayer", windowX, windowY, windowWidth, windowHeight, SDL_WINDOW_SHOWN)
        if (window == null) {
            println("SDL_CreateWindow Error: ${get_SDL_Error()}")
            SDL_Quit()
            throw Error()
        }
        this.window = window

        val renderer = SDL_CreateRenderer(window, -1, SDL_RENDERER_ACCELERATED or SDL_RENDERER_PRESENTVSYNC)
        if (renderer == null) {
            SDL_DestroyWindow(window)
            println("SDL_CreateRenderer Error: ${get_SDL_Error()}")
            SDL_Quit()
            throw Error()
        }
        this.renderer = renderer

        this.texture = SDL_CreateTexture(renderer, SDL_GetWindowPixelFormat(window), 0, videoWidth, videoHeight)
    }

    fun pixelFormat(): PixelFormat {
        if (window == null)
            return PixelFormat.INVALID
        return when (SDL_GetWindowPixelFormat(window)) {
            SDL_PIXELFORMAT_RGB24 -> PixelFormat.RGB24
            SDL_PIXELFORMAT_ARGB8888, SDL_PIXELFORMAT_RGB888 -> PixelFormat.ARGB32
            else -> {
                println("Pixel format ${SDL_GetWindowPixelFormat(window)} unknown")
                TODO()
            }
        }
    }

    fun nextFrame(frameData: CPointer<Uint8Var>?, linesize: Int) {
        SDL_UpdateTexture(texture, rect, frameData, linesize)
        SDL_RenderClear(renderer)
        SDL_RenderCopy(renderer, texture, rect, rect)
        SDL_RenderPresent(renderer)
    }

    fun stop() {
        if (renderer != null) {
            SDL_DestroyRenderer(renderer)
            renderer = null
        }
        if (window != null) {
            SDL_DestroyWindow(window)
            window = null
        }
        if (rect != null) {
            SDL_free(rect)
            rect = null
        }
    }
}
