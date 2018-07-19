#!/usr/bin/env bash

KONAN_USER_DIR=${KONAN_DATA_DIR:-"$HOME/.konan"}
WPI_TARGET_DIRECTORY="$KONAN_USER_DIR/third-party/allwpilib"

if [ ! -d $WPI_TARGET_DIRECTORY ]; then
 echo "Installing WPILib into $WPI_TARGET_DIRECTORY ..."
 mkdir -p $WPI_TARGET_DIRECTORY
 cd $WPI_TARGET_DIRECTORY/../
 git clone https://github.com/wpilibsuite/allwpilib.git # C-compatable headers aren't published yet, so we must clone
 cd $WPI_TARGET_DIRECTORY
 ./gradlew :hal:build
fi