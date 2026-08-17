package com.example.data

import androidx.room.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@Entity(tableName = "recent_searches")
data class RecentSearchQuery(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val query: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface RecentSearchDao {
    @Query("SELECT * FROM recent_searches ORDER BY timestamp DESC LIMIT 5")
    fun getRecentSearches(): Flow<List<RecentSearchQuery>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearch(search: RecentSearchQuery)

    @Query("DELETE FROM recent_searches WHERE LOWER(query) = LOWER(:query)")
    suspend fun deleteSearchByQuery(query: String)

    @Query("DELETE FROM recent_searches WHERE id = :id")
    suspend fun deleteSearchById(id: Long)

    @Query("DELETE FROM recent_searches WHERE id NOT IN (SELECT id FROM recent_searches ORDER BY timestamp DESC LIMIT 5)")
    suspend fun pruneOldSearches()

    @Query("DELETE FROM recent_searches")
    suspend fun clearAll()
}

class RecentSearchRepository(
    private val dao: RecentSearchDao,
    private val externalScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    val recentSearches: StateFlow<List<RecentSearchQuery>> = dao.getRecentSearches()
        .stateIn(externalScope, SharingStarted.Eagerly, emptyList())

    suspend fun saveSearchQuery(rawQuery: String) {
        val trimmed = rawQuery.trim()
        if (trimmed.isBlank() || trimmed.length < 2) return

        dao.deleteSearchByQuery(trimmed)
        dao.insertSearch(RecentSearchQuery(query = trimmed, timestamp = System.currentTimeMillis()))
        dao.pruneOldSearches()
    }

    suspend fun removeSearch(query: String) {
        dao.deleteSearchByQuery(query.trim())
    }

    suspend fun clearAll() {
        dao.clearAll()
    }
}
