cmake_minimum_required(VERSION 3.15)
project(simd_wrapper C)

set(CMAKE_C_STANDARD 99)

add_library(simd_wrapper simd_lib.c simd_lib.h)

add_executable(ctest simd_lib.c simd_lib.h)
target_compile_definitions(ctest PUBLIC "-DMAIN")
