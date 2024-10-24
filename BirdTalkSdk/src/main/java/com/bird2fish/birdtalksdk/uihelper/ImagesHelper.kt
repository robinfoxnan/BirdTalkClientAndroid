package com.bird2fish.birdtalksdk.uihelper

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import com.bird2fish.birdtalksdk.R

public object ImagesHelper {

    fun copyToClipboard(context: Context, text: String) {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("label", text)
        clipboardManager.setPrimaryClip(clipData)
    }

    fun getClipboardText(context: Context): String {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        // 检查剪贴板中是否有数据
        if (clipboardManager.hasPrimaryClip()) {
            val clipData = clipboardManager.primaryClip

            // 获取剪贴板中的第一条数据
            val clipItem = clipData?.getItemAt(0)

            // 返回文本数据
            return clipItem?.text?.toString() ?: ""
        }

        return ""
    }

    fun showMessage(applicationContext: Context, str:CharSequence){
        var toast = Toast.makeText(
            applicationContext,
            str,
            Toast.LENGTH_LONG
        )
        toast.setGravity(Gravity.CENTER, 0, 0)
        toast.show()
    }

    fun showCenterMessage(applicationContext: Context, str:CharSequence){
        var toast=  Toast.makeText(
            applicationContext,
            str,
            Toast.LENGTH_SHORT
        )
        toast.setGravity(Gravity.CENTER, 0, 0)
        toast.show()
    }
    // 35个
    val iconIds = intArrayOf(
        R.drawable.widget_avatar_2,
        R.drawable.widget_avatar_3,
        R.drawable.widget_avatar_4,
        R.drawable.widget_avatar_6,
        R.drawable.widget_avatar_7,
        R.drawable.icon1,
        R.drawable.icon2,
        R.drawable.icon3,
        R.drawable.icon4,
        R.drawable.icon5,
        R.drawable.icon6,
        R.drawable.icon7,
        R.drawable.icon8,
        R.drawable.icon9,
        R.drawable.icon10,
        R.drawable.icon11,
        R.drawable.icon12,
        R.drawable.icon13,
        R.drawable.icon14,
        R.drawable.icon15,
        R.drawable.icon16,
        R.drawable.icon17,
        R.drawable.icon18,
        R.drawable.icon19,
        R.drawable.icon20,
        R.drawable.icon21,
        R.drawable.icon22,
        R.drawable.icon24,
        R.drawable.icon25,
        R.drawable.icon26,
        R.drawable.icon27,
        R.drawable.icon30,
        R.drawable.icon31,
        R.drawable.icon32,
        R.drawable.icon33
    )

    // 索引从1开始
    fun getIconResIndex(name :String) :Int{
        if (name.startsWith("sys:")) {
            val idStr = name.substring(4)
            val index = idStr.toIntOrNull()
            if (index != null) {
                if (index > iconIds.size || index <1)
                    return 0
                return index -1
            }
        }
        return 0
    }

    fun getIconResId(name :String) :Int{
        if (name.startsWith("sys:")){
            val idStr = name.substring(4)
            val index = idStr.toIntOrNull()
            if (index != null){
                if (index > iconIds.size || index <1)
                    return R.drawable.icon1

                return iconIds[index-1]
            }
        }

        return  R.drawable.icon1
    }

    // 为地图创建小图标
    fun getSmallIconBitmap(name :String, ctx: Context) : Bitmap {
        val id = getIconResId(name)
        val bitmapOld = BitmapFactory.decodeResource(ctx.resources, id)
        val bitmapIcon = Bitmap.createScaledBitmap(bitmapOld, 160, 160, false)
        return bitmapIcon
    }

    fun getSmallIconBitmap(id: Int, ctx: Context) : Bitmap {
        val bitmapOld = BitmapFactory.decodeResource(ctx.resources, id)
        val bitmapIcon = Bitmap.createScaledBitmap(bitmapOld, 120, 120, false)
        return bitmapIcon
    }

    fun resizeImage(bitmap: Bitmap, width: Int, height: Int): Bitmap {
        val bmpWidth = bitmap.width
        val bmpHeight = bitmap.height

        val scaleWidth = width.toFloat() / bmpWidth
        val scaleHeight = height.toFloat() / bmpHeight

        val matrix = Matrix()
        matrix.postScale(scaleWidth, scaleHeight)

        return Bitmap.createBitmap(bitmap, 0, 0, bmpWidth, bmpHeight, matrix, true)
    }

    // 创建 ImageView 并设置参数
//    fun createImageViewForBottomView(ctx: Context, friend:Friend?): ImageView {
//        val imageView = ImageView(ctx)
//        val sizePx = dpToPx(50f, ctx).toInt()
//
//        // 设置图标大小
//        val params = LinearLayout.LayoutParams(sizePx, sizePx)
//
//        // 设置图标之间的间隔
//        params.marginStart = dpToPx(8.0f, ctx).toInt()
//        imageView.layoutParams = params
//
//        // 设置图标资源
//        var id = com.bird2fish.travelbook.R.drawable.icon1
//        if (friend != null){
//            id = getIconResId(friend.icon)
//        }
//        imageView.setImageResource(id)
//
//        // 设置圆角背景
//        imageView.setBackgroundResource(com.bird2fish.travelbook.R.drawable.rounded_border)
//
//        return imageView
//    }

    // 将 dp 转换为像素
    private fun dpToPx(dp: Float, context: Context): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics
        )
    }

    fun formatSpeed(speed: Float): String {
        if (speed < 5.0){
            return String.format("%.1f 米/秒", speed)
        }else{
            return String.format("%.1f 千米/时", speed * 3.6 )
        }
    }


    //加载资源
    fun idToDrawable(context: Context, id: Int): Drawable? {
        return ContextCompat.getDrawable(context, id)
    }

    fun computeHeight(img : Drawable, width: Int):Int{
        val scaleFactor = width * 1.0 / img.intrinsicWidth
        val height = scaleFactor * img.intrinsicHeight

        return height.toInt()
    }

    fun computeHeight(img : Bitmap, width: Int):Int{
        val scaleFactor = width * 1.0 / img.width
        val height = scaleFactor * img.height

        return height.toInt()
    }

    // 将imageview中图片的不透明的部分变为另一种颜色，用于按钮点击
    fun replaceOpaqueWithColor(context: Context, resourceId: Int, replacementColorResId: Int, imageView: ImageView) {
        // 原始图片
        val originalBitmap = BitmapFactory.decodeResource(context.resources, resourceId)

        // 创建一个新的Bitmap，用于修改颜色
        val modifiedBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)

        // 获取要替换的颜色
        val replacementColor = ContextCompat.getColor(context, replacementColorResId)

        // 遍历所有像素
        for (x in 0 until modifiedBitmap.width) {
            for (y in 0 until modifiedBitmap.height) {
                // 获取像素颜色
                val pixel = modifiedBitmap.getPixel(x, y)

                // 判断是否为不透明的颜色
                if (Color.alpha(pixel) == 255) {
                    // 将不透明部分替换为指定颜色
                    modifiedBitmap.setPixel(x, y, replacementColor)
                }
            }
        }

        // 将修改后的Bitmap显示在ImageView中
        imageView.setImageBitmap(modifiedBitmap)
    }

    fun loadAndScaleImage(context: Context, resourceId: Int): Drawable? {
        try {
            // 从资源中加载原始图片
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeResource(context.resources, resourceId, options)

            // 计算缩放比例
            val targetWidth = 24
            val targetHeight = 24
            val scaleFactor = Math.min(
                options.outWidth / targetWidth,
                options.outHeight / targetHeight
            )

            // 设置缩放比例
            options.inJustDecodeBounds = false
            options.inSampleSize = scaleFactor

            // 重新加载图片并缩放
            val scaledBitmap = BitmapFactory.decodeResource(context.resources, resourceId, options)

            // 创建 Drawable
            return BitmapDrawable(context.resources, scaledBitmap)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    fun loadAndScaleImage(context: Context, resourceId: Int, width:Int): Drawable? {
        try {
            // 从资源中加载原始图片
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeResource(context.resources, resourceId, options)

            // 计算缩放比例
            val targetWidth = width

            val scaleFactor = options.outWidth / width
            var targetHeight = options.outHeight * scaleFactor



            // 设置缩放比例
            options.inJustDecodeBounds = false
            options.inSampleSize = scaleFactor

            // 重新加载图片并缩放
            val scaledBitmap = BitmapFactory.decodeResource(context.resources, resourceId, options)

            // 创建 Drawable
            return BitmapDrawable(context.resources, scaledBitmap)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    // 加载圆形的图标
    fun loadRoundAvatar(uri: Uri, context: Context): Bitmap? {
        val bitmap = getBitmapFromUri(uri, context) ?: return null
        return getCircularBitmapWithTransparentEdge(bitmap, 3)
    }

    // 从URI加载文件
    fun getBitmapFromUri(uri: Uri, context: Context): Bitmap? {
        return try {
            // 打开输入流从 URI 读取文件
            val inputStream = context.contentResolver.openInputStream(uri)
            // 使用 BitmapFactory 将输入流解码为 Bitmap
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    // 重新计算圆形的蒙版
    fun getCircularBitmapWithTransparentEdge(bitmap: Bitmap, edgeWidth: Int): Bitmap {
        val size = Math.min(bitmap.width, bitmap.height)

        // 创建一个正方形的位图
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val paint = Paint().apply {
            isAntiAlias = true
            shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }

        // 半径为正方形的一半
        val radius = size / 2f

        // 画出圆形
        canvas.drawCircle(radius, radius, radius, paint)

        // 创建透明边缘的画笔
        val edgePaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            color = Color.TRANSPARENT
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)  // 使用 CLEAR 模式来绘制透明边缘
            strokeWidth = edgeWidth.toFloat() // 设置透明边缘的宽度
        }

        // 绘制透明的边缘
        canvas.drawCircle(radius, radius, radius - edgeWidth / 2f, edgePaint)

        return output
    }

    fun scaleBitmap(bmp: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        var width = bmp.width
        var height = bmp.height
        var factor = 1.0f
        // Calculate scaling factor due to large linear dimensions.
        if (width >= height) {
            if (width > maxWidth) {
                factor = width.toFloat() / maxWidth
            }
        } else {
            if (height > maxHeight) {
                factor = height.toFloat() / maxHeight
            }
        }
        // Scale down.
        if (factor > 1.0) {
            height = (height / factor).toInt()
            width = (width / factor).toInt()
            return Bitmap.createScaledBitmap(bmp, width, height, true)
        }
        return bmp
    }

    // 根据exif 旋转图片
    fun rotateBitmap(bmp: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.setRotate(180f)
                matrix.postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90f)
                matrix.postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            ExifInterface.ORIENTATION_NORMAL -> return bmp
            else -> return bmp
        }
        try {
            val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
            bmp.recycle()
            return rotated
        } catch (ex: OutOfMemoryError) {
            Log.e("translate image", "Out of memory while rotating bitmap")
            return bmp
        }
    }

}