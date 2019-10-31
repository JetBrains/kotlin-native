#ifndef SIMD_WRAPPER_SIMD_LIB_H
#define SIMD_WRAPPER_SIMD_LIB_H

//#include <Accelerate/Accelerate.h>
//#include <simd/geometry.h>


#include <inttypes.h>
#ifdef __cplusplus
extern C {
#endif

typedef __attribute__((__ext_vector_type__(4))) float simd_float4;
simd_float4 f4;

//uint8_t __attribute__ ((__vector_size__ (16))) cv_var;
//unsigned char __attribute__ ((__vector_size__ (16))) cv2;
//signed char __attribute__ ((__vector_size__ (16))) cv3;
//char __attribute__ ((__vector_size__ (16))) cv31;
//char volatile const __attribute__ ((__vector_size__ (16))) cv4;

typedef __attribute__((__ext_vector_type__(2))) float simd_float2;
simd_float2 sf2;  // not supported

typedef float vFloat  __attribute__ ((__vector_size__ (16)));

void printVFloat(vFloat v);
vFloat getVFloat(float f0, float f1, float f2, float f3);
float my_simd_distance(vFloat v1, vFloat v2);

#ifdef __cplusplus
}; // extern C {
#endif


#endif //SIMD_WRAPPER_SIMD_LIB_H