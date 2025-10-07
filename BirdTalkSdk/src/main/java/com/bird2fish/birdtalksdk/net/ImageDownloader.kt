package com.bird2fish.birdtalksdk.net

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.widget.ImageView
import android.widget.Toast
import com.bird2fish.birdtalksdk.R
import com.bird2fish.birdtalksdk.uihelper.ImagesHelper
import com.squareup.picasso.Callback
import com.squareup.picasso.OkHttp3Downloader
import com.squareup.picasso.Picasso
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class ImageDownloader {

    /**
     * 使用 Picasso 下载图片并保存到本地
     * @param fullUrl 远程图片URL
     * @param destDir 目标保存目录
     * @param fileName 保存的文件名
     * @param callback 下载完成回调
     */
    fun downloadAndSaveImage(
        context:Context,
        remote: String,
        destDir: String,
        view:ImageView,
        defaultIcon:Int
    ) : Boolean {
        // 确保目录存在
        val mediaDir = File(context.getExternalFilesDir(null), destDir)
        // 检查并创建目录
        if (!mediaDir.exists()) {
            if (!mediaDir.mkdirs()) {
                // 创建目录失败，记录错误或返回
                //println("无法创建目录: $mediaDirPath")
                return false
            }
        } else if (!mediaDir.isDirectory) {
            // 路径存在但不是目录
            //println("路径存在但不是目录: $mediaDirPath")
            return false
        }

        if (remote == null || remote == ""){
            return false
        }

        val destFile = File(mediaDir, remote)

        // 构建请求
        val fullUrl = WebSocketClient.instance!!.getRemoteFilePath( remote)


        val target = object : com.squareup.picasso.Target {
            override fun onBitmapLoaded(bitmap: Bitmap, from: Picasso.LoadedFrom) {
                // 直接获取加载后的Bitmap
                val bitmapRound = ImagesHelper.getRoundAvatar(bitmap, context)
                (context as? Activity)?.runOnUiThread {
                    //Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    view.setImageBitmap(bitmapRound)
                }


                // 保存或处理Bitmap
                saveBitmapToAppDir(
                    context,
                    bitmap = bitmap,
                    dirName = "avatar",
                    fileName = remote
                )
            }

            override fun onBitmapFailed(e: Exception, errorDrawable: Drawable?) {
                // 加载失败

                view.setImageResource(defaultIcon)
            }

            override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
                // 加载中
            }
        }

        // 使用 Picasso 加载图片
        val picasso = Picasso.get()
        picasso.load(fullUrl).into(target)
        return true
    }



    /**
     * 保存Bitmap到文件
     */
    fun saveBitmapToAppDir(
        context: Context,
        bitmap: Bitmap,
        dirName: String,
        fileName: String
    ): String? {
        return try {
            // 创建目录
            val dir = File(context.getExternalFilesDir(null), dirName)
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    throw IOException("无法创建目录")
                }
            }

            // 创建文件
            val file = File(dir, fileName)
            if (file.exists()) {
                file.delete()
            }

            // 保存Bitmap
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.flush()
            outputStream.close()

            // 返回文件路径
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // 下载回调接口
    interface DownloadCallback {
        fun onSuccess(bitmap: Bitmap)
        fun onError(e: Exception)
    }
}