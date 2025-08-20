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

    /**
     * Encrypts plain text.
     * Returns an object containing the ciphertext and the unique salt used.
     */
    fun encrypt(plainText: String, pin: String): EncryptedPayload {
        // 1. Generate a NEW, RANDOM salt for this specific encryption
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        val key = getKeyFromPin(pin, salt)
        val cipher = Cipher.getInstance(ALGORITHM)

        val iv = ByteArray(12) // GCM standard IV size
        SecureRandom().nextBytes(iv)
        val ivParameterSpec = IvParameterSpec(iv)

        cipher.init(Cipher.ENCRYPT_MODE, key, ivParameterSpec)
        val encryptedBytes = cipher.doFinal(plainText.toByteArray())

        // 2. Combine IV + Ciphertext and encode it
        val combined = iv + encryptedBytes
        val encryptedContent = Base64.encodeToString(combined, Base64.DEFAULT)

        // 3. Return both the encrypted content and the salt we used
        return EncryptedPayload(salt = salt, encryptedData = encryptedContent)
    }

    /**
     * Decrypts ciphertext.
     * Requires the data, the salt that was used to encrypt it, and the PIN.
     */
    fun decrypt(encryptedPayload: String, salt: ByteArray, pin: String): String? {
        return try {
            val key = getKeyFromPin(pin, salt)
            val cipher = Cipher.getInstance(ALGORITHM)

            val decodedBytes = Base64.decode(encryptedPayload, Base64.DEFAULT)
            val iv = decodedBytes.copyOfRange(0, 12)
            val encryptedContent = decodedBytes.copyOfRange(12, decodedBytes.size)
            val ivParameterSpec = IvParameterSpec(iv)

            cipher.init(Cipher.DECRYPT_MODE, key, ivParameterSpec)
            val decryptedBytes = cipher.doFinal(encryptedContent)
            String(decryptedBytes)
        } catch (e: Exception) {
            e.printStackTrace()
            null // Decryption failed, likely wrong PIN
        }
    }
}

// A simple data class to hold our encryption results
data class EncryptedPayload(val salt: ByteArray, val encryptedData: String)
