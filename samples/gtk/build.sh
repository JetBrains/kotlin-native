#!/usr/bin/env bash

DIR=$(cd "$(dirname "${BASH_SOURCE[0]}" )" && pwd )

source "$DIR/../konan.sh"

IPREFIX_macbook=-I/opt/local/include
IPREFIX_linux=-I/usr/include

if [ x$TARGET == x ]; then
case "$OSTYPE" in
  darwin*)  TARGET=macbook ;;
  linux*)   TARGET=linux ;;
  *)        echo "unknown: $OSTYPE" && exit 1;;
esac
fi

var=IPREFIX_${TARGET}
IPREFIX="${!var}"

mkdir -p $DIR/build/c_interop/
mkdir -p $DIR/build/bin/

echo "Generating GTK stubs, may take few mins depending on the hardware..."
cinterop -J-Xmx8g -copt $IPREFIX/atk-1.0 -copt $IPREFIX/gdk-pixbuf-2.0 -copt $IPREFIX/cairo -copt $IPREFIX/pango-1.0 \
         -copt -I/opt/local/lib/glib-2.0/include -copt -I/usr/lib/x86_64-linux-gnu/glib-2.0/include \
         -copt $IPREFIX/gtk-3.0 -copt $IPREFIX/glib-2.0 -def $DIR/src/main/c_interop/gtk3.def \
         -target $TARGET -o $DIR/build/c_interop/gtk3 || exit 1

konanc -target $TARGET $DIR/src/main/kotlin -library $DIR/build/c_interop/gtk3 \
       -o $DIR/build/bin/Gtk3Demo || exit 1

echo "Artifact path is $DIR/build/bin/Gtk3Demo.kexe"
