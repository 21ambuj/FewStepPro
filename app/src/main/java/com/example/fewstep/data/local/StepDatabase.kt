package com.example.fewstep.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [StepRecord::class], version = 1, exportSchema = false)
abstract class StepDatabase : RoomDatabase() {

    abstract fun stepRecordDao(): StepRecordDao

    companion object {
        @Volatile
        private var INSTANCE: StepDatabase? = null

        fun getInstance(context: Context): StepDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    StepDatabase::class.java,
                    "step_history.db"
                )
                .fallbackToDestructiveMigration(true)
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
