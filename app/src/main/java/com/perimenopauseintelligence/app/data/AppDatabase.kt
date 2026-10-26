package com.perimenopauseintelligence.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [SymptomEntity::class, EntryEntity::class, ObservationEntity::class, QuestionEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): TrackerDao

    companion object {
        fun create(context: Context): AppDatabase {
            System.loadLibrary("sqlcipher")
            val factory = SupportOpenHelperFactory(DbKeyProvider.passphrase(context))
            return Room.databaseBuilder(context, AppDatabase::class.java, "symptoms.db")
                .openHelperFactory(factory)
                .build()
        }
    }
}
