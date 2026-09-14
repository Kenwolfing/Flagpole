package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val textSize: Float = 14f,
    val colorHex: String = "#6750A4",
    val secondaryColorHex: String = "DEFAULT",
    val imageUri: String? = null,
    val parentGroupId: Int = 0,
    val cardBorderHex: String = "NONE",
    val createdAt: Long = System.currentTimeMillis()
)
