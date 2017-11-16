/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import ffmpeg.*
import kotlin.system.*
import kotlinx.cinterop.*
import platform.posix.*

enum class State {
    PLAYING,
    STOPPED,
    PAUSED;

    inline fun transition(from: State, to: State, block: () -> Unit): State =
        if (this == from) {
            block()
            to
        } else this
}

enum class PixelFormat {
    INVALID,
    RGB24,
    ARGB32
}

enum class PlayMode {
    VIDEO,
    AUDIO,
    BOTH;

    val useVideo: Boolean get() = this != AUDIO
    val useAudio: Boolean get() = this != VIDEO
}

class VideoPlayer(val requestedSize: Vec2D?) : DisposableContainer() {
    private val video = disposable { SDLVideo() }
    private val audio = disposable { SDLAudio(this) }
    private val input = disposable { SDLInput(this) }
    private val decoder = disposable { DecoderWorker() }
    private val now = arena.alloc<platform.posix.timespec>().ptr

    private var state = State.STOPPED

    val workerId get() = decoder.workerId
    var lastTimeStamp = 0.0
    
    fun stop() {
        state = State.STOPPED
    }

    fun pause() {
        when (state) {
            State.PAUSED -> {
                state = State.PLAYING
                audio.resume()
            }
            State.PLAYING -> {
                state = State.PAUSED
                audio.pause()
            }
            State.STOPPED -> throw Error("Cannot pause in stopped state")
        }
    }

    private fun getTime(): Double {
        clock_gettime(platform.posix.CLOCK_MONOTONIC, now)
        return now.pointed.tv_sec + now.pointed.tv_nsec / 1e9
    }

    fun playFile(fileName: String, mode: PlayMode) {
        println("playFile $fileName")
        val file = AVFile(fileName)
        try {
            file.dumpFormat()
            val info = decoder.initDecode(file.context, mode.useVideo, mode.useAudio)
            val videoSize = requestedSize ?: info.video?.size ?: Vec2D(400, 200)
            info.video?.let { video.start(videoSize) }
            decoder.start(videoSize, video.pixelFormat())
            info.audio?.let { audio.start(it) }
            lastTimeStamp = getTime()
            state = State.PLAYING
            decoder.requestDecodeChunk() // Fill in frame caches.
            while (state != State.STOPPED) {
                // Fetch video
                if (info.hasVideo) playVideoFrame()
                // Audio is being auto-fetched by the audio thread.
                // Check if there are any input.
                input.check()
                // Pause support.
                checkPause()
                // Inter-frame pause, may lead to broken A/V sync, think of better approach.
                if (state == State.PLAYING) syncAV(info)
                if (decoder.done()) stop()
            }
        } finally {
            stop()
            audio.stop()
            video.stop()
            decoder.stop()
            file.dispose()
        }
    }

    private fun playVideoFrame() {
        val frame = decoder.nextVideoFrame() ?: return
        video.nextFrame(frame.buffer.pointed.data!!, frame.lineSize)
        frame.unref()
    }

    private fun checkPause() {
        while (state == State.PAUSED) {
            audio.pause()
            input.check()
            usleep(1 * 1000)
        }
        audio.resume()
    }
    
    private fun syncAV(info: CodecInfo) {
        info.video?.let { video ->
            if (info.hasAudio) {
                // Use sound for A/V sync.
                while (!decoder.audioVideoSynced() && state == State.PLAYING) {
                    usleep(500)
                    input.check()
                }
            } else {
                // Use video FPS for frame rate.
                val now = getTime()
                val delta = now - lastTimeStamp
                if (delta < 1.0 / video.fps) {
                    usleep((1000 * 1000 * (1.0 / video.fps - delta)).toInt())
                }
                lastTimeStamp = now
            }
        } ?: run {
            // For pure sound, playback is driven by demand.
            usleep(10 * 1000)
        }
    }
}

fun main(args: Array<String>) {
    if (args.size < 1) {
        println("usage: koplayer file.ext [<width> <height> | 'video' | 'audio' | 'both']")
        exitProcess(1)
    }
    av_register_all()
    val mode = if (args.size == 2) PlayMode.valueOf(args[1].toUpperCase()) else PlayMode.BOTH
    val requestedSize = if (args.size < 3) null else Vec2D(args[1].toInt(), args[2].toInt())
    val player = VideoPlayer(requestedSize)
    try {
        player.playFile(args[0], mode)
    } finally {
        player.dispose()
    }
}
