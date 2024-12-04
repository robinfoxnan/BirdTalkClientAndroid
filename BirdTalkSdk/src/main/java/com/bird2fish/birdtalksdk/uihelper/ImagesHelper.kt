package com.bird2fish.birdtalksdk.uihelper

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
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
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.exifinterface.media.ExifInterface
import com.bird2fish.birdtalksdk.R
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.math.max
import kotlin.math.min

public object ImagesHelper {

    const val ACTION_UPDATE_SELF_SUB: Int = 0
    const val ACTION_UPDATE_SUB: Int = 1
    const val ACTION_UPDATE_AUTH: Int = 2
    const val ACTION_UPDATE_ANON: Int = 3
    const val PREF_TYPING_NOTIF: String = "pref_typingNotif"
    const val PREF_READ_RCPT: String = "pref_readReceipts"

    // Maximum length of user name or topic title.
    const val MAX_TITLE_LENGTH: Int = 60
    // Maximum length of topic description.
    const val MAX_DESCRIPTION_LENGTH: Int = 360
    // Length of quoted text.
    const val QUOTED_REPLY_LENGTH: Int = 64

    // Maximum linear dimensions of images.
    const val MAX_BITMAP_SIZE: Int = 1024
    const val AVATAR_THUMBNAIL_DIM: Int = 36 // dip
    // Image thumbnail in quoted replies and reply/forward previews.
    const val REPLY_THUMBNAIL_DIM: Int = 36
    // Image preview size in messages.
    const val IMAGE_PREVIEW_DIM: Int = 64
    const val MIN_AVATAR_SIZE: Int = 8
    const val MAX_AVATAR_SIZE: Int = 384
    // Maximum byte size of avatar sent in-band.
    const val MAX_INBAND_AVATAR_SIZE: Int = 4096

    // Default tag parameters

    const val DEFAULT_MIN_TAG_LENGTH: Int = 4

    const val DEFAULT_MAX_TAG_LENGTH: Int = 96

    const val DEFAULT_MAX_TAG_COUNT: Int = 16


    const val COLOR_GREEN_BORDER: Int = -0xb350b0

    const val COLOR_RED_BORDER: Int = -0x1a8c8d

    const val COLOR_GRAY_BORDER: Int = -0x616162
    // private static final int COLOR_BLUE_BORDER = 0xFF2196F3;

    const val COLOR_YELLOW_BORDER: Int = -0x35d8
    // Logo LayerDrawable IDs

    const val LOGO_LAYER_AVATAR: Int = 0

    const val LOGO_LAYER_ONLINE: Int = 1

    const val LOGO_LAYER_TYPING: Int = 2
    // If StoredMessage activity is visible, this is the current topic in that activity.

    var sVisibleTopic: String? = null

    // Creates LayerDrawable of the right size with gray background and 'fg' in the middle.
    // Used in chat bubbled to generate placeholder and error images for Picasso.
    fun getPlaceholder(
        ctx: Context,
        fg: Drawable,
        bkg: Drawable?,
        width: Int,
        height: Int
    ): Drawable {
        var bkg = bkg
        val filter: Drawable
        if (bkg == null) {
            // Uniformly gray background with rounded corners.
            bkg = ResourcesCompat.getDrawable(ctx.resources, R.drawable.placeholder_image_bkg, null)
            // Transparent filter.
            filter = ColorDrawable(0x00000000)
        } else {
            // Translucent filter.
            filter = ColorDrawable(-0x33333334)
        }

        val fgWidth = fg.intrinsicWidth
        val fgHeight = fg.intrinsicHeight
        val result = LayerDrawable(arrayOf(bkg, filter, fg))
        result.setBounds(0, 0, width, height)
        // Move foreground to the center of the drawable.
        val dx = max(((width - fgWidth) / 2).toDouble(), 0.0).toInt()
        val dy = max(((height - fgHeight) / 2).toDouble(), 0.0).toInt()
        fg.setBounds(dx, dy, dx + fgWidth, dy + fgHeight)
        return result
    }


    fun bitmapToBytes(bmp: Bitmap, mimeType: String): ByteArray {
        val fmt = if ("image/jpeg" == mimeType) {
            CompressFormat.JPEG
        } else {
            CompressFormat.PNG
        }
        val bos = ByteArrayOutputStream()
        bmp.compress(fmt, 70, bos)
        val bits = bos.toByteArray()
        try {
            bos.close()
        } catch (ignored: IOException) {
        }

        return bits
    }


    /**
     * Convert drawable to bitmap.
     *
     * @param drawable vector drawable to convert to bitmap
     * @return bitmap extracted from the drawable.
     */
    fun bitmapFromDrawable(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }

        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return bitmap
    }

    /**
     * Ensure that the bitmap is square and no larger than the given max size.
     * @param bmp       bitmap to scale
     * @param size   maximum linear size of the bitmap.
     * @return scaled bitmap or original, it it does not need ot be cropped or scaled.
     */
    fun scaleSquareBitmap(bmp: Bitmap, size: Int): Bitmap {
        // Sanity check
        var bmp = bmp
        var size = size
        size = min(size.toDouble(), MAX_BITMAP_SIZE.toDouble()).toInt()

        var width = bmp.width
        var height = bmp.height

        // Does it need to be scaled down?
        if (width > size && height > size) {
            // Scale down.
            if (width > height) /* landscape */ {
                width = width * size / height
                height = size
            } else  /* portrait or square */ {
                height = height * size / width
                width = size
            }
            // Scale down.
            bmp = Bitmap.createScaledBitmap(bmp, width, height, true)
        }
        size = min(width.toDouble(), height.toDouble()).toInt()

        if (width != height) {
            // Bitmap is not square. Chop the square from the middle.
            bmp = Bitmap.createBitmap(
                bmp, (width - size) / 2, (height - size) / 2,
                size, size
            )
        }

        return bmp
    }


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