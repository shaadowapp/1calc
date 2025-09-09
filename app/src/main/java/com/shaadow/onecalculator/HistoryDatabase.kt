package com.shaadow.onecalculator

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [HistoryEntity::class, PreferenceEntity::class, EncryptedFolderEntity::class, EncryptedFileEntity::class, DeviceInfoEntity::class], version = 11, exportSchema = false) // Updated for new architecture - added isLocked field to EncryptedFolderEntity
abstract class HistoryDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun preferenceDao(): PreferenceDao
    abstract fun encryptedFolderDao(): EncryptedFolderDao
    abstract fun encryptedFileDao(): EncryptedFileDao
    abstract fun deviceInfoDao(): DeviceInfoDao

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
                    .fallbackToDestructiveMigration(dropAllTables = true)
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