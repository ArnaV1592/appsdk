package com.example.smartwatchapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "health_data")
data class HealthData(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Date,
    val heartRate: Int?,
    val steps: Int?,
    val bloodOxygen: Int?
) 