package com.example.smartwatchapp.data

import android.content.Context
import androidx.room.*
import java.util.Date

@Dao
interface HealthDataDao {
    @Query("SELECT * FROM health_data ORDER BY timestamp DESC")
    suspend fun getAllData(): List<HealthData>

    @Insert
    suspend fun insert(healthData: HealthData)

    @Query("SELECT * FROM health_data WHERE timestamp >= :startDate ORDER BY timestamp DESC")
    suspend fun getDataFromDate(startDate: Date): List<HealthData>
}

@Database(entities = [HealthData::class], version = 1)
@TypeConverters(Converters::class)
abstract class HealthDatabase : RoomDatabase() {
    abstract fun healthDataDao(): HealthDataDao

    companion object {
        @Volatile
        private var INSTANCE: HealthDatabase? = null

        fun getDatabase(context: Context): HealthDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HealthDatabase::class.java,
                    "health_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
} 