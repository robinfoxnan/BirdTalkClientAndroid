package com.bird2fish.birdtalksdk.net

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.text.TextUtils
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import okio.buffer
import okio.sink
import okio.source
import java.io.InputStream


/*
FileDownloader.downloadAndOpen(
    context,
    "https://example.com/test.pdf",
    onProgress = { percent ->
        Log.d("Download", "进度: $percent%")
    },
    onFinished = { file ->
        Log.d("Download", "下载完成: ${file.absolutePath}")
    },
    onError = { e ->
        Log.e("Download", "出错: ${e.message}")
    }
)
 */
object FileDownloader {

    private val client = UnsafeOkHttpClient.getUnsafeOkHttpClient()
    private val executor = java.util.concurrent.Executors.newFixedThreadPool(3)

    /**
     * 下载文件并尝试打开（在主线程调用此函数）
     * @param context 上下文
     * @param url HTTPS 下载链接
     */
    fun downloadAndOpen(
        context: Context,
        url: String,
        fname:String,
        onProgress: ((Float) -> Unit)? = null,
        onFinished: ((File) -> Unit)? = null,
        onError: ((Exception) -> Unit)? = null
    ) {
        executor.execute {
            try {
                val file = checkOrDownloadFile(context, url, fname, onProgress)
                if (file != null) {
                    onFinished?.invoke(file)
                    openFile(context, file)
                } else {
                    showToast(context, "下载失败")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onError?.invoke(e)
                showToast(context, "下载出错: ${e.message}")
            }
        }
    }

    /**
     * 实际下载逻辑，支持进度回调
     */
    private fun downloadFile(
        context: Context,
        url: String,
        onProgress: ((Float) -> Unit)?
    ): File? {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()

        if (!response.isSuccessful) throw IOException("HTTP ${response.code}")

        val filename = guessFileName(url, response.header("Content-Disposition"))
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) downloadsDir.mkdirs()
        val outFile = File(downloadsDir, filename)

        val body = response.body ?: return null
        val total = body.contentLength().takeIf { it > 0 } ?: -1L

        body.source().use { source ->
            outFile.sink().buffer().use { sink ->
                var bytesCopied = 0L
                var lastProgress = 0f
                val buffer = okio.Buffer()
                var read: Long

                while (source.read(buffer, 8 * 1024).also { read = it } != -1L) {
                    sink.write(buffer, read)
                    bytesCopied += read
                    if (total > 0) {
                        val progress = bytesCopied * 100f / total
                        if (progress - lastProgress >= 1f) { // 每增加1%再回调，避免太频繁
                            lastProgress = progress
                            onProgress?.invoke(progress)
                        }
                    }
                }
            }
        }
        return outFile
    }

    /**
     * 检查文件是否已存在，否则下载
     */
    private fun checkOrDownloadFile(
        context: Context,
        url: String,
        fname:String,
        onProgress: ((Float) -> Unit)?
    ): File? {
        var filename = guessFileName(url, null)
        if (!TextUtils.isEmpty(fname)){
            filename = fname
        }
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) downloadsDir.mkdirs()

        val outFile = File(downloadsDir, filename)

        // ✅ 已存在则直接返回
        if (outFile.exists() && outFile.length() > 0) {
            showToast(context, "文件已存在，直接打开")
            return outFile
        }

        // 否则执行下载
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")

            val body = response.body ?: return null
            val total = body.contentLength().takeIf { it > 0 } ?: -1L
            val inputStream = body.byteStream()

            FileOutputStream(outFile).use { output ->
                copyStream(inputStream, output, total, onProgress)
            }
        }
        return outFile
    }


    /**
     * 辅助函数：复制流并汇报进度
     */
    private fun copyStream(
        input: InputStream,
        output: FileOutputStream,
        total: Long,
        onProgress: ((Float) -> Unit)?
    ) {
        val buffer = ByteArray(8 * 1024)
        var bytesCopied = 0L
        var read: Int
        var lastProgress = 0f
        while (input.read(buffer).also { read = it } != -1) {
            output.write(buffer, 0, read)
            bytesCopied += read
            if (total > 0) {
                val progress = bytesCopied * 100f / total
                if (progress - lastProgress >= 1f) {
                    lastProgress = progress
                    onProgress?.invoke(progress)
                }
            }
        }
        output.flush()
    }


    /**
     * 使用 OkHttp3 下载文件到 Downloads 目录
     */
    private fun downloadFile(context: Context, url: String): File? {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")

            // 根据 Content-Disposition 或 URL 推测文件名
            val filename = guessFileName(url, response.header("Content-Disposition"))
            val mimeType = getMimeType(filename)

            // 保存路径：/storage/emulated/0/Download/
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()

            val file = File(downloadsDir, filename)
            FileOutputStream(file).use { fos ->
                response.body?.byteStream()?.copyTo(fos)
            }

            return file
        }
    }

    /**
     * 猜测文件名
     */
    private fun guessFileName(url: String, contentDisposition: String?): String {
        contentDisposition?.let {
            val match = Regex("filename=\"?([^\";]+)\"?").find(it)
            if (match != null) return match.groupValues[1]
        }
        return Uri.parse(url).lastPathSegment ?: "download_${System.currentTimeMillis()}"
    }

    /**
     * 根据文件名获取 MIME 类型
     */
    private fun getMimeType(filename: String): String {
        val ext = filename.substringAfterLast('.', "")
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.lowercase()) ?: "application/octet-stream"
    }

    /**
     * 打开文件
     */
    private fun openFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "com.bird2fish.birdtalksdk.fileprovider", // ✅ 改成 SDK 模块注册的 authority
            file
        )
        val mimeType = getMimeType(file.name)

        val bOpen = checkFix(file)
        // 常见类型直接打开
        if (bOpen || mimeType.startsWith("application/pdf") ||
            mimeType.startsWith("audio/") ||
            mimeType.startsWith("video/") ||
            mimeType.startsWith("image/"))
        {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                showToast(context, "没有找到可以打开此文件的应用")
            }
        } else {
            showToast(context, "文件已下载到: ${file.absolutePath}")
        }
    }

    private fun checkFix(file: File) :Boolean{
        val ret = when {
            file.name.endsWith(".pdf", true) -> true
            file.name.endsWith(".doc", true) -> true
            file.name.endsWith(".docx", true) -> true
            file.name.endsWith(".xls", true) -> true
            file.name.endsWith(".xlsx", true) -> true
            file.name.endsWith(".ppt", true) -> true
            file.name.endsWith(".pptx", true) -> true

            file.name.endsWith(".mp3", true) -> true
            file.name.endsWith(".mp4", true) -> true
            file.name.endsWith(".jpg", true) -> true
            file.name.endsWith(".jpeg", true) -> true
            file.name.endsWith(".png", true) -> true
            else -> false // 其他未知类型
        }

        return ret
    }

    private fun showToast(context: Context, msg: String) {
        android.os.Handler(context.mainLooper).post {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 打开本地文件或 content URI 文件
     *
     * @param context 上下文
     * @param path 文件路径或者 URI，可以是：
     *  - 本地路径 /storage/emulated/0/Download/test.pdf
     *  - file:// URI
     *  - content:// URI
     */
    fun openLocalFile(context: Context, path: String) {
        val uri: Uri
        val mimeType: String

        when {
            path.startsWith("content://") -> {
                uri = Uri.parse(path)
                mimeType = context.contentResolver.getType(uri) ?: "*/*"
            }
            path.startsWith("file://") -> {
                val file = File(Uri.parse(path).path!!)
                if (!file.exists()) {
                    android.widget.Toast.makeText(context, "文件不存在", android.widget.Toast.LENGTH_SHORT).show()
                    return
                }
                uri = Uri.fromFile(file)
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase()) ?: "*/*"
            }
            else -> {
                val file = File(path)
                if (!file.exists()) {
                    android.widget.Toast.makeText(context, "文件不存在", android.widget.Toast.LENGTH_SHORT).show()
                    return
                }
                // 使用 FileProvider 提供 content:// URI
                uri = try {
                    FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
                } catch (e: Exception) {
                    Uri.fromFile(file)
                }
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase()) ?: "*/*"
            }
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "无法打开此文件类型", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
