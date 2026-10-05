package com.student.wifihelper.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_config")
data class UserConfig(
    @PrimaryKey val id: Int = 1,
    val username: String = "",
    val password: String = "",
    val targetSsid: String = "utarwifi",
    val autoLoginEnabled: Boolean = true,
    val pollingIntervalSeconds: Int = 30,
    val lastLoginTime: Long = 0L,
    val lastLoginSuccess: Boolean = false,
    val lastStatusMessage: String = "Not initiated",
    val isOnboardingCompleted: Boolean = false,
    val biometricLockEnabled: Boolean = false
)
