// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

// Common defines and includes needed by all J2ObjC header files.

#ifndef _J2OBJC_HEADER_H_
#define _J2OBJC_HEADER_H_

#import "IOSObjectArray.h"
#import "J2ObjC_common.h"
#import "JavaObject.h"
#import "NSObject+JavaObject.h"

CF_EXTERN_C_BEGIN

id JreStrAppend(__unsafe_unretained id *lhs, const char *types, ...);
id JreStrAppendStrong(__strong id *lhs, const char *types, ...);
id JreStrAppendVolatile(volatile_id *lhs, const char *types, ...);
id JreStrAppendVolatileStrong(volatile_id *lhs, const char *types, ...);
id JreStrAppendArray(JreArrayRef lhs, const char *types, ...);

CF_EXTERN_C_END

/*!
 * Defines the getter for a static variable. For class "Foo" and field "bar_"
 * with type "int" the getter will have the following signature:
 *   inline int Foo_get_bar_();
 *
 * @define J2OBJC_STATIC_FIELD_BASIC_GETTER
 * @define J2OBJC_STATIC_FIELD_PRIMITIVE_REF_GETTER
 * @define J2OBJC_STATIC_FIELD_PRIMITIVE_VOLATILE_GETTER
 * @define J2OBJC_STATIC_FIELD_OBJ_VOLATILE_GETTER
 * @param CLASS The class containing the static variable.
 * @param FIELD The name of the static variable prefixed with an underscore.
 * @param TYPE The type of the static variable.
 */
#define J2OBJC_STATIC_FIELD_BASIC_GETTER(CLASS, FIELD, TYPE) \
  __attribute__((always_inline)) inline TYPE CLASS##_get##FIELD() { \
    CLASS##_initialize(); \
    return CLASS##FIELD; \
  }
#define J2OBJC_STATIC_FIELD_PRIMITIVE_REF_GETTER(CLASS, FIELD, TYPE) \
  __attribute__((always_inline)) inline TYPE *CLASS##_getRef##FIELD() { \
    CLASS##_initialize(); \
    return &CLASS##FIELD; \
  }
#define J2OBJC_STATIC_FIELD_PRIMITIVE_VOLATILE_GETTER(CLASS, FIELD, TYPE) \
  __attribute__((always_inline)) inline TYPE CLASS##_get##FIELD() { \
    CLASS##_initialize(); \
    return __c11_atomic_load(&CLASS##FIELD, __ATOMIC_SEQ_CST); \
  }
#define J2OBJC_STATIC_FIELD_OBJ_VOLATILE_GETTER(CLASS, FIELD, TYPE) \
  __attribute__((always_inline)) inline TYPE CLASS##_get##FIELD() { \
    CLASS##_initialize(); \
    return JreLoadVolatileId(&CLASS##FIELD); \
  }

/*!
 * Defines the setter for a static variable with an object type. For class "Foo"
 * and field "bar_" with type "NSString *" the getter will have the following
 * signature:
 *   inline NSString *Foo_set_bar_(NSString *value);
 *
 * @define J2OBJC_STATIC_FIELD_PRIMITIVE_SETTER
 * @define J2OBJC_STATIC_FIELD_PRIMITIVE_VOLATILE_SETTER
 * @define J2OBJC_STATIC_FIELD_OBJ_SETTER
 * @define J2OBJC_STATIC_FIELD_OBJ_VOLATILE_SETTER
 * @param CLASS The class containing the static variable.
 * @param FIELD The name of the static variable prefixed with an underscore.
 * @param TYPE The type of the static variable.
 */
#define J2OBJC_STATIC_FIELD_PRIMITIVE_SETTER(CLASS, FIELD, TYPE) \
  __attribute__((always_inline)) inline TYPE CLASS##_set##FIELD(TYPE value) { \
    CLASS##_initialize(); \
    return CLASS##FIELD = value; \
  }
#define J2OBJC_STATIC_FIELD_PRIMITIVE_VOLATILE_SETTER(CLASS, FIELD, TYPE) \
  __attribute__((always_inline)) inline TYPE CLASS##_set##FIELD(TYPE value) { \
    CLASS##_initialize(); \
    __c11_atomic_store(&CLASS##FIELD, value, __ATOMIC_SEQ_CST); \
    return value; \
  }
#if __has_feature(objc_arc)
#define J2OBJC_STATIC_FIELD_OBJ_SETTER(CLASS, FIELD, TYPE) \
  __attribute__((always_inline)) inline TYPE CLASS##_set##FIELD(TYPE value) { \
    CLASS##_initialize(); \
    return CLASS##FIELD = value; \
  }
#else
#define J2OBJC_STATIC_FIELD_OBJ_SETTER(CLASS, FIELD, TYPE) \
  __attribute__((always_inline)) inline TYPE CLASS##_set##FIELD(TYPE value) { \
    CLASS##_initialize(); \
    return JreStrongAssign(&CLASS##FIELD, value); \
  } \
  __attribute__((always_inline)) inline TYPE CLASS##_setAndConsume##FIELD(TYPE value) { \
    CLASS##_initialize(); \
    return JreStrongAssignAndConsume(&CLASS##FIELD, value); \
  }
#endif
#define J2OBJC_STATIC_FIELD_OBJ_VOLATILE_SETTER(CLASS, FIELD, TYPE) \
  __attribute__((always_inline)) inline TYPE CLASS##_set##FIELD(TYPE value) { \
    CLASS##_initialize(); \
    return JreVolatileStrongAssign(&CLASS##FIELD, value); \
  }

/*!
 * These macros aggregate all the accessors for each type of static field.
 *
 * An underscore is prepended to the field name before calling further macros
 * to prevent macro expansion of the field name.
 *
 * @define J2OBJC_STATIC_FIELD_CONSTANT
 * @define J2OBJC_STATIC_FIELD_PRIMITIVE
 * @define J2OBJC_STATIC_FIELD_PRIMITIVE_VOLATILE
 * @define J2OBJC_STATIC_FIELD_PRIMITIVE_FINAL
 * @define J2OBJC_STATIC_FIELD_OBJ
 * @define J2OBJC_STATIC_FIELD_OBJ_VOLATILE
 * @define J2OBJC_STATIC_FIELD_OBJ_FINAL
 * @param CLASS The class containing the static variable.
 * @param FIELD The name of the static variable.
 * @param TYPE The type of the static variable.
 */
// Java constants do not cause static initialization.
#define J2OBJC_STATIC_FIELD_CONSTANT(CLASS, FIELD, TYPE) \
  __attribute__((always_inline)) inline TYPE CLASS##_get_##FIELD() { \
    return CLASS##_##FIELD; \
  }
#define J2OBJC_STATIC_FIELD_PRIMITIVE(CLASS, FIELD, TYPE) \
  J2OBJC_STATIC_FIELD_BASIC_GETTER(CLASS, _##FIELD, TYPE) \
  J2OBJC_STATIC_FIELD_PRIMITIVE_SETTER(CLASS, _##FIELD, TYPE) \
  J2OBJC_STATIC_FIELD_PRIMITIVE_REF_GETTER(CLASS, _##FIELD, TYPE)
#define J2OBJC_STATIC_FIELD_PRIMITIVE_VOLATILE(CLASS, FIELD, TYPE) \
  J2OBJC_STATIC_FIELD_PRIMITIVE_VOLATILE_GETTER(CLASS, _##FIELD, TYPE) \
  J2OBJC_STATIC_FIELD_PRIMITIVE_VOLATILE_SETTER(CLASS, _##FIELD, TYPE)
#define J2OBJC_STATIC_FIELD_PRIMITIVE_FINAL(CLASS, FIELD, TYPE) \
  J2OBJC_STATIC_FIELD_BASIC_GETTER(CLASS, _##FIELD, TYPE)
#define J2OBJC_STATIC_FIELD_OBJ(CLASS, FIELD, TYPE) \
  J2OBJC_STATIC_FIELD_BASIC_GETTER(CLASS, _##FIELD, TYPE) \
  J2OBJC_STATIC_FIELD_OBJ_SETTER(CLASS, _##FIELD, TYPE)
#define J2OBJC_STATIC_FIELD_OBJ_VOLATILE(CLASS, FIELD, TYPE) \
  J2OBJC_STATIC_FIELD_OBJ_VOLATILE_GETTER(CLASS, _##FIELD, TYPE) \
  J2OBJC_STATIC_FIELD_OBJ_VOLATILE_SETTER(CLASS, _##FIELD, TYPE)
#define J2OBJC_STATIC_FIELD_OBJ_FINAL(CLASS, FIELD, TYPE) \
  J2OBJC_STATIC_FIELD_BASIC_GETTER(CLASS, _##FIELD, TYPE)

/*!
 * Defines the getter for an enum constant. For enum class "FooEnum" and constant "BAR"
 * the getter will have the following signature:
 *   inline Foo *Color_get_BLUE();
 *
 * @define J2OBJC_ENUM_CONSTANT
 * @param CLASS The enum class.
 * @param CONSTANT The name of the enum constant.
 */
#define J2OBJC_ENUM_CONSTANT(CLASS, CONSTANT) \
  __attribute__((always_inline)) inline CLASS *CLASS##_get_##CONSTANT() { \
    CLASS##_initialize(); \
    return CLASS##_values_[CLASS##_Enum_##CONSTANT]; \
  }

#define BOXED_INC_AND_DEC_INNER(CNAME, VALUE_METHOD, TYPE, OPNAME, OP) \
  __attribute__((always_inline)) inline TYPE *JreBoxedPre##OPNAME##CNAME( \
      __unsafe_unretained TYPE **value) { \
    (void)nil_chk(*value); \
    return *value = TYPE##_valueOfWith##CNAME##_([*value VALUE_METHOD] OP 1); \
  } \
  __attribute__((always_inline)) inline TYPE *JreBoxedPre##OPNAME##Strong##CNAME( \
      __strong TYPE **value) { \
    (void)nil_chk(*value); \
    return JreStrongAssign(value, TYPE##_valueOfWith##CNAME##_([*value VALUE_METHOD] OP 1)); \
  } \
  __attribute__((always_inline)) inline TYPE *JreBoxedPre##OPNAME##Volatile##CNAME( \
      volatile_id *value) { \
    TYPE *original = JreLoadVolatileId(value); \
    (void)nil_chk(original); \
    return JreAssignVolatileId(value, TYPE##_valueOfWith##CNAME##_([original VALUE_METHOD] OP 1)); \
  } \
  __attribute__((always_inline)) inline TYPE *JreBoxedPre##OPNAME##VolatileStrong##CNAME( \
      volatile_id *value) { \
    TYPE *original = JreLoadVolatileId(value); \
    (void)nil_chk(original); \
    return JreVolatileStrongAssign(value, \
        TYPE##_valueOfWith##CNAME##_([original VALUE_METHOD] OP 1)); \
  } \
  __attribute__((always_inline)) inline TYPE *JreBoxedPre##OPNAME##Array##CNAME(JreArrayRef ref) { \
    (void)nil_chk(*ref.pValue); \
    return IOSObjectArray_SetRef( \
        ref, TYPE##_valueOfWith##CNAME##_([*((TYPE **)ref.pValue) VALUE_METHOD] OP 1)); \
  } \
  __attribute__((always_inline)) inline TYPE *JreBoxedPost##OPNAME##CNAME( \
      __unsafe_unretained TYPE **value) { \
    (void)nil_chk(*value); \
    TYPE *original = *value; \
    *value = TYPE##_valueOfWith##CNAME##_([*value VALUE_METHOD] OP 1); \
    return original; \
  } \
  __attribute__((always_inline)) inline TYPE *JreBoxedPost##OPNAME##Strong##CNAME( \
      __strong TYPE **value) { \
    (void)nil_chk(*value); \
    TYPE *original = *value; \
    JreStrongAssign(value, TYPE##_valueOfWith##CNAME##_([*value VALUE_METHOD] OP 1)); \
    return original; \
  } \
  __attribute__((always_inline)) inline TYPE *JreBoxedPost##OPNAME##Volatile##CNAME( \
      volatile_id *value) { \
    TYPE *original = JreLoadVolatileId(value); \
    (void)nil_chk(original); \
    JreAssignVolatileId(value, TYPE##_valueOfWith##CNAME##_([original VALUE_METHOD] OP 1)); \
    return original; \
  } \
  __attribute__((always_inline)) inline TYPE *JreBoxedPost##OPNAME##VolatileStrong##CNAME( \
      volatile_id *value) { \
    TYPE *original = JreLoadVolatileId(value); \
    (void)nil_chk(original); \
    JreVolatileStrongAssign(value, TYPE##_valueOfWith##CNAME##_([original VALUE_METHOD] OP 1)); \
    return original; \
  } \
  __attribute__((always_inline)) inline TYPE *JreBoxedPost##OPNAME##Array##CNAME(JreArrayRef ref) { \
    (void)nil_chk(*ref.pValue); \
    TYPE *original = *ref.pValue; \
    IOSObjectArray_SetRef( \
        ref, TYPE##_valueOfWith##CNAME##_([*((TYPE **)ref.pValue) VALUE_METHOD] OP 1)); \
    return original; \
  }

/*!
 * Defines increment and decrement operators on boxed types. The translator will
 * use this macro to add these operators to the headers of the boxed types.
 *
 * @define BOXED_INC_AND_DEC
 * @param CNAME The capitalized name of the primitive type (eg. "Int").
 * @param VALUE_METHOD The method on the boxed type that returns the value.
 * @param TYPE The boxed type name (eg. "JavaLangInteger").
 */
#define BOXED_INC_AND_DEC(CNAME, VALUE_METHOD, TYPE) \
    BOXED_INC_AND_DEC_INNER(CNAME, VALUE_METHOD, TYPE, Incr, +) \
    BOXED_INC_AND_DEC_INNER(CNAME, VALUE_METHOD, TYPE, Decr, -)

#define ADD_OP(a, b) a + b
#define MINUS_OP(a, b) a - b
#define TIMES_OP(a, b) a * b
#define DIVIDE_OP(a, b) a / b
#define REMAINDER_OP(a, b) a % b
#define FREMAINDER_OP(a, b) fmod(a, b)
#define BITAND_OP(a, b) a & b
#define BITOR_OP(a, b) a | b
#define BITXOR_OP(a, b) a ^ b
#define LSHIFT_32_OP(a, b) a << (b & 0x1f)
#define RSHIFT_32_OP(a, b) a >> (b & 0x1f)
#define LSHIFT_64_OP(a, b) a << (b & 0x3f)
#define RSHIFT_64_OP(a, b) a >> (b & 0x3f)

/*!
 * Template for compound assign operators on boxed types.
 *
 * @define BOXED_COMPOUND_ASSIGN
 * @param CNAME The capitalized name of the primitive type (eg. "Int").
 * @param VALUE_METHOD The method on the boxed type that returns the value.
 * @param TYPE The primitive type name (eg. "jint").
 * @param BOXED_TYPE The boxed type name (eg. "JavaLangInteger").
 * @param RTYPE The type of the right hand side of the assignment.
 * @param OPNAME The name of the operator, used to construct the function name.
 * @param OP A macro that takes two parameters and prints the operation.
 * @param OP_LTYPE The cast type for the left hand side of the operation.
 */
#define BOXED_COMPOUND_ASSIGN( \
    CNAME, VALUE_METHOD, TYPE, BOXED_TYPE, RTYPE, OPNAME, OP, OP_LTYPE) \
  __attribute__((always_inline)) inline BOXED_TYPE *JreBoxed##OPNAME##Assign##CNAME( \
      __unsafe_unretained BOXED_TYPE **lhs, RTYPE rhs) { \
    (void)nil_chk(*lhs); \
    return *lhs = BOXED_TYPE##_valueOfWith##CNAME##_( \
        (TYPE)(OP((OP_LTYPE)[*lhs VALUE_METHOD], rhs))); \
  } \
  __attribute__((always_inline)) inline BOXED_TYPE *JreBoxed##OPNAME##AssignStrong##CNAME( \
      __strong BOXED_TYPE **lhs, RTYPE rhs) { \
    (void)nil_chk(*lhs); \
    return JreStrongAssign(lhs, \
        BOXED_TYPE##_valueOfWith##CNAME##_((TYPE)(OP((OP_LTYPE)[*lhs VALUE_METHOD], rhs)))); \
  } \
  __attribute__((always_inline)) inline BOXED_TYPE *JreBoxed##OPNAME##AssignVolatile##CNAME( \
      volatile_id *lhs, RTYPE rhs) { \
    BOXED_TYPE *lhsValue = JreLoadVolatileId(lhs); \
    (void)nil_chk(lhsValue); \
    return JreAssignVolatileId(lhs, \
        BOXED_TYPE##_valueOfWith##CNAME##_((TYPE)(OP((OP_LTYPE)[lhsValue VALUE_METHOD], rhs)))); \
  } \
  __attribute__((always_inline)) inline BOXED_TYPE *JreBoxed##OPNAME##AssignVolatileStrong##CNAME( \
      volatile_id *lhs, RTYPE rhs) { \
    BOXED_TYPE *lhsValue = JreLoadVolatileId(lhs); \
    (void)nil_chk(lhsValue); \
    return JreVolatileStrongAssign(lhs, \
        BOXED_TYPE##_valueOfWith##CNAME##_((TYPE)(OP((OP_LTYPE)[lhsValue VALUE_METHOD], rhs)))); \
  } \
  __attribute__((always_inline)) inline BOXED_TYPE *JreBoxed##OPNAME##AssignArray##CNAME( \
      JreArrayRef lhs, RTYPE rhs) { \
    (void)nil_chk(*lhs.pValue); \
    return IOSObjectArray_SetRef(lhs, BOXED_TYPE##_valueOfWith##CNAME##_( \
        (TYPE)(OP((OP_LTYPE)[*((BOXED_TYPE **)lhs.pValue) VALUE_METHOD], rhs)))); \
  }

// This macros are used in the boxed primitive header files.
#define BOXED_COMPOUND_ASSIGN_ARITHMETIC(CNAME, VALUE_METHOD, TYPE, BOXED_TYPE) \
    BOXED_COMPOUND_ASSIGN(CNAME, VALUE_METHOD, TYPE, BOXED_TYPE, TYPE, Plus, ADD_OP, TYPE) \
    BOXED_COMPOUND_ASSIGN(CNAME, VALUE_METHOD, TYPE, BOXED_TYPE, TYPE, Minus, MINUS_OP, TYPE) \
    BOXED_COMPOUND_ASSIGN(CNAME, VALUE_METHOD, TYPE, BOXED_TYPE, TYPE, Times, TIMES_OP, TYPE) \
    BOXED_COMPOUND_ASSIGN(CNAME, VALUE_METHOD, TYPE, BOXED_TYPE, TYPE, Divide, DIVIDE_OP, TYPE)
#define BOXED_COMPOUND_ASSIGN_MOD(CNAME, VALUE_METHOD, TYPE, BOXED_TYPE) \
    BOXED_COMPOUND_ASSIGN(CNAME, VALUE_METHOD, TYPE, BOXED_TYPE, TYPE, Mod, REMAINDER_OP, TYPE)
#define BOXED_COMPOUND_ASSIGN_FPMOD(CNAME, VALUE_METHOD, TYPE, BOXED_TYPE) \
    BOXED_COMPOUND_ASSIGN(CNAME, VALUE_METHOD, TYPE, BOXED_TYPE, TYPE, Mod, FREMAINDER_OP, TYPE)
#define BOXED_COMPOUND_ASSIGN_BITWISE(CNAME, VALUE_METHOD, TYPE, BOXED_TYPE) \
    BOXED_COMPOUND_ASSIGN(CNAME, VALUE_METHOD, TYPE, BOXED_TYPE, TYPE, BitAnd, BITAND_OP, TYPE) \
    BOXED_COMPOUND_ASSIGN(CNAME, VALUE_METHOD, TYPE, BOXED_TYPE, TYPE, BitOr, BITOR_OP, TYPE) \
    BOXED_COMPOUND_ASSIGN(CNAME, VALUE_METHOD, TYPE, BOXED_TYPE, TYPE, BitXor, BITXOR_OP, TYPE)
#define BOXED_SHIFT_ASSIGN_32(CNAME, VALUE_METHOD, TYPE, BOXED_TYPE) \
    BOXED_COMPOUND_ASSIGN( \
        CNAME, VALUE_METHOD, TYPE, BOXED_TYPE, jlong, LShift, LSHIFT_32_OP, jint) \
    BOXED_COMPOUND_ASSIGN( \
        CNAME, VALUE_METHOD, TYPE, BOXED_TYPE, jlong, RShift, RSHIFT_32_OP, jint) \
    BOXED_COMPOUND_ASSIGN( \
        CNAME, VALUE_METHOD, TYPE, BOXED_TYPE, jlong, URShift, RSHIFT_32_OP, uint32_t)
#define BOXED_SHIFT_ASSIGN_64(CNAME, VALUE_METHOD, TYPE, BOXED_TYPE) \
    BOXED_COMPOUND_ASSIGN( \
        CNAME, VALUE_METHOD, TYPE, BOXED_TYPE, jlong, LShift, LSHIFT_64_OP, jlong) \
    BOXED_COMPOUND_ASSIGN( \
        CNAME, VALUE_METHOD, TYPE, BOXED_TYPE, jlong, RShift, RSHIFT_64_OP, jlong) \
    BOXED_COMPOUND_ASSIGN( \
        CNAME, VALUE_METHOD, TYPE, BOXED_TYPE, jlong, URShift, RSHIFT_64_OP, uint64_t)

#endif  // _J2OBJC_HEADER_H_
