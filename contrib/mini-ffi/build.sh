FILES="prep_cif.c ffi.c sysV-thumb.S"
CFLAGS="-mabi=aapcs -mthumb -mcpu=cortex-m4 -fno-pie -fno-pic"
CC=/Users/jetbrains/.konan/dependencies/gcc-arm-none-eabi-7-2017-q4-major-mac/bin/arm-none-eabi-gcc
AR=/Users/jetbrains/.konan/dependencies/gcc-arm-none-eabi-7-2017-q4-major-mac/bin/arm-none-eabi-ar

for i in $FILES; do $CC $CFLAGS -I. -c $i; done
$AR rcs libffi.a *.o
