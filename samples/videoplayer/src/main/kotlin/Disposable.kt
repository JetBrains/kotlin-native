
import kotlinx.cinterop.*

interface Disposable {
    fun dispose()
}

abstract class DisposableContainer : Disposable {
    val arena = Arena()
    
    override fun dispose() {
        arena.clear()
    }

    // These functions should be used in constructor only.
    // They will properly dispose of partially initialized object on failure

    inline fun <T> tryConstruct(init: () -> T): T =
        try { init() }
        catch (e: Throwable) {
            dispose()
            throw e
        }

    // TODO: It shall be inline, but crashes backend
    fun <T> disposable(
        message: String = "disposable",
        create: () -> T?,
        dispose: (T) -> Unit
    ): T =
        tryConstruct {
            create()?.also {
                arena.defer { dispose(it) }
            } ?: throw Error(message)
        }

    // TODO: It shall be inline, but crashes backend
    fun <T : Disposable> disposable(create: () -> T): T =
        disposable(
            create = create,
            dispose = { it.dispose() })

    fun <T : CPointed> sdlDisposable(message: String, ptr: CPointer<T>?, dispose: (CPointer<T>) -> Unit): CPointer<T> =
        disposable(
            create = { ptr ?: throwSDLError(message) },
            dispose = dispose)
}
