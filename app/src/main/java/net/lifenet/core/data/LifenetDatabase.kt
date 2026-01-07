package net.lifenet.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
// import net.sqlcipher.database.SupportOpenHelperFactory
// import net.sqlcipher.database.SQLiteDatabase
import net.lifenet.core.identity.IdentityManager

@Database(entities = [Message::class, Contact::class, SOSAlert::class], version = 2, exportSchema = false)
abstract class LifenetDatabase : RoomDatabase() {
    
    abstract fun messageDao(): MessageDao
    abstract fun contactDao(): ContactDao
    abstract fun sosAlertDao(): SOSAlertDao
    
    companion object {
        @Volatile
        private var instance: LifenetDatabase? = null
        
        fun getInstance(context: Context): LifenetDatabase {
            return instance ?: synchronized(this) {
                // Encryption temporarily disabled for build fix
                // val identityManager = IdentityManager(context)
                // val passphrase = identityManager.getDatabasePassphrase()
                
                // SQLiteDatabase.loadLibs(context)
                // val factory = SupportOpenHelperFactory(passphrase.toByteArray())

                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    LifenetDatabase::class.java,
                    "lifenet_database"
                )
                // .openHelperFactory(factory)
                .fallbackToDestructiveMigration()
                .build().also { instance = it }
            }
        }
    }
}
