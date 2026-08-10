package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PropertyDealDao {
    @Query("SELECT * FROM property_deals ORDER BY createdAt DESC")
    fun getAllDeals(): Flow<List<PropertyDeal>>

    @Query("SELECT * FROM property_deals WHERE isBookmarked = 1 ORDER BY createdAt DESC")
    fun getBookmarkedDeals(): Flow<List<PropertyDeal>>

    @Query("SELECT * FROM property_deals WHERE sourceKey = :sourceKey ORDER BY createdAt DESC")
    fun getDealsBySource(sourceKey: String): Flow<List<PropertyDeal>>

    @Query("SELECT * FROM property_deals WHERE id = :id")
    suspend fun getDealById(id: Long): PropertyDeal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeal(deal: PropertyDeal): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeals(deals: List<PropertyDeal>)

    @Update
    suspend fun updateDeal(deal: PropertyDeal)

    @Query("UPDATE property_deals SET isBookmarked = :isBookmarked WHERE id = :id")
    suspend fun setBookmarked(id: Long, isBookmarked: Boolean)

    @Query("UPDATE property_deals SET notes = :notes WHERE id = :id")
    suspend fun updateNotes(id: Long, notes: String)

    @Delete
    suspend fun deleteDeal(deal: PropertyDeal)

    @Query("DELETE FROM property_deals")
    suspend fun clearAll()
}
