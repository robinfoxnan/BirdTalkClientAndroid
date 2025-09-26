package com.bird2fish.birdtalksdk.uihelper

import android.content.Context
import android.graphics.Bitmap
import android.widget.ImageView
import com.bird2fish.birdtalksdk.R
import com.bird2fish.birdtalksdk.net.ImageDownloader

object AvatarHelper {


    // 本地加载
    private  fun loadLocalAvatar( ctx: Context, iconName:String, view: ImageView, isMale:Int) : Boolean{

        if (iconName.isBlank()){
            when (isMale) {
                1 -> {
                    view.setImageResource(R.drawable.icon27)
                }

                0 -> {
                    view.setImageResource(R.drawable.icon7)
                }

                2->{
                    view.setImageResource(R.drawable.icon19)
                }
            }

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
        downloader.downloadAndSaveImage(ctx, remoteName, "avatar", view, R.drawable.icon27)

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
    fun tryLoadAvatar(ctx: Context, iconName:String, view: ImageView, gender:String = "male"){
        val isMale = isMale(gender)
        val ret = loadLocalAvatar(ctx, iconName, view, isMale)
        if (!ret){
            loadRemoteImage(ctx, iconName, view)
        }
    }
}