package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goal_groups")
data class GoalGroup(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val colorHex: String = "#6750A4",
    val secondaryColorHex: String = "DEFAULT",
    val imageUri: String? = null,
    val applyBg: Boolean = true,
    val outlineMainColor: Boolean = false,
    val parentGroupId: Int = 0,
    val cardBorderHex: String = "NONE",
    val displayType: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

data class GroupGoalStats(
    val parentGroupId: Int,
    val totalGoals: Int,
    val completedGoals: Int
)
