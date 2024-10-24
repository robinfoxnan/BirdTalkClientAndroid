package com.bird2fish.birdtalksdk.ui

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.OnScaleGestureListener
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import com.bird2fish.birdtalksdk.R
import com.bird2fish.birdtalksdk.uihelper.ImagesHelper
import com.bird2fish.birdtalksdk.widgets.OverlaidImageView
import java.io.IOException
import kotlin.math.max
import kotlin.math.min

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"


class ImageViewFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private enum class RemoteState {
        NONE,
        LOADING,
        SUCCESS,
        FAILED
    }

    // 来自相机的位图可能太大。
// 1792 大约是方形位图的 3MP。
    private val MAX_BITMAP_DIM:Int = 1792

    // 缩放图像的最大像素数：宽度 * 高度 * 缩放比例^2。
    private val MAX_SCALED_PIXELS:Int = 1024 * 1024 * 12
    // 图像的任一维度与屏幕尺寸相比允许的最大比例。
    private val MAX_SCALE_FACTOR:Float = 8f

    // 实际用于缩放的矩阵。
    private var mMatrix: Matrix? = null
    // 用于预测试图像边界的工作矩阵。
    private var mWorkingMatrix: Matrix? = null
    // 缩放之前的初始图像边界。
    private lateinit var mInitialRect: RectF
    // 屏幕边界。
    private lateinit var mScreenRect: RectF
    // 屏幕中间正方形切出的边界。
    private lateinit var mCutOutRect: RectF

    // 用于测试平移和缩放后图像边界的工作矩形。
    private lateinit var mWorkingRect: RectF

    private lateinit var mGestureDetector: GestureDetector
    private lateinit var mScaleGestureDetector: ScaleGestureDetector
    private lateinit var mImageView: OverlaidImageView
    // 这是上传前的头像预览。
    private var mAvatarUpload: Boolean = false

    // 远程图像的状态。
    private lateinit var mRemoteState: RemoteState

    private var bmp : Bitmap? = null
    private lateinit var root:android. view. View


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_image_view, container, false)
        root = view

        mMatrix = Matrix()
        mImageView = view.findViewById(R.id.image)
        mImageView.imageMatrix = mMatrix

        val listener: GestureDetector.OnGestureListener = object : SimpleOnGestureListener() {
            override fun onScroll(e1: MotionEvent, e2: MotionEvent, dX: Float, dY: Float): Boolean {
                if (mWorkingRect == null || mInitialRect == null) {
                    // The image is not initialized yet.
                    return false
                }

                mWorkingMatrix!!.postTranslate(-dX, -dY)
                mWorkingMatrix!!.mapRect(mWorkingRect, mInitialRect)

                // Make sure the image cannot be pushed off the viewport.
                val bounds = if (mAvatarUpload) mCutOutRect else mScreenRect
                // Matrix.set* operations are retarded: they *reset* the entire matrix instead of adjusting either
                // translation or scale. Thus using postTranslate instead of setTranslate.
                mWorkingMatrix!!.postTranslate(
                    translateToBoundsX(bounds),
                    translateToBoundsY(bounds)
                )

                mMatrix!!.set(mWorkingMatrix)
                mImageView.imageMatrix = mMatrix
                return true
            }
        }
        mGestureDetector = GestureDetector(activity, listener)

        val scaleListener: OnScaleGestureListener = object : SimpleOnScaleGestureListener() {
            override fun onScale(scaleDetector: ScaleGestureDetector): Boolean {
                if (mWorkingRect == null || mInitialRect == null) {
                    // The image is not initialized yet.
                    return false
                }

                val factor = scaleDetector.scaleFactor
                mWorkingMatrix!!.postScale(
                    factor,
                    factor,
                    scaleDetector.focusX,
                    scaleDetector.focusY
                )

                // Make sure it's not too large or too small: not larger than MAX_SCALE_FACTOR the screen size,
                // and not smaller of either the screen size or the actual image size.
                mWorkingMatrix!!.mapRect(mWorkingRect, mInitialRect)

                val width = mWorkingRect.width()
                val height = mWorkingRect.height()

                // Prevent scaling too much: much bigger than the screen size or overall pixel count too high.
                if (width > mScreenRect.width() * MAX_SCALE_FACTOR ||
                    height > mScreenRect.height() * MAX_SCALE_FACTOR ||
                    width * height > MAX_SCALED_PIXELS) {
                    mWorkingMatrix!!.set(mMatrix)
                    return true
                }

                if (/* covers cut out area */(mAvatarUpload && width >= mCutOutRect.width() && height >= mCutOutRect.height())
                    || ( /* not too small */!mAvatarUpload && (width >= mInitialRect.width() || width >= mScreenRect.width() || height >= mScreenRect.height()))
                ) {
                    mMatrix!!.set(mWorkingMatrix)
                    mImageView.imageMatrix = mMatrix
                } else {
                    // Skip the change: the image is too large or too small already.
                    mWorkingMatrix!!.set(mMatrix)
                }
                return true
            }
        }
        mScaleGestureDetector = ScaleGestureDetector(activity, scaleListener)

        view.setOnTouchListener { v: View?, event: MotionEvent? ->
            if (mWorkingMatrix == null) {
                // The image is invalid. Disable scrolling/panning.
                return@setOnTouchListener false
            }
            mGestureDetector.onTouchEvent(event)
            mScaleGestureDetector.onTouchEvent(event)
            true
        }


        // Send message on button click.
        view.findViewById<View>(R.id.chatSendButton).setOnClickListener { v: View? -> sendImage() }

        // Upload avatar.
        view.findViewById<View>(R.id.acceptAvatar).setOnClickListener { v: View? -> acceptAvatar() }

        // Send message on Enter.
        (view.findViewById<View>(R.id.editMessage) as EditText).setOnEditorActionListener { v: TextView?, actionId: Int, event: KeyEvent? ->
            sendImage()
            true
        }

        return view
    }

    fun sendImage(){

    }

    fun acceptAvatar(){

    }

    fun showImage(uri: Uri?){
        if (uri != null) {

            // Local image.
            val resolver = requireActivity().contentResolver

            // Resize image to ensure it's under the maximum in-band size.
            try {
                var inputStream = resolver.openInputStream(uri)
                if (inputStream != null) {
                    bmp = BitmapFactory.decodeStream(inputStream, null, null)

                    val exif = ExifInterface(inputStream)
                    val orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_UNDEFINED
                    )
                    if (bmp != null) {
                        bmp = ImagesHelper.rotateBitmap(bmp!!, orientation)
                    }

                    inputStream.close()
                }else{
                    return
                }


            } catch (ex: IOException) {
                Log.i("", "Failed to read image from $uri", ex)
            }
        }

        if (bmp != null) {
            // Must ensure the bitmap is not too big (some cameras can produce
            // bigger bitmaps that the phone can render)
            bmp = ImagesHelper.scaleBitmap(
                bmp!!,
               MAX_BITMAP_DIM,
                MAX_BITMAP_DIM
            )

            mImageView.enableOverlay(mAvatarUpload)

            root.findViewById<View>(R.id.metaPanel).visibility = View.VISIBLE

            mInitialRect = RectF(0f, 0f, bmp!!.width.toFloat(), bmp!!.height.toFloat())
            mWorkingRect = RectF(mInitialRect)

            mImageView.setImageDrawable(BitmapDrawable(resources, bmp))


        }


        mWorkingMatrix = Matrix(mMatrix)
        mImageView.imageMatrix = mMatrix

    }

    override fun onResume() {
        super.onResume()

        val activity: Activity? = requireActivity()


//        val toolbar = activity.findViewById<Toolbar>(R.id.toolbar)
//        if (toolbar != null) {
//            toolbar.setTitle(R.string.image_preview)
//            toolbar.subtitle = null
//            toolbar.logo = null
//        }

        //mAvatarUpload = args.getBoolean(AttachmentHandler.ARG_AVATAR)
        mRemoteState = RemoteState.NONE

        mMatrix!!.reset()

        // ImageView is not laid out at this time. Must add an observer to get the size of the view.
        mImageView.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                // Ensure we call it only once.
                mImageView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                mScreenRect = RectF(
                    0f, 0f, mImageView.width.toFloat(),
                    mImageView.height.toFloat()
                )
                mCutOutRect = RectF()
                if (mScreenRect.width() > mScreenRect.height()) {
                    mCutOutRect.left = (mScreenRect.width() - mScreenRect.height()) * 0.5f
                    mCutOutRect.right = mCutOutRect.left + mScreenRect.height()
                    mCutOutRect.top = 0f
                    mCutOutRect.bottom = mScreenRect.height()
                } else {
                    mCutOutRect.top = (mScreenRect.height() - mScreenRect.width()) * 0.5f
                    mCutOutRect.bottom = mCutOutRect.top + mScreenRect.width()
                    mCutOutRect.left = 0f
                    mCutOutRect.right = mScreenRect.width()
                }

                // Load bitmap into ImageView.
                //loadImage(activity, args)
            }
        })
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ImageViewFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ImageViewFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    private fun translateToBoundsX(bounds: RectF): Float {
        var left = mWorkingRect.left
        left = if (mWorkingRect.width() >= bounds.width()) {
            // Image wider than the viewport.
            max(
                min(bounds.left.toDouble(), left.toDouble()),
                (bounds.left + bounds.width() - mWorkingRect.width()).toDouble()
            )
                .toFloat()
        } else {
            min(
                max(bounds.left.toDouble(), left.toDouble()),
                (bounds.left + bounds.width() - mWorkingRect.width()).toDouble()
            )
                .toFloat()
        }
        return left - mWorkingRect.left
    }

    private fun translateToBoundsY(bounds: RectF): Float {
        var top = mWorkingRect.top
        top = if (mWorkingRect.height() >= bounds.height()) {
            // Image taller than the viewport.
            max(
                min(bounds.top.toDouble(), top.toDouble()),
                (bounds.top + bounds.height() - mWorkingRect.height()).toDouble()
            )
                .toFloat()
        } else {
            min(
                max(bounds.top.toDouble(), top.toDouble()),
                (bounds.top + bounds.height() - mWorkingRect.height()).toDouble()
            )
                .toFloat()
        }
        return top - mWorkingRect.top
    }

    interface AvatarCompletionHandler {
        fun onAcceptAvatar(topicName: String?, avatar: Bitmap?)
    }
}