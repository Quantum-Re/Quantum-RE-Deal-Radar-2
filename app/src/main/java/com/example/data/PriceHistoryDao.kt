package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PriceHistoryDao {
    @Query("SELECT * FROM price_history WHERE dealId = :dealId ORDER BY id ASC")
    fun getHistoryForDeal(dealId: Long): Flow<List<PriceHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: PriceHistory)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistories(histories: List<PriceHistory>)
}
