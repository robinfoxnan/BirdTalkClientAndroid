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
import android.graphics.Typeface
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
import com.bird2fish.birdtalksdk.model.MessageStatus
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
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



    enum class AvatarBgType(val color: Int) {
        MALE(Color.parseColor("#AED6F1")),     // 淡蓝
        FEMALE(Color.parseColor("#F5B7B1")),   // 淡粉
        UNKNOWN(Color.parseColor("#CDE8E6"))   // 中性
    }
    /**
     * 根据名字生成默认头像
     * @param name 用户名或群名
     * @param size 图片边长（正方形）
     * @return Bitmap 圆形头像，透明背景
     */
    fun generateDefaultAvatar(name: String?, isMale:Int = 0, size: Int = 200): Bitmap {
        val avatar = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(avatar)

        // 背景色：淡蓝色或淡粉色
        val bgColor = if (isMale == 1) {
            Color.parseColor("#AED6F1") // 淡蓝色
        } else if (isMale == 0){
            Color.parseColor("#F5B7B1") // 淡粉色
        }else{
            Color.parseColor("#CDE8E6") // 中性
        }

        // 画圆形背景
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = bgColor
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        // 准备文字
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        textPaint.color = Color.WHITE
        textPaint.textSize = size / 3f
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.typeface = Typeface.DEFAULT_BOLD

        // 提取显示文字：前2汉字或前5英文
        val displayText = name?.let {
            val trimmed = it.trim()
            if (trimmed.isEmpty()) "?" else {
                val first = if (trimmed[0].toInt() in 0x4e00..0x9fff) { // 汉字范围
                    trimmed.take(2)
                } else {
                    trimmed.replace("\\s+".toRegex(), "").take(5)
                }
                first
            }
        } ?: "?"

        // 测量文字居中位置
        val fontMetrics = textPaint.fontMetrics
        val x = size / 2f
        val y = size / 2f - (fontMetrics.ascent + fontMetrics.descent) / 2

        canvas.drawText(displayText, x, y, textPaint)

        // 返回 Bitmap
        return avatar
    }

    /**
     * 尝试从图片 Uri 获取经纬度和高度
     * @return 四元组: (latitude, longitude, altitude, hasGps)
     *         如果没有GPS信息，latitude/longitude/altitude为0，hasGps=false
     */
    fun getPhotoGpsInfo(context: Context, uri: Uri): Quadruple<Double, Double, Double, Boolean> {
        try {
            val inputStream: InputStream = context.contentResolver.openInputStream(uri) ?: return Quadruple(0.0, 0.0, 0.0, false)
            val exif = ExifInterface(inputStream)
            inputStream.close()

            val latLong = exif.latLong // 返回 DoubleArray? [lat, long]
            val altitude = exif.getAltitude(0.0) // 如果没有高度返回默认值 0.0

            return if (latLong != null) {
                Quadruple(latLong[0], latLong[1], altitude, true)
            } else {
                Quadruple(0.0, 0.0, 0.0, false)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return Quadruple(0.0, 0.0, 0.0, false)
        }
    }

    /**
     * Kotlin 没有内置四元组类，这里自定义一个
     */
    data class Quadruple<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )

    fun resizeImageIfNeeded(context: Context, uri: Uri, destDir: String): Pair<Uri, Boolean> {
        // 获取输入流
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: return Pair(uri, false) // 无法打开则直接返回原Uri

        // 先解码尺寸，不加载到内存
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeStream(inputStream, null, options)
        inputStream.close()

        val originalWidth = options.outWidth
        val originalHeight = options.outHeight
        val maxSide = maxOf(originalWidth, originalHeight)

        // 如果长边 <= 1024，直接返回原Uri
        if (maxSide <= 1024) return Pair(uri, false)

        // 计算缩放比例
        val scale = 1024f / maxSide
        val newWidth = (originalWidth * scale).toInt()
        val newHeight = (originalHeight * scale).toInt()

        // 解码并缩放
        val inputStream2 = context.contentResolver.openInputStream(uri)
            ?: return Pair(uri, false)
        val bitmap = BitmapFactory.decodeStream(inputStream2)
        inputStream2.close()
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        bitmap.recycle()

        // 保存到 cache 目录
        val mediaDir = File(context.getExternalFilesDir(null), destDir)
        if (!mediaDir.exists()) mediaDir.mkdirs()
        val outputFile = File(mediaDir, "resized_${System.currentTimeMillis()}.jpg")
        FileOutputStream(outputFile).use { out ->
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        resizedBitmap.recycle()

        return  Pair(Uri.fromFile(outputFile), true)
    }

    fun resizeImageIfNeeded1(context: Context, uri: Uri, destDir: String): Pair<Uri, Boolean> {
        // 获取输入流
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: return Pair(uri, false) // 无法打开则直接返回原Uri

        // 先解码尺寸，不加载到内存
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeStream(inputStream, null, options)
        inputStream.close()

        val originalWidth = options.outWidth
        val originalHeight = options.outHeight
        val maxSide = maxOf(originalWidth, originalHeight)

        // 如果长边 <= 1024，直接返回原Uri
        if (maxSide <= 1024) return Pair(uri, false)

        // 计算缩放比例
        val scale = 1024f / maxSide
        val newWidth = (originalWidth * scale).toInt()
        val newHeight = (originalHeight * scale).toInt()

        // 解码并缩放
        val inputStream2 = context.contentResolver.openInputStream(uri)
            ?: return Pair(uri, false)
        val bitmap = BitmapFactory.decodeStream(inputStream2)
        inputStream2.close()
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        bitmap.recycle()

        // 保存到 cache 目录
        val mediaDir = File(context.getExternalFilesDir(null), destDir)
        if (!mediaDir.exists()) mediaDir.mkdirs()

        // 获取原始文件格式
        val mimeType = context.contentResolver.getType(uri)
        val format = when (mimeType?.lowercase()) {
            "image/png" -> Bitmap.CompressFormat.PNG
            "image/webp" -> Bitmap.CompressFormat.WEBP
            else -> Bitmap.CompressFormat.JPEG
        }
        // 获取文件扩展名
        val ext = when (format) {
            Bitmap.CompressFormat.PNG -> "png"
            Bitmap.CompressFormat.WEBP -> "webp"
            else -> "jpg"
        }

        val outputFile = File(mediaDir, "resized_${System.currentTimeMillis()}.$ext")
        FileOutputStream(outputFile).use { out ->
            resizedBitmap.compress(format, 90, out)
        }
        resizedBitmap.recycle()

        return Pair(Uri.fromFile(outputFile), true)
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

            Log.d("缓存本地文件：",  file.absolutePath)
            // 返回文件路径
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // 加载保存的图片文件
    fun loadBitmapFromAppDir(
        context: Context,
        dirName: String,
        fileName: String
    ): Bitmap? {
        return try {
            // 构建文件路径（与保存时的路径规则一致）
            val dir = File(context.getExternalFilesDir(null), dirName)
            val file = File(dir, fileName)

            // 检查文件是否存在
            if (!file.exists()) {
                return null // 文件不存在
            }
            Log.d("加载本地文件：", fileName)

            // 加载并返回Bitmap
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun loadLocalImage(context:Context, fileName: String): Bitmap? {


        val mediaDir = File(context.getExternalFilesDir(null), "avatar")

        // 检查目录是否存在
        if (!mediaDir.exists() || !mediaDir.isDirectory) {
            return null
        }

        val imageFile = File(mediaDir, fileName)

        // 检查文件是否存在
        if (!imageFile.exists() || !imageFile.isFile) {
            return null
        }

        return try {
            // 解码文件为 Bitmap
            BitmapFactory.decodeFile(imageFile.absolutePath)
        } catch (e: Exception) {
            // 处理解码异常
            e.printStackTrace()
            null
        }
    }

    fun setMessageStatusIcon(holder: ImageView, status: MessageStatus, read: Boolean, recv:Boolean) {
        if (status == MessageStatus.UPLOADING || status == MessageStatus.SENDING || status == MessageStatus.DOWNLOADING) {
            holder.setImageResource(R.drawable.ic_schedule)
        } else if (status == MessageStatus.FAIL) {
            holder.setImageResource(R.drawable.ic_warning)
        } else {
            if (read) {
                holder.setImageResource(R.drawable.ic_done_all2)
            } else if (recv) {
                holder.setImageResource(R.drawable.ic_done_all)
            } else {
                holder.setImageResource(R.drawable.ic_done)
            }
        }
    }

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

    // 将加载的图片做成圆形的
    fun getRoundAvatar(image:Bitmap, context: Context): Bitmap?{
        return getCircularBitmapWithTransparentEdge(image, 3)
    }
    // 加载圆形的图标
    fun loadRoundAvatar(uri: Uri, context: Context): Bitmap? {
        val (bitmap, file) = getBitmapAndFileFromUri(context, uri)
        file?.let {
            Log.d("loadRoundAvatar", "临时文件路径: ${it.absolutePath}")
        }
        if (bitmap == null)
            return null

        return getCircularBitmapWithTransparentEdge(bitmap, 3)
    }

    /**自动识别并兼容老相册与新相册 URI 的 Kotlin 工具函数。
    它能自动处理：
    ✅ content://（现代 Android 相册 URI）
    ✅ file://（旧版 Android 相册 URI）
    ✅ 沙盒存储（Android 10+，无法直接访问文件时自动复制到缓存）
    ✅ 返回 Bitmap 同时可选返回对应 File
     * 从任意 Uri 获取 Bitmap，同时返回对应的 File（如果有）
     * @return Pair<Bitmap?, File?> => 第一个是 Bitmap，第二个是临时文件
     */
    fun getBitmapAndFileFromUri(context: Context, uri: Uri): Pair<Bitmap?, File?> {
        return try {
            when (uri.scheme) {
                "file" -> {
                    // 老式 file:// URI
                    val file = File(uri.path!!)
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    Pair(bitmap, file)
                }

                "content" -> {
                    // 新式 content:// URI
                    val inputStream = context.contentResolver.openInputStream(uri)
                    inputStream?.use {
                        // 复制到临时文件（避免沙盒访问问题）
                        val tempFile = File(context.cacheDir, "img_${System.currentTimeMillis()}.jpg")
                        FileOutputStream(tempFile).use { output ->
                            it.copyTo(output)
                        }
                        val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
                        Pair(bitmap, tempFile)
                    } ?: Pair(null, null)
                }

                else -> Pair(null, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(null, null)
        }
    }
    // 从URI加载文件
//    fun getBitmapFromUri(uri: Uri, context: Context): Bitmap? {
//        return try {
//            // 打开输入流从 URI 读取文件
//            val inputStream = context.contentResolver.openInputStream(uri)
//            // 使用 BitmapFactory 将输入流解码为 Bitmap
//            BitmapFactory.decodeStream(inputStream)
//        } catch (e: Exception) {
//            e.printStackTrace()
//            null
//        }
//    }


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