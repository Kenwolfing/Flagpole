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
    val showMilestoneProgressBar: Boolean = true,
    val parentGroupId: Int = 0,
    val progressType: Int = 0, // 0 = bar, 1 = meter, 2 = big dots
    val meterThickness: Float = 14f,
    val dotsPerRow: Int = 8,
    val cardBorderHex: String = "NONE",
    val unit: String = "pts",
    val statsEnabled: Boolean = true,
    val streaksEnabled: Boolean = false,
    val pointTrackingEnabled: Boolean = true,
    val statsVisualType: Int = 0, // 0 = bar, 1 = graph/line
    val showHighlights: Boolean = true,
    val groupValue: Double = 1.0,
    val allowGroupPointsOverflow: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
