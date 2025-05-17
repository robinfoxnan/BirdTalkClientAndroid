package com.bird2fish.birdtalksdk.net

import java.security.*
import java.security.spec.X509EncodedKeySpec
import javax.crypto.KeyAgreement
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.io.File
import kotlin.experimental.and
import android.util.Base64
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ECDHKeyExchange {
    private var privateKey: PrivateKey? = null
    private var publicKey: PublicKey? = null
    private var sharedKey: ByteArray? = null
    //private var sharedKeyHash : ByteArray? = null   // 共享密钥通过SHA256得到一个哈希，用于对称秘钥
    private var keyPrint: Long = 0L

    // 如果本地有KEY，则使用旧的
    fun setLocalShare(keyPrint:Long, key:ByteArray){
        this.keyPrint = keyPrint
        this.sharedKey = key
    }

    fun clear(){
        this.keyPrint = 0L
        this.sharedKey = null
        privateKey = null
        publicKey = null
    }

    fun generateKeyPair() {
        val keyPairGenerator = KeyPairGenerator.getInstance("EC")
        keyPairGenerator.initialize(256)
        val keyPair = keyPairGenerator.generateKeyPair()
        privateKey = keyPair.private
        publicKey = keyPair.public
    }

    fun exportPublicKey(): String {
        requireNotNull(publicKey) { "Public key not generated yet." }
        return Base64.encodeToString(publicKey!!.encoded, Base64.DEFAULT)
    }

    fun exportPublicKeyToPem(): ByteArray {
        if (publicKey == null){
            return "".toByteArray(Charsets.UTF_8)
        }
        val base64EncodedKey = Base64.encodeToString(this.publicKey!!.encoded, Base64.NO_WRAP)
        val pem = """
        -----BEGIN PUBLIC KEY-----
        $base64EncodedKey
        -----END PUBLIC KEY-----
    """.trimIndent()
        return pem.toByteArray(Charsets.UTF_8)
    }


    fun importPublicKeyFromPem(pemBytes: ByteArray): PublicKey {
        val pem = String(pemBytes, Charsets.UTF_8)
        val base64EncodedKey = pem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\n", "")
            .replace("\r", "")
        val keyBytes = Base64.decode(base64EncodedKey, Base64.DEFAULT)
        val keySpec = X509EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("EC") // 或 "RSA"，取决于你的密钥类型
        return keyFactory.generatePublic(keySpec)
    }


    fun importPeerPublicKey(base64Key: String): PublicKey {
        val keyBytes = Base64.decode(base64Key, Base64.DEFAULT)
        val keySpec = X509EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("EC")
        return keyFactory.generatePublic(keySpec)
    }

    fun exchangeKeys(peerPublicKey: PublicKey):Boolean {
        requireNotNull(privateKey) { "Private key not generated yet." }

        try {
            val keyAgreement = KeyAgreement.getInstance("ECDH")
            keyAgreement.init(privateKey)
            keyAgreement.doPhase(peerPublicKey, true)
            sharedKey = keyAgreement.generateSecret()
            return true
        } catch (e: Exception) {
            // 处理异常
            println("Error during key exchange: ${e.message}")
        }
        return false
    }

    // 共享密钥使用BASE64传递
    fun getSharedKeyBase64(): String {
        requireNotNull(sharedKey) { "Shared key not generated yet." }
        return Base64.encodeToString(sharedKey, Base64.DEFAULT)
    }

    fun getSharedKey(): ByteArray? {
        return this.sharedKey
    }

    // 加密
    fun encryptAESCTR(plaintext: ByteArray): ByteArray {
        requireNotNull(sharedKey) { "Shared key not generated yet." }
        val iv = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        val cipher = Cipher.getInstance("AES/CTR/NoPadding")
        val secretKey = SecretKeySpec(sharedKey!!, "AES")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))
        return iv + cipher.doFinal(plaintext)
    }

    // 使用 AES CTR 模式加密字符串并输出为 Base64 格式
    fun encryptAESCTR_Str2Base64(data: String): String {
        requireNotNull(sharedKey) { "Shared key not generated yet." }
        val plaintext = data.toByteArray(Charsets.UTF_8)

        // 加密后将结果拼接 IV 和密文，并进行 Base64 编码
        val ciphertext = encryptAESCTR(plaintext)
        return Base64.encodeToString(ciphertext, Base64.NO_WRAP)
    }

    // 使用 AES CTR 模式解密 Base64 格式的密文为字符串
    fun decryptAESCTR_StrBase64(data: String): String {
        requireNotNull(sharedKey) { "Shared key not generated yet." }

        // 将 Base64 解码后的数据分割为 IV 和加密数据
        val decodedData = Base64.decode(data, Base64.DEFAULT)
        val plaintext = decryptAESCTR(decodedData)
        return String(plaintext, Charsets.UTF_8)
    }


    // AES CTR 解密
    fun decryptAESCTR(ciphertext: ByteArray): ByteArray {
        requireNotNull(sharedKey) { "Shared key not generated yet." }

        // 从密文中提取初始化向量（IV）
        val iv = ciphertext.copyOf(16)
        val encryptedData = ciphertext.copyOfRange(16, ciphertext.size)
        val cipher = Cipher.getInstance("AES/CTR/NoPadding")

        val secretKey = SecretKeySpec(sharedKey!!, "AES")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
        val plain = cipher.doFinal(encryptedData)
       // val txt = String(plain, Charsets.UTF_8)
        return plain
    }

    fun bytesToInt64Little(data: ByteArray): Long {
        if (data.size < 8) {
           // throw IllegalArgumentException("Insufficient bytes to convert to Long (requires 8 bytes)")
            return -1
        }

        // 使用 ByteBuffer 处理小端字节序
        return ByteBuffer.wrap(data, 0, 8)
            .order(ByteOrder.LITTLE_ENDIAN)
            .long
    }

    // 计算那个key，取低8字节
    fun calculateKeyPrint(): Long {
        requireNotNull(sharedKey) { "Shared key not generated yet." }

        this.keyPrint= bytesToInt64Little(sharedKey!!)
        return keyPrint
    }

    fun getKeyPrint(): Long {
        return keyPrint
    }
}

/*
fun main() {
    val alice = ECDHKeyExchange()
    alice.generateKeyPair()
    val alicePubKey = alice.exportPublicKey()
    println("Alice's Public Key: $alicePubKey")

    val bob = ECDHKeyExchange()
    bob.generateKeyPair()
    val bobPubKey = bob.exportPublicKey()
    println("Bob's Public Key: $bobPubKey")

    val bobPeerKey = alice.importPeerPublicKey(bobPubKey)
    alice.exchangeKeys(bobPeerKey)

    val sharedKey = alice.getSharedKeyBase64()
    println("Shared Key: $sharedKey")
}


val keyPairGenerator = java.security.KeyPairGenerator.getInstance("EC")
keyPairGenerator.initialize(256)
val keyPair = keyPairGenerator.generateKeyPair()

// 导出公钥为 PEM 格式
val pemBytes = exportPublicKeyToPem(keyPair.public)
println(String(pemBytes))

// 从 PEM 格式导入公钥
val importedPublicKey = importPublicKeyFromPem(pemBytes)
println("Keys are equal: ${keyPair.public == importedPublicKey}")
 */