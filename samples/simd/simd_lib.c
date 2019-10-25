#include "simd_lib.h"
#include <Accelerate/Accelerate.h>
#include <simd/simd.h>


#include <stdio.h>
////typedef __attribute__((__ext_vector_type__(2))) float simd_float2
//simd_float2 getSimd8(float f1, float f2) {
//	simd_float2 f = {f1, f2};
//	return f;
//}
//
//float length(simd_float2 f) {
//    fprintf(stderr, "length> (%f, %f)\n", f[0], f[1]);
//	float len = simd_length((simd_float2)f);
//    fprintf(stderr, "length(%f, %f) = %f!\n", f[0], f[1], len);
//    return len;
//}

vFloat getVFloat(float f0, float f1, float f2, float f3) {
	vU128 v;
	vFloat ret = {f0, f1, f2, f3};
	printVFloat(ret);
	return ret;
}

float my_simd_distance(vFloat v1, vFloat v2) {
	fprintf(stderr, "v1(%f, %f, %f, %f)\n", v1[0], v1[1], v1[2], v1[3]);
	fprintf(stderr, "v2(%f, %f, %f, %f)\n", v2[0], v2[1], v2[2], v2[3]);
	float dist = simd_distance(v1, v2);
	fprintf(stderr, "simd_distance = %f\n", dist);
	return dist;
}

void printVFloat(vFloat v) {
	fprintf(stderr, "vFloat(%f, %f, %f, %f)\n", v[0], v[1], v[2], v[3]);
}

void Kotlin_Vector_set(vFloat* thiz, uint32_t index, float value) {
	if (index < 4) {
		((float*)thiz)[index] = value;
	}
}

void setDouble(double x) {
	static double f;
	f = x;
}

#ifdef MAIN

typedef __int128_t i128;

int main(void) {
//	length( getSimd8(3, 4) );

	vFloat v1 = getVFloat(-1,0,0,-7); //  {-1,0,0,-7};
	vFloat v2 = getVFloat(1,4,3,7);
	float dist = my_simd_distance(v1, v2);

//	i128 v1 = (i128)getVFloat(-1,0,0,-7); //  {-1,0,0,-7};
//	i128 v2 = (i128)getVFloat(1,4,3,7);
//	float dist = my_simd_distance((vFloat)v1, (vFloat)v2);

	vFloat f = {0.1, 1.3, 2.6, 3.8};
	vFloat ceil = vceilf(f);
	printVFloat(ceil);

	vFloat f2 = {1, 3.162, 10, 31};
	vFloat lg = vlog10f(f2);
	printVFloat(ceil);

	return 0;
}

#endif
