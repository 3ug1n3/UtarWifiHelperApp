package com.student.wifihelper.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.student.wifihelper.data.model.PortalEndpoint
import kotlinx.coroutines.flow.Flow

@Dao
interface PortalDao {
    @Query("SELECT * FROM discovered_endpoints ORDER BY lastUsedTime DESC")
    fun getAllEndpointsFlow(): Flow<List<PortalEndpoint>>

    @Query("SELECT * FROM discovered_endpoints")
    suspend fun getAllEndpoints(): List<PortalEndpoint>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEndpoint(endpoint: PortalEndpoint)

    @Query("DELETE FROM discovered_endpoints WHERE url = :url")
    suspend fun deleteEndpoint(url: String)
}
