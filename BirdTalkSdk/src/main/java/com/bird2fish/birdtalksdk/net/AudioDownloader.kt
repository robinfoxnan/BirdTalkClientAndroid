package com.bird2fish.birdtalksdk.net
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.widget.ImageView
import android.widget.Toast
import com.bird2fish.birdtalksdk.R
import com.bird2fish.birdtalksdk.uihelper.CryptHelper
import com.bird2fish.birdtalksdk.uihelper.ImagesHelper
import com.squareup.picasso.Callback
import com.squareup.picasso.OkHttp3Downloader
import com.squareup.picasso.Picasso
import okhttp3.Call
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest


object AudioDownloader {

    private const val DIR_NAME = "audio_cache"

    /**
     * 下载音频到本地（有缓存）
     */
    fun download(
        context: Context,
        uuid: String,
        callback: (Result<File>) -> Unit
    ) {
        val cacheFile = getCacheFile(context, uuid)

        // 已缓存，直接返回
        if (cacheFile.exists() && cacheFile.length() > 0) {
            callback(Result.success(cacheFile))
            return
        }

        var fullUrl = uuid
        if (!uuid.startsWith("http")){
            fullUrl = CryptHelper.getUrl(uuid)
        }

        val request = Request.Builder()
            .url(fullUrl)
            .get()
            .build()

        UnsafeOkHttpClient.getUnsafeOkHttpClient().newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    callback(
                        Result.failure(
                            IOException("HTTP ${response.code}")
                        )
                    )
                    return
                }

                try {
                    response.body?.byteStream()?.use { input ->
                        cacheFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    callback(Result.success(cacheFile))
                } catch (e: Exception) {
                    callback(Result.failure(e))
                }
            }
        })
    }

    /**
     * 缓存文件路径（URL hash）
     */
    private fun getCacheFile(context: Context, uuid: String): File {
        val dir = File(context.cacheDir, DIR_NAME)
        if (!dir.exists()) dir.mkdirs()

        val name = md5(uuid) + ".m4a"
        return File(dir, name)
    }

    private fun md5(text: String): String {
        val md = MessageDigest.getInstance("MD5")
        val bytes = md.digest(text.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
