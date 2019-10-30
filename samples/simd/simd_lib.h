#ifndef SIMD_WRAPPER_SIMD_LIB_H
#define SIMD_WRAPPER_SIMD_LIB_H

//#include <Accelerate/Accelerate.h>
//#include <simd/geometry.h>


#include <inttypes.h>
#ifdef __cplusplus
extern C {
#endif

//typedef __attribute__ ((__vector_size__ (8))) float simdFloat2;
//simdFloat2 getSimd8(float f1, float f2);
//float length(simdFloat2);

//uint8_t __attribute__ ((__vector_size__ (16))) cv_var;
//unsigned char __attribute__ ((__vector_size__ (16))) cv2;
//signed char __attribute__ ((__vector_size__ (16))) cv3;
//char __attribute__ ((__vector_size__ (16))) cv31;
//char volatile const __attribute__ ((__vector_size__ (16))) cv4;

//typedef __attribute__((__ext_vector_type__(2))) float simd_float2;
typedef __attribute__((__ext_vector_type__(2))) float simd_float2;
//typedef __attribute__((__ext_vector_type__(2))) const float simd_float2_c;
simd_float2 sf2;

typedef const float CFloat;
typedef CFloat __attribute__ ((__vector_size__ (16))) CFloatV;
CFloatV const CFloat_var = {1., 3.162, 1., 31.};
const float __attribute__ ((__vector_size__ (16)))    CFloat_var2  = {1., 3.162, 1., 31.};

typedef float vFloat  __attribute__ ((__vector_size__ (16)));

void printVFloat(vFloat v);
vFloat getVFloat(float f0, float f1, float f2, float f3);
float my_simd_distance(vFloat v1, vFloat v2);

#ifdef __cplusplus
}; // extern C {
#endif


#endif //SIMD_WRAPPER_SIMD_LIB_H