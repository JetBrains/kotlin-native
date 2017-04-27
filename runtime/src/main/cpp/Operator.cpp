/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

#include "Natives.h"
#include "Exceptions.h"

namespace {

template<typename R, typename Ta, typename Tb> R div(Ta a, Tb b) {
    if (__builtin_expect(b == 0, false)) {
        ThrowArithmeticException();
    }
    return a / b;
}

}

extern "C" {

//--- Boolean -----------------------------------------------------------------//

KBoolean Kotlin_Boolean_not              (KBoolean a            ) { return     !a; }
KBoolean Kotlin_Boolean_and_Boolean      (KBoolean a, KBoolean b) { return a && b; }
KBoolean Kotlin_Boolean_or_Boolean       (KBoolean a, KBoolean b) { return a || b; }
KBoolean Kotlin_Boolean_xor_Boolean      (KBoolean a, KBoolean b) { return a != b; }
KInt     Kotlin_Boolean_compareTo_Boolean(KBoolean a, KBoolean b) { if (a == b) return 0; return (a < b) ? -1 : 1; }

//--- Char --------------------------------------------------------------------//

KInt    Kotlin_Char_compareTo_Char   (KChar a, KChar   b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KChar   Kotlin_Char_plus_Int         (KChar a, KInt    b) { return a + b; }
KInt    Kotlin_Char_minus_Char       (KChar a, KChar   b) { return a - b; }
KChar   Kotlin_Char_minus_Int        (KChar a, KInt    b) { return a - b; }
KChar   Kotlin_Char_inc              (KChar a           ) { return a + 1; }
KChar   Kotlin_Char_dec              (KChar a           ) { return a - 1; }

KByte   Kotlin_Char_toByte           (KChar a           ) { return a; }
KChar   Kotlin_Char_toChar           (KChar a           ) { return a; }
KShort  Kotlin_Char_toShort          (KChar a           ) { return a; }
KInt    Kotlin_Char_toInt            (KChar a           ) { return a; }
KLong   Kotlin_Char_toLong           (KChar a           ) { return a; }
KFloat  Kotlin_Char_toFloat          (KChar a           ) { return a; }
KDouble Kotlin_Char_toDouble         (KChar a           ) { return a; }

//--- Byte --------------------------------------------------------------------//

KInt    Kotlin_Byte_compareTo_Byte   (KByte a, KByte   b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Byte_compareTo_Short  (KByte a, KShort  b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Byte_compareTo_Int    (KByte a, KInt    b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Byte_compareTo_Long   (KByte a, KLong   b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Byte_compareTo_Float  (KByte a, KFloat  b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Byte_compareTo_Double (KByte a, KDouble b) { if (a == b) return 0; return (a < b) ? -1 : 1; }

KInt    Kotlin_Byte_plus_Byte        (KByte a, KByte   b) { return a + b; }
KInt    Kotlin_Byte_plus_Short       (KByte a, KShort  b) { return a + b; }
KInt    Kotlin_Byte_plus_Int         (KByte a, KInt    b) { return a + b; }
KLong   Kotlin_Byte_plus_Long        (KByte a, KLong   b) { return a + b; }
KFloat  Kotlin_Byte_plus_Float       (KByte a, KFloat  b) { return a + b; }
KDouble Kotlin_Byte_plus_Double      (KByte a, KDouble b) { return a + b; }

KInt    Kotlin_Byte_minus_Byte       (KByte a, KByte   b) { return a - b; }
KInt    Kotlin_Byte_minus_Short      (KByte a, KShort  b) { return a - b; }
KInt    Kotlin_Byte_minus_Int        (KByte a, KInt    b) { return a - b; }
KLong   Kotlin_Byte_minus_Long       (KByte a, KLong   b) { return a - b; }
KFloat  Kotlin_Byte_minus_Float      (KByte a, KFloat  b) { return a - b; }
KDouble Kotlin_Byte_minus_Double     (KByte a, KDouble b) { return a - b; }

KInt    Kotlin_Byte_div_Byte         (KByte a, KByte   b) { return div<KInt>(a, b); }
KInt    Kotlin_Byte_div_Short        (KByte a, KShort  b) { return div<KInt>(a, b); }
KInt    Kotlin_Byte_div_Int          (KByte a, KInt    b) { return div<KInt>(a, b); }
KLong   Kotlin_Byte_div_Long         (KByte a, KLong   b) { return div<KLong>(a, b); }
KFloat  Kotlin_Byte_div_Float        (KByte a, KFloat  b) { return a / b; }
KDouble Kotlin_Byte_div_Double       (KByte a, KDouble b) { return a / b; }

KInt    Kotlin_Byte_rem_Byte         (KByte a, KByte   b) { return a % b; }
KInt    Kotlin_Byte_rem_Short        (KByte a, KShort  b) { return a % b; }
KInt    Kotlin_Byte_rem_Int          (KByte a, KInt    b) { return a % b; }
KLong   Kotlin_Byte_rem_Long         (KByte a, KLong   b) { return a % b; }
KFloat  Kotlin_Byte_rem_Float        (KByte a, KFloat  b) { return fmodf(a, b); }
KDouble Kotlin_Byte_rem_Double       (KByte a, KDouble b) { return fmod (a, b); }

KInt    Kotlin_Byte_times_Byte       (KByte a, KByte   b) { return a * b; }
KInt    Kotlin_Byte_times_Short      (KByte a, KShort  b) { return a * b; }
KInt    Kotlin_Byte_times_Int        (KByte a, KInt    b) { return a * b; }
KLong   Kotlin_Byte_times_Long       (KByte a, KLong   b) { return a * b; }
KFloat  Kotlin_Byte_times_Float      (KByte a, KFloat  b) { return a * b; }
KDouble Kotlin_Byte_times_Double     (KByte a, KDouble b) { return a * b; }

KByte   Kotlin_Byte_inc              (KByte a           ) { return ++a; }
KByte   Kotlin_Byte_dec              (KByte a           ) { return --a; }
KInt    Kotlin_Byte_unaryPlus        (KByte a           ) { return  +a; }
KInt    Kotlin_Byte_unaryMinus       (KByte a           ) { return  -a; }

KByte  Kotlin_Byte_or_Byte           (KByte a, KByte   b) { return a | b; }
KByte  Kotlin_Byte_xor_Byte          (KByte a, KByte   b) { return a ^ b; }
KByte  Kotlin_Byte_and_Byte          (KByte a, KByte   b) { return a & b; }
KByte  Kotlin_Byte_inv               (KByte a           ) { return  ~a;   }

KByte   Kotlin_Byte_toByte           (KByte a           ) { return a; }
KChar   Kotlin_Byte_toChar           (KByte a           ) { return a; }
KShort  Kotlin_Byte_toShort          (KByte a           ) { return a; }
KInt    Kotlin_Byte_toInt            (KByte a           ) { return a; }
KLong   Kotlin_Byte_toLong           (KByte a           ) { return a; }
KFloat  Kotlin_Byte_toFloat          (KByte a           ) { return a; }
KDouble Kotlin_Byte_toDouble         (KByte a           ) { return a; }

//--- Short -------------------------------------------------------------------//

KInt    Kotlin_Short_compareTo_Byte   (KShort a, KByte   b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Short_compareTo_Short  (KShort a, KShort  b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Short_compareTo_Int    (KShort a, KInt    b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Short_compareTo_Long   (KShort a, KLong   b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Short_compareTo_Float  (KShort a, KFloat  b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Short_compareTo_Double (KShort a, KDouble b) { if (a == b) return 0; return (a < b) ? -1 : 1; }

KInt    Kotlin_Short_plus_Byte        (KShort a, KByte   b) { return a + b; }
KInt    Kotlin_Short_plus_Short       (KShort a, KShort  b) { return a + b; }
KInt    Kotlin_Short_plus_Int         (KShort a, KInt    b) { return a + b; }
KLong   Kotlin_Short_plus_Long        (KShort a, KLong   b) { return a + b; }
KFloat  Kotlin_Short_plus_Float       (KShort a, KFloat  b) { return a + b; }
KDouble Kotlin_Short_plus_Double      (KShort a, KDouble b) { return a + b; }

KInt    Kotlin_Short_minus_Byte       (KShort a, KByte   b) { return a - b; }
KInt    Kotlin_Short_minus_Short      (KShort a, KShort  b) { return a - b; }
KInt    Kotlin_Short_minus_Int        (KShort a, KInt    b) { return a - b; }
KLong   Kotlin_Short_minus_Long       (KShort a, KLong   b) { return a - b; }
KFloat  Kotlin_Short_minus_Float      (KShort a, KFloat  b) { return a - b; }
KDouble Kotlin_Short_minus_Double     (KShort a, KDouble b) { return a - b; }

KInt    Kotlin_Short_div_Byte         (KShort a, KByte   b) { return div<KInt>(a, b); }
KInt    Kotlin_Short_div_Short        (KShort a, KShort  b) { return div<KInt>(a, b); }
KInt    Kotlin_Short_div_Int          (KShort a, KInt    b) { return div<KInt>(a, b); }
KLong   Kotlin_Short_div_Long         (KShort a, KLong   b) { return div<KLong>(a, b); }
KFloat  Kotlin_Short_div_Float        (KShort a, KFloat  b) { return a / b; }
KDouble Kotlin_Short_div_Double       (KShort a, KDouble b) { return a / b; }

KInt    Kotlin_Short_rem_Byte         (KShort a, KByte   b) { return a % b; }
KInt    Kotlin_Short_rem_Short        (KShort a, KShort  b) { return a % b; }
KInt    Kotlin_Short_rem_Int          (KShort a, KInt    b) { return a % b; }
KLong   Kotlin_Short_rem_Long         (KShort a, KLong   b) { return a % b; }
KFloat  Kotlin_Short_rem_Float        (KShort a, KFloat  b) { return fmodf(a, b); }
KDouble Kotlin_Short_rem_Double       (KShort a, KDouble b) { return fmod (a, b); }

KInt    Kotlin_Short_times_Byte       (KShort a, KByte   b) { return a * b; }
KInt    Kotlin_Short_times_Short      (KShort a, KShort  b) { return a * b; }
KInt    Kotlin_Short_times_Int        (KShort a, KInt    b) { return a * b; }
KLong   Kotlin_Short_times_Long       (KShort a, KLong   b) { return a * b; }
KFloat  Kotlin_Short_times_Float      (KShort a, KFloat  b) { return a * b; }
KDouble Kotlin_Short_times_Double     (KShort a, KDouble b) { return a * b; }

KShort  Kotlin_Short_inc              (KShort a           ) { return ++a; }
KShort  Kotlin_Short_dec              (KShort a           ) { return --a; }
KInt    Kotlin_Short_unaryPlus        (KShort a           ) { return  +a; }
KInt    Kotlin_Short_unaryMinus       (KShort a           ) { return  -a; }

KShort  Kotlin_Short_or_Short         (KShort a, KShort  b) { return a | b; }
KShort  Kotlin_Short_xor_Short        (KShort a, KShort  b) { return a ^ b; }
KShort  Kotlin_Short_and_Short        (KShort a, KShort  b) { return a & b; }
KShort  Kotlin_Short_inv              (KShort a           ) { return  ~a;   }

KByte   Kotlin_Short_toByte           (KShort a           ) { return a; }
KChar   Kotlin_Short_toChar           (KShort a           ) { return a; }
KShort  Kotlin_Short_toShort          (KShort a           ) { return a; }
KInt    Kotlin_Short_toInt            (KShort a           ) { return a; }
KLong   Kotlin_Short_toLong           (KShort a           ) { return a; }
KFloat  Kotlin_Short_toFloat          (KShort a           ) { return a; }
KDouble Kotlin_Short_toDouble         (KShort a           ) { return a; }

//--- Int ---------------------------------------------------------------------//

KInt    Kotlin_Int_compareTo_Byte   (KInt a, KByte   b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Int_compareTo_Short  (KInt a, KShort  b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Int_compareTo_Int    (KInt a, KInt    b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Int_compareTo_Long   (KInt a, KLong   b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Int_compareTo_Float  (KInt a, KFloat  b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Int_compareTo_Double (KInt a, KDouble b) { if (a == b) return 0; return (a < b) ? -1 : 1; }

KInt    Kotlin_Int_plus_Byte        (KInt a, KByte   b) { return a + b; }
KInt    Kotlin_Int_plus_Short       (KInt a, KShort  b) { return a + b; }
KInt    Kotlin_Int_plus_Int         (KInt a, KInt    b) { return a + b; }
KLong   Kotlin_Int_plus_Long        (KInt a, KLong   b) { return a + b; }
KFloat  Kotlin_Int_plus_Float       (KInt a, KFloat  b) { return a + b; }
KDouble Kotlin_Int_plus_Double      (KInt a, KDouble b) { return a + b; }

KInt    Kotlin_Int_minus_Byte       (KInt a, KByte   b) { return a - b; }
KInt    Kotlin_Int_minus_Short      (KInt a, KShort  b) { return a - b; }
KInt    Kotlin_Int_minus_Int        (KInt a, KInt    b) { return a - b; }
KLong   Kotlin_Int_minus_Long       (KInt a, KLong   b) { return a - b; }
KFloat  Kotlin_Int_minus_Float      (KInt a, KFloat  b) { return a - b; }
KDouble Kotlin_Int_minus_Double     (KInt a, KDouble b) { return a - b; }

KInt    Kotlin_Int_div_Byte         (KInt a, KByte   b) { return div<KInt>(a, b); }
KInt    Kotlin_Int_div_Short        (KInt a, KShort  b) { return div<KInt>(a, b); }
KInt    Kotlin_Int_div_Int          (KInt a, KInt    b) { return div<KInt>(a, b); }
KLong   Kotlin_Int_div_Long         (KInt a, KLong   b) { return div<KLong>(a, b); }
KFloat  Kotlin_Int_div_Float        (KInt a, KFloat  b) { return a / b; }
KDouble Kotlin_Int_div_Double       (KInt a, KDouble b) { return a / b; }

KInt    Kotlin_Int_rem_Byte         (KInt a, KByte   b) { return a % b; }
KInt    Kotlin_Int_rem_Short        (KInt a, KShort  b) { return a % b; }
KInt    Kotlin_Int_rem_Int          (KInt a, KInt    b) { return a % b; }
KLong   Kotlin_Int_rem_Long         (KInt a, KLong   b) { return a % b; }
KFloat  Kotlin_Int_rem_Float        (KInt a, KFloat  b) { return fmodf(a, b); }
KDouble Kotlin_Int_rem_Double       (KInt a, KDouble b) { return fmod (a, b); }

KInt    Kotlin_Int_times_Byte       (KInt a, KByte   b) { return a * b; }
KInt    Kotlin_Int_times_Short      (KInt a, KShort  b) { return a * b; }
KInt    Kotlin_Int_times_Int        (KInt a, KInt    b) { return a * b; }
KLong   Kotlin_Int_times_Long       (KInt a, KLong   b) { return a * b; }
KFloat  Kotlin_Int_times_Float      (KInt a, KFloat  b) { return a * b; }
KDouble Kotlin_Int_times_Double     (KInt a, KDouble b) { return a * b; }

KInt    Kotlin_Int_inc              (KInt a           ) { return ++a; }
KInt    Kotlin_Int_dec              (KInt a           ) { return --a; }
KInt    Kotlin_Int_unaryPlus        (KInt a           ) { return  +a; }
KInt    Kotlin_Int_unaryMinus       (KInt a           ) { return  -a; }

KInt    Kotlin_Int_or_Int           (KInt a, KInt   b)  { return a | b; }
KInt    Kotlin_Int_xor_Int          (KInt a, KInt   b)  { return a ^ b; }
KInt    Kotlin_Int_and_Int          (KInt a, KInt   b)  { return a & b; }
KInt    Kotlin_Int_inv              (KInt a          )  { return  ~a;   }

// According to C++11 the result is undefined if the second operator is < 0 or >= <first operand bit length>.
// We avoid it by using only the least significant bits
KInt    Kotlin_Int_shl_Int          (KInt a, KInt   b)  { return a << (b & 31); }
KInt    Kotlin_Int_shr_Int          (KInt a, KInt   b)  { return a >> (b & 31); }
KInt    Kotlin_Int_ushr_Int         (KInt a, KInt   b) {
  return static_cast<uint32_t>(a) >> (b & 31);
}

KByte   Kotlin_Int_toByte           (KInt a           ) { return a; }
KChar   Kotlin_Int_toChar           (KInt a           ) { return a; }
KShort  Kotlin_Int_toShort          (KInt a           ) { return a; }
KInt    Kotlin_Int_toInt            (KInt a           ) { return a; }
KLong   Kotlin_Int_toLong           (KInt a           ) { return a; }
KFloat  Kotlin_Int_toFloat          (KInt a           ) { return a; }
KDouble Kotlin_Int_toDouble         (KInt a           ) { return a; }

//--- Long --------------------------------------------------------------------//

KInt    Kotlin_Long_compareTo_Byte   (KLong a, KByte   b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Long_compareTo_Short  (KLong a, KShort  b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Long_compareTo_Int    (KLong a, KInt    b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Long_compareTo_Long   (KLong a, KLong   b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Long_compareTo_Float  (KLong a, KFloat  b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Long_compareTo_Double (KLong a, KDouble b) { if (a == b) return 0; return (a < b) ? -1 : 1; }

KLong   Kotlin_Long_plus_Byte        (KLong a, KByte   b) { return a + b; }
KLong   Kotlin_Long_plus_Short       (KLong a, KShort  b) { return a + b; }
KLong   Kotlin_Long_plus_Int         (KLong a, KInt    b) { return a + b; }
KLong   Kotlin_Long_plus_Long        (KLong a, KLong   b) { return a + b; }
KFloat  Kotlin_Long_plus_Float       (KLong a, KFloat  b) { return a + b; }
KDouble Kotlin_Long_plus_Double      (KLong a, KDouble b) { return a + b; }

KLong   Kotlin_Long_minus_Byte       (KLong a, KByte   b) { return a - b; }
KLong   Kotlin_Long_minus_Short      (KLong a, KShort  b) { return a - b; }
KLong   Kotlin_Long_minus_Int        (KLong a, KInt    b) { return a - b; }
KLong   Kotlin_Long_minus_Long       (KLong a, KLong   b) { return a - b; }
KFloat  Kotlin_Long_minus_Float      (KLong a, KFloat  b) { return a - b; }
KDouble Kotlin_Long_minus_Double     (KLong a, KDouble b) { return a - b; }

KLong   Kotlin_Long_div_Byte         (KLong a, KByte   b) { return div<KLong>(a, b); }
KLong   Kotlin_Long_div_Short        (KLong a, KShort  b) { return div<KLong>(a, b); }
KLong   Kotlin_Long_div_Int          (KLong a, KInt    b) { return div<KLong>(a, b); }
KLong   Kotlin_Long_div_Long         (KLong a, KLong   b) { return div<KLong>(a, b); }
KFloat  Kotlin_Long_div_Float        (KLong a, KFloat  b) { return a / b; }
KDouble Kotlin_Long_div_Double       (KLong a, KDouble b) { return a / b; }

KLong   Kotlin_Long_rem_Byte         (KLong a, KByte   b) { return a % b; }
KLong   Kotlin_Long_rem_Short        (KLong a, KShort  b) { return a % b; }
KLong   Kotlin_Long_rem_Int          (KLong a, KInt    b) { return a % b; }
KLong   Kotlin_Long_rem_Long         (KLong a, KLong   b) { return a % b; }
KFloat  Kotlin_Long_rem_Float        (KLong a, KFloat  b) { return fmodf(a, b); }
KDouble Kotlin_Long_rem_Double       (KLong a, KDouble b) { return fmod (a, b); }

KLong   Kotlin_Long_times_Byte       (KLong a, KByte   b) { return a * b; }
KLong   Kotlin_Long_times_Short      (KLong a, KShort  b) { return a * b; }
KLong   Kotlin_Long_times_Int        (KLong a, KInt    b) { return a * b; }
KLong   Kotlin_Long_times_Long       (KLong a, KLong   b) { return a * b; }
KFloat  Kotlin_Long_times_Float      (KLong a, KFloat  b) { return a * b; }
KDouble Kotlin_Long_times_Double     (KLong a, KDouble b) { return a * b; }

KLong   Kotlin_Long_inc              (KLong a           ) { return ++a; }
KLong   Kotlin_Long_dec              (KLong a           ) { return --a; }
KLong   Kotlin_Long_unaryPlus        (KLong a           ) { return  +a; }
KLong   Kotlin_Long_unaryMinus       (KLong a           ) { return  -a; }

KLong   Kotlin_Long_xor_Long         (KLong a, KLong   b) { return a ^ b; }
KLong   Kotlin_Long_or_Long          (KLong a, KLong   b) { return a | b; }
KLong   Kotlin_Long_and_Long         (KLong a, KLong   b) { return a & b; }
KLong   Kotlin_Long_inv              (KLong a           ) { return  ~a;   }

// According to C++11 the result is undefined if the second operator is < 0 or >= <first operand bit length>.
// We avoid it by using only the least significant bits
KLong   Kotlin_Long_shl_Int          (KLong a, KInt    b) { return a << (b & 63); }
KLong   Kotlin_Long_shr_Int          (KLong a, KInt    b) { return a >> (b & 63); }
KLong   Kotlin_Long_ushr_Int         (KLong a, KInt    b) {
  return static_cast<uint64_t>(a) >> (b & 63);
}

KByte   Kotlin_Long_toByte           (KLong a           ) { return a; }
KChar   Kotlin_Long_toChar           (KLong a           ) { return a; }
KShort  Kotlin_Long_toShort          (KLong a           ) { return a; }
KInt    Kotlin_Long_toInt            (KLong a           ) { return a; }
KLong   Kotlin_Long_toLong           (KLong a           ) { return a; }
KFloat  Kotlin_Long_toFloat          (KLong a           ) { return a; }
KDouble Kotlin_Long_toDouble         (KLong a           ) { return a; }

//--- Float -------------------------------------------------------------------//

KInt    Kotlin_Float_compareTo_Byte   (KFloat a, KByte   b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Float_compareTo_Short  (KFloat a, KShort  b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Float_compareTo_Int    (KFloat a, KInt    b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Float_compareTo_Long   (KFloat a, KLong   b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Float_compareTo_Float  (KFloat a, KFloat  b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Float_compareTo_Double (KFloat a, KDouble b) { if (a == b) return 0; return (a < b) ? -1 : 1; }

KFloat  Kotlin_Float_plus_Byte        (KFloat a, KByte   b) { return a + b; }
KFloat  Kotlin_Float_plus_Short       (KFloat a, KShort  b) { return a + b; }
KFloat  Kotlin_Float_plus_Int         (KFloat a, KInt    b) { return a + b; }
KFloat  Kotlin_Float_plus_Long        (KFloat a, KLong   b) { return a + b; }
KFloat  Kotlin_Float_plus_Float       (KFloat a, KFloat  b) { return a + b; }
KDouble Kotlin_Float_plus_Double      (KFloat a, KDouble b) { return a + b; }

KFloat  Kotlin_Float_minus_Byte       (KFloat a, KByte   b) { return a - b; }
KFloat  Kotlin_Float_minus_Short      (KFloat a, KShort  b) { return a - b; }
KFloat  Kotlin_Float_minus_Int        (KFloat a, KInt    b) { return a - b; }
KFloat  Kotlin_Float_minus_Long       (KFloat a, KLong   b) { return a - b; }
KFloat  Kotlin_Float_minus_Float      (KFloat a, KFloat  b) { return a - b; }
KDouble Kotlin_Float_minus_Double     (KFloat a, KDouble b) { return a - b; }

KFloat  Kotlin_Float_div_Byte         (KFloat a, KByte   b) { return a / b; }
KFloat  Kotlin_Float_div_Short        (KFloat a, KShort  b) { return a / b; }
KFloat  Kotlin_Float_div_Int          (KFloat a, KInt    b) { return a / b; }
KFloat  Kotlin_Float_div_Long         (KFloat a, KLong   b) { return a / b; }
KFloat  Kotlin_Float_div_Float        (KFloat a, KFloat  b) { return a / b; }
KDouble Kotlin_Float_div_Double       (KFloat a, KDouble b) { return a / b; }

KFloat  Kotlin_Float_rem_Byte         (KFloat a, KByte   b) { return fmodf(a, b); }
KFloat  Kotlin_Float_rem_Short        (KFloat a, KShort  b) { return fmodf(a, b); }
KFloat  Kotlin_Float_rem_Int          (KFloat a, KInt    b) { return fmodf(a, b); }
KFloat  Kotlin_Float_rem_Long         (KFloat a, KLong   b) { return fmodf(a, b); }
KFloat  Kotlin_Float_rem_Float        (KFloat a, KFloat  b) { return fmodf(a, b); }
KDouble Kotlin_Float_rem_Double       (KFloat a, KDouble b) { return fmod (a, b); }

KFloat  Kotlin_Float_times_Byte       (KFloat a, KByte   b) { return a * b; }
KFloat  Kotlin_Float_times_Short      (KFloat a, KShort  b) { return a * b; }
KFloat  Kotlin_Float_times_Int        (KFloat a, KInt    b) { return a * b; }
KFloat  Kotlin_Float_times_Long       (KFloat a, KLong   b) { return a * b; }
KFloat  Kotlin_Float_times_Float      (KFloat a, KFloat  b) { return a * b; }
KDouble Kotlin_Float_times_Double     (KFloat a, KDouble b) { return a * b; }

KFloat  Kotlin_Float_inc              (KFloat a           ) { return ++a; }
KFloat  Kotlin_Float_dec              (KFloat a           ) { return --a; }
KFloat  Kotlin_Float_unaryPlus        (KFloat a           ) { return  +a; }
KFloat  Kotlin_Float_unaryMinus       (KFloat a           ) { return  -a; }

KByte   Kotlin_Float_toByte           (KFloat a           ) { return a; }
KChar   Kotlin_Float_toChar           (KFloat a           ) { return a; }
KShort  Kotlin_Float_toShort          (KFloat a           ) { return a; }
KInt    Kotlin_Float_toInt            (KFloat a           ) { return a; }
KLong   Kotlin_Float_toLong           (KFloat a           ) { return a; }
KFloat  Kotlin_Float_toFloat          (KFloat a           ) { return a; }
KDouble Kotlin_Float_toDouble         (KFloat a           ) { return a; }

KInt   Kotlin_Float_bits              (KFloat a) {
  union {
    KFloat f;
    KInt i;
  } alias;
  alias.f = a;
  return alias.i;
}

KBoolean Kotlin_Float_isNaN           (KFloat a)          { return isnan(a); }
KBoolean Kotlin_Float_isInfinite      (KFloat a)          { return isinf(a); }
KBoolean Kotlin_Float_isFinite        (KFloat a)          { return isfinite(a); }

  //--- Double ------------------------------------------------------------------//

KInt    Kotlin_Double_compareTo_Byte   (KDouble a, KByte   b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Double_compareTo_Short  (KDouble a, KShort  b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Double_compareTo_Int    (KDouble a, KInt    b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Double_compareTo_Long   (KDouble a, KLong   b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Double_compareTo_Float  (KDouble a, KFloat  b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Double_compareTo_Double (KDouble a, KDouble b) { if (a == b) return 0; return (a < b) ? -1 : 1; }

KDouble Kotlin_Double_plus_Byte        (KDouble a, KByte   b) { return a + b; }
KDouble Kotlin_Double_plus_Short       (KDouble a, KShort  b) { return a + b; }
KDouble Kotlin_Double_plus_Int         (KDouble a, KInt    b) { return a + b; }
KDouble Kotlin_Double_plus_Long        (KDouble a, KLong   b) { return a + b; }
KDouble Kotlin_Double_plus_Float       (KDouble a, KFloat  b) { return a + b; }
KDouble Kotlin_Double_plus_Double      (KDouble a, KDouble b) { return a + b; }

KDouble Kotlin_Double_minus_Byte       (KDouble a, KByte   b) { return a - b; }
KDouble Kotlin_Double_minus_Short      (KDouble a, KShort  b) { return a - b; }
KDouble Kotlin_Double_minus_Int        (KDouble a, KInt    b) { return a - b; }
KDouble Kotlin_Double_minus_Long       (KDouble a, KLong   b) { return a - b; }
KDouble Kotlin_Double_minus_Float      (KDouble a, KFloat  b) { return a - b; }
KDouble Kotlin_Double_minus_Double     (KDouble a, KDouble b) { return a - b; }

KDouble Kotlin_Double_div_Byte         (KDouble a, KByte   b) { return a / b; }
KDouble Kotlin_Double_div_Short        (KDouble a, KShort  b) { return a / b; }
KDouble Kotlin_Double_div_Int          (KDouble a, KInt    b) { return a / b; }
KDouble Kotlin_Double_div_Long         (KDouble a, KLong   b) { return a / b; }
KDouble Kotlin_Double_div_Float        (KDouble a, KFloat  b) { return a / b; }
KDouble Kotlin_Double_div_Double       (KDouble a, KDouble b) { return a / b; }

KDouble Kotlin_Double_rem_Byte         (KDouble a, KByte   b) { return fmod(a, b); }
KDouble Kotlin_Double_rem_Short        (KDouble a, KShort  b) { return fmod(a, b); }
KDouble Kotlin_Double_rem_Int          (KDouble a, KInt    b) { return fmod(a, b); }
KDouble Kotlin_Double_rem_Long         (KDouble a, KLong   b) { return fmod(a, b); }
KDouble Kotlin_Double_rem_Float        (KDouble a, KFloat  b) { return fmod(a, b); }
KDouble Kotlin_Double_rem_Double       (KDouble a, KDouble b) { return fmod(a, b); }

KDouble Kotlin_Double_times_Byte       (KDouble a, KByte   b) { return a * b; }
KDouble Kotlin_Double_times_Short      (KDouble a, KShort  b) { return a * b; }
KDouble Kotlin_Double_times_Int        (KDouble a, KInt    b) { return a * b; }
KDouble Kotlin_Double_times_Long       (KDouble a, KLong   b) { return a * b; }
KDouble Kotlin_Double_times_Float      (KDouble a, KFloat  b) { return a * b; }
KDouble Kotlin_Double_times_Double     (KDouble a, KDouble b) { return a * b; }

KDouble Kotlin_Double_inc              (KDouble a           ) { return ++a; }
KDouble Kotlin_Double_dec              (KDouble a           ) { return --a; }
KDouble Kotlin_Double_unaryPlus        (KDouble a           ) { return  +a; }
KDouble Kotlin_Double_unaryMinus       (KDouble a           ) { return  -a; }

KByte   Kotlin_Double_toByte           (KDouble a           ) { return a; }
KChar   Kotlin_Double_toChar           (KDouble a           ) { return a; }
KShort  Kotlin_Double_toShort          (KDouble a           ) { return a; }
KInt    Kotlin_Double_toInt            (KDouble a           ) { return a; }
KLong   Kotlin_Double_toLong           (KDouble a           ) { return a; }
KFloat  Kotlin_Double_toFloat          (KDouble a           ) { return a; }
KDouble Kotlin_Double_toDouble         (KDouble a           ) { return a; }

KLong   Kotlin_Double_bits             (KDouble a) {
  union {
    KDouble d;
    KLong l;
  } alias;
  alias.d = a;
  return alias.l;
}

KBoolean Kotlin_Double_isNaN           (KDouble a)          { return isnan(a); }
KBoolean Kotlin_Double_isInfinite      (KDouble a)          { return isinf(a); }
KBoolean Kotlin_Double_isFinite        (KDouble a)          { return isfinite(a); }

// Autogenerated with tools/generators/Operators.kt (unsigned_c).
KUInt Kotlin_UByte_plus_Byte(KUByte arg0, KByte arg1) { return arg0 + arg1; }
KUInt Kotlin_UByte_plus_Short(KUByte arg0, KShort arg1) { return arg0 + arg1; }
KUInt Kotlin_UByte_plus_Int(KUByte arg0, KInt arg1) { return arg0 + arg1; }
KLong Kotlin_UByte_plus_Long(KUByte arg0, KLong arg1) { return arg0 + arg1; }
KFloat Kotlin_UByte_plus_Float(KUByte arg0, KFloat arg1) { return arg0 + arg1; }
KDouble Kotlin_UByte_plus_Double(KUByte arg0, KDouble arg1) { return arg0 + arg1; }
KUInt Kotlin_UByte_minus_Byte(KUByte arg0, KByte arg1) { return arg0 - arg1; }
KUInt Kotlin_UByte_minus_Short(KUByte arg0, KShort arg1) { return arg0 - arg1; }
KUInt Kotlin_UByte_minus_Int(KUByte arg0, KInt arg1) { return arg0 - arg1; }
KLong Kotlin_UByte_minus_Long(KUByte arg0, KLong arg1) { return arg0 - arg1; }
KFloat Kotlin_UByte_minus_Float(KUByte arg0, KFloat arg1) { return arg0 - arg1; }
KDouble Kotlin_UByte_minus_Double(KUByte arg0, KDouble arg1) { return arg0 - arg1; }
KUInt Kotlin_UByte_times_Byte(KUByte arg0, KByte arg1) { return arg0 * arg1; }
KUInt Kotlin_UByte_times_Short(KUByte arg0, KShort arg1) { return arg0 * arg1; }
KUInt Kotlin_UByte_times_Int(KUByte arg0, KInt arg1) { return arg0 * arg1; }
KLong Kotlin_UByte_times_Long(KUByte arg0, KLong arg1) { return arg0 * arg1; }
KFloat Kotlin_UByte_times_Float(KUByte arg0, KFloat arg1) { return arg0 * arg1; }
KDouble Kotlin_UByte_times_Double(KUByte arg0, KDouble arg1) { return arg0 * arg1; }
KUInt Kotlin_UByte_div_Byte(KUByte arg0, KByte arg1) { return div<KUInt>(arg0, arg1); }
KUInt Kotlin_UByte_div_Short(KUByte arg0, KShort arg1) { return div<KUInt>(arg0, arg1); }
KUInt Kotlin_UByte_div_Int(KUByte arg0, KInt arg1) { return div<KUInt>(arg0, arg1); }
KLong Kotlin_UByte_div_Long(KUByte arg0, KLong arg1) { return div<KLong>(arg0, arg1); }
KFloat Kotlin_UByte_div_Float(KUByte arg0, KFloat arg1) { return arg0 / arg1; }
KDouble Kotlin_UByte_div_Double(KUByte arg0, KDouble arg1) { return arg0 / arg1; }
KUInt Kotlin_UByte_rem_Byte(KUByte arg0, KByte arg1) { return arg0 % arg1; }
KUInt Kotlin_UByte_rem_Short(KUByte arg0, KShort arg1) { return arg0 % arg1; }
KUInt Kotlin_UByte_rem_Int(KUByte arg0, KInt arg1) { return arg0 % arg1; }
KLong Kotlin_UByte_rem_Long(KUByte arg0, KLong arg1) { return arg0 % arg1; }
KFloat Kotlin_UByte_rem_Float(KUByte arg0, KFloat arg1) { return fmodf(arg0, arg1); }
KDouble Kotlin_UByte_rem_Double(KUByte arg0, KDouble arg1) { return fmod(arg0, arg1); }
KInt Kotlin_UByte_compareTo_Byte(KUByte arg0, KByte arg1) { if (arg0 == arg1) return 0; return (arg0 < arg1) ? -1 : 1; }
KInt Kotlin_UByte_compareTo_Short(KUByte arg0, KShort arg1) { if (arg0 == arg1) return 0; return (arg0 < arg1) ? -1 : 1; }
KInt Kotlin_UByte_compareTo_Int(KUByte arg0, KInt arg1) { if (arg0 == arg1) return 0; return (arg0 < arg1) ? -1 : 1; }
KInt Kotlin_UByte_compareTo_Long(KUByte arg0, KLong arg1) { if (arg0 == arg1) return 0; return (arg0 < arg1) ? -1 : 1; }
KInt Kotlin_UByte_compareTo_Float(KUByte arg0, KFloat arg1) { if (arg0 == arg1) return 0; return (arg0 < arg1) ? -1 : 1; }
KInt Kotlin_UByte_compareTo_Double(KUByte arg0, KDouble arg1) { if (arg0 == arg1) return 0; return (arg0 < arg1) ? -1 : 1; }
KUInt Kotlin_UByte_and_UByte(KUByte arg0, KUByte arg1) { return arg0 & arg1; }
KUInt Kotlin_UByte_or_UByte(KUByte arg0, KUByte arg1) { return arg0 | arg1; }
KUInt Kotlin_UByte_xor_UByte(KUByte arg0, KUByte arg1) { return arg0 ^ arg1; }
KUInt Kotlin_UByte_shl_Int(KUByte arg0, KInt arg1) { return arg0 << (arg1 & 31); }
KUInt Kotlin_UByte_shr_Int(KUByte arg0, KInt arg1) { return arg0 >> (arg1 & 31); }
KUByte Kotlin_UByte_inv(KUByte arg0) { return ~arg0; }
KUByte Kotlin_UByte_inc(KUByte arg0) { return ++arg0; }
KUByte Kotlin_UByte_dec(KUByte arg0) { return --arg0; }
KUInt Kotlin_UByte_unaryPlus(KUByte arg0) { return +arg0; }
KUInt Kotlin_UByte_unaryMinus(KUByte arg0) { return -arg0; }
KByte Kotlin_UByte_toByte(KUByte arg0) { return arg0; }
KShort Kotlin_UByte_toShort(KUByte arg0) { return arg0; }
KInt Kotlin_UByte_toInt(KUByte arg0) { return arg0; }
KLong Kotlin_UByte_toLong(KUByte arg0) { return arg0; }
KFloat Kotlin_UByte_toFloat(KUByte arg0) { return arg0; }
KDouble Kotlin_UByte_toDouble(KUByte arg0) { return arg0; }
KChar Kotlin_UByte_toChar(KUByte arg0) { return arg0; }
KUInt Kotlin_UShort_plus_Byte(KUShort arg0, KByte arg1) { return arg0 + arg1; }
KUInt Kotlin_UShort_plus_Short(KUShort arg0, KShort arg1) { return arg0 + arg1; }
KUInt Kotlin_UShort_plus_Int(KUShort arg0, KInt arg1) { return arg0 + arg1; }
KLong Kotlin_UShort_plus_Long(KUShort arg0, KLong arg1) { return arg0 + arg1; }
KFloat Kotlin_UShort_plus_Float(KUShort arg0, KFloat arg1) { return arg0 + arg1; }
KDouble Kotlin_UShort_plus_Double(KUShort arg0, KDouble arg1) { return arg0 + arg1; }
KUInt Kotlin_UShort_minus_Byte(KUShort arg0, KByte arg1) { return arg0 - arg1; }
KUInt Kotlin_UShort_minus_Short(KUShort arg0, KShort arg1) { return arg0 - arg1; }
KUInt Kotlin_UShort_minus_Int(KUShort arg0, KInt arg1) { return arg0 - arg1; }
KLong Kotlin_UShort_minus_Long(KUShort arg0, KLong arg1) { return arg0 - arg1; }
KFloat Kotlin_UShort_minus_Float(KUShort arg0, KFloat arg1) { return arg0 - arg1; }
KDouble Kotlin_UShort_minus_Double(KUShort arg0, KDouble arg1) { return arg0 - arg1; }
KUInt Kotlin_UShort_times_Byte(KUShort arg0, KByte arg1) { return arg0 * arg1; }
KUInt Kotlin_UShort_times_Short(KUShort arg0, KShort arg1) { return arg0 * arg1; }
KUInt Kotlin_UShort_times_Int(KUShort arg0, KInt arg1) { return arg0 * arg1; }
KLong Kotlin_UShort_times_Long(KUShort arg0, KLong arg1) { return arg0 * arg1; }
KFloat Kotlin_UShort_times_Float(KUShort arg0, KFloat arg1) { return arg0 * arg1; }
KDouble Kotlin_UShort_times_Double(KUShort arg0, KDouble arg1) { return arg0 * arg1; }
KUInt Kotlin_UShort_div_Byte(KUShort arg0, KByte arg1) { return div<KUInt>(arg0, arg1); }
KUInt Kotlin_UShort_div_Short(KUShort arg0, KShort arg1) { return div<KUInt>(arg0, arg1); }
KUInt Kotlin_UShort_div_Int(KUShort arg0, KInt arg1) { return div<KUInt>(arg0, arg1); }
KLong Kotlin_UShort_div_Long(KUShort arg0, KLong arg1) { return div<KLong>(arg0, arg1); }
KFloat Kotlin_UShort_div_Float(KUShort arg0, KFloat arg1) { return arg0 / arg1; }
KDouble Kotlin_UShort_div_Double(KUShort arg0, KDouble arg1) { return arg0 / arg1; }
KUInt Kotlin_UShort_rem_Byte(KUShort arg0, KByte arg1) { return arg0 % arg1; }
KUInt Kotlin_UShort_rem_Short(KUShort arg0, KShort arg1) { return arg0 % arg1; }
KUInt Kotlin_UShort_rem_Int(KUShort arg0, KInt arg1) { return arg0 % arg1; }
KLong Kotlin_UShort_rem_Long(KUShort arg0, KLong arg1) { return arg0 % arg1; }
KFloat Kotlin_UShort_rem_Float(KUShort arg0, KFloat arg1) { return fmodf(arg0, arg1); }
KDouble Kotlin_UShort_rem_Double(KUShort arg0, KDouble arg1) { return fmod(arg0, arg1); }
KInt Kotlin_UShort_compareTo_Byte(KUShort arg0, KByte arg1) { if (arg0 == arg1) return 0; return (arg0 < arg1) ? -1 : 1; }
KInt Kotlin_UShort_compareTo_Short(KUShort arg0, KShort arg1) { if (arg0 == arg1) return 0; return (arg0 < arg1) ? -1 : 1; }
KInt Kotlin_UShort_compareTo_Int(KUShort arg0, KInt arg1) { if (arg0 == arg1) return 0; return (arg0 < arg1) ? -1 : 1; }
KInt Kotlin_UShort_compareTo_Long(KUShort arg0, KLong arg1) { if (arg0 == arg1) return 0; return (arg0 < arg1) ? -1 : 1; }
KInt Kotlin_UShort_compareTo_Float(KUShort arg0, KFloat arg1) { if (arg0 == arg1) return 0; return (arg0 < arg1) ? -1 : 1; }
KInt Kotlin_UShort_compareTo_Double(KUShort arg0, KDouble arg1) { if (arg0 == arg1) return 0; return (arg0 < arg1) ? -1 : 1; }
KUInt Kotlin_UShort_and_UShort(KUShort arg0, KUShort arg1) { return arg0 & arg1; }
KUInt Kotlin_UShort_or_UShort(KUShort arg0, KUShort arg1) { return arg0 | arg1; }
KUInt Kotlin_UShort_xor_UShort(KUShort arg0, KUShort arg1) { return arg0 ^ arg1; }
KUInt Kotlin_UShort_shl_Int(KUShort arg0, KInt arg1) { return arg0 << (arg1 & 31); }
KUInt Kotlin_UShort_shr_Int(KUShort arg0, KInt arg1) { return arg0 >> (arg1 & 31); }
KUShort Kotlin_UShort_inv(KUShort arg0) { return ~arg0; }
KUShort Kotlin_UShort_inc(KUShort arg0) { return ++arg0; }
KUShort Kotlin_UShort_dec(KUShort arg0) { return --arg0; }
KUInt Kotlin_UShort_unaryPlus(KUShort arg0) { return +arg0; }
KUInt Kotlin_UShort_unaryMinus(KUShort arg0) { return -arg0; }
KByte Kotlin_UShort_toByte(KUShort arg0) { return arg0; }
KShort Kotlin_UShort_toShort(KUShort arg0) { return arg0; }
KInt Kotlin_UShort_toInt(KUShort arg0) { return arg0; }
KLong Kotlin_UShort_toLong(KUShort arg0) { return arg0; }
KFloat Kotlin_UShort_toFloat(KUShort arg0) { return arg0; }
KDouble Kotlin_UShort_toDouble(KUShort arg0) { return arg0; }
KChar Kotlin_UShort_toChar(KUShort arg0) { return arg0; }
KUInt Kotlin_UInt_plus_Byte(KUInt arg0, KByte arg1) { return arg0 + arg1; }
KUInt Kotlin_UInt_plus_Short(KUInt arg0, KShort arg1) { return arg0 + arg1; }
KUInt Kotlin_UInt_plus_Int(KUInt arg0, KInt arg1) { return arg0 + arg1; }
KLong Kotlin_UInt_plus_Long(KUInt arg0, KLong arg1) { return arg0 + arg1; }
KFloat Kotlin_UInt_plus_Float(KUInt arg0, KFloat arg1) { return arg0 + arg1; }
KDouble Kotlin_UInt_plus_Double(KUInt arg0, KDouble arg1) { return arg0 + arg1; }
KUInt Kotlin_UInt_minus_Byte(KUInt arg0, KByte arg1) { return arg0 - arg1; }
KUInt Kotlin_UInt_minus_Short(KUInt arg0, KShort arg1) { return arg0 - arg1; }
KUInt Kotlin_UInt_minus_Int(KUInt arg0, KInt arg1) { return arg0 - arg1; }
KLong Kotlin_UInt_minus_Long(KUInt arg0, KLong arg1) { return arg0 - arg1; }
KFloat Kotlin_UInt_minus_Float(KUInt arg0, KFloat arg1) { return arg0 - arg1; }
KDouble Kotlin_UInt_minus_Double(KUInt arg0, KDouble arg1) { return arg0 - arg1; }
KUInt Kotlin_UInt_times_Byte(KUInt arg0, KByte arg1) { return arg0 * arg1; }
KUInt Kotlin_UInt_times_Short(KUInt arg0, KShort arg1) { return arg0 * arg1; }
KUInt Kotlin_UInt_times_Int(KUInt arg0, KInt arg1) { return arg0 * arg1; }
KLong Kotlin_UInt_times_Long(KUInt arg0, KLong arg1) { return arg0 * arg1; }
KFloat Kotlin_UInt_times_Float(KUInt arg0, KFloat arg1) { return arg0 * arg1; }
KDouble Kotlin_UInt_times_Double(KUInt arg0, KDouble arg1) { return arg0 * arg1; }
KUInt Kotlin_UInt_div_Byte(KUInt arg0, KByte arg1) { return div<KUInt>(arg0, arg1); }
KUInt Kotlin_UInt_div_Short(KUInt arg0, KShort arg1) { return div<KUInt>(arg0, arg1); }
KUInt Kotlin_UInt_div_Int(KUInt arg0, KInt arg1) { return div<KUInt>(arg0, arg1); }
KLong Kotlin_UInt_div_Long(KUInt arg0, KLong arg1) { return div<KLong>(arg0, arg1); }
KFloat Kotlin_UInt_div_Float(KUInt arg0, KFloat arg1) { return arg0 / arg1; }
KDouble Kotlin_UInt_div_Double(KUInt arg0, KDouble arg1) { return arg0 / arg1; }
KUInt Kotlin_UInt_rem_Byte(KUInt arg0, KByte arg1) { return arg0 % arg1; }
KUInt Kotlin_UInt_rem_Short(KUInt arg0, KShort arg1) { return arg0 % arg1; }
KUInt Kotlin_UInt_rem_Int(KUInt arg0, KInt arg1) { return arg0 % arg1; }
KLong Kotlin_UInt_rem_Long(KUInt arg0, KLong arg1) { return arg0 % arg1; }
KFloat Kotlin_UInt_rem_Float(KUInt arg0, KFloat arg1) { return fmodf(arg0, arg1); }
KDouble Kotlin_UInt_rem_Double(KUInt arg0, KDouble arg1) { return fmod(arg0, arg1); }
KInt Kotlin_UInt_compareTo_Byte(KUInt arg0, KByte arg1) { if (arg0 == arg1) return 0; return (arg0 < arg1) ? -1 : 1; }
KInt Kotlin_UInt_compareTo_Short(KUInt arg0, KShort arg1) { if (arg0 == arg1) return 0; return (arg0 < arg1) ? -1 : 1; }
KInt Kotlin_UInt_compareTo_Int(KUInt arg0, KInt arg1) { if (arg0 == arg1) return 0; return (arg0 < arg1) ? -1 : 1; }
KInt Kotlin_UInt_compareTo_Long(KUInt arg0, KLong arg1) { if (arg0 == arg1) return 0; return (arg0 < arg1) ? -1 : 1; }
KInt Kotlin_UInt_compareTo_Float(KUInt arg0, KFloat arg1) { if (arg0 == arg1) return 0; return (arg0 < arg1) ? -1 : 1; }
KInt Kotlin_UInt_compareTo_Double(KUInt arg0, KDouble arg1) { if (arg0 == arg1) return 0; return (arg0 < arg1) ? -1 : 1; }
KUInt Kotlin_UInt_and_UInt(KUInt arg0, KUInt arg1) { return arg0 & arg1; }
KUInt Kotlin_UInt_or_UInt(KUInt arg0, KUInt arg1) { return arg0 | arg1; }
KUInt Kotlin_UInt_xor_UInt(KUInt arg0, KUInt arg1) { return arg0 ^ arg1; }
KUInt Kotlin_UInt_shl_Int(KUInt arg0, KInt arg1) { return arg0 << (arg1 & 31); }
KUInt Kotlin_UInt_shr_Int(KUInt arg0, KInt arg1) { return arg0 >> (arg1 & 31); }
KUInt Kotlin_UInt_inv(KUInt arg0) { return ~arg0; }
KUInt Kotlin_UInt_inc(KUInt arg0) { return ++arg0; }
KUInt Kotlin_UInt_dec(KUInt arg0) { return --arg0; }
KUInt Kotlin_UInt_unaryPlus(KUInt arg0) { return +arg0; }
KUInt Kotlin_UInt_unaryMinus(KUInt arg0) { return -arg0; }
KByte Kotlin_UInt_toByte(KUInt arg0) { return arg0; }
KShort Kotlin_UInt_toShort(KUInt arg0) { return arg0; }
KInt Kotlin_UInt_toInt(KUInt arg0) { return arg0; }
KLong Kotlin_UInt_toLong(KUInt arg0) { return arg0; }
KFloat Kotlin_UInt_toFloat(KUInt arg0) { return arg0; }
KDouble Kotlin_UInt_toDouble(KUInt arg0) { return arg0; }
KChar Kotlin_UInt_toChar(KUInt arg0) { return arg0; }
KULong Kotlin_ULong_plus_Byte(KULong arg0, KByte arg1) { return arg0 + arg1; }
KULong Kotlin_ULong_plus_Short(KULong arg0, KShort arg1) { return arg0 + arg1; }
KULong Kotlin_ULong_plus_Int(KULong arg0, KInt arg1) { return arg0 + arg1; }
KULong Kotlin_ULong_plus_Long(KULong arg0, KLong arg1) { return arg0 + arg1; }
KFloat Kotlin_ULong_plus_Float(KULong arg0, KFloat arg1) { return arg0 + arg1; }
KDouble Kotlin_ULong_plus_Double(KULong arg0, KDouble arg1) { return arg0 + arg1; }
KULong Kotlin_ULong_minus_Byte(KULong arg0, KByte arg1) { return arg0 - arg1; }
KULong Kotlin_ULong_minus_Short(KULong arg0, KShort arg1) { return arg0 - arg1; }
KULong Kotlin_ULong_minus_Int(KULong arg0, KInt arg1) { return arg0 - arg1; }
KULong Kotlin_ULong_minus_Long(KULong arg0, KLong arg1) { return arg0 - arg1; }
KFloat Kotlin_ULong_minus_Float(KULong arg0, KFloat arg1) { return arg0 - arg1; }
KDouble Kotlin_ULong_minus_Double(KULong arg0, KDouble arg1) { return arg0 - arg1; }
KULong Kotlin_ULong_times_Byte(KULong arg0, KByte arg1) { return arg0 * arg1; }
KULong Kotlin_ULong_times_Short(KULong arg0, KShort arg1) { return arg0 * arg1; }
KULong Kotlin_ULong_times_Int(KULong arg0, KInt arg1) { return arg0 * arg1; }
KULong Kotlin_ULong_times_Long(KULong arg0, KLong arg1) { return arg0 * arg1; }
KFloat Kotlin_ULong_times_Float(KULong arg0, KFloat arg1) { return arg0 * arg1; }
KDouble Kotlin_ULong_times_Double(KULong arg0, KDouble arg1) { return arg0 * arg1; }
KULong Kotlin_ULong_div_Byte(KULong arg0, KByte arg1) { return div<KULong>(arg0, arg1); }
KULong Kotlin_ULong_div_Short(KULong arg0, KShort arg1) { return div<KULong>(arg0, arg1); }
KULong Kotlin_ULong_div_Int(KULong arg0, KInt arg1) { return div<KULong>(arg0, arg1); }
KULong Kotlin_ULong_div_Long(KULong arg0, KLong arg1) { return div<KULong>(arg0, arg1); }
KFloat Kotlin_ULong_div_Float(KULong arg0, KFloat arg1) { return arg0 / arg1; }
KDouble Kotlin_ULong_div_Double(KULong arg0, KDouble arg1) { return arg0 / arg1; }
KULong Kotlin_ULong_rem_Byte(KULong arg0, KByte arg1) { return arg0 % arg1; }
KULong Kotlin_ULong_rem_Short(KULong arg0, KShort arg1) { return arg0 % arg1; }
KULong Kotlin_ULong_rem_Int(KULong arg0, KInt arg1) { return arg0 % arg1; }
KULong Kotlin_ULong_rem_Long(KULong arg0, KLong arg1) { return arg0 % arg1; }
KFloat Kotlin_ULong_rem_Float(KULong arg0, KFloat arg1) { return fmodf(arg0, arg1); }
KDouble Kotlin_ULong_rem_Double(KULong arg0, KDouble arg1) { return fmod(arg0, arg1); }
KInt Kotlin_ULong_compareTo_Byte(KULong arg0, KByte arg1) { if (arg0 == arg1) return 0; return (arg0 < arg1) ? -1 : 1; }
KInt Kotlin_ULong_compareTo_Short(KULong arg0, KShort arg1) { if (arg0 == arg1) return 0; return (arg0 < arg1) ? -1 : 1; }
KInt Kotlin_ULong_compareTo_Int(KULong arg0, KInt arg1) { if (arg0 == arg1) return 0; return (arg0 < arg1) ? -1 : 1; }
KInt Kotlin_ULong_compareTo_Long(KULong arg0, KLong arg1) { if (arg0 == arg1) return 0; return (arg0 < arg1) ? -1 : 1; }
KInt Kotlin_ULong_compareTo_Float(KULong arg0, KFloat arg1) { if (arg0 == arg1) return 0; return (arg0 < arg1) ? -1 : 1; }
KInt Kotlin_ULong_compareTo_Double(KULong arg0, KDouble arg1) { if (arg0 == arg1) return 0; return (arg0 < arg1) ? -1 : 1; }
KULong Kotlin_ULong_and_ULong(KULong arg0, KULong arg1) { return arg0 & arg1; }
KULong Kotlin_ULong_or_ULong(KULong arg0, KULong arg1) { return arg0 | arg1; }
KULong Kotlin_ULong_xor_ULong(KULong arg0, KULong arg1) { return arg0 ^ arg1; }
KULong Kotlin_ULong_shl_Int(KULong arg0, KInt arg1) { return arg0 << (arg1 & 63); }
KULong Kotlin_ULong_shr_Int(KULong arg0, KInt arg1) { return arg0 >> (arg1 & 63); }
KULong Kotlin_ULong_inv(KULong arg0) { return ~arg0; }
KULong Kotlin_ULong_inc(KULong arg0) { return ++arg0; }
KULong Kotlin_ULong_dec(KULong arg0) { return --arg0; }
KULong Kotlin_ULong_unaryPlus(KULong arg0) { return +arg0; }
KULong Kotlin_ULong_unaryMinus(KULong arg0) { return -arg0; }
KByte Kotlin_ULong_toByte(KULong arg0) { return arg0; }
KShort Kotlin_ULong_toShort(KULong arg0) { return arg0; }
KInt Kotlin_ULong_toInt(KULong arg0) { return arg0; }
KLong Kotlin_ULong_toLong(KULong arg0) { return arg0; }
KFloat Kotlin_ULong_toFloat(KULong arg0) { return arg0; }
KDouble Kotlin_ULong_toDouble(KULong arg0) { return arg0; }
KChar Kotlin_ULong_toChar(KULong arg0) { return arg0; }


}  // extern "C"
