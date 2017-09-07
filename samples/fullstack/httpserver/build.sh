#!/usr/bin/env bash

DIR=$(cd "$(dirname "${BASH_SOURCE[0]}" )" && pwd )
PATH=$DIR/../../../dist/bin:$DIR/../../../bin:$PATH

konanc HttpServer.kt -library $DIR/microhttpd/microhttpd.klib \
                     -library $DIR/../json/jansson.klib \
                     -library $DIR/../sql/sqlite3.klib \
                     -o HttpServer
