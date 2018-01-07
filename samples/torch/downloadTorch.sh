#!/usr/bin/env bash

TH_TARGET_DIRECTORY=~/.konan/third-party/torch
BUILD_TH=build_th
NO_CUDA=true # set to false for GPU support

if [ ! -d $TH_TARGET_DIRECTORY/include/THNN ]; then
    # git clone https://github.com/torch/torch7
    # git clone https://github.com/torch/nn
    # git clone https://github.com/pytorch/pytorch.git

    # sudo python -m ensurepip --default-pip
    # sudo python -m pip install pyyaml

    mkdir $BUILD_TH
    cd $BUILD_TH

    cmake -NO_CUDA=$NO_CUDA ../pytorch/aten
    make
    make DESTDIR=$TH_TARGET_DIRECTORY install

    cd ..
    rm -dr # $BUILD_TH $BUILD_THNN # torch7 nn

    cd $TH_TARGET_DIRECTORY
    # remove the usr/local prefix produced by make:
    mv usr/local/* .
    rm -d usr/local usr

    # hack to solve "fatal error: 'generic/THNN.h' file not found" when linking, -I$<DIR>/include/THNN did not work
    cp include/THNN/generic/THNN.h include/TH/generic/THNN.h

    # hack to avoid "dyld: Library not loaded: @rpath/libATen.1.dylib" when running
    # (happens although libATen.1.dylib is in same folder as libATen.dylib, why?)
    cp .konan/third-party/torch/lib/libATen.1.dylib /usr/local/lib
fi
