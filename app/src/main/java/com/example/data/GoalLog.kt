package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goal_logs")
data class GoalLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val goalId: Int,
    val pointsBefore: Int,
    val pointsAfter: Int,
    val changeAmount: Int,
    val timestamp: Long = System.currentTimeMillis()
)
