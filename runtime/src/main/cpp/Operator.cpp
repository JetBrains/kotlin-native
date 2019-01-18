/*
 * Copyright 2010-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <math.h>
#include <limits.h>

#include "DoubleConversions.h"
#include "Natives.h"
#include "Exceptions.h"

#include "Common.h"

namespace {

template<typename T> void checkNotZero(T t) {
    if (__builtin_expect(t == 0, false)) {
        ThrowArithmeticException();
    }
}

template<typename R, typename Ta, typename Tb> R div(Ta a, Tb b) {
    checkNotZero(b);
    return a / b;
}

template<typename R, typename Ta, typename Tb> R mod(Ta a, Tb b) {
    checkNotZero(b);
    return a % b;
}

}

extern "C" {

//--- Boolean -----------------------------------------------------------------//

ALWAYS_INLINE KBoolean Kotlin_Boolean_not              (KBoolean a            ) { return     !a; }
ALWAYS_INLINE KBoolean Kotlin_Boolean_and_Boolean      (KBoolean a, KBoolean b) { return a && b; }
ALWAYS_INLINE KBoolean Kotlin_Boolean_or_Boolean       (KBoolean a, KBoolean b) { return a || b; }
ALWAYS_INLINE KBoolean Kotlin_Boolean_xor_Boolean      (KBoolean a, KBoolean b) { return a != b; }
KInt     Kotlin_Boolean_compareTo_Boolean(KBoolean a, KBoolean b) { if (a == b) return 0; return (a < b) ? -1 : 1; }

//--- Char --------------------------------------------------------------------//

KInt    Kotlin_Char_compareTo_Char   (KChar a, KChar   b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
ALWAYS_INLINE KChar   Kotlin_Char_plus_Int         (KChar a, KInt    b) { return a + b; }
ALWAYS_INLINE KInt    Kotlin_Char_minus_Char       (KChar a, KChar   b) { return a - b; }
ALWAYS_INLINE KChar   Kotlin_Char_minus_Int        (KChar a, KInt    b) { return a - b; }
ALWAYS_INLINE KChar   Kotlin_Char_inc              (KChar a           ) { return a + 1; }
ALWAYS_INLINE KChar   Kotlin_Char_dec              (KChar a           ) { return a - 1; }

ALWAYS_INLINE KByte   Kotlin_Char_toByte           (KChar a           ) { return a; }
ALWAYS_INLINE KChar   Kotlin_Char_toChar           (KChar a           ) { return a; }
ALWAYS_INLINE KShort  Kotlin_Char_toShort          (KChar a           ) { return a; }
ALWAYS_INLINE KInt    Kotlin_Char_toInt            (KChar a           ) { return a; }
ALWAYS_INLINE KLong   Kotlin_Char_toLong           (KChar a           ) { return a; }
ALWAYS_INLINE KFloat  Kotlin_Char_toFloat          (KChar a           ) { return a; }
ALWAYS_INLINE KDouble Kotlin_Char_toDouble         (KChar a           ) { return a; }

//--- Byte --------------------------------------------------------------------//

KInt    Kotlin_Byte_compareTo_Byte   (KByte a, KByte   b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Byte_compareTo_Short  (KByte a, KShort  b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Byte_compareTo_Int    (KByte a, KInt    b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Byte_compareTo_Long   (KByte a, KLong   b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Byte_compareTo_Float  (KByte a, KFloat  b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Byte_compareTo_Double (KByte a, KDouble b) { if (a == b) return 0; return (a < b) ? -1 : 1; }

ALWAYS_INLINE KInt    Kotlin_Byte_plus_Byte        (KByte a, KByte   b) { return a + b; }
ALWAYS_INLINE KInt    Kotlin_Byte_plus_Short       (KByte a, KShort  b) { return a + b; }
ALWAYS_INLINE KInt    Kotlin_Byte_plus_Int         (KByte a, KInt    b) { return a + b; }
ALWAYS_INLINE KLong   Kotlin_Byte_plus_Long        (KByte a, KLong   b) { return a + b; }
ALWAYS_INLINE KFloat  Kotlin_Byte_plus_Float       (KByte a, KFloat  b) { return a + b; }
ALWAYS_INLINE KDouble Kotlin_Byte_plus_Double      (KByte a, KDouble b) { return a + b; }

ALWAYS_INLINE KInt    Kotlin_Byte_minus_Byte       (KByte a, KByte   b) { return a - b; }
ALWAYS_INLINE KInt    Kotlin_Byte_minus_Short      (KByte a, KShort  b) { return a - b; }
ALWAYS_INLINE KInt    Kotlin_Byte_minus_Int        (KByte a, KInt    b) { return a - b; }
ALWAYS_INLINE KLong   Kotlin_Byte_minus_Long       (KByte a, KLong   b) { return a - b; }
ALWAYS_INLINE KFloat  Kotlin_Byte_minus_Float      (KByte a, KFloat  b) { return a - b; }
ALWAYS_INLINE KDouble Kotlin_Byte_minus_Double     (KByte a, KDouble b) { return a - b; }

ALWAYS_INLINE KInt    Kotlin_Byte_div_Byte         (KByte a, KByte   b) { return div<KInt>(a, b); }
ALWAYS_INLINE KInt    Kotlin_Byte_div_Short        (KByte a, KShort  b) { return div<KInt>(a, b); }
ALWAYS_INLINE KInt    Kotlin_Byte_div_Int          (KByte a, KInt    b) { return div<KInt>(a, b); }
ALWAYS_INLINE KLong   Kotlin_Byte_div_Long         (KByte a, KLong   b) { return div<KLong>(a, b); }
ALWAYS_INLINE KFloat  Kotlin_Byte_div_Float        (KByte a, KFloat  b) { return a / b; }
ALWAYS_INLINE KDouble Kotlin_Byte_div_Double       (KByte a, KDouble b) { return a / b; }

ALWAYS_INLINE KInt    Kotlin_Byte_mod_Byte         (KByte a, KByte   b) { return mod<KInt>(a, b); }
ALWAYS_INLINE KInt    Kotlin_Byte_mod_Short        (KByte a, KShort  b) { return mod<KInt>(a, b); }
ALWAYS_INLINE KInt    Kotlin_Byte_mod_Int          (KByte a, KInt    b) { return mod<KInt>(a, b); }
ALWAYS_INLINE KLong   Kotlin_Byte_mod_Long         (KByte a, KLong   b) { return mod<KLong>(a, b); }
ALWAYS_INLINE KFloat  Kotlin_Byte_mod_Float        (KByte a, KFloat  b) { return fmodf(a, b); }
ALWAYS_INLINE KDouble Kotlin_Byte_mod_Double       (KByte a, KDouble b) { return fmod (a, b); }

ALWAYS_INLINE KInt    Kotlin_Byte_times_Byte       (KByte a, KByte   b) { return a * b; }
ALWAYS_INLINE KInt    Kotlin_Byte_times_Short      (KByte a, KShort  b) { return a * b; }
ALWAYS_INLINE KInt    Kotlin_Byte_times_Int        (KByte a, KInt    b) { return a * b; }
ALWAYS_INLINE KLong   Kotlin_Byte_times_Long       (KByte a, KLong   b) { return a * b; }
ALWAYS_INLINE KFloat  Kotlin_Byte_times_Float      (KByte a, KFloat  b) { return a * b; }
ALWAYS_INLINE KDouble Kotlin_Byte_times_Double     (KByte a, KDouble b) { return a * b; }

ALWAYS_INLINE KByte   Kotlin_Byte_inc              (KByte a           ) { return ++a; }
ALWAYS_INLINE KByte   Kotlin_Byte_dec              (KByte a           ) { return --a; }
ALWAYS_INLINE KInt    Kotlin_Byte_unaryPlus        (KByte a           ) { return  +a; }
ALWAYS_INLINE KInt    Kotlin_Byte_unaryMinus       (KByte a           ) { return  -a; }

ALWAYS_INLINE KByte   Kotlin_Byte_toByte           (KByte a           ) { return a; }
ALWAYS_INLINE KChar   Kotlin_Byte_toChar           (KByte a           ) { return a; }
ALWAYS_INLINE KShort  Kotlin_Byte_toShort          (KByte a           ) { return a; }
ALWAYS_INLINE KInt    Kotlin_Byte_toInt            (KByte a           ) { return a; }
ALWAYS_INLINE KLong   Kotlin_Byte_toLong           (KByte a           ) { return a; }
ALWAYS_INLINE KFloat  Kotlin_Byte_toFloat          (KByte a           ) { return a; }
ALWAYS_INLINE KDouble Kotlin_Byte_toDouble         (KByte a           ) { return a; }

//--- Short -------------------------------------------------------------------//

KInt    Kotlin_Short_compareTo_Byte   (KShort a, KByte   b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Short_compareTo_Short  (KShort a, KShort  b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Short_compareTo_Int    (KShort a, KInt    b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Short_compareTo_Long   (KShort a, KLong   b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Short_compareTo_Float  (KShort a, KFloat  b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Short_compareTo_Double (KShort a, KDouble b) { if (a == b) return 0; return (a < b) ? -1 : 1; }

ALWAYS_INLINE KInt    Kotlin_Short_plus_Byte        (KShort a, KByte   b) { return a + b; }
ALWAYS_INLINE KInt    Kotlin_Short_plus_Short       (KShort a, KShort  b) { return a + b; }
ALWAYS_INLINE KInt    Kotlin_Short_plus_Int         (KShort a, KInt    b) { return a + b; }
ALWAYS_INLINE KLong   Kotlin_Short_plus_Long        (KShort a, KLong   b) { return a + b; }
ALWAYS_INLINE KFloat  Kotlin_Short_plus_Float       (KShort a, KFloat  b) { return a + b; }
ALWAYS_INLINE KDouble Kotlin_Short_plus_Double      (KShort a, KDouble b) { return a + b; }

ALWAYS_INLINE KInt    Kotlin_Short_minus_Byte       (KShort a, KByte   b) { return a - b; }
ALWAYS_INLINE KInt    Kotlin_Short_minus_Short      (KShort a, KShort  b) { return a - b; }
ALWAYS_INLINE KInt    Kotlin_Short_minus_Int        (KShort a, KInt    b) { return a - b; }
ALWAYS_INLINE KLong   Kotlin_Short_minus_Long       (KShort a, KLong   b) { return a - b; }
ALWAYS_INLINE KFloat  Kotlin_Short_minus_Float      (KShort a, KFloat  b) { return a - b; }
ALWAYS_INLINE KDouble Kotlin_Short_minus_Double     (KShort a, KDouble b) { return a - b; }

ALWAYS_INLINE KInt    Kotlin_Short_div_Byte         (KShort a, KByte   b) { return div<KInt>(a, b); }
ALWAYS_INLINE KInt    Kotlin_Short_div_Short        (KShort a, KShort  b) { return div<KInt>(a, b); }
ALWAYS_INLINE KInt    Kotlin_Short_div_Int          (KShort a, KInt    b) { return div<KInt>(a, b); }
ALWAYS_INLINE KLong   Kotlin_Short_div_Long         (KShort a, KLong   b) { return div<KLong>(a, b); }
ALWAYS_INLINE KFloat  Kotlin_Short_div_Float        (KShort a, KFloat  b) { return a / b; }
ALWAYS_INLINE KDouble Kotlin_Short_div_Double       (KShort a, KDouble b) { return a / b; }

ALWAYS_INLINE KInt    Kotlin_Short_mod_Byte         (KShort a, KByte   b) { return mod<KInt>(a, b); }
ALWAYS_INLINE KInt    Kotlin_Short_mod_Short        (KShort a, KShort  b) { return mod<KInt>(a, b); }
ALWAYS_INLINE KInt    Kotlin_Short_mod_Int          (KShort a, KInt    b) { return mod<KInt>(a, b); }
ALWAYS_INLINE KLong   Kotlin_Short_mod_Long         (KShort a, KLong   b) { return mod<KLong>(a, b); }
ALWAYS_INLINE KFloat  Kotlin_Short_mod_Float        (KShort a, KFloat  b) { return fmodf(a, b); }
ALWAYS_INLINE KDouble Kotlin_Short_mod_Double       (KShort a, KDouble b) { return fmod (a, b); }

ALWAYS_INLINE KInt    Kotlin_Short_times_Byte       (KShort a, KByte   b) { return a * b; }
ALWAYS_INLINE KInt    Kotlin_Short_times_Short      (KShort a, KShort  b) { return a * b; }
ALWAYS_INLINE KInt    Kotlin_Short_times_Int        (KShort a, KInt    b) { return a * b; }
ALWAYS_INLINE KLong   Kotlin_Short_times_Long       (KShort a, KLong   b) { return a * b; }
ALWAYS_INLINE KFloat  Kotlin_Short_times_Float      (KShort a, KFloat  b) { return a * b; }
ALWAYS_INLINE KDouble Kotlin_Short_times_Double     (KShort a, KDouble b) { return a * b; }

ALWAYS_INLINE KShort  Kotlin_Short_inc              (KShort a           ) { return ++a; }
ALWAYS_INLINE KShort  Kotlin_Short_dec              (KShort a           ) { return --a; }
ALWAYS_INLINE KInt    Kotlin_Short_unaryPlus        (KShort a           ) { return  +a; }
ALWAYS_INLINE KInt    Kotlin_Short_unaryMinus       (KShort a           ) { return  -a; }

ALWAYS_INLINE KByte   Kotlin_Short_toByte           (KShort a           ) { return a; }
ALWAYS_INLINE KChar   Kotlin_Short_toChar           (KShort a           ) { return a; }
ALWAYS_INLINE KShort  Kotlin_Short_toShort          (KShort a           ) { return a; }
ALWAYS_INLINE KInt    Kotlin_Short_toInt            (KShort a           ) { return a; }
ALWAYS_INLINE KLong   Kotlin_Short_toLong           (KShort a           ) { return a; }
ALWAYS_INLINE KFloat  Kotlin_Short_toFloat          (KShort a           ) { return a; }
ALWAYS_INLINE KDouble Kotlin_Short_toDouble         (KShort a           ) { return a; }

//--- Int ---------------------------------------------------------------------//

KInt    Kotlin_Int_compareTo_Byte   (KInt a, KByte   b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Int_compareTo_Short  (KInt a, KShort  b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Int_compareTo_Int    (KInt a, KInt    b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Int_compareTo_Long   (KInt a, KLong   b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Int_compareTo_Float  (KInt a, KFloat  b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Int_compareTo_Double (KInt a, KDouble b) { if (a == b) return 0; return (a < b) ? -1 : 1; }

ALWAYS_INLINE KInt    Kotlin_Int_plus_Byte        (KInt a, KByte   b) { return a + b; }
ALWAYS_INLINE KInt    Kotlin_Int_plus_Short       (KInt a, KShort  b) { return a + b; }
ALWAYS_INLINE KInt    Kotlin_Int_plus_Int         (KInt a, KInt    b) { return a + b; }
ALWAYS_INLINE KLong   Kotlin_Int_plus_Long        (KInt a, KLong   b) { return a + b; }
ALWAYS_INLINE KFloat  Kotlin_Int_plus_Float       (KInt a, KFloat  b) { return a + b; }
ALWAYS_INLINE KDouble Kotlin_Int_plus_Double      (KInt a, KDouble b) { return a + b; }

ALWAYS_INLINE KInt    Kotlin_Int_minus_Byte       (KInt a, KByte   b) { return a - b; }
ALWAYS_INLINE KInt    Kotlin_Int_minus_Short      (KInt a, KShort  b) { return a - b; }
ALWAYS_INLINE KInt    Kotlin_Int_minus_Int        (KInt a, KInt    b) { return a - b; }
ALWAYS_INLINE KLong   Kotlin_Int_minus_Long       (KInt a, KLong   b) { return a - b; }
ALWAYS_INLINE KFloat  Kotlin_Int_minus_Float      (KInt a, KFloat  b) { return a - b; }
ALWAYS_INLINE KDouble Kotlin_Int_minus_Double     (KInt a, KDouble b) { return a - b; }

ALWAYS_INLINE KInt    Kotlin_Int_div_Byte         (KInt a, KByte   b) { return div<KInt>(a, b); }
ALWAYS_INLINE KInt    Kotlin_Int_div_Short        (KInt a, KShort  b) { return div<KInt>(a, b); }
ALWAYS_INLINE KInt    Kotlin_Int_div_Int          (KInt a, KInt    b) { return div<KInt>(a, b); }
ALWAYS_INLINE KLong   Kotlin_Int_div_Long         (KInt a, KLong   b) { return div<KLong>(a, b); }
ALWAYS_INLINE KFloat  Kotlin_Int_div_Float        (KInt a, KFloat  b) { return a / b; }
ALWAYS_INLINE KDouble Kotlin_Int_div_Double       (KInt a, KDouble b) { return a / b; }

ALWAYS_INLINE KInt    Kotlin_Int_mod_Byte         (KInt a, KByte   b) { return mod<KInt>(a, b); }
ALWAYS_INLINE KInt    Kotlin_Int_mod_Short        (KInt a, KShort  b) { return mod<KInt>(a, b); }
ALWAYS_INLINE KInt    Kotlin_Int_mod_Int          (KInt a, KInt    b) { return mod<KInt>(a, b); }
ALWAYS_INLINE KLong   Kotlin_Int_mod_Long         (KInt a, KLong   b) { return mod<KLong>(a, b); }
ALWAYS_INLINE KFloat  Kotlin_Int_mod_Float        (KInt a, KFloat  b) { return fmodf(a, b); }
ALWAYS_INLINE KDouble Kotlin_Int_mod_Double       (KInt a, KDouble b) { return fmod (a, b); }

ALWAYS_INLINE KInt    Kotlin_Int_times_Byte       (KInt a, KByte   b) { return a * b; }
ALWAYS_INLINE KInt    Kotlin_Int_times_Short      (KInt a, KShort  b) { return a * b; }
ALWAYS_INLINE KInt    Kotlin_Int_times_Int        (KInt a, KInt    b) { return a * b; }
ALWAYS_INLINE KLong   Kotlin_Int_times_Long       (KInt a, KLong   b) { return a * b; }
ALWAYS_INLINE KFloat  Kotlin_Int_times_Float      (KInt a, KFloat  b) { return a * b; }
ALWAYS_INLINE KDouble Kotlin_Int_times_Double     (KInt a, KDouble b) { return a * b; }

ALWAYS_INLINE KInt    Kotlin_Int_inc              (KInt a           ) { return ++a; }
ALWAYS_INLINE KInt    Kotlin_Int_dec              (KInt a           ) { return --a; }
ALWAYS_INLINE KInt    Kotlin_Int_unaryPlus        (KInt a           ) { return  +a; }
ALWAYS_INLINE KInt    Kotlin_Int_unaryMinus       (KInt a           ) { return  -a; }

ALWAYS_INLINE KInt    Kotlin_Int_or_Int           (KInt a, KInt   b)  { return a | b; }
ALWAYS_INLINE KInt    Kotlin_Int_xor_Int          (KInt a, KInt   b)  { return a ^ b; }
ALWAYS_INLINE KInt    Kotlin_Int_and_Int          (KInt a, KInt   b)  { return a & b; }
ALWAYS_INLINE KInt    Kotlin_Int_inv              (KInt a          )  { return  ~a;   }

// According to C++11 the result is undefined if the second operator is < 0 or >= <first operand bit length>.
// We avoid it by using only the least significant bits
ALWAYS_INLINE KInt    Kotlin_Int_shl_Int          (KInt a, KInt   b)  { return a << (b & 31); }
ALWAYS_INLINE KInt    Kotlin_Int_shr_Int          (KInt a, KInt   b)  { return a >> (b & 31); }
ALWAYS_INLINE KInt    Kotlin_Int_ushr_Int         (KInt a, KInt   b) {
  return static_cast<uint32_t>(a) >> (b & 31);
}

ALWAYS_INLINE KByte   Kotlin_Int_toByte           (KInt a           ) { return a; }
ALWAYS_INLINE KChar   Kotlin_Int_toChar           (KInt a           ) { return a; }
ALWAYS_INLINE KShort  Kotlin_Int_toShort          (KInt a           ) { return a; }
ALWAYS_INLINE KInt    Kotlin_Int_toInt            (KInt a           ) { return a; }
ALWAYS_INLINE KLong   Kotlin_Int_toLong           (KInt a           ) { return a; }
ALWAYS_INLINE KFloat  Kotlin_Int_toFloat          (KInt a           ) { return a; }
ALWAYS_INLINE KDouble Kotlin_Int_toDouble         (KInt a           ) { return a; }

//--- Long --------------------------------------------------------------------//

KInt    Kotlin_Long_compareTo_Byte   (KLong a, KByte   b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Long_compareTo_Short  (KLong a, KShort  b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Long_compareTo_Int    (KLong a, KInt    b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Long_compareTo_Long   (KLong a, KLong   b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Long_compareTo_Float  (KLong a, KFloat  b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Long_compareTo_Double (KLong a, KDouble b) { if (a == b) return 0; return (a < b) ? -1 : 1; }

ALWAYS_INLINE KLong   Kotlin_Long_plus_Byte        (KLong a, KByte   b) { return a + b; }
ALWAYS_INLINE KLong   Kotlin_Long_plus_Short       (KLong a, KShort  b) { return a + b; }
ALWAYS_INLINE KLong   Kotlin_Long_plus_Int         (KLong a, KInt    b) { return a + b; }
ALWAYS_INLINE KLong   Kotlin_Long_plus_Long        (KLong a, KLong   b) { return a + b; }
ALWAYS_INLINE KFloat  Kotlin_Long_plus_Float       (KLong a, KFloat  b) { return a + b; }
ALWAYS_INLINE KDouble Kotlin_Long_plus_Double      (KLong a, KDouble b) { return a + b; }

ALWAYS_INLINE KLong   Kotlin_Long_minus_Byte       (KLong a, KByte   b) { return a - b; }
ALWAYS_INLINE KLong   Kotlin_Long_minus_Short      (KLong a, KShort  b) { return a - b; }
ALWAYS_INLINE KLong   Kotlin_Long_minus_Int        (KLong a, KInt    b) { return a - b; }
ALWAYS_INLINE KLong   Kotlin_Long_minus_Long       (KLong a, KLong   b) { return a - b; }
ALWAYS_INLINE KFloat  Kotlin_Long_minus_Float      (KLong a, KFloat  b) { return a - b; }
ALWAYS_INLINE KDouble Kotlin_Long_minus_Double     (KLong a, KDouble b) { return a - b; }

ALWAYS_INLINE KLong   Kotlin_Long_div_Byte         (KLong a, KByte   b) { return div<KLong>(a, b); }
ALWAYS_INLINE KLong   Kotlin_Long_div_Short        (KLong a, KShort  b) { return div<KLong>(a, b); }
ALWAYS_INLINE KLong   Kotlin_Long_div_Int          (KLong a, KInt    b) { return div<KLong>(a, b); }
ALWAYS_INLINE KLong   Kotlin_Long_div_Long         (KLong a, KLong   b) { return div<KLong>(a, b); }
ALWAYS_INLINE KFloat  Kotlin_Long_div_Float        (KLong a, KFloat  b) { return a / b; }
ALWAYS_INLINE KDouble Kotlin_Long_div_Double       (KLong a, KDouble b) { return a / b; }

ALWAYS_INLINE KLong   Kotlin_Long_mod_Byte         (KLong a, KByte   b) { return mod<KLong>(a, b); }
ALWAYS_INLINE KLong   Kotlin_Long_mod_Short        (KLong a, KShort  b) { return mod<KLong>(a, b); }
ALWAYS_INLINE KLong   Kotlin_Long_mod_Int          (KLong a, KInt    b) { return mod<KLong>(a, b); }
ALWAYS_INLINE KLong   Kotlin_Long_mod_Long         (KLong a, KLong   b) { return mod<KLong>(a, b); }
ALWAYS_INLINE KFloat  Kotlin_Long_mod_Float        (KLong a, KFloat  b) { return fmodf(a, b); }
ALWAYS_INLINE KDouble Kotlin_Long_mod_Double       (KLong a, KDouble b) { return fmod (a, b); }

ALWAYS_INLINE KLong   Kotlin_Long_times_Byte       (KLong a, KByte   b) { return a * b; }
ALWAYS_INLINE KLong   Kotlin_Long_times_Short      (KLong a, KShort  b) { return a * b; }
ALWAYS_INLINE KLong   Kotlin_Long_times_Int        (KLong a, KInt    b) { return a * b; }
ALWAYS_INLINE KLong   Kotlin_Long_times_Long       (KLong a, KLong   b) { return a * b; }
ALWAYS_INLINE KFloat  Kotlin_Long_times_Float      (KLong a, KFloat  b) { return a * b; }
ALWAYS_INLINE KDouble Kotlin_Long_times_Double     (KLong a, KDouble b) { return a * b; }

ALWAYS_INLINE KLong   Kotlin_Long_inc              (KLong a           ) { return ++a; }
ALWAYS_INLINE KLong   Kotlin_Long_dec              (KLong a           ) { return --a; }
ALWAYS_INLINE KLong   Kotlin_Long_unaryPlus        (KLong a           ) { return  +a; }
ALWAYS_INLINE KLong   Kotlin_Long_unaryMinus       (KLong a           ) { return  -a; }

ALWAYS_INLINE KLong   Kotlin_Long_xor_Long         (KLong a, KLong   b) { return a ^ b; }
ALWAYS_INLINE KLong   Kotlin_Long_or_Long          (KLong a, KLong   b) { return a | b; }
ALWAYS_INLINE KLong   Kotlin_Long_and_Long         (KLong a, KLong   b) { return a & b; }
ALWAYS_INLINE KLong   Kotlin_Long_inv              (KLong a           ) { return  ~a;   }

// According to C++11 the result is undefined if the second operator is < 0 or >= <first operand bit length>.
// We avoid it by using only the least significant bits
ALWAYS_INLINE KLong   Kotlin_Long_shl_Int          (KLong a, KInt    b) { return a << (b & 63); }
ALWAYS_INLINE KLong   Kotlin_Long_shr_Int          (KLong a, KInt    b) { return a >> (b & 63); }
ALWAYS_INLINE KLong   Kotlin_Long_ushr_Int         (KLong a, KInt    b) {
  return static_cast<uint64_t>(a) >> (b & 63);
}

ALWAYS_INLINE KByte   Kotlin_Long_toByte           (KLong a           ) { return a; }
ALWAYS_INLINE KChar   Kotlin_Long_toChar           (KLong a           ) { return a; }
ALWAYS_INLINE KShort  Kotlin_Long_toShort          (KLong a           ) { return a; }
ALWAYS_INLINE KInt    Kotlin_Long_toInt            (KLong a           ) { return a; }
ALWAYS_INLINE KLong   Kotlin_Long_toLong           (KLong a           ) { return a; }
ALWAYS_INLINE KFloat  Kotlin_Long_toFloat          (KLong a           ) { return a; }
ALWAYS_INLINE KDouble Kotlin_Long_toDouble         (KLong a           ) { return a; }

//--- Float -------------------------------------------------------------------//

ALWAYS_INLINE KFloat  Kotlin_Float_plus_Byte        (KFloat a, KByte   b) { return a + b; }
ALWAYS_INLINE KFloat  Kotlin_Float_plus_Short       (KFloat a, KShort  b) { return a + b; }
ALWAYS_INLINE KFloat  Kotlin_Float_plus_Int         (KFloat a, KInt    b) { return a + b; }
ALWAYS_INLINE KFloat  Kotlin_Float_plus_Long        (KFloat a, KLong   b) { return a + b; }
ALWAYS_INLINE KFloat  Kotlin_Float_plus_Float       (KFloat a, KFloat  b) { return a + b; }
ALWAYS_INLINE KDouble Kotlin_Float_plus_Double      (KFloat a, KDouble b) { return a + b; }

ALWAYS_INLINE KFloat  Kotlin_Float_minus_Byte       (KFloat a, KByte   b) { return a - b; }
ALWAYS_INLINE KFloat  Kotlin_Float_minus_Short      (KFloat a, KShort  b) { return a - b; }
ALWAYS_INLINE KFloat  Kotlin_Float_minus_Int        (KFloat a, KInt    b) { return a - b; }
ALWAYS_INLINE KFloat  Kotlin_Float_minus_Long       (KFloat a, KLong   b) { return a - b; }
ALWAYS_INLINE KFloat  Kotlin_Float_minus_Float      (KFloat a, KFloat  b) { return a - b; }
ALWAYS_INLINE KDouble Kotlin_Float_minus_Double     (KFloat a, KDouble b) { return a - b; }

ALWAYS_INLINE KFloat  Kotlin_Float_div_Byte         (KFloat a, KByte   b) { return a / b; }
ALWAYS_INLINE KFloat  Kotlin_Float_div_Short        (KFloat a, KShort  b) { return a / b; }
ALWAYS_INLINE KFloat  Kotlin_Float_div_Int          (KFloat a, KInt    b) { return a / b; }
ALWAYS_INLINE KFloat  Kotlin_Float_div_Long         (KFloat a, KLong   b) { return a / b; }
ALWAYS_INLINE KFloat  Kotlin_Float_div_Float        (KFloat a, KFloat  b) { return a / b; }
ALWAYS_INLINE KDouble Kotlin_Float_div_Double       (KFloat a, KDouble b) { return a / b; }

ALWAYS_INLINE KFloat  Kotlin_Float_mod_Byte         (KFloat a, KByte   b) { return fmodf(a, b); }
ALWAYS_INLINE KFloat  Kotlin_Float_mod_Short        (KFloat a, KShort  b) { return fmodf(a, b); }
ALWAYS_INLINE KFloat  Kotlin_Float_mod_Int          (KFloat a, KInt    b) { return fmodf(a, b); }
ALWAYS_INLINE KFloat  Kotlin_Float_mod_Long         (KFloat a, KLong   b) { return fmodf(a, b); }
ALWAYS_INLINE KFloat  Kotlin_Float_mod_Float        (KFloat a, KFloat  b) { return fmodf(a, b); }
ALWAYS_INLINE KDouble Kotlin_Float_mod_Double       (KFloat a, KDouble b) { return fmod (a, b); }

ALWAYS_INLINE KFloat  Kotlin_Float_times_Byte       (KFloat a, KByte   b) { return a * b; }
ALWAYS_INLINE KFloat  Kotlin_Float_times_Short      (KFloat a, KShort  b) { return a * b; }
ALWAYS_INLINE KFloat  Kotlin_Float_times_Int        (KFloat a, KInt    b) { return a * b; }
ALWAYS_INLINE KFloat  Kotlin_Float_times_Long       (KFloat a, KLong   b) { return a * b; }
ALWAYS_INLINE KFloat  Kotlin_Float_times_Float      (KFloat a, KFloat  b) { return a * b; }
ALWAYS_INLINE KDouble Kotlin_Float_times_Double     (KFloat a, KDouble b) { return a * b; }

ALWAYS_INLINE KFloat  Kotlin_Float_inc              (KFloat a           ) { return ++a; }
ALWAYS_INLINE KFloat  Kotlin_Float_dec              (KFloat a           ) { return --a; }
ALWAYS_INLINE KFloat  Kotlin_Float_unaryPlus        (KFloat a           ) { return  +a; }
ALWAYS_INLINE KFloat  Kotlin_Float_unaryMinus       (KFloat a           ) { return  -a; }

KInt    Kotlin_Float_toInt            (KFloat a           ) {
  if (isnan(a)) return 0;
  if (a >= (KFloat) INT32_MAX) return INT32_MAX;
  if (a <= (KFloat) INT32_MIN) return INT32_MIN;
  return a;
}
KLong   Kotlin_Float_toLong           (KFloat a           ) {
  if (isnan(a)) return 0;
  if (a >= (KFloat) INT64_MAX) return INT64_MAX;
  if (a <= (KFloat) INT64_MIN) return INT64_MIN;
  return a;
}
ALWAYS_INLINE KFloat  Kotlin_Float_toFloat          (KFloat a           ) { return a; }
ALWAYS_INLINE KDouble Kotlin_Float_toDouble         (KFloat a           ) { return a; }
KByte   Kotlin_Float_toByte           (KFloat a           ) { return (KByte)  Kotlin_Float_toInt(a); }
KShort  Kotlin_Float_toShort          (KFloat a           ) { return (KShort) Kotlin_Float_toInt(a); }

ALWAYS_INLINE KInt    Kotlin_Float_bits             (KFloat a           ) { return floatToBits(a); }
ALWAYS_INLINE KFloat  Kotlin_Float_fromBits         (KInt a             ) { return bitsToFloat(a); }

ALWAYS_INLINE KBoolean Kotlin_Float_isNaN           (KFloat a)          { return isnan(a); }
ALWAYS_INLINE KBoolean Kotlin_Float_isInfinite      (KFloat a)          { return isinf(a); }
ALWAYS_INLINE KBoolean Kotlin_Float_isFinite        (KFloat a)          { return isfinite(a); }

  //--- Double ------------------------------------------------------------------//

ALWAYS_INLINE KDouble Kotlin_Double_plus_Byte        (KDouble a, KByte   b) { return a + b; }
ALWAYS_INLINE KDouble Kotlin_Double_plus_Short       (KDouble a, KShort  b) { return a + b; }
ALWAYS_INLINE KDouble Kotlin_Double_plus_Int         (KDouble a, KInt    b) { return a + b; }
ALWAYS_INLINE KDouble Kotlin_Double_plus_Long        (KDouble a, KLong   b) { return a + b; }
ALWAYS_INLINE KDouble Kotlin_Double_plus_Float       (KDouble a, KFloat  b) { return a + b; }
ALWAYS_INLINE KDouble Kotlin_Double_plus_Double      (KDouble a, KDouble b) { return a + b; }

ALWAYS_INLINE KDouble Kotlin_Double_minus_Byte       (KDouble a, KByte   b) { return a - b; }
ALWAYS_INLINE KDouble Kotlin_Double_minus_Short      (KDouble a, KShort  b) { return a - b; }
ALWAYS_INLINE KDouble Kotlin_Double_minus_Int        (KDouble a, KInt    b) { return a - b; }
ALWAYS_INLINE KDouble Kotlin_Double_minus_Long       (KDouble a, KLong   b) { return a - b; }
ALWAYS_INLINE KDouble Kotlin_Double_minus_Float      (KDouble a, KFloat  b) { return a - b; }
ALWAYS_INLINE KDouble Kotlin_Double_minus_Double     (KDouble a, KDouble b) { return a - b; }

ALWAYS_INLINE KDouble Kotlin_Double_div_Byte         (KDouble a, KByte   b) { return a / b; }
ALWAYS_INLINE KDouble Kotlin_Double_div_Short        (KDouble a, KShort  b) { return a / b; }
ALWAYS_INLINE KDouble Kotlin_Double_div_Int          (KDouble a, KInt    b) { return a / b; }
ALWAYS_INLINE KDouble Kotlin_Double_div_Long         (KDouble a, KLong   b) { return a / b; }
ALWAYS_INLINE KDouble Kotlin_Double_div_Float        (KDouble a, KFloat  b) { return a / b; }
ALWAYS_INLINE KDouble Kotlin_Double_div_Double       (KDouble a, KDouble b) { return a / b; }

ALWAYS_INLINE KDouble Kotlin_Double_mod_Byte         (KDouble a, KByte   b) { return fmod(a, b); }
ALWAYS_INLINE KDouble Kotlin_Double_mod_Short        (KDouble a, KShort  b) { return fmod(a, b); }
ALWAYS_INLINE KDouble Kotlin_Double_mod_Int          (KDouble a, KInt    b) { return fmod(a, b); }
ALWAYS_INLINE KDouble Kotlin_Double_mod_Long         (KDouble a, KLong   b) { return fmod(a, b); }
ALWAYS_INLINE KDouble Kotlin_Double_mod_Float        (KDouble a, KFloat  b) { return fmod(a, b); }
ALWAYS_INLINE KDouble Kotlin_Double_mod_Double       (KDouble a, KDouble b) { return fmod(a, b); }

ALWAYS_INLINE KDouble Kotlin_Double_times_Byte       (KDouble a, KByte   b) { return a * b; }
ALWAYS_INLINE KDouble Kotlin_Double_times_Short      (KDouble a, KShort  b) { return a * b; }
ALWAYS_INLINE KDouble Kotlin_Double_times_Int        (KDouble a, KInt    b) { return a * b; }
ALWAYS_INLINE KDouble Kotlin_Double_times_Long       (KDouble a, KLong   b) { return a * b; }
ALWAYS_INLINE KDouble Kotlin_Double_times_Float      (KDouble a, KFloat  b) { return a * b; }
ALWAYS_INLINE KDouble Kotlin_Double_times_Double     (KDouble a, KDouble b) { return a * b; }

ALWAYS_INLINE KDouble Kotlin_Double_inc              (KDouble a           ) { return ++a; }
ALWAYS_INLINE KDouble Kotlin_Double_dec              (KDouble a           ) { return --a; }
ALWAYS_INLINE KDouble Kotlin_Double_unaryPlus        (KDouble a           ) { return  +a; }
ALWAYS_INLINE KDouble Kotlin_Double_unaryMinus       (KDouble a           ) { return  -a; }

KInt    Kotlin_Double_toInt            (KDouble a ) {
  if (isnan(a)) return 0;
  if (a >= (KDouble) INT32_MAX) return INT32_MAX;
  if (a <= (KDouble) INT32_MIN) return INT32_MIN;
  return a;
}
KLong   Kotlin_Double_toLong           (KDouble a           ) {
  if (isnan(a)) return 0;
  if (a >= (KDouble) INT64_MAX) return INT64_MAX;
  if (a <= (KDouble) INT64_MIN) return INT64_MIN;
  return a;
}

ALWAYS_INLINE KFloat  Kotlin_Double_toFloat          (KDouble a           ) { return a; }
ALWAYS_INLINE KDouble Kotlin_Double_toDouble         (KDouble a           ) { return a; }

ALWAYS_INLINE KLong   Kotlin_Double_bits             (KDouble a           ) { return doubleToBits(a); }
ALWAYS_INLINE KDouble Kotlin_Double_fromBits         (KLong a             ) { return bitsToDouble(a); }

ALWAYS_INLINE KBoolean Kotlin_Double_isNaN           (KDouble a)          { return isnan(a); }
ALWAYS_INLINE KBoolean Kotlin_Double_isInfinite      (KDouble a)          { return isinf(a); }
ALWAYS_INLINE KBoolean Kotlin_Double_isFinite        (KDouble a)          { return isfinite(a); }

}  // extern "C"
