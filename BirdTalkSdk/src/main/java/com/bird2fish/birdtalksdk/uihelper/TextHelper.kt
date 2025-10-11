package com.bird2fish.birdtalksdk.uihelper

import android.app.Activity
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Base64
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.bird2fish.birdtalksdk.model.Drafty
import java.io.File
import java.net.MalformedURLException
import java.net.URL
import java.text.DecimalFormat
import java.util.Locale
import kotlin.math.floor
import kotlin.math.pow
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import com.fasterxml.jackson.databind.ObjectMapper;

object  TextHelper {

    private val mAppName: String? = "BirdTalk"
    private val mOsVersion:String? = "6.0"



    /**
     * 将Drafty对象序列化为JSON字符串
     * @param drafty 要序列化的Drafty对象
     * @return 序列化后的JSON字符串
     * @throws Exception 序列化过程中可能抛出的异常
     */
    @Throws(Exception::class)
    fun serializeDrafty(drafty: Drafty): String {
        val mapper = ObjectMapper()
        // Jackson会自动识别Drafty类中的Jackson注解并应用相应规则
        return mapper.writeValueAsString(drafty)
    }

    /**
     * 将JSON字符串反序列化为Drafty对象
     * @param json 要反序列化的JSON字符串
     * @return 反序列化后的Drafty对象
     * @throws Exception 反序列化过程中可能抛出的异常
     */
    @Throws(Exception::class)
    fun deserializeDrafty(json: String): Drafty {
        val mapper = ObjectMapper()
        return mapper.readValue(json, Drafty::class.java)
    }


    fun showDialogInCallback(context: Context, message: String) {
        // 假设这是你的回调
        (context as? Activity)?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 根据 Uri 获取文件的 MIME 类型
     *
     * @param context 应用上下文
     * @param uri 文件 Uri
     * @return MIME 类型字符串，如果无法确定则返回 null
     */
    fun getMimeTypeFromUri(context: Context, uri: Uri?): String? {
        if (uri == null) return null

        return when {
            // 处理 content:// Uri
            ContentResolver.SCHEME_CONTENT.equals(uri.scheme, ignoreCase = true) -> {
                try {
                    // 尝试通过 ContentResolver 获取 MIME 类型
                    context.contentResolver.getType(uri) ?: run {
                        // 如果 ContentResolver 无法确定，尝试从路径扩展名推断
                        val path = getFilePathFromUri(context, uri)
                        path?.let { getMimeTypeFromExtension(it) }
                    }
                } catch (e: Exception) {
                    // 出错时尝试从路径扩展名推断
                    val path = getFilePathFromUri(context, uri)
                    path?.let { getMimeTypeFromExtension(it) }
                }
            }

            // 处理 file:// Uri
            ContentResolver.SCHEME_FILE.equals(uri.scheme, ignoreCase = true) -> {
                uri.path?.let { getMimeTypeFromExtension(it) }
            }

            // 处理其他类型的 Uri（如 http://、ftp:// 等）
            else -> {
                uri.path?.let { getMimeTypeFromExtension(it) } ?: uri.lastPathSegment?.let { getMimeTypeFromExtension(it) }
            }
        }
    }

    /**
     * 从文件路径或文件名推断 MIME 类型
     */
    private fun getMimeTypeFromExtension(filePath: String): String? {
        val extension = MimeTypeMap.getFileExtensionFromUrl(filePath)
        return if (extension.isNotEmpty()) {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
        } else {
            null
        }
    }

    /**
     * 从 Uri 获取文件路径（辅助函数）
     * 注意：此方法在某些情况下可能返回 null，建议结合 getMimeTypeFromUri 使用
     */
    private fun getFilePathFromUri(context: Context, uri: Uri): String? {
        var path: String? = null
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = context.contentResolver.query(uri, projection, null, null, null)

        cursor?.use {
            if (it.moveToFirst()) {
                val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                path = it.getString(columnIndex)
            }
        }

        return path
    }

    /**
     * 生成指定长度的随机字符串（使用安全随机数生成器）
     *
     * @param length 生成的字节数（注意：Base64编码后字符串长度会增加约33%）
     * @return 编码后的随机字符串
     */
    fun generateRandomByteString(length: Int = 10): String {
        val secureRandom = SecureRandom()
        val bytes = ByteArray(length)
        secureRandom.nextBytes(bytes)

        // 使用Base64编码确保生成的字符串可打印且长度固定
        // 使用URL安全变体，避免包含'+'和'/'字符
        //return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

        // 使用Android提供的Base64类（URL安全模式，不含填充）
        return Base64.encodeToString(bytes, Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING)
    }

    /**
     * 从 Uri 中提取文件名
     *
     * @param context 应用上下文
     * @param uri 要处理的 Uri
     * @return 提取的文件名，如果无法提取则返回 null
     */
    fun getFileNameFromUri(context: Context, uri: Uri?): String? {
        if (uri == null) return null

        return when {
            // 处理 content:// Uri
            ContentResolver.SCHEME_CONTENT.equals(uri.scheme, ignoreCase = true) -> {
                // 对于 Android 4.4 (API 19) 及以上版本的 DocumentProvider
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(context, uri)) {
                    // ExternalStorageProvider
                    if (isExternalStorageDocument(uri)) {
                        val docId = DocumentsContract.getDocumentId(uri)
                        val split = docId.split(":").toTypedArray()
                        if (split.size >= 2) {
                            return split[1]
                        }
                    }
                    // DownloadsProvider
                    else if (isDownloadsDocument(uri)) {
                        val id = DocumentsContract.getDocumentId(uri)
                        if (id.startsWith("raw:")) {
                            return id.substring(4)
                        }

                        val contentUri = ContentUris.withAppendedId(
                            Uri.parse("content://downloads/public_downloads"),
                            id.toLongOrNull() ?: return null
                        )

                        return queryFileName(context, contentUri)
                    }
                    // MediaProvider
                    else if (isMediaDocument(uri)) {
                        val docId = DocumentsContract.getDocumentId(uri)
                        val split = docId.split(":").toTypedArray()
                        if (split.size < 2) return null

                        val type = split[0]
                        val contentUri = when (type) {
                            "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                            "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                            "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                            else -> return null
                        }

                        val selection = "_id=?"
                        val selectionArgs = arrayOf(split[1])

                        return queryFileName(context, contentUri, selection, selectionArgs)
                    }
                }

                // 普通的 ContentProvider Uri
                return queryFileName(context, uri)
            }

            // 处理 file:// Uri
            ContentResolver.SCHEME_FILE.equals(uri.scheme, ignoreCase = true) -> {
                uri.path?.let { path ->
                    return File(path).name
                }
                null
            }

            // 其他情况
            else -> {
                uri.lastPathSegment ?: uri.path?.let { File(it).name }
            }
        }
    }

    /**
     * 查询 ContentProvider 中的文件名
     */
    private fun queryFileName(
        context: Context,
        uri: Uri,
        selection: String? = null,
        selectionArgs: Array<String>? = null
    ): String? {
        var cursor: Cursor? = null
        val projection = arrayOf(MediaStore.Files.FileColumns.DISPLAY_NAME)

        try {
            cursor = context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                null
            )

            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                return cursor.getString(index)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }

        return null
    }

    /**
     * 判断 Uri 是否为 ExternalStorageProvider
     */
    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    /**
     * 判断 Uri 是否为 DownloadsProvider
     */
    private fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    /**
     * 判断 Uri 是否为 MediaProvider
     */
    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }


    /**
     * 计算 Uri 对应文件的大小（字节）
     * @param uri 文件的 Uri，可为空
     * @return 文件大小（字节），若出错或无法获取则返回 -1
     */
    fun getFileSize(context: Context, uri: Uri?): Long {
        // 输入验证
        if (uri == null) return -1

        // 处理 content:// 类型的 Uri
        if (ContentResolver.SCHEME_CONTENT == uri.scheme) {
            var cursor: Cursor? = null
            try {
                cursor = context.contentResolver.query(
                    uri,
                    arrayOf(OpenableColumns.SIZE),
                    null,
                    null,
                    null
                )

                if (cursor != null && cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1) {
                        return cursor.getLong(sizeIndex)
                    }
                }
            } catch (e: Exception) {
                // 忽略异常，尝试其他方法
            } finally {
                cursor?.close()
            }
        }

        // 处理 file:// 类型的 Uri 或 content Uri 无法获取大小时
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                return descriptor.statSize
            }
        } catch (e: IOException) {
            // 文件访问失败
        }

        return -1
    }


    fun calculateMD5FromUri(context: Context, uri: Uri?): String {
        if (uri == null) return ""

        var inputStream: InputStream? = null
        return try {
            // 打开 Uri 的输入流
            inputStream = context.contentResolver.openInputStream(uri) ?: return ""

            // 创建 MD5 消息摘要
            val md5Digest = MessageDigest.getInstance("MD5")

            // 使用缓冲区循环读取数据
            val buffer = ByteArray(1024 *1024) // 8KB 缓冲区
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                md5Digest.update(buffer, 0, bytesRead)
            }

            // 将 MD5 字节数组转换为十六进制字符串
            md5Digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            // 打印异常日志
            e.printStackTrace()
            ""
        } finally {
            // 确保关闭流
            try {
                inputStream?.close()
            } catch (closeException: Exception) {
                closeException.printStackTrace()
            }
        }
    }


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