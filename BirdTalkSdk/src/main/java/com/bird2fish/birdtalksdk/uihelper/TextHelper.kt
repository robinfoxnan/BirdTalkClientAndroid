package com.bird2fish.birdtalksdk.uihelper

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.core.net.toFile
import androidx.core.net.toUri
import java.io.File
import java.net.MalformedURLException
import java.net.URL
import java.text.DecimalFormat
import java.util.Locale
import kotlin.math.floor
import kotlin.math.pow

object  TextHelper {

    private val mAppName: String? = "BirdTalk"
    private val mOsVersion:String? = "6.0"

    // 写入二进制文件
    fun writeToBinaryFile(context: Context, data: ByteArray, fileName: String):Int {
        try {
            val uri = File(context.getExternalFilesDir(null), fileName).toUri()
            val file: File = uri.toFile()
            //file.writeText(text)
            return file.writeBytes(data) as Int

        } catch (e: Exception) {
            e.printStackTrace()
            //throw RuntimeException("Failed to write to file: $fileName", e)
            return 0
        }
    }

    // 从二进制文件读取
    fun readFromBinaryFile(context: Context, fileName: String): ByteArray? {
        return try {
            val uri = File(context.getExternalFilesDir(null), fileName).toUri()
            val file: File = uri.toFile()
            if (file.exists()) file.readBytes() else null
        } catch (e: Exception) {
            e.printStackTrace()
            //throw RuntimeException("Failed to read from file: $fileName", e)
            return null
        }
    }

    fun getRequestHeaders(): Map<String, String> {
        val headers = HashMap<String, String>()
//        if (mApiKey != null) {
//            headers["X-Tinode-APIKey"] = mApiKey
//        }
//        if (mAuthToken != null) {
//            headers["X-Tinode-Auth"] = "Token $mAuthToken"
//        }
        headers["User-Agent"] = makeUserAgent()
        return headers
    }

    private fun makeUserAgent(): String {
        return (mAppName + " (Android " + mOsVersion + "; "
                + Locale.getDefault() + "); " + "bird_talk_sdk")
    }

    // 显示简单的 Toast 消息
    fun showToast(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(context, message, duration).show()
    }

    // 显示简单的 Toast 消息，使用默认的短时间
    fun showToast(context: Context, messageResId: Int, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(context, messageResId, duration).show()
    }

    fun bytesToHumanSize(bytes: Long): String {
        if (bytes <= 0) {
            // 0x202F - narrow non-breaking space.
            return "0\u202FBytes"
        }
        val k = 1024.0

        val sizes = arrayOf("Bytes", "KB", "MB", "GB", "TB")
        val bucket = (63 - java.lang.Long.numberOfLeadingZeros(bytes)) / 10
        val count: Double = bytes / k.pow(bucket.toDouble())
        val roundTo = if (bucket > 0) (if (count < 3) 2 else (if (count < 30) 1 else 0)) else 0
        val fmt = DecimalFormat.getInstance()
        fmt.maximumFractionDigits = roundTo
        return fmt.format(count) + "\u202F" + sizes[bucket]
    }

    fun millisToTime(millis: Int): String {
        val sb = StringBuilder()
        val duration = millis / 1000f
        val min = floor((duration / 60f).toDouble()).toInt()
        if (min < 10) {
            sb.append("0")
        }
        sb.append(min).append(":")
        val sec = (duration % 60f).toInt()
        if (sec < 10) {
            sb.append("0")
        }
        return sb.append(sec).toString()
    }

    // 拼接服务器的基础地址和给出的部分字符串，如果出错了，给出一个默认的地址
    fun toAbsoluteURL(value:String) : URL {
        val uri = Uri.parse(value)
        if (uri.isAbsolute){

            try {
                return URL(value) // 使用 Uri 的字符串表示创建 URL
            } catch (ignored: MalformedURLException) {
                return URL("https://127.0.0.1")
            }

        }

        var url: URL? = null
        try {
            url = URL(URL("https://127.0.0.1"), value)
        } catch (ignored: MalformedURLException) {
            return URL("https://127.0.0.1")
        }
        return url!!
    }

    fun isUriAbsolute(uriString: String): Boolean {
        val uri = Uri.parse(uriString)
        return uri.isAbsolute // 或者手动检查 scheme 是否为空： uri.scheme != null
    }
}