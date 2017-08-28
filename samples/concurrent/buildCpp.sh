#!/usr/bin/env bash

DIR=$(cd "$(dirname "${BASH_SOURCE[0]}" )" && pwd )
PATH="$DIR/../../dist/bin:$DIR/../../bin:$PATH"
DEPS="$(dirname `type -p konanc`)/../dependencies"

if [ x$TARGET == x ]; then
case "$OSTYPE" in
  darwin*)  TARGET=macbook ;;
  linux*)   TARGET=linux ;;
  *)        echo "unknown: $OSTYPE" && exit 1;;
esac
fi

CLANG_PATH_linux=$DEPS/clang-llvm-3.9.0-linux-x86-64/bin/
CLANG_PATH_macbook=$DEPS/clang-llvm-3.9.0-darwin-macos/bin/

var=CLANG_PATH_${TARGET}
CLANG_PATH=${!var}
PATH="$PATH:$CLANG_PATH"

mkdir -p $DIR/build/clang/

clang++ -std=c++11 -c $DIR/src/main/cpp/MessageChannel.cpp -o $DIR/build/clang/MessageChannel.bc -emit-llvm || exit 1
