package com.bird2fish.birdtalksdk.net

import java.math.BigInteger
import java.util.zip.CRC32

// CRC64 实现
object CRC64 {
    private val CRC64_TABLE = LongArray(256)

    init {
        // 使用 BigInteger 来避免溢出问题
        val poly = BigInteger("C96C5795D7870F42", 16) // CRC64-ISO polynomial

        for (i in 0 until 256) {
            var crc = i.toLong()
            for (j in 0 until 8) {
                if ((crc and 1L) == 1L) {
                    crc = (crc ushr 1) xor poly.toLong()
                } else {
                    crc = crc ushr 1
                }
            }
            CRC64_TABLE[i] = crc
        }
    }

    fun crc64(data: ByteArray): Long {
        var crc = -1L
        for (byte in data) {
            crc = (crc ushr 8) xor CRC64_TABLE[((crc xor byte.toLong()) and 0xFF).toInt()]
        }
        return crc
    }
}