package com.student.wifihelper.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "login_logs")
data class LoginLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val status: LogStatus,
    val message: String,
    val ssid: String? = null,
    val portalUrl: String? = null
)

enum class LogStatus {
    INFO,
    SUCCESS,
    WARNING,
    ERROR
}
