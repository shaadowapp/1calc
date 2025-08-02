package com.shaadow.onecalculator

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [HistoryEntity::class, PreferenceEntity::class, EncryptedFolderEntity::class], version = 5, exportSchema = false)
abstract class HistoryDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun preferenceDao(): PreferenceDao
    abstract fun encryptedFolderDao(): EncryptedFolderDao

    companion object {
        @Volatile
        private var INSTANCE: HistoryDatabase? = null

        fun getInstance(context: Context): HistoryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HistoryDatabase::class.java,
                    "history_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        fun destroyInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
} 