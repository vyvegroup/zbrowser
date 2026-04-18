package com.zbrowser.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val url: String,
    val faviconUrl: String? = null,
    val visitedAt: Long = System.currentTimeMillis()
)
