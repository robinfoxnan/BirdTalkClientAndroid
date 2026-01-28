package com.bird2fish.birdtalksdk.uihelper

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.bird2fish.birdtalksdk.InterErrorType
import com.bird2fish.birdtalksdk.MsgEventType
import com.bird2fish.birdtalksdk.SdkGlobalData
import com.bird2fish.birdtalksdk.StatusCallback
import com.bird2fish.birdtalksdk.model.User
import com.bird2fish.birdtalksdk.net.Session
import com.yalantis.ucrop.UCrop
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AvatarUploadHelper(private val fragment: Fragment): StatusCallback {

    private var photoUri: Uri? = null
    private var localUploadName : String? = ""
    private var uuidImageName:String = ""

    var onUploadOk: ((localUri:Uri?, localName:String, uuidName:String) -> Unit)? = null
    var onUploadErr: ((localUri:Uri?, localName:String, uuidName:String) -> Unit)? = null

    private lateinit var cropLauncher: ActivityResultLauncher<Intent>
    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>
    private lateinit var takePhotoLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionsHelper: PermissionsHelper

    // 私有化构造函数，强制通过 withContext 方法创建（可选，更规范）
    private constructor(fragment: Fragment, unused: Boolean) : this(fragment)

    companion object {
        // 🔥 推荐：通过静态方法创建实例，自动处理 ApplicationContext
        fun withContext(fragment: Fragment): AvatarUploadHelper {
            // 强制使用 ApplicationContext，避免持有 Activity/Fragment 导致内存泄漏
            return AvatarUploadHelper(fragment, true)
        }

        // 头像存储目录（自定义）
        private const val AVATAR_DIR = "avatar"
        // 头像默认文件名
        private const val AVATAR_FILE_NAME = "user_avatar.jpg"
    }

    fun showImagePickerDialog() {
        val options = arrayOf("从相册选择", "拍照")
        AlertDialog.Builder(fragment.requireContext())
            .setTitle("选择头像")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> openGallery()
                    1 -> openCamera()
                }
            }
            .show()
    }

    // 开始浏览图片
    fun openGallery() {
        if (!permissionsHelper.hasGalleryPermission()){
            permissionsHelper.requestGalleryPermission()
            return
        }
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun openCamera() {
        if (!permissionsHelper.hasCameraPermission()) {
            permissionsHelper.requestCameraPermission()
            return
        }
// 获取当前时间作为文件名
        val timeStampFormat = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
        val filename = "${timeStampFormat.format(Date())}.jpg"

        // 使用本地相册保存拍摄照片
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES) // 保存到Pictures目录
        }

        this.photoUri = null

        // 插入图片信息到媒体库
        this.photoUri = fragment.requireContext().contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        if (photoUri != null) {
            // 创建启动相机的Intent
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, photoUri) // 设置图片保存路径
            }

            try {
                takePhotoLauncher.launch(intent)
            } catch (e: ActivityNotFoundException) {
                runOnMainThread {
                    TextHelper.showToast(fragment.requireContext(), "摄像头未准备好！")
                }

            }
        } else {
            runOnMainThread {
                TextHelper.showToast(fragment.requireContext(), "无法保存照片")
            }
        }
    }

    private fun startCrop(uri: Uri) {
        var dir = getScreenshotFolderPath(fragment.requireContext())
        val destinationUri = Uri.fromFile(File(dir, System.currentTimeMillis().toString() + ".jpg") )
        //Uri.fromFile(File(requireContext().cacheDir, "croppedImage.jpg"))
        val uCrop = UCrop.of(uri, destinationUri)
        uCrop.withAspectRatio(1f, 1f)
        uCrop.withMaxResultSize(500, 500)

        //uCrop.start(this.requireActivity())

        val intent = uCrop.getIntent(fragment.requireContext())
        cropLauncher.launch(intent)
    }

    fun getScreenshotFolderPath(context: Context): String? {
        // 定义查询参数
        //val internalStorageDir = context.filesDir

        val dicmDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "birdtalk")
        if (!dicmDir.exists())
        {
            val ret = dicmDir.mkdirs()
            if (ret == false)
                return null
        }
        return dicmDir.path
    }

    fun initHelper(avatarView:ImageView){

        // 获取权限来加载相册
        permissionsHelper = PermissionsHelper(fragment.requireActivity())

        // Register the launcher for picking an image
        pickImageLauncher = fragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    startCrop(uri)
                }
            }
        }

        cropLauncher = fragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val resultUri = UCrop.getOutput(result.data!!)
                resultUri?.let {
                    // 设置图片
                    //profileImageView.setImageURI(it)
                    val bitmap = ImagesHelper.loadRoundAvatar(it, fragment.requireContext())
                    synchronized(avatarView) {
                        avatarView.setImageBitmap(bitmap)
                    }
                    //TextHelper.showToast(this.requireContext(), "裁剪成功: ${it.toString()}")
                    photoUri = resultUri

                    // 尝试上传
                    this.photoUri?.let{
                        // 设置关注返回的文件消息
                        SdkGlobalData.userCallBackManager.addCallback(this)

                        this.localUploadName = TextHelper.getFileNameFromUri(fragment.requireContext(), this.photoUri)
                        val msgId = SdkGlobalData.nextId()
                        Session.uploadSmallFile(fragment.requireContext(), it, 0, msgId)
                    }
                }
            } else if (result.resultCode == UCrop.RESULT_ERROR) {
                photoUri = null
                val error = UCrop.getError(result.data!!)
                error?.let {
                    TextHelper.showToast(fragment.requireContext(), "裁剪出错: ${it.message}")
                }
            }
        }

        // 拍照的结果
        takePhotoLauncher = fragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {

                var tempPhotoUri : Uri? = null
                if (result.data == null){
                    if (this.photoUri != null){

                        tempPhotoUri = this.photoUri!!
                    }else{
                        return@registerForActivityResult
                    }

                }else{
                    tempPhotoUri = result.data as Uri
                }

                val filePath = getFilePathFromUri(tempPhotoUri)
                if (filePath != null) {
                    startCrop(tempPhotoUri)
                } else {
                    runOnMainThread {
                        TextHelper.showToast(fragment.requireContext(), "无法获取照片路径")
                    }
                }
            } else {
                runOnMainThread {
                    TextHelper.showToast(fragment.requireContext(), "拍照失败")
                }

            }
        }
    }

    private fun getFilePathFromUri(uri: Uri): String? {
        var path: String? = null
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = fragment.requireContext().contentResolver.query(uri, projection, null, null, null)

        cursor?.use {
            if (it.moveToFirst()) {
                val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                path = it.getString(columnIndex)
            }
        }

        return path
    }


    override fun onError(code : InterErrorType, lastAction:String, errType:String, detail:String){
        if (this.onUploadErr != null){
            // 上传成功：触发 onUploadOk 回调（切换到主线程）
            runOnMainThread {
                onUploadErr?.invoke(this.photoUri, this.localUploadName!!, detail)
            }
        }
    }

    /**
     * 自定义工具方法：将代码块切换到主线程执行
     * @param block 要在主线程执行的代码（如更新UI、触发回调）
     */
    private fun runOnMainThread(block: () -> Unit) {
        // 情况1：上下文是Activity（可直接调用Activity的runOnUiThread）
        if (fragment.requireActivity() is android.app.Activity) {
            fragment.requireActivity().runOnUiThread(block)
        } else {
            // 情况2：上下文是ApplicationContext（用Handler切换到主线程）
            android.os.Handler(android.os.Looper.getMainLooper()).post(block)
        }
    }


    // 上传或下载事件
    // 这里是回调函数，无法操作界面
    override fun onEvent(eventType: MsgEventType, msgType:Int, msgId:Long, fid:Long, params:Map<String, String>){
        if (eventType == MsgEventType.MSG_UPLOAD_OK){

            val fileName = params.get("fileName")
            if (fileName == this.localUploadName){
                params.get("uuidName")?.let {

                    uuidImageName = it
                    SdkGlobalData.userCallBackManager.removeCallback(this)
                    if (this.onUploadOk != null){
                        // 上传成功：触发 onUploadOk 回调（切换到主线程）
                        runOnMainThread {
                            this.onUploadOk?.invoke(this.photoUri, this.localUploadName!!, uuidImageName)
                        }
                    }
                }
            }

        }else if (eventType == MsgEventType.MSG_UPLOAD_FAIL){
            TextHelper.showDialogInCallback(fragment.requireContext(), "上传头像失败")
        }
    }

}