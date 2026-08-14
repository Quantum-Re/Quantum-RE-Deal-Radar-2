package com.example.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext

class DistressedPropertyRepository(
    private val distressedPropertyDao: DistressedPropertyDao
) {
    val allDistressedProperties: Flow<List<DistressedProperty>> = 
        distressedPropertyDao.getAllDistressedProperties().onEach { list ->
            Log.d("DistressedPropertyRepository", "allDistressedProperties flow emitted ${list.size} distressed properties")
        }

    fun getDistressedPropertiesByLevel(level: String): Flow<List<DistressedProperty>> {
        Log.d("DistressedPropertyRepository", "getDistressedPropertiesByLevel(level=$level) called")
        return distressedPropertyDao.getDistressedPropertiesByLevel(level).onEach { list ->
            Log.d("DistressedPropertyRepository", "getDistressedPropertiesByLevel(level=$level) flow emitted ${list.size} items")
        }
    }

    fun getDistressedPropertiesByStatus(status: String): Flow<List<DistressedProperty>> {
        Log.d("DistressedPropertyRepository", "getDistressedPropertiesByStatus(status=$status) called")
        return distressedPropertyDao.getDistressedPropertiesByStatus(status).onEach { list ->
            Log.d("DistressedPropertyRepository", "getDistressedPropertiesByStatus(status=$status) flow emitted ${list.size} items")
        }
    }

    fun getFilteredDistressedProperties(level: String, minPrice: Double?, maxPrice: Double?): Flow<List<DistressedProperty>> {
        Log.d("DistressedPropertyRepository", "getFilteredDistressedProperties(level=$level, minPrice=$minPrice, maxPrice=$maxPrice) called")
        return distressedPropertyDao.getFilteredDistressedProperties(level, minPrice, maxPrice)
    }

    suspend fun getDistressedPropertyById(id: Long): DistressedProperty? = withContext(Dispatchers.IO) {
        Log.d("DistressedPropertyRepository", "getDistressedPropertyById(id=$id) called")
        val result = distressedPropertyDao.getDistressedPropertyById(id)
        Log.d("DistressedPropertyRepository", "getDistressedPropertyById(id=$id) result: ${result?.address ?: "null"}")
        result
    }

    suspend fun insertDistressedProperty(distressedProperty: DistressedProperty): Long = withContext(Dispatchers.IO) {
        Log.d("DistressedPropertyRepository", "insertDistressedProperty called for address=${distressedProperty.address}")
        val id = distressedPropertyDao.insertDistressedProperty(distressedProperty)
        Log.d("DistressedPropertyRepository", "insertDistressedProperty succeeded with id=$id")
        id
    }

    suspend fun insertDistressedProperties(distressedProperties: List<DistressedProperty>) = withContext(Dispatchers.IO) {
        Log.d("DistressedPropertyRepository", "insertDistressedProperties called with ${distressedProperties.size} items")
        distressedPropertyDao.insertDistressedProperties(distressedProperties)
        Log.d("DistressedPropertyRepository", "insertDistressedProperties inserted ${distressedProperties.size} items")
    }

    suspend fun updateDistressedProperty(distressedProperty: DistressedProperty) = withContext(Dispatchers.IO) {
        Log.d("DistressedPropertyRepository", "updateDistressedProperty called for id=${distressedProperty.id}")
        distressedPropertyDao.updateDistressedProperty(distressedProperty)
    }

    suspend fun deleteDistressedProperty(distressedProperty: DistressedProperty) = withContext(Dispatchers.IO) {
        Log.d("DistressedPropertyRepository", "deleteDistressedProperty called for id=${distressedProperty.id}")
        distressedPropertyDao.deleteDistressedProperty(distressedProperty)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        Log.d("DistressedPropertyRepository", "clearAll() called")
        distressedPropertyDao.clearAll()
    }
}

