package com.student.wifihelper.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.student.wifihelper.data.model.UserConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface ConfigDao {
    @Query("SELECT * FROM user_config WHERE id = 1")
    fun getConfigFlow(): Flow<UserConfig?>

    @Query("SELECT * FROM user_config WHERE id = 1")
    suspend fun getConfig(): UserConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(config: UserConfig)
}
