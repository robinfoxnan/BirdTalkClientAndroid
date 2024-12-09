import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import androidx.core.view.GestureDetectorCompat
import com.bird2fish.birdtalksdk.R
import com.squareup.picasso.Picasso
import java.net.URL

class FullscreenImageDialog(context: Context, private val bitmap: Bitmap?, val uri: URL?) : Dialog(context) {

    private lateinit var imageView: ImageView
    private lateinit var scaleDetector: ScaleGestureDetector
    private lateinit var gestureDetector: GestureDetectorCompat
    private var matrix = Matrix()
    private var scaleFactor = 1f

    private var originalBitmap: Bitmap? = null

    // 确保图片在初始时居中显示
    private fun centerImage(bitmap: Bitmap) {

        val imageWidth = bitmap.width
        val imageHeight = bitmap.height

        var viewWidth = 0
        var viewHeight = 0
        if (imageView.width > 0){
            viewWidth = imageView.width
        }
        if (imageView.height > 0){
            viewHeight = imageView.height
        }

        // 计算图片和视图的偏移量
        val dx = (viewWidth - imageWidth) / 2f
        val dy = (viewHeight - imageHeight) / 2f

        // 将偏移量应用到矩阵
        matrix = Matrix()
        matrix.postTranslate(dx, dy)
        imageView.imageMatrix = matrix
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_dialog_image)

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

        imageView = findViewById(R.id.imageView)

        // 设置位图，或者URL
        if (bitmap != null){
            originalBitmap = bitmap
            imageView.setImageBitmap(bitmap) // 使用传入的 Bitmap
            // 监听布局完成后设置图片居中
            imageView.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    imageView.viewTreeObserver.removeOnPreDrawListener(this)
                    centerImage(bitmap)
                    return true
                }
            })

        }else if (uri == null){
            imageView.setImageResource(R.drawable.ic_broken_image)
        }else{
            Picasso.get()
                .load(uri.toString()) // 加载远程图片
                .into(imageView, object : com.squareup.picasso.Callback {
                    override fun onSuccess() {
                        imageView.drawable?.let {
                            val bitmap = (it as BitmapDrawable).bitmap
                            originalBitmap = bitmap
                            imageView.setImageBitmap(bitmap)
                            // 监听布局完成后设置图片居中
                            imageView.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                                override fun onPreDraw(): Boolean {
                                    imageView.viewTreeObserver.removeOnPreDrawListener(this)
                                    centerImage(bitmap)  // 确保图片居中
                                    return true
                                }
                            })
                        }
                    }

                    override fun onError(e: Exception?) {
                        // 处理加载失败的情况
                    }
                })
        }


        // 设置 ImageView 的矩阵变换
        imageView.imageMatrix = matrix

        // 初始化 ScaleGestureDetector
        scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                // 获取当前缩放比例
                val newScaleFactor = detector.scaleFactor
                // 限制缩放比例在合理范围内
                scaleFactor = Math.max(0.5f, Math.min(newScaleFactor, 3.0f))

                // 确保从图片的中心缩放
                val focusX = detector.focusX
                val focusY = detector.focusY
                matrix.postScale(scaleFactor, scaleFactor, focusX, focusY)
                imageView.imageMatrix = matrix
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

