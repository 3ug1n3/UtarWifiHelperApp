package com.student.wifihelper.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.student.wifihelper.data.model.LoginLog
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Query("SELECT * FROM login_logs ORDER BY timestamp DESC LIMIT 200")
    fun getAllLogs(): Flow<List<LoginLog>>

    @Insert
    suspend fun insertLog(log: LoginLog)

    @Query("DELETE FROM login_logs")
    suspend fun clearLogs()
}
