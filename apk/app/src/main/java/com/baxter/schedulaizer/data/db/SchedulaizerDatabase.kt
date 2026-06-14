package com.baxter.schedulaizer.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.baxter.schedulaizer.data.db.dao.*
import com.baxter.schedulaizer.data.db.entity.*

@Database(
    entities = [EventEntity::class, BillEntity::class, MemoEntity::class, AttachmentEntity::class, AlertEntity::class],
    // v2: AlertEntity gained soundUri + repeatDaily (custom alarm tones / standalone
    // alarms). Schema change is handled by fallbackToDestructiveMigration (pre-release).
    version = 2,
    exportSchema = false
)
@TypeConverters(AppTypeConverters::class)
abstract class SchedulaizerDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun billDao(): BillDao
    abstract fun memoDao(): MemoDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun alertDao(): AlertDao

    companion object {
        @Volatile
        private var INSTANCE: SchedulaizerDatabase? = null

        fun getInstance(context: Context): SchedulaizerDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SchedulaizerDatabase::class.java,
                    "schedualizer.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }

        fun buildInMemory(context: Context): SchedulaizerDatabase {
            return Room.inMemoryDatabaseBuilder(context, SchedulaizerDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        }
    }
}
