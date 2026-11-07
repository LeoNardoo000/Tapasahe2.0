package com.example.rfidpaymentsys.data.dao

import androidx.room.*
import com.example.rfidpaymentsys.data.entity.IncomeReport
import kotlinx.coroutines.flow.Flow

@Dao
interface IncomeReportDao {
    @Query("SELECT * FROM income_reports ORDER BY dateGenerated DESC")
    fun getAllReports(): Flow<List<IncomeReport>>

    @Insert
    suspend fun insertReport(report: IncomeReport)
}
