package co.uk.doverguitarteacher.securenotesaug20th

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object EncryptionManager {

    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATION_COUNT = 65536
    private const val KEY_LENGTH = 256

    private fun getKeyFromPin(pin: String, salt: ByteArray): SecretKey {
        val factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM)
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }

    // --- TEXT ENCRYPTION (Unchanged) ---
    fun encrypt(plainText: String, pin: String): EncryptedPayload {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val encryptedData = encryptBytes(plainText.toByteArray(), pin, salt)
        return EncryptedPayload(salt = salt, encryptedData = Base64.encodeToString(encryptedData, Base64.DEFAULT))
    }

    fun decrypt(encryptedPayload: String, salt: ByteArray, pin: String): String? {
        val decodedBytes = Base64.decode(encryptedPayload, Base64.DEFAULT)
        val decryptedBytes = decryptBytes(decodedBytes, pin, salt)
        return decryptedBytes?.let { String(it) }
    }


    // --- NEW GENERIC BYTE/FILE ENCRYPTION ---
    fun encryptBytes(data: ByteArray, pin: String, salt: ByteArray): ByteArray? {
        return try {
            val key = getKeyFromPin(pin, salt)
            val cipher = Cipher.getInstance(ALGORITHM)
            val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
            val ivSpec = IvParameterSpec(iv)
            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec)
            val encryptedBytes = cipher.doFinal(data)
            iv + encryptedBytes // Prepend IV for decryption
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun decryptBytes(encryptedData: ByteArray, pin: String, salt: ByteArray): ByteArray? {
        return try {
            val key = getKeyFromPin(pin, salt)
            val cipher = Cipher.getInstance(ALGORITHM)
            val iv = encryptedData.copyOfRange(0, 12)
            val content = encryptedData.copyOfRange(12, encryptedData.size)
            val ivSpec = IvParameterSpec(iv)
            cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)
            cipher.doFinal(content)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

data class EncryptedPayload(val salt: ByteArray, val encryptedData: String)
