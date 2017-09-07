#!/usr/bin/env bash

DIR=$(cd "$(dirname "${BASH_SOURCE[0]}" )" && pwd )
PATH=$DIR/../../../dist/bin:$DIR/../../../bin:$PATH

konanc HttpServer.kt -library $DIR/microhttpd/microhttpd.klib -library $DIR/../json/jansson.klib -linkerOpts "$DIR/microhttpd/osx/libmicrohttpd.a" -o HttpServer
