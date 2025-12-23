package com.bird2fish.birdtalksdk.uihelper
import android.provider.Settings.Global
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import android.util.Base64
import android.util.Log
import com.bird2fish.birdtalksdk.SdkGlobalData
import com.bird2fish.birdtalksdk.net.MsgEncocder
import com.bird2fish.birdtalksdk.net.WebSocketClient
import java.sql.Time

object CryptHelper {

    fun getUrl(filename:String) : String{
        try {
            val sharedKey = MsgEncocder.getKeyExchange().getSharedKey() ?: return ""

            // 秒级别
            val expires = System.currentTimeMillis()/1000 + 5 * 60
            val sign = encryptFileToken(sharedKey, filename, expires)
            val u = MsgEncocder.getKeyExchange().getKeyPrintString()
            val url = WebSocketClient.instance!!.getRemoteFilePathWithSign(sign, u)
            return url
        }catch (e:Exception){

            Log.e("CryptHelper", e.toString())
            return ""
        }
        return ""
    }

    @Throws(Exception::class)
    fun encryptFileToken(
        secretKey: ByteArray,
        filename: String,
        expires: Long
    ): String {

        // 拼接明文：expires|filename
        val data = "$expires|$filename"
        val plaintext = data.toByteArray(Charsets.UTF_8)

        // AES key
        val keySpec = SecretKeySpec(secretKey, "AES")

        // AES-GCM
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")

        // GCM nonce (12 bytes is standard)
        val nonce = ByteArray(12)
        SecureRandom().nextBytes(nonce)

        val gcmSpec = GCMParameterSpec(128, nonce) // 128-bit auth tag
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)

        // 加密
        val ciphertext = cipher.doFinal(plaintext)

        // nonce + ciphertext
        val combined = ByteArray(nonce.size + ciphertext.size)
        System.arraycopy(nonce, 0, combined, 0, nonce.size)
        System.arraycopy(ciphertext, 0, combined, nonce.size, ciphertext.size)

        // Base64 URL Safe（无 padding）
        return Base64.encodeToString(
            combined,
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
        )
    }

    @Throws(Exception::class)
    fun decryptFileToken(
        secretKey: ByteArray,
        token: String
    ): Pair<Long, String> {

        // Base64 URL Safe 解码（等价 base64.RawURLEncoding）
        val raw = Base64.decode(
            token,
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
        )

        // AES key
        val keySpec = SecretKeySpec(secretKey, "AES")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")

        // GCM nonce size（Go 默认 12 bytes）
        val nonceSize = 12
        if (raw.size < nonceSize) {
            throw IllegalArgumentException("invalid token")
        }

        // 拆分 nonce 和 ciphertext
        val nonce = raw.copyOfRange(0, nonceSize)
        val ciphertext = raw.copyOfRange(nonceSize, raw.size)

        val gcmSpec = GCMParameterSpec(128, nonce)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)

        // 解密
        val plaintextBytes = cipher.doFinal(ciphertext)
        val plaintext = plaintextBytes.toString(Charsets.UTF_8)

        // SplitN("|", 2)，防止文件名里有 |
        val parts = plaintext.split("|", limit = 2)
        if (parts.size != 2) {
            throw IllegalArgumentException("invalid plaintext format")
        }

        // 解析 expires
        val expires = parts[0].toLongOrNull()
            ?: throw IllegalArgumentException("invalid expires")

        val filename = parts[1]

        return Pair(expires, filename)
    }
}