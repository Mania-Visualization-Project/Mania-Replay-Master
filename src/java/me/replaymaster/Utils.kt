package me.replaymaster

import me.replaymaster.model.BeatMap
import me.replaymaster.model.Config
import me.replaymaster.model.ReplayModel
import net.lingala.zip4j.ZipFile
import java.io.File
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*

val RESOURCE_BUNDLE = ResourceBundle.getBundle("res/language", Utf8Control())!!

fun logLine(key: String, vararg args: Any? = arrayOf()) {
    if (Config.INSTANCE.isDesktop) {
        return
    }
    if (args.size == 0) {
        println(RESOURCE_BUNDLE.getString(key))
    } else {
        println(String.format(RESOURCE_BUNDLE.getString(key), *args))
    }
}

fun debug(content: String) {
    if (Config.INSTANCE.debug) {
        println("[DEBUG] $content")
    }
}

fun warning(key: String, vararg args: Any? = arrayOf()) {
    logLine(key, *args)
    if (Config.INSTANCE.isDesktop) {
        println("warning: $key")
    }
}

fun adjust(beatMap: BeatMap, replayModel: ReplayModel) {
    if (replayModel.rate != 1.0) {
        logLine("rate.scale", replayModel.rate)
        replayModel.replayData.forEach {
            it.scale(replayModel.rate)
        }
        beatMap.notes.forEach {
            it.scale(replayModel.rate)
        }
    }

    if (replayModel.mirror) {
        logLine("adjust.mirror")
        beatMap.notes.forEach {
            it.column = beatMap.key - it.column - 1
        }
    }
}

fun getMD5(file: File): String? {
    try {
        val instance: MessageDigest = MessageDigest.getInstance("MD5")
        val digest: ByteArray = instance.digest(file.readBytes())
        val sb = StringBuffer()
        for (b in digest) {
            val i: Int = b.toInt() and 0xff
            var hexString = Integer.toHexString(i)
            if (hexString.length < 2) {
                hexString = "0" + hexString
            }
            sb.append(hexString)
        }
        return sb.toString()

    } catch (e: NoSuchAlgorithmException) {
        e.printStackTrace()
    }

    return null
}

fun unzip(zipFilePath: String, desDirectory: String) {
    ZipFile(zipFilePath).extractAll(desDirectory)
//    val desDir = File(desDirectory)
//    if (!desDir.exists()) {
//        check(desDir.mkdir())
//    }
//    val zipInputStream = ZipInputStream(FileInputStream(zipFilePath))
//    var zipEntry = zipInputStream.nextEntry
//    while (zipEntry != null) {
//        val unzipFilePath = desDirectory + File.separator + zipEntry.name
//        if (zipEntry.isDirectory) {
//            mkdir(File(unzipFilePath))
//        } else {
//            val file = File(unzipFilePath)
//            mkdir(file.parentFile)
//            val bufferedOutputStream = BufferedOutputStream(FileOutputStream(unzipFilePath))
//            val bytes = ByteArray(1024)
//            var readLen: Int
//            while (zipInputStream.read(bytes).also { readLen = it } > 0) {
//                bufferedOutputStream.write(bytes, 0, readLen)
//            }
//            bufferedOutputStream.close()
//        }
//        zipInputStream.closeEntry()
//        zipEntry = zipInputStream.nextEntry
//    }
}

//private fun mkdir(file: File) {
//    if (file.exists()) {
//        return
//    } else {
//        file.parentFile.mkdir()
//        file.mkdir()
//    }
//}