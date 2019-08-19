	
	../../dist/bin/cinterop -def SkTime.def -o SkTime -compiler-options "-I$HOME/work/cpptools/skia"

	g++ -std=c++14 main.cpp  -I$HOME/work/cpptools/skia /Users/vdi/work/cpptools/skia/out/Static/libskia.a -framework CoreServices -framework CoreText -framework CoreGraphicsl
	
	../../dist/bin/kotlinc src/skiaMain/kotlin/sktimeSample.kt  -o sktimeSample -l SkTime  -linker-options "$HOME/work/cpptools/skia/out/Static/libskia.a"  -linker-options "-framework CoreServices" -linker-options "-framework CoreText" -linker-options "-framework CoreGraphics"
