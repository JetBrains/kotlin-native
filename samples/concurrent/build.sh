#!/usr/bin/env bash


DIR=$(cd "$(dirname "${BASH_SOURCE[0]}" )" && pwd )
PATH=$DIR/../../dist/bin:$DIR/../../bin:$PATH

if [ x$TARGET == x ]; then
case "$OSTYPE" in
  darwin*)  TARGET=macbook ;;
  linux*)   TARGET=linux ;;
  *)        echo "unknown: $OSTYPE" && exit 1;;
esac
fi

var=CFLAGS_${TARGET}
CFLAGS=${!var}
var=LINKER_ARGS_${TARGET}
LINKER_ARGS=${!var}
var=COMPILER_ARGS_${TARGET}
COMPILER_ARGS=${!var} # add -opt for an optimized build.

mkdir -p $DIR/build/c_interop
mkdir -p $DIR/build/bin

$DIR/buildCpp.sh

cinterop -def $DIR/src/main/c_interop/MessageChannel.def -copt "-I$DIR/src/main/cpp" -target $TARGET \
         -o $DIR/build/c_interop/MessageChannel.kt.bc || exit 1

konanc $DIR/src/main/kotlin/Concurrent.kt -library $DIR/build/c_interop/MessageChannel.kt.bc \
       -nativelibrary $DIR/build/clang/MessageChannel.bc -o $DIR/build/bin/Concurrent.kexe || exit 1

echo "Artifact path is $DIR/build/bin/Concurrent.kexe"
