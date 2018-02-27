#!/usr/bin/env bash

BOARD_ARCH=arm_cortex_m3
BOARD_NAME=qemu_cortex_m3
export ZEPHYR_BASE="PLEASE_SET_ZEPHYR_BASE"
#export ZEPHYR_BASE="$HOME/zephyr"
#export PATH="$HOME/cmake-3.8.1/bin:$PATH"

if [ "$ZEPHYR_BASE" == "PLEASE_SET_ZEPHYR_BASE" ] ; then
    echo "Please set ZEPHYR_BASE in this build.sh."
    exit 1
fi

DIR=$(cd "$(dirname "${BASH_SOURCE[0]}" )" && pwd )

source "$DIR/../konan.sh"

if [ x$TARGET == x ]; then
case "$OSTYPE" in
  darwin*)  TOOLCHAIN=gcc-arm-none-eabi-7-2017-q4-major-mac ;;
  linux*)   TOOLCHAIN=gcc-arm-none-eabi-7-2017-q4-major-linux ;;
  *)        echo "unknown: $OSTYPE" && exit 1;;
esac
fi

GCC_ARM="$KONAN_DEPS/$TOOLCHAIN"

rm -rf $DIR/build || exit 1
mkdir -p $DIR/build && cd $DIR/build

export ZEPHYR_TOOLCHAIN_VARIANT=gccarmemb
export GCCARMEMB_TOOLCHAIN_PATH=$GCC_ARM

[ -f CMakeCache.txt ] || cmake -GNinja -DBOARD=$BOARD_NAME .. || exit 1

# We need generated headers to be consumed by `cinterop`,
# so we preconfigure the project with `make zephyr`.
ninja zephyr

rm -f program.o

mkdir -p $DIR/build/kotlin

konanc $DIR/src/main.kt \
        -target zephyr_$BOARD_ARCH \
        -opt -g -o $DIR/build/kotlin/program || exit 1

ninja run || exit 1
