package com.example.rfidpaymentsys.data.dao

import androidx.room.*
import com.example.rfidpaymentsys.data.entity.ValidationLog
import kotlinx.coroutines.flow.Flow

@Dao
interface ValidationLogDao {
    @Query("SELECT * FROM validation_logs ORDER BY timestamp DESC")
    fun getAllValidationLogs(): Flow<List<ValidationLog>>

    @Insert
    suspend fun insertLog(log: ValidationLog)
}
