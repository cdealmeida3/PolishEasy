package com.polisheasy.audio

import java.io.File
import java.io.PrintStream
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    // Without this a Gradle JavaExec prints Polish letters as question marks.
    val out = PrintStream(System.out, true, "UTF-8")
    val err = PrintStream(System.err, true, "UTF-8")
    exitProcess(runMakeClips(args.toList(), File("."), System.getenv(KEY_VARIABLE), ::GoogleVoice, ::FfmpegEncoder, out, err))
}
