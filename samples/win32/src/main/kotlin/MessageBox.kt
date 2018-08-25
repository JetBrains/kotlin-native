/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlinx.cinterop.*
import platform.windows.*

fun main(args: Array<String>) {
    MessageBoxW(null, "Konan говорит:\nЗДРАВСТВУЙ МИР!\n",
            "Заголовок окна", (MB_YESNOCANCEL or MB_ICONQUESTION).convert())
}
