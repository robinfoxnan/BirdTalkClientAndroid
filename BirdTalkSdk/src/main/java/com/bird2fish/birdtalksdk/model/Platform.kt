package com.bird2fish.birdtalksdk.model

import android.os.Build
import android.provider.Settings
import com.bird2fish.birdtalksdk.SdkGlobalData.Companion.context
import com.bird2fish.birdtalksdk.net.CRC64


data class PlatformInfo(
    val platform: String,      // 平台名称，如 "Mobile", "Tablet"
    val os: String,            // 操作系统名称，如 "Android"
    val osVersion: String,     // 操作系统版本，如 "13" 或 "API 33"
    val programmingLang: String, // 编程语言，如 "Kotlin"
    val langVersion: String,   // 编程语言版本，如 "1.8.20"
    val deviceModel: String? = null, // 设备型号（可选）
    val appVersion: String? = null,  // 应用版本（可选）
    var deviceId: String? = "",
    var osInfo :String? = "",
    var sdkVersion :String = "1.0"
)

object Platform {
   fun getBasic(): PlatformInfo{
        return PlatformInfo(
            platform = "${Build.MANUFACTURER}",
            os = "${Build.BRAND}",
            osVersion = android.os.Build.VERSION.RELEASE,
            programmingLang = "Kotlin",
            langVersion = "1.8.20",
            deviceModel = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
            appVersion = "0.1",
            deviceId = ""

        )
    }

    // OS Version: 10, SDK Version: 29, OS Code Name: Q, OS: google, Build ID: QKQ1.190828.002, Security Patch Level: 2020-10-05
    fun getOSInfo(): String {
        val osInfo = StringBuilder()

        // Android 版本
        osInfo.append("OSVersion: ${Build.VERSION.RELEASE}, ")

        // SDK 版本
        osInfo.append("SDKVersion: ${Build.VERSION.SDK_INT}, ")

        // 系统版本号
        osInfo.append("OSCodeName: ${Build.VERSION.CODENAME}, ")

        // 操作系统类型（比如 `android`）
        osInfo.append("OS: ${Build.BRAND}, ")

        // 系统构建版本
        osInfo.append("BuildID: ${Build.ID}, ")

        // 系统版本的安全补丁等级
        osInfo.append("SecurityPatchLevel: ${Build.VERSION.SECURITY_PATCH}")

        return osInfo.toString()
    }

    fun crc64ToHex(crc64Value: Long): String {
        return String.format("%016X", crc64Value)
    }

    fun generateUniqueDeviceId(): String {
        // 获取设备的唯一信息
        val deviceInfo = StringBuilder()

        // 添加设备信息
        deviceInfo.append("Manufacturer: ${Build.MANUFACTURER}, ")
        deviceInfo.append("Model: ${Build.MODEL}, ")
        deviceInfo.append("Fingerprint: ${Build.FINGERPRINT}, ")
        val androidId = Settings.Secure.getString(context!!.contentResolver, Settings.Secure.ANDROID_ID)
        deviceInfo.append("AndroidId: $androidId, ")

        // 使用 CRC64 计算设备的唯一 ID
        val deviceInfoBytes = deviceInfo.toString().toByteArray()
        val crc64Value = CRC64.crc64(deviceInfoBytes)

        // 将 CRC64 值转为十六进制字符串
        return crc64ToHex(crc64Value)
    }
}
