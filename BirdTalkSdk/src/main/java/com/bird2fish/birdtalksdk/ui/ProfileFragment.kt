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
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import com.yalantis.ucrop.UCrop
import com.bird2fish.birdtalksdk.R
import com.bird2fish.birdtalksdk.uihelper.ImagesHelper
import com.bird2fish.birdtalksdk.uihelper.PermissionsHelper
import com.bird2fish.birdtalksdk.uihelper.TextHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class ProfileFragment : Fragment() {


    private lateinit var profileImageView: ImageView

    private lateinit var permissionsHelper: PermissionsHelper

    private lateinit var cropLauncher: ActivityResultLauncher<Intent>
    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>
    private lateinit var takePhotoLauncher: ActivityResultLauncher<Intent>

    //private val REQUEST_IMAGE_CAPTURE = 1
    private var photoUri: Uri? = null


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
                    //profileImageView.setImageURI(it)
                    val bitmap = ImagesHelper.loadRoundAvatar(it, this.requireContext())
                    profileImageView.setImageBitmap(bitmap)
                    TextHelper.showToast(this.requireContext(), "裁剪成功: ${it.toString()}")
                }
            } else if (result.resultCode == UCrop.RESULT_ERROR) {
                val error = UCrop.getError(result.data!!)
                error?.let {
                    TextHelper.showToast(this.requireContext(), "裁剪出错: ${it.message}")
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

        profileImageView = view.findViewById(R.id.imageViewIcon)
        profileImageView.setImageResource(R.drawable.icon4)
        profileImageView.setOnClickListener { showImagePickerDialog() }

        permissionsHelper = PermissionsHelper(this.requireActivity())
        return view
    }

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