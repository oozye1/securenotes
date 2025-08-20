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
        // NO MORE HARDCODED PASSPHRASE.
        // Get it securely from the KeystoreManager instead.
        val passphrase = KeystoreManager.getDatabasePassphrase(context.applicationContext)
        val factory = SupportFactory(passphrase)

        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java, "secure_notes.db"
        )
            .openHelperFactory(factory)
            // If the schema changes (e.g., version bump), destroy and recreate the database.
            // FOR DEVELOPMENT ONLY. A production app needs a real migration strategy.
            .fallbackToDestructiveMigration()
            .build()
    }
}
