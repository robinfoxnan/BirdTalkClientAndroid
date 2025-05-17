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
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber

object  TextHelper {

    private val mAppName: String? = "BirdTalk"
    private val mOsVersion:String? = "6.0"

    /**
     * 删除外部存储目录中的指定文件。
     *
     * @param context 应用上下文，用于获取外部文件目录
     * @param fileName 要删除的文件名
     * @return 删除操作的结果：1表示成功，0表示失败
     */
    fun deleteBinaryFile(context: Context, fileName: String): Int {
        try {
            // 构建文件路径，与写入函数使用相同的逻辑
            val file = File(context.getExternalFilesDir(null), fileName)

            // 检查文件是否存在
            if (file.exists()) {
                // 执行删除操作
                val isDeleted = file.delete()
                return if (isDeleted) 1 else 0
            } else {
                // 文件不存在，返回失败
                return 0
            }
        } catch (e: Exception) {
            // 异常处理
            e.printStackTrace()
            return 0
        }
    }
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

    /**
     * 使用 libphonenumber 验证国际手机号
     */
    private fun isValidInternationalPhone(phone: String): Boolean {
        return try {
            val phoneUtil = PhoneNumberUtil.getInstance()
            val number = phoneUtil.parse(phone, "ZZ") // ZZ表示未知国家代码
            phoneUtil.isValidNumber(number)
        } catch (e: Exception) {
            false
        }
    }

    // 检查输入是否合法
    fun checkStringType(input: String): String {
        // 检查是否为纯数字
        if (input.all { it.isDigit() }) {
            return "id"
        }

        // 检查是否为电子邮件
        val emailPattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
        if (input.matches(emailPattern.toRegex())) {
            return "email"
        }

        // 检查是否为手机号（以中国手机号为例，11位数字，以1开头）
//        val phonePattern = "^1\\d{10}$"
//        if (input.matches(phonePattern.toRegex())) {
//            return "phone"
//        }
        if (isValidInternationalPhone(input)){
            return "phone"
        }

        // 不符合任何类型
        return "invalid"
    }
}