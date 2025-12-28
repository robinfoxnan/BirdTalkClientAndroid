package com.bird2fish.birdtalksdk.uihelper

import android.content.Context
import android.graphics.Bitmap
import android.text.TextUtils
import android.widget.ImageView
import com.bird2fish.birdtalksdk.R
import com.bird2fish.birdtalksdk.net.ImageDownloader

object AvatarHelper {


    // 本地加载
    private  fun loadLocalAvatar( ctx: Context, iconName:String, nick:String, view: ImageView, isMale:Int) : Boolean{

        if (iconName.isBlank()){
            var name = nick
            if (TextUtils.isEmpty(name))
                name = "momo"
            val bitmap = ImagesHelper.generateDefaultAvatar(name, isMale)
            view.setImageBitmap(bitmap)
            return true
        }

        val bitmap = ImagesHelper.loadBitmapFromAppDir(ctx, "avatar", iconName)
        if (bitmap != null) {
            val bitmapRound = ImagesHelper.getRoundAvatar(bitmap, ctx)
            view.setImageBitmap(bitmapRound )
            return true
        }
        return false
    }

    // 使用pissaco直接从远程获取文件
    private  fun loadRemoteImage(ctx: Context, remoteName:String,  view: ImageView) {

        // 从远程加载
        val downloader = ImageDownloader()
        downloader.downloadAndSaveImage(ctx, remoteName, "avatar", view, R.drawable.book_user)

    }

    private fun isMale(gender: String?): Int {
        // 先判断是否为null，避免空指针
        if (gender == null) {
            return 2
        }
        // 转换为小写后比较，忽略大小写（如Male、MALE等也会被识别）
        val lowerGender = gender.lowercase()
        if (lowerGender == "男" || lowerGender == "male"){
            return 1
        }else if (lowerGender == "女" || lowerGender == "female"){
            return 0
        }

        return 2

    }
    // 加载头像
    fun tryLoadAvatar(ctx: Context, iconName:String, view: ImageView,  gender:String = "male", nick:String="momo"){
        val isMale = isMale(gender)
        val ret = loadLocalAvatar(ctx, iconName, nick, view, isMale)
        //val ret = false
        if (!ret){
            loadRemoteImage(ctx, iconName, view)
        }
    }
}