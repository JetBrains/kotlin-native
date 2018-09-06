import kotlinx.cinterop.*
import platform.windows.*

fun main(args: Array<String>) {
    val icon = memScoped {
      val buffer = alloc<UShortVar>(256)
      GetModuleFileNameW(null, buffer.ptr, 256)
      val dir = buffer.toKString()
      LoadImageW(null, "$dir\\..\\..\\..\\icon.bmp".wcstr.ptr, IMAGE_BITMAP, 128, 128, LR_LOADFROMFILE)
    }
    if (icon != null) {
      SetClassLongPtrW(GetActiveWindow(), GCLP_HICON, icon.toLong())
    }
    MessageBoxW(null, "Konan говорит:\nЗДРАВСТВУЙ МИР!\nИконка $icon",
            "Заголовок окна", (MB_YESNOCANCEL or MB_ICONQUESTION).convert())
}
