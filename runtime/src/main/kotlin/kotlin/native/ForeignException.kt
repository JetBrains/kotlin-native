/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package kotlin.native

import kotlin.RuntimeException
import kotlin.native.internal.NativePtr

public class ForeignException internal constructor(val payload: NativePtr)  : RuntimeException()
