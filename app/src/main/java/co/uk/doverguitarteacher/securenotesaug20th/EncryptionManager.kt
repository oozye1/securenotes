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
    private const val ITERATION_COUNT = 65536 // Recommended standard
    private const val KEY_LENGTH = 256 // AES-256

    // A fixed, non-secret salt for key derivation. In a real-world high-security app,
    // you might store a unique salt per note. For simplicity, we use one here.
    private val salt = "a_fixed_salt_for_secure_notes".toByteArray()

    /**
     * Derives a 256-bit AES key from a user's PIN.
     */
    private fun getKeyFromPin(pin: String): SecretKey {
        val factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM)
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }

    /**
     * Encrypts the note content.
     * Returns a Base64 encoded string containing "iv:encrypted_content".
     */
    fun encrypt(plainText: String, pin: String): String {
        try {
            val key = getKeyFromPin(pin)
            val cipher = Cipher.getInstance(ALGORITHM)

            // Generate a random, non-secret Initialization Vector (IV)
            val iv = ByteArray(12) // GCM standard IV size
            SecureRandom().nextBytes(iv)
            val ivParameterSpec = IvParameterSpec(iv)

            cipher.init(Cipher.ENCRYPT_MODE, key, ivParameterSpec)
            val encryptedBytes = cipher.doFinal(plainText.toByteArray())

            // Combine IV and encrypted content, then encode to Base64
            val combined = iv + encryptedBytes
            return Base64.encodeToString(combined, Base64.DEFAULT)
        } catch (e: Exception) {
            // In a real app, handle this error more gracefully
            e.printStackTrace()
            return ""
        }
    }

    /**
     * Decrypts the note content.
     * Expects a Base64 encoded string in the format "iv:encrypted_content".
     */
    fun decrypt(encryptedText: String, pin: String): String? {
        try {
            val key = getKeyFromPin(pin)
            val cipher = Cipher.getInstance(ALGORITHM)

            val decodedBytes = Base64.decode(encryptedText, Base64.DEFAULT)

            // Extract the IV from the beginning of the decoded data
            val iv = decodedBytes.copyOfRange(0, 12)
            val encryptedContent = decodedBytes.copyOfRange(12, decodedBytes.size)
            val ivParameterSpec = IvParameterSpec(iv)

            cipher.init(Cipher.DECRYPT_MODE, key, ivParameterSpec)
            val decryptedBytes = cipher.doFinal(encryptedContent)

            return String(decryptedBytes)
        } catch (e: Exception) {
            // This will often fail if the wrong PIN is used. Return null to indicate failure.
            e.printStackTrace()
            return null
        }
    }
}
