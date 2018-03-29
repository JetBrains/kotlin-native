import kotlinx.wasm.jsinterop.*
import org.w3c.dom.*

fun main(args: Array<String>) {

    val canvas = document.getElementById("myCanvas")!!.asHTMLCanvasElement
    val ctx = canvas.getContext("2d")!!.asCanvasRenderingContext2D
    val rect = canvas.getBoundingClientRect()
    val rectLeft = rect.left
    val rectTop = rect.top

    var mouseX: Double = 0.0
    var mouseY: Double = 0.0
    var draw: Boolean = false

    document.setter("onmousedown") {
        draw = true
    }

    document.setter("onmouseup") {
        draw = false
    }

    document.setter("onmousemove") { 
        arguments: ArrayList<JsValue> ->

        val event = arguments[0].asMouseEvent
        mouseX = event.clientX - rectLeft
        mouseY = event.clientY - rectTop

        if (mouseX < 0.0) mouseX = 0.0
        if (mouseX > 639.0) mouseX = 639.0
        if (mouseY < 0.0) mouseY = 0.0
        if (mouseY > 479.0) mouseY = 479.0
    }

    window.setInterval({ _: ArrayList<JsValue> ->
        if (draw) {
            // TODO: properly support union types to eliminate 
            // explicit boxing here.
            // Or implement overloaded setters in the frontend?
            ctx.strokeStyle = "#222222".box 
            ctx.lineTo(mouseX, mouseY) 
            ctx.stroke()
        } else {
            ctx.moveTo(mouseX, mouseY)
            ctx.stroke()
        }
    }.wrapFunction.asTimerHandler , 10) // TODO: properly support union types
}

