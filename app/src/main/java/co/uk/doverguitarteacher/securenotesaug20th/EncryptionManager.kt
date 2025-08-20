package co.uk.doverguitarteacher.securenotesaug20th

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object EncryptionManager {

    private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val KDF_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATION_COUNT = 65_536
    private const val KEY_LENGTH_BITS = 256
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val IV_LENGTH_BYTES = 12
    private const val SALT_LENGTH_BYTES = 16

    private val secureRandom = SecureRandom()

    private fun deriveKeyFromPin(pin: String, salt: ByteArray): SecretKey {
        val factory = SecretKeyFactory.getInstance(KDF_ALGORITHM)
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH_BITS)
        val encoded = factory.generateSecret(spec).encoded
        return SecretKeySpec(encoded, "AES")
    }

    // --- TEXT ENCRYPTION ---
    fun encrypt(plainText: String, pin: String): EncryptedPayload {
        val salt = ByteArray(SALT_LENGTH_BYTES).also { secureRandom.nextBytes(it) }
        val cipherText = encryptBytes(plainText.toByteArray(Charsets.UTF_8), pin, salt)
            ?: throw IllegalStateException("Text encryption failed")
        // For text we keep salt separate (returned in payload) and Base64-encode iv+ciphertext
        val encoded = Base64.encodeToString(cipherText, Base64.DEFAULT)
        return EncryptedPayload(salt = salt, encryptedData = encoded)
    }

    fun decrypt(encryptedPayload: String, salt: ByteArray, pin: String): String? {
        return try {
            val decoded = Base64.decode(encryptedPayload, Base64.DEFAULT)
            val plain = decryptBytes(decoded, pin, salt) ?: return null
            String(plain, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // --- GENERIC BYTE ENCRYPTION (iv + ciphertext returned) ---
    fun encryptBytes(data: ByteArray, pin: String, salt: ByteArray): ByteArray? {
        return try {
            val key = deriveKeyFromPin(pin, salt)
            val iv = ByteArray(IV_LENGTH_BYTES).also { secureRandom.nextBytes(it) }
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            val params = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.ENCRYPT_MODE, key, params)
            val ct = cipher.doFinal(data)
            // Prepend IV so decryptBytes can recover it
            iv + ct
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun decryptBytes(encryptedData: ByteArray, pin: String, salt: ByteArray): ByteArray? {
        return try {
            if (encryptedData.size <= IV_LENGTH_BYTES) return null
            val iv = encryptedData.copyOfRange(0, IV_LENGTH_BYTES)
            val ct = encryptedData.copyOfRange(IV_LENGTH_BYTES, encryptedData.size)
            val key = deriveKeyFromPin(pin, salt)
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            val params = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, params)
            cipher.doFinal(ct)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

data class EncryptedPayload(val salt: ByteArray, val encryptedData: String)
