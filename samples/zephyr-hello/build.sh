#!/usr/bin/env bash

BOARD_ARCH=arm_cortex_m3
BOARD_NAME=qemu_cortex_m3

if [ -z "ZEPHYR_BASE" ]; then
    export ZEPHYR_BASE="PLEASE_SET_ZEPHYR_BASE"
fi

if [ "$ZEPHYR_BASE" == "PLEASE_SET_ZEPHYR_BASE" ] ; then
    echo "Please set ZEPHYR_BASE in the environment or this build.sh."
    exit 1
fi

export KONAN_DATA_DIR=$HOME/.konan
export KONAN_DEPS=$KONAN_DATA_DIR/dependencies

DIR=$(cd "$(dirname "${BASH_SOURCE[0]}" )" && pwd )

if [ -z "$KONAN_HOME" ]; then
    PATH="$DIR/../../dist/bin:$DIR/../../bin:$PATH"
else
    PATH="$KONAN_HOME/bin:$PATH"
fi

if [ x$TARGET == x ]; then
case "$OSTYPE" in
  darwin*)  TOOLCHAIN=gcc-arm-none-eabi-7-2017-q4-major-mac ;;
  linux*)   TOOLCHAIN=gcc-arm-none-eabi-7-2017-q4-major-linux ;;
  *)        echo "unknown: $OSTYPE" && exit 1;;
esac
fi

GNU_ARM="$KONAN_DEPS/$TOOLCHAIN"

rm -rf $DIR/build || exit 1
mkdir -p $DIR/build && cd $DIR/build

export ZEPHYR_TOOLCHAIN_VARIANT=gnuarmemb
export GNUARMEMB_TOOLCHAIN_PATH=$GNU_ARM

[ -f CMakeCache.txt ] || cmake -GNinja -DBOARD=$BOARD_NAME .. || exit 1

rm -f program.o

mkdir -p $DIR/build/kotlin

konanc $DIR/src/main.kt \
        -target zephyr_$BOARD_ARCH \
        -g -o $DIR/build/kotlin/program || exit 1

ninja run || exit 1
