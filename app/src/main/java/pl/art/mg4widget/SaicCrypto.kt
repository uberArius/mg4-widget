package pl.art.mg4widget

import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/** Protocol port from SAIC-iSmart-API/saic-python-client-ng (MIT); see THIRD_PARTY_NOTICES. */
object SaicCrypto {
    fun hash(algorithm: String, value: String) = hex(MessageDigest.getInstance(algorithm).digest(value.toByteArray(Charsets.UTF_8)))
    fun md5(value: String) = hash("MD5", value)
    fun hex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it.toInt() and 255) }
    private fun unhex(value: String): ByteArray {
        require(value.length % 2 == 0)
        return value.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
    fun aes(value: String, key: String, iv: String, decrypt: Boolean = false): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(if (decrypt) Cipher.DECRYPT_MODE else Cipher.ENCRYPT_MODE,
            SecretKeySpec(unhex(key), "AES"), IvParameterSpec(unhex(iv)))
        val result = cipher.doFinal(if (decrypt) unhex(value) else value.toByteArray(Charsets.UTF_8))
        return if (decrypt) result.toString(Charsets.UTF_8) else hex(result)
    }
    data class Envelope(val body: String, val signature: String)
    fun encrypt(path: String, timestamp: String, contentType: String, body: String, token: String): Envelope {
        val prefix = path + "459771" + token + "app"
        val key = md5(md5(prefix) + timestamp + "1" + contentType)
        val encrypted = if (body.isBlank()) "" else aes(body.trim(), key, md5(timestamp))
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(md5(key + timestamp).toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return Envelope(encrypted, hex(mac.doFinal((prefix + timestamp + "1" + contentType + encrypted).toByteArray(Charsets.UTF_8))))
    }
    fun decrypt(body: String, timestamp: String, contentType: String) =
        aes(body.trim(), md5(timestamp + "1" + contentType), md5(timestamp), true)
}
