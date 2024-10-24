package com.bird2fish.birdtalksdk.uihelper

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionsHelper(private val activity: Activity) {

    // 定义请求代码
    companion object {
        const val PERMISSION_REQUEST_CODE = 100
        const val PERMISSION_REQUEST_CAMERA = 101
        const val PERMISSION_REQUEST_GALLERY = 102
        const val PERMISSION_REQUEST_LOCATION = 103
        const val PERMISSION_REQUEST_RECORD_AUDIO = 104
        const val PERMISSION_REQUEST_WRITE_STORAGE = 105
    }

    // 检查是否有相机权限
    fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    // 检查是否有存储权限（相册访问需要存储权限）
    fun hasGalleryPermission(): Boolean {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    // 检查是否有定位权限
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    // 录音
    fun hasAudioRecordPermission(): Boolean{
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    // 外置文件读写
    fun hasWritePermission(): Boolean{
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    fun hasReadPermission(): Boolean {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    // 请求相机权限
    fun requestCameraPermission() {
        ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.CAMERA), PERMISSION_REQUEST_CAMERA)
    }

    // 请求相册权限
    fun requestGalleryPermission() {
        ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSION_REQUEST_GALLERY)
    }

    // 请求定位权限
    fun requestLocationPermission() {
        ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_LOCATION)
    }





    // 处理权限请求结果
    fun onRequestPermissionsResult(requestCode: Int, grantResults: IntArray): Boolean {
        when (requestCode) {
            PERMISSION_REQUEST_CAMERA -> {
                return grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            }
            PERMISSION_REQUEST_GALLERY -> {
                return grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            }
            PERMISSION_REQUEST_LOCATION -> {
                return grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            }
        }
        return false
    }
}
