package com.bird2fish.birdtalksdk.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.MediaScannerConnection
import android.os.Bundle
import android.os.Environment
import android.text.TextUtils
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.core.view.GestureDetectorCompat
import com.bird2fish.birdtalksdk.R
import com.bird2fish.birdtalksdk.uihelper.CryptHelper
import com.bird2fish.birdtalksdk.uihelper.ImagesHelper
import com.squareup.picasso.Picasso
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class FullscreenImageDialog(context: Context, val fileName :String, private val bitmap: Bitmap?, val uuid: String?) : Dialog(context) {

    private lateinit var imageView: ImageView
    private lateinit var scaleDetector: ScaleGestureDetector
    private lateinit var gestureDetector: GestureDetectorCompat
    private var matrix = Matrix()
    private var scaleFactor = 1f
    private var picassoTarget: com.squareup.picasso.Target? = null

    private var originalBitmap: Bitmap? = null

    // 确保图片在初始时居中显示
    private fun centerImage(bitmap: Bitmap) {

        val imageWidth = bitmap.width
        val imageHeight = bitmap.height
        val viewWidth = imageView.width
        val viewHeight = imageView.height

        val dx = (viewWidth - imageWidth) / 2f
        val dy = (viewHeight - imageHeight) / 2f

        // 重置 matrix 和 scaleFactor
        matrix.reset()
        scaleFactor = 1f
        matrix.postTranslate(dx, dy)
        imageView.imageMatrix = matrix
    }

    fun saveImageToLocal(context: Context, bitmap: Bitmap) {
        synchronized(this) {
            try {
                var fileNameLocal = "IMG_" + System.currentTimeMillis() + ".jpg"
                if (this.fileName != null && !TextUtils.isEmpty(this.fileName)){
                    fileNameLocal = fileName
                }

                // 兼容早期安卓，优先保存在 Pictures 目录
                val picturesDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES
                )
                val file = File(picturesDir, fileNameLocal)

                val fos = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
                fos.flush()
                fos.close()

                Log.d("SAVE", "✅ 保存成功: $file")
                Toast.makeText(context, "保存成功: ${file.absolutePath}", Toast.LENGTH_LONG).show()

                // 让相册立刻可见（MediaScanner）
                MediaScannerConnection.scanFile(context, arrayOf(file.path), null) { path, uri ->
                    Log.d("SAVE", "📷 触发相册刷新: $path -> $uri")
                }

            } catch (e: Exception) {
                Log.e("SAVE", "❗ 保存失败: ${e.message}")
            }
        }
    }

//    fun centerImage(imageView: ImageView){
//        imageView.drawable?.let {
//            val bitmap = (it as BitmapDrawable).bitmap
//            originalBitmap = bitmap
//            imageView.setImageBitmap(bitmap)
//            // 监听布局完成后设置图片居中
//            imageView.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
//                override fun onPreDraw(): Boolean {
//                    imageView.viewTreeObserver.removeOnPreDrawListener(this)
//                    centerImage(bitmap)  // 确保图片居中
//                    return true
//                }
//            })
//
//        }
//    }

    fun centerImage(imageView: ImageView) {
        imageView.drawable?.let {
            val bitmap = (it as BitmapDrawable).bitmap
            originalBitmap = bitmap

            // 获取 ImageView 的显示尺寸
            val viewWidth = imageView.width
            val viewHeight = imageView.height

            if (viewWidth == 0 || viewHeight == 0) {
                // 等待布局完成
                imageView.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                    override fun onPreDraw(): Boolean {
                        imageView.viewTreeObserver.removeOnPreDrawListener(this)
                        centerImage(imageView) // 再次调用
                        return true
                    }
                })
                return
            }

            val bitmapWidth = bitmap.width
            val bitmapHeight = bitmap.height

            // 计算缩放比例（保持宽高比）
            val scale = minOf(viewWidth.toFloat() / bitmapWidth, viewHeight.toFloat() / bitmapHeight)

            // 计算居中偏移
            val dx = (viewWidth - bitmapWidth * scale) / 2f
            val dy = (viewHeight - bitmapHeight * scale) / 2f

            // 应用 matrix 缩放和偏移
            matrix = Matrix()
            matrix.postScale(scale, scale)
            matrix.postTranslate(dx, dy)

            imageView.scaleType = ImageView.ScaleType.MATRIX
            imageView.imageMatrix = matrix
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_dialog_image)

        this.picassoTarget = object :  com.squareup.picasso.Target {
            override fun onBitmapLoaded(bitmap: Bitmap, from: Picasso.LoadedFrom) {

                ImagesHelper.saveBitmapToAppDir(
                    context,
                    bitmap,
                    dirName = "cache",
                    fileName = uuid!!
                )

                imageView.setImageBitmap(bitmap)

                imageView.post {
                    centerImage(imageView)
                }
            }

            override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {}
            override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}
        }
        // 设置对话框为全屏
        // 获取当前窗口的属性并设置为全屏
        window?.apply {
            // 设置对话框为全屏
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

            // 设置没有默认的边距
            setBackgroundDrawableResource(android.R.color.darker_gray)

            // 设置对话框内容的边距为0，避免空白区域
            val params = attributes
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
            params.height = ViewGroup.LayoutParams.MATCH_PARENT
            params.x = 0
            params.y = 0
            attributes = params
        }

        val saveBtn = findViewById<ImageView>(R.id.btnSave)
        saveBtn.setOnClickListener {
            val drawable = imageView.drawable
            if (drawable == null) {
                Log.e("SAVE", "❗ ImageView 里没有图片")
                return@setOnClickListener
            }

            // 将 ImageView 里的内容取成 Bitmap
            val bitmap = (drawable as BitmapDrawable).bitmap
            saveImageToLocal(this.context, bitmap)
        }

        val closeBtn = findViewById<ImageView>(R.id.btnClose)
        closeBtn.setOnClickListener{
            dismiss()  // 关闭对话框
        }


        imageView = findViewById(R.id.imageView)
        // 这里很重要，没这2行，Picasso 一加载完就“接管显示权”
        imageView.scaleType = ImageView.ScaleType.MATRIX
        imageView.imageMatrix = matrix

        // 设置位图，或者URL
        if (bitmap != null){
            originalBitmap = bitmap
            imageView.setImageBitmap(bitmap) // 使用传入的 Bitmap
            // 监听布局完成后设置图片居中
            centerImage(imageView)

        }else if (uuid == null){
            imageView.setImageResource(R.drawable.ic_broken_image)
        }else{

            val bitmap = ImagesHelper.loadBitmapFromAppDir(context, "cache", this.uuid)
            if (bitmap != null) {
                originalBitmap = bitmap
                imageView.setImageBitmap(bitmap) // 使用传入的 Bitmap
                // 监听布局完成后设置图片居中
                centerImage(imageView)

            }

            var url = CryptHelper.getUrl(uuid)
            Picasso.get()
                .load(url) // 加载远程图片
                .into(this.picassoTarget!!)
        }


        // 设置 ImageView 的矩阵变换
        imageView.imageMatrix = matrix

        // 初始化 ScaleGestureDetector
        scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                // detector.scaleFactor 是 相对增量, 如果当做绝对变量会出问题
                val scale = detector.scaleFactor
                val newScale = scaleFactor * scale

                if (newScale in 0.5f..3.0f) {
                    scaleFactor = newScale
                    matrix.postScale(scale, scale, detector.focusX, detector.focusY)
                    imageView.imageMatrix = matrix
                }
                return true
            }
        })

        // 初始化 GestureDetector，用于拖动
        gestureDetector = GestureDetectorCompat(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent?): Boolean {
                return true
            }

            override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
                // 减少拖动灵敏度，控制每次的拖动量
                matrix.postTranslate(-distanceX * 0.5f, -distanceY * 0.5f) // 0.5f 是拖动灵敏度的调节系数
                imageView.imageMatrix = matrix
                return true
            }
        })


        // 获取根视图并处理触摸事件
        val rootView = findViewById<View>(android.R.id.content)
        rootView.setOnTouchListener { _, event ->
            scaleDetector.onTouchEvent(event)
            gestureDetector.onTouchEvent(event)
            true
        }
    }
}

