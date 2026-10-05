package com.student.wifihelper.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "discovered_endpoints")
data class PortalEndpoint(
    @PrimaryKey val url: String,
    val campusBlock: String? = null,
    val lastUsedTime: Long = System.currentTimeMillis(),
    val discoveryTime: Long = System.currentTimeMillis()
)
