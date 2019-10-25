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

typedef float                   vFloat          __attribute__ ((__vector_size__ (16)));

void printVFloat(vFloat v);
vFloat getVFloat(float f0, float f1, float f2, float f3);
float my_simd_distance(vFloat v1, vFloat v2);

#ifdef __cplusplus
}; // extern C {
#endif


#endif //SIMD_WRAPPER_SIMD_LIB_H