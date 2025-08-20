package co.uk.doverguitarteacher.securenotesaug20th

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlin.random.Random

object KeystoreManager {

    private const val MASTER_KEY_ALIAS = "database_master_key"
    private const val PREF_FILE_NAME = "secure_notes_prefs"
    private const val PREF_DB_PASSPHRASE_KEY = "db_passphrase"

    private fun getOrCreateMasterKey(context: Context): MasterKey {
        return MasterKey.Builder(context, MASTER_KEY_ALIAS)
            .setKeyGenParameterSpec(
                KeyGenParameterSpec.Builder(
                    MASTER_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
            .build()
    }

    private fun getEncryptedPrefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        PREF_FILE_NAME,
        getOrCreateMasterKey(context),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getDatabasePassphrase(context: Context): ByteArray {
        val prefs = getEncryptedPrefs(context)
        val encryptedPassphraseString = prefs.getString(PREF_DB_PASSPHRASE_KEY, null)

        return if (encryptedPassphraseString == null) {
            val newPassphrase = Random.Default.nextBytes(32)
            prefs.edit().putString(PREF_DB_PASSPHRASE_KEY, Base64.encodeToString(newPassphrase, Base64.DEFAULT)).apply()
            newPassphrase
        } else {
            Base64.decode(encryptedPassphraseString, Base64.DEFAULT)
        }
    }
}
