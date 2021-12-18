package me.replaymaster

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import me.replaymaster.model.Config
import kotlin.math.pow

object UpdateHelper {

    var latestVersion: String? = null
    var latestPackageUrl: String? = null

    fun checkUpdate() {
        if (Config.INSTANCE.isServer || Config.INSTANCE.isDesktop) {
            return
        }
        Thread {
            val result = Monitor.request("latest", "GET", null)
            try {
                if (result != null) {
                    val resultInfo = Gson().fromJson(result, UpdateInfo::class.java)
                    latestPackageUrl = resultInfo.data.packageUrl
                    latestVersion = resultInfo.data.version
                }
            } catch (ignore: Throwable) {

            }
        }.start()
    }

    fun notifyLatestVersionIfNecessary() {
        if (Config.INSTANCE.isServer || Config.INSTANCE.isDesktop) {
            return
        }
        try {
            if (latestPackageUrl != null && latestVersion != null) {
                if (toVersionCode(latestVersion) > toVersionCode(
                                RESOURCE_BUNDLE.getString("app.version").replace("v", "")
                        )) {
                    println()
                    logLine("app.update", latestVersion, latestPackageUrl)
                    println()
                }
            }
        } catch (ignore: Throwable) {

        }
    }

    fun toVersionCode(version: String?): Double {
        version ?: return -1.0
        val code = version.split(".").mapIndexed { index: Int, piece: String ->
            piece.toDouble() / (100.0.pow(index.toDouble()))
        }
                .sum()
        debug("version code: $version -> $code")
        return code
    }
}

data class UpdateInfo(
        @SerializedName("data")
        val data: UpdateData
)

data class UpdateData(
        @SerializedName("version")
        val version: String,
        @SerializedName("package_url")
        val packageUrl: String
)
