package com.student.wifihelper.data.crypto

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object Rc4Cipher {
    fun encrypt(key: String, plaintext: String): String {
        val keyBytes = key.toByteArray(StandardCharsets.UTF_8)
        if (keyBytes.isEmpty()) return ""

        val sbox = IntArray(256) { it }
        var j = 0
        for (i in 0..255) {
            val keyByte = keyBytes[i % keyBytes.size].toInt() and 0xFF
            j = (j + sbox[i] + keyByte) and 0xFF
            val temp = sbox[i]
            sbox[i] = sbox[j]
            sbox[j] = temp
        }

        var i = 0
        j = 0
        val sb = StringBuilder()
        val plainBytes = plaintext.toByteArray(StandardCharsets.UTF_8)
        for (b in plainBytes) {
            i = (i + 1) and 0xFF
            j = (j + sbox[i]) and 0xFF
            val temp = sbox[i]
            sbox[i] = sbox[j]
            sbox[j] = temp
            val k = sbox[(sbox[i] + sbox[j]) and 0xFF]
            val cipherByte = (b.toInt() and 0xFF) xor k
            sb.append(String.format("%02x", cipherByte))
        }
        return sb.toString()
    }

    fun urlEncode(value: String): String {
        return try {
            URLEncoder.encode(value, StandardCharsets.UTF_8.name())
        } catch (e: Exception) {
            value
        }
    }
}
