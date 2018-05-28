package org.jetbrains.kotlin.native.interop.gen.wasm.idl

// This shall be an output of Web IDL parser.
val idlDom = listOf(
    Interface("Context",
        Attribute("lineWidth", idlInt),
        Attribute("fillStyle", idlString),
        Attribute("strokeStyle", idlString),

        Operation("lineTo", idlVoid, Arg("x", idlInt), Arg("y", idlInt)),
        Operation("moveTo", idlVoid, Arg("x", idlInt), Arg("y", idlInt)),
        Operation("beginPath", idlVoid),
        Operation("stroke", idlVoid),
        Operation("fillRect", idlVoid, Arg("x", idlInt), Arg("y", idlInt), Arg("width", idlInt), Arg("height", idlInt)),
        Operation("fillText", idlVoid, Arg("test", idlString), Arg("x", idlInt),  Arg("y", idlInt), Arg("maxWidth", idlInt)),
        Operation("fill", idlVoid),
        Operation("closePath", idlVoid),
        Operation("rect", idlVoid, Arg("x", idlInt), Arg("y", idlInt), Arg("width", idlInt), Arg("height", idlInt)),
        Operation("translate", idlVoid, Arg("x", idlInt), Arg("y", idlInt)),
        Operation("setTransform", idlVoid, Arg("a", idlInt), Arg("b", idlInt), Arg("c", idlInt), Arg("d", idlInt), Arg("e", idlInt), Arg("f", idlInt)),
        Operation("save", idlVoid),
        Operation("restore", idlVoid),
        Operation("measureText", idlInterfaceRef("TextMetrics"), Arg("text", idlString))
   ),
    Interface("DOMRect",
        Attribute("left", idlInt),
        Attribute("right", idlInt),
        Attribute("top", idlInt),
        Attribute("bottom", idlInt)
    ),
    Interface("Canvas",
        Operation("getContext", idlInterfaceRef("Context"), Arg("context", idlString)),
        Operation("getBoundingClientRect", idlInterfaceRef("DOMRect"))
    ),
    Interface("Document",
        Operation("getElementById", idlObject, Arg("id", idlString)),
        Operation("createElement", idlObject, Arg("tagName", idlString))
    ),
    Interface("HTMLImage",
        Attribute("src", idlString)
    ),
    Interface("MouseEvent",
        Attribute("clientX", idlInt, readOnly = true),
        Attribute("clientY", idlInt, readOnly = true)
    ),
    Interface("KeyboardEvent",
        Attribute("key", idlString, readOnly = true)
    ),
    Interface("TextMetrics",
        Attribute("width", idlDouble, readOnly = true)
    ),
    Interface("Response",
        Operation("json", idlObject)
    ),
    Interface("Promise",
        Operation("then", idlInterfaceRef("Promise"), Arg("lambda", idlFunction))
    ),
    Interface("__Global",
        Attribute("document", idlInterfaceRef("Document"), readOnly = true),

        Operation("fetch", idlInterfaceRef("Promise"), Arg("url", idlString)),
        Operation("setInterval", idlInt, Arg("lambda", idlFunction), Arg("interval", idlInt)),
        Operation("clearInterval", idlVoid, Arg("intervalID", idlInt))
    )
)

   
