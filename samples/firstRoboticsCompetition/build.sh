#!/usr/bin/env bash

DIR=$(cd "$(dirname "${BASH_SOURCE[0]}" )" && pwd )

source "$DIR/../konan.sh"

$DIR/downloadWpilib.sh

# KONAN_USER_DIR is set by konan.sh
WPI_TARGET_DIRECTORY="$KONAN_USER_DIR/third-party/allwpilib"

mkdir -p $DIR/build/c_interop/
mkdir -p $DIR/build/bin/

cinterop -def $DIR/src/main/c_interop/hal.def -target frc \
     -copt -I$WPI_TARGET_DIRECTORY/hal/src/main/native/include \
	 -o $DIR/build/c_interop/hal || exit 1

LINK_ARGS="-L$WPI_TARGET_DIRECTORY/ni-libraries/lib \
-L$WPI_TARGET_DIRECTORY/wpiutil/build/libs/wpiutil/shared/athena/ \
-L$WPI_TARGET_DIRECTORY/hal/build/libs/hal/shared/athena/ \
-lwpiHal -lwpiutil -lvisa \
-l:libFRC_NetworkCommunication.so.18.0.0 -l:libNiFpga.so.17.0.0 -l:libNiFpgaLv.so.17.0.0 -l:libNiRioSrv.so.17.0.0 \
-l:libRoboRIO_FRC_ChipObject.so.18.0.0 -l:libniriodevenum.so.17.0.0 -l:libniriosession.so.17.0.0"

konanc -target frc $DIR/src/main/kotlin/HelloFRC.kt \
       -library $DIR/build/c_interop/hal \
       -o $DIR/build/bin/FRCUserProgram \
       -linkerOpts "$LINK_ARGS" || exit 1

echo "Artifact path is $DIR/build/bin/FRCUserProgram.kexe"