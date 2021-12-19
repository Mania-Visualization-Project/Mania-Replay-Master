package me.replaymaster

import java.io.IOException
import java.lang.RuntimeException

interface BaseException {
    val errorCode: String
}

class ModeNotSupportedException(val mode: String) : IOException(
        "Not support this kind of map: $mode"
), BaseException {
    override val errorCode: String = "mode_not_support"
}

class ReplayModeNotMatchedException(mode: String, beatmapMode: String) : IOException(
        "Replay mode ($mode) doesn't match the beatmap's mode ($beatmapMode)."
), BaseException {
    override val errorCode: String = "convert_not_support"
}

class VideoGenerationException(rootCause: Throwable) : RuntimeException(
        "Generate file failed!",
        rootCause
), BaseException {
    override val errorCode: String = "render_failed"
}

class BeatmapNotFoundException(md5: String) : IOException(
        "Cannot find the beatmap with the given MD5: $md5"
), BaseException {
    override val errorCode: String = "beatmap_not_found"
}

class InvalidBeatmapException(beatmapPath: String, rootCause: Throwable) : IOException(
        "Invalid beatmap file: $beatmapPath", rootCause
), BaseException {
    override val errorCode: String = "beatmap_invalid"
}

class InvalidReplayException(replayPath: String, rootCause: Throwable) : IOException(
        "Invalid replay file: $replayPath", rootCause
), BaseException {
    override val errorCode: String = "replay_invalid"
}
