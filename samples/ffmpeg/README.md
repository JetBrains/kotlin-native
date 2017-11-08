# Simple video player

 This example shows how one could implement a video player in Kotlin.
Almost any video file supported by ffmpeg could be played with it.
ffmpeg and SDL2 is needed for that to work, i.e.

     apt install libavcodec-dev libavformat-dev libavutil-dev libswscale-dev
     apt install libsdl2-dev

To build use `../gradlew build`.

To run use `./build/konan/bin/<platform>/Player.kexe file.mp4`.


