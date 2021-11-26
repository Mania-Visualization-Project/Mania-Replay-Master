package me.replaymaster

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import me.replaymaster.model.Config
import java.io.*
import java.net.HttpURLConnection
import java.net.URL


object Monitor {

    fun reportTask(startTime: Long, beatMap: File, replay: File, bgm: File?, error: String) {
        if (Config.INSTANCE.isServer) {
            return
        }
        Thread {
            request("report_offline", "POST", Gson().toJson(ReportContent(
                    map = beatMap.name, replay = replay.name, bgm = bgm?.name ?: "",
                    startTime = startTime, endTime = System.currentTimeMillis(),
                    version = RESOURCE_BUNDLE.getString("app.version"), error = error
            )))
        }.start()
    }

    fun request(api: String, method: String, data: String?): String? {
        try {
            val url = URL("http://1.116.195.211/mania/api/" + api)
            val conn: HttpURLConnection = url.openConnection() as HttpURLConnection
            var out: PrintWriter? = null
            conn.setRequestMethod(method)
            conn.setRequestProperty("accept", "*/*")
            conn.setRequestProperty("connection", "Keep-Alive")
            conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1)")
            conn.setDoOutput(true)
            if (data != null) {
                conn.setDoInput(true)
                out = PrintWriter(conn.getOutputStream())
                out.print(data)
                out.flush()
            }
            val `is`: InputStream = conn.getInputStream()
            val br = BufferedReader(InputStreamReader(`is`))
            var str: String? = ""
            val sb = StringBuilder()
            while (br.readLine().also { str = it } != null) {
                sb.appendLine(str)
            }
            `is`.close()
            conn.disconnect()
            val result = sb.toString()
            debug("$method $api ($data) -> $sb")
            return result
        } catch (e: Exception) {
            if (Config.INSTANCE.debug) {
                e.printStackTrace()
            }
            return null
        }
    }
}

class ReportContent(
        @SerializedName("map")
        val map: String,
        @SerializedName("replay")
        val replay: String,
        @SerializedName("bgm")
        val bgm: String,
        @SerializedName("start_time")
        val startTime: Long,
        @SerializedName("end_time")
        val endTime: Long,
        @SerializedName("version")
        val version: String,
        @SerializedName("error")
        val error: String
)