package co.uk.doverguitarteacher.securenotesaug20th

import android.content.Context
import androidx.room.Room
import net.sqlcipher.database.SupportFactory

object DatabaseProvider {
    @Volatile
    private var instance: AppDatabase? = null

    fun getInstance(context: Context): AppDatabase {
        return instance ?: synchronized(this) {
            instance ?: buildDatabase(context).also { instance = it }
        }
    }

    private fun buildDatabase(context: Context): AppDatabase {
        val passphrase = getOrCreatePassphrase() // Simplified for now
        val factory = SupportFactory(passphrase)
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java, "secure_notes.db"
        )
            .openHelperFactory(factory)
            .build()
    }

    private fun getOrCreatePassphrase(): ByteArray {
        // IMPORTANT: We will replace this with secure Keystore logic later.
        return "your-super-secret-passphrase".toByteArray()
    }
}
