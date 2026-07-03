package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val currentPoints: Int,
    val maxPoints: Int,
    val allowExceed: Boolean,
    val colorHex: String = "#6750A4",
    val secondaryColorHex: String = "DEFAULT",
    val milestonesStr: String = "",
    val barBehavior: Int = 0,
    val hidePassedMilestones: Boolean = false,
    val showRemainingCount: Boolean = true,
    val parentGroupId: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
