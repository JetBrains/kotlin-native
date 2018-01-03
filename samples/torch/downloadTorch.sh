#!/usr/bin/env bash

TH_TARGET_DIRECTORY=~/.konan/third-party/torch
BUILD_DIRECTORY=build_th

if [ ! -d $TH_TARGET_DIRECTORY/include/TH ]; then
    git clone https://github.com/torch/torch7

    mkdir $BUILD_DIRECTORY
    cd $BUILD_DIRECTORY

    cmake ../torch7/lib/TH
    make
    make DESTDIR=$TH_TARGET_DIRECTORY install

    cd ..
    rm -dr $BUILD_DIRECTORY torch7

    cd $TH_TARGET_DIRECTORY
    # remove the usr/local prefix produced by make:
    mv usr/local/* .
    rm -d usr/local usr
fi
