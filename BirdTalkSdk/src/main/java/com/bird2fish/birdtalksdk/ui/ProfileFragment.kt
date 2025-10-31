package com.bird2fish.birdtalksdk.ui

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Config
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.bird2fish.birdtalksdk.InterErrorType
import com.bird2fish.birdtalksdk.MsgEventType
import java.io.File
import com.yalantis.ucrop.UCrop
import com.bird2fish.birdtalksdk.R
import com.bird2fish.birdtalksdk.SdkGlobalData
import com.bird2fish.birdtalksdk.StatusCallback
import com.bird2fish.birdtalksdk.net.ImageDownloader
import com.bird2fish.birdtalksdk.net.MsgEncocder
import com.bird2fish.birdtalksdk.net.Session
import com.bird2fish.birdtalksdk.uihelper.AvatarHelper
import com.bird2fish.birdtalksdk.uihelper.ImagesHelper
import com.bird2fish.birdtalksdk.uihelper.PermissionsHelper
import com.bird2fish.birdtalksdk.uihelper.TextHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class ProfileFragment : Fragment(), StatusCallback {


    private lateinit var profileImageView: ImageView

    private lateinit var permissionsHelper: PermissionsHelper

    private lateinit var cropLauncher: ActivityResultLauncher<Intent>
    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>
    private lateinit var takePhotoLauncher: ActivityResultLauncher<Intent>

    //private val REQUEST_IMAGE_CAPTURE = 1
    private var photoUri: Uri? = null

    private  lateinit var tvId : TextView
    private  lateinit var tvName : TextView
    private  lateinit var tvNick : EditText
    private  lateinit var tvAge : EditText
    private  lateinit var tvGender : EditText
    private  lateinit var tvEmail : EditText
    private  lateinit var tvPhone : EditText
    private  lateinit var tvIntro : EditText
    private lateinit var  tvPwd :EditText

    private  lateinit var btnSave :Button

//    private var localImageName : String? = ""
    private var localUploadName : String? = ""    //上传

    private val _uuidImageName = MutableLiveData<String?>("")
    val uuidImageName: LiveData<String?> = _uuidImageName

    override fun onError(code : InterErrorType, lastAction:String, errType:String, detail:String){

    }


    // 上传或下载事件
    // 这里是回调函数，无法操作界面
    override fun onEvent(eventType: MsgEventType, msgType:Int, msgId:Long, fid:Long, params:Map<String, String>){
        if (eventType == MsgEventType.MSG_UPLOAD_OK){

            val fileName = params.get("fileName")
            if (fileName == this.localUploadName){
                params.get("uuidName")?.let {
                    SdkGlobalData.selfUserinfo.icon = it
                    this._uuidImageName.postValue(it)
                    // 这里应该发送消息，重新设置头像
                    saveUserAvatar(it)
                    this.localUploadName = ""
                    onHide()
                }
            }


        }else if (eventType == MsgEventType.MSG_UPLOAD_FAIL){
            TextHelper.showDialogInCallback(this.requireContext(), "上传头像失败")
        }else if (eventType == MsgEventType.USR_UPDATEINFO_OK){
            TextHelper.showDialogInCallback(this.requireContext(), "保存信息完毕")
        }else if (eventType == MsgEventType.USR_UPDATEINFO_FAIL){
            TextHelper.showDialogInCallback(this.requireContext(), "更新失败")
        }
    }

    // js:
    // paramsMap.set("UserName", "Robin.fox");
    //        paramsMap.set("NickName", "飞鸟真人");
    //        paramsMap.set("Age", "35");
    //        paramsMap.set("Intro", "我是一个爱运动的博主>_<...");
    //        paramsMap.set("Gender", "男");
    //        paramsMap.set("Region", "北京");
    //        paramsMap.set("Icon", "飞鸟真人");
    //        paramsMap.set("Params.title", "经理")

    // 上传成功后，需要保存一下
    // 保存头像
    private fun saveUserAvatar(newIcon:String){

        val data = mapOf(
            "Icon" to newIcon,
            //"Params.title" to "setting"
        )
        System.out.println("change avatar "+ newIcon)
        MsgEncocder.setUserInfo(SdkGlobalData.selfUserinfo.getId(), data)
    }

    private fun getFilePathFromUri(uri: Uri): String? {
        var path: String? = null
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = requireContext().contentResolver.query(uri, projection, null, null, null)

        cursor?.use {
            if (it.moveToFirst()) {
                val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                path = it.getString(columnIndex)
            }
        }

        return path
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        // Register the launcher for picking an image
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    startCrop(uri)
                }
            }
        }

        // 拍照的结果
        takePhotoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
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
                    TextHelper.showToast(requireContext(), "无法获取照片路径")
                }
            } else {
                TextHelper.showToast(requireContext(), "拍照失败")
            }
        }

        cropLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val resultUri = UCrop.getOutput(result.data!!)
                resultUri?.let {
                    // 设置图片
                    //profileImageView.setImageURI(it)
                    val bitmap = ImagesHelper.loadRoundAvatar(it, this.requireContext())
                    synchronized(profileImageView) {
                        profileImageView.setImageBitmap(bitmap)
                    }
                    //TextHelper.showToast(this.requireContext(), "裁剪成功: ${it.toString()}")
                    photoUri = resultUri

                    // 尝试上传
                    this.photoUri?.let{
                        // 设置关注返回的文件消息
                        SdkGlobalData.userCallBackManager.addCallback(this)

                        this.localUploadName = TextHelper.getFileNameFromUri(requireContext(), this.photoUri)
                        val msgId = SdkGlobalData.nextId()
                        Session.uploadSmallFile(requireContext(), it, 0, msgId)
                    }
                }
            } else if (result.resultCode == UCrop.RESULT_ERROR) {
                photoUri = null
                val error = UCrop.getError(result.data!!)
                error?.let {
                    TextHelper.showToast(this.requireContext(), "裁剪出错: ${it.message}")
                }
            }
        }

        // 上传成功了，需要在界面线程中操作
        this.uuidImageName.observe(this) { newValue ->
            // 处理变化
            if (newValue != ""){
                //TextHelper.showToast(requireContext(), "上传头像完毕，重新加载：" + newValue)
                //loadImage(newValue!!)
                synchronized(profileImageView){
                    AvatarHelper.tryLoadAvatar(requireContext(), newValue!!, profileImageView, SdkGlobalData.selfUserinfo.gender)
                }

            }

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_profile, container, false)


        // 初始化控件
        tvId = view.findViewById(R.id.tvID)
        tvName = view.findViewById(R.id.tvName)

        tvNick = view.findViewById(R.id.tv_nick)
        tvGender = view.findViewById(R.id.tvGender)
        tvAge = view.findViewById(R.id.tvAge)
        tvEmail = view.findViewById(R.id.tvEmail)
        tvPhone = view.findViewById(R.id.tvPhone)
        tvIntro = view.findViewById(R.id.tvDes)
        tvPwd = view.findViewById(R.id.tvPass)

        // 手动保存按钮
        btnSave = view.findViewById(R.id.btnSave)
        btnSave.setOnClickListener{
            onSave()
        }
        // 头像
        profileImageView = view.findViewById(R.id.imageViewIcon)
        //profileImageView.setImageResource(R.drawable.icon4)
        //loadLocalAvatar(SdkGlobalData.selfUserinfo.icon)
        synchronized(profileImageView){
            AvatarHelper.tryLoadAvatar(requireContext(), SdkGlobalData.selfUserinfo.icon, profileImageView, SdkGlobalData.selfUserinfo.gender)
        }

        profileImageView.setOnClickListener { showImagePickerDialog() }

        // 获取权限来加载相册
        permissionsHelper = PermissionsHelper(this.requireActivity())
        onShow()


        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 保底清理，防止泄漏
        onHide()
    }

    // 保存个人信息
    fun onSave(){
        // 2. 创建可变 Map（可动态添加/修改元素）
        val data = mutableMapOf<String, String>()

        // 添加键值对
        data["UserName"] = "user"+ SdkGlobalData.selfUserinfo.id.toString()
        val nick = tvNick.text?.toString()?.takeIf { it.isNotEmpty() } ?: "momo"
        data["NickName"] = nick
        val age = tvAge.text?.toString()?.takeIf { it.isNotEmpty() } ?: "0"
        data["Age"] = age
        val gender = tvGender.text?.toString()?.takeIf { it.isNotEmpty() } ?: "-"
        data["Gender"] = gender
        val pwd =  tvPwd.text?.toString()?.takeIf { it.isNotEmpty() } ?: "-"
        if (pwd != null && pwd.isNotEmpty()) {
            data["Pwd"] =pwd
        }

        //data["Phone"] = tvPhone.text?.toString()?.takeIf { it.isNotEmpty() } ?: "-"
        //data["Email"] = tvEmail.text?.toString()?.takeIf { it.isNotEmpty() } ?: "-"
        data["Intro"] = tvIntro.text?.toString()?.takeIf { it.isNotEmpty() } ?: "-"
        //data["Params.Title"] = "先生"

        MsgEncocder.setUserInfo(SdkGlobalData.selfUserinfo.id, data)
    }



    // 当 Fragment 被隐藏或显示时调用
//    override fun onHiddenChanged(hidden: Boolean) {
//        super.onHiddenChanged(hidden)
//        if (!hidden) {
//            onShow()
//        } else {
//            onHide()
//        }
//    }
    fun onShow(){
        // Fragment 可见且可交互时执行
        tvId.text = SdkGlobalData.selfUserinfo.id.toString()
        tvName.text = SdkGlobalData.selfUserinfo.name
        SdkGlobalData.selfUserinfo.nick?.let { tvNick.setText(it) } ?: tvNick.setText("")
        tvAge.setText(SdkGlobalData.selfUserinfo.age.toString())
        tvGender.setText(SdkGlobalData.selfUserinfo.gender)
        tvEmail.setText(SdkGlobalData.selfUserinfo.email)
        tvPhone.setText(SdkGlobalData.selfUserinfo.phone)
        tvIntro.setText(SdkGlobalData.selfUserinfo.introduction)

        // 头像

        //this._uuidImageName.postValue(SdkGlobalData.selfUserinfo.icon)
        //loadImage(SdkGlobalData.selfUserinfo.icon)
        // 关注消息

    }

    fun onHide(){
        SdkGlobalData.userCallBackManager.removeCallback(this)
    }


    // 加载远程的图片，先检查本地是否存在
    // /storage/emulated/0/Android/data/com.bird2fish.birdtalkclient/files/avatar
//    private  fun loadLocalAvatar(iconName:String){
//        val bitmap = ImagesHelper.loadBitmapFromAppDir(requireContext(), "avatar", iconName)
//        if (bitmap != null) {
//            val bitmapRound = ImagesHelper.getRoundAvatar(bitmap, requireContext())
//            profileImageView.setImageBitmap(bitmapRound )
//            return
//        }
//    }
//
//    // 使用pissaco直接从远程获取文件
//    private  fun loadImage(remoteName:String) {
//
//        // 从远程加载
//        val downloader = ImageDownloader()
//        downloader.downloadAndSaveImage(requireContext(), remoteName, "avatar", this.profileImageView, R.drawable.icon19)
//    }



    private fun showImagePickerDialog() {
        val options = arrayOf("从相册选择", "拍照")
        AlertDialog.Builder(requireContext())
            .setTitle("选择头像")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> openGallery()
                    1 -> openCamera()
                }
            }
            .show()
    }

    private fun openGallery() {
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
        this.photoUri = requireContext().contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        if (photoUri != null) {
            // 创建启动相机的Intent
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, photoUri) // 设置图片保存路径
            }

            try {
                takePhotoLauncher.launch(intent)
            } catch (e: ActivityNotFoundException) {
                TextHelper.showToast(requireContext(), "摄像头未准备好！")
            }
        } else {
            TextHelper.showToast(requireContext(), "无法保存照片")
        }
    }

//    private fun openCamera1() {
//        if (!permissionsHelper.hasCameraPermission()) {
//            permissionsHelper.requestCameraPermission()
//            return
//        }
//
//        // 获取当前时间作为文件名
//        val timeStampFormat = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
//        val filename = "${timeStampFormat.format(Date())}.jpg"
//
//        // 使用本地相册保存拍摄照片
//        val values = ContentValues().apply {
//            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
//            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
//            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES) // 保存到Pictures目录
//        }
//
//        // 插入图片信息到媒体库
//        photoUri = requireContext().contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
//
//        if (photoUri != null) {
//            // 创建启动相机的Intent
//            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
//                putExtra(MediaStore.EXTRA_OUTPUT, photoUri) // 设置图片保存路径
//            }
//
//            try {
//                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
//            } catch (e: ActivityNotFoundException) {
//                TextHelper.showToast(requireContext(), "摄像头未准备好！")
//            }
//        } else {
//            TextHelper.showToast(requireContext(), "无法保存照片")
//        }
//    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//
//        if (resultCode == Activity.RESULT_OK) {
//            when (requestCode) {
//                REQUEST_IMAGE_CAPTURE -> {
//                    photoUri?.let {
//                        // 在这里你可以使用 photoUri 来进行下一步操作，例如裁剪
//                        startCrop(it)
//                    } ?: run {
//                        TextHelper.showToast(requireContext(), "无法获取照片 URI")
//                    }
//                }
//            }
//        } else {
//            TextHelper.showToast(requireContext(), "拍照失败")
//        }
//    }



    private fun requestPermissions() {
        val permissions = mutableListOf<String>()

        if (!permissionsHelper.hasCameraPermission()) {
            permissions.add(Manifest.permission.CAMERA)
        }
        if (!permissionsHelper.hasGalleryPermission()) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (!permissionsHelper.hasLocationPermission()) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this.requireActivity(), permissions.toTypedArray(), PermissionsHelper.PERMISSION_REQUEST_CODE)
        }
    }

    // 处理权限请求结果
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PermissionsHelper.PERMISSION_REQUEST_CODE) {
            // 检查每个请求的权限结果
            for (i in permissions.indices) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    // 权限已被授予
                } else {
                    // 权限被拒绝，提示用户
                }
            }
        }
    }

    private fun showPermissionRationale() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this.requireActivity(), Manifest.permission.CAMERA) ||
            ActivityCompat.shouldShowRequestPermissionRationale(this.requireActivity(), Manifest.permission.READ_EXTERNAL_STORAGE) ||
            ActivityCompat.shouldShowRequestPermissionRationale(this.requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION)) {
            // 显示一个解释性的对话框
            AlertDialog.Builder(this.requireActivity())
                .setTitle("权限请求")
                .setMessage("应用需要这些权限来提供完整的功能。")
                .setPositiveButton("确定") { _, _ ->
                    requestPermissions()
                }
                .setNegativeButton("取消") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
                .show()
        } else {
            requestPermissions()
        }
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

    private fun startCrop(uri: Uri) {
        var dir = getScreenshotFolderPath(this.requireActivity())
        val destinationUri = Uri.fromFile(File(dir, System.currentTimeMillis().toString() + ".jpg") )
            //Uri.fromFile(File(requireContext().cacheDir, "croppedImage.jpg"))
        val uCrop = UCrop.of(uri, destinationUri)
        uCrop.withAspectRatio(1f, 1f)
        uCrop.withMaxResultSize(500, 500)

        //uCrop.start(this.requireActivity())

        val intent = uCrop.getIntent(requireContext())
        cropLauncher.launch(intent)
    }


}