#!/usr/bin/env bash

DIR=$(cd "$(dirname "${BASH_SOURCE[0]}" )" && pwd )
PATH=$DIR/../../../dist/bin:$DIR/../../../bin:$PATH

cinterop -def sqlite3.def -o sqlite3.klib
