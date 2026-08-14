package com.example.data

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext

class PropertyRepository(
    private val propertyDao: PropertyDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    val allProperties: Flow<List<Property>> = propertyDao.getAllProperties().onEach { list ->
        Log.d("PropertyRepository", "allProperties flow emitted ${list.size} properties from database")
    }

    fun getPropertiesByDistressStatus(status: String): Flow<List<Property>> {
        Log.d("PropertyRepository", "getPropertiesByDistressStatus(status=$status) called")
        return propertyDao.getPropertiesByDistressStatus(status).onEach { list ->
            Log.d("PropertyRepository", "getPropertiesByDistressStatus(status=$status) emitted ${list.size} items")
        }
    }

    suspend fun getPropertyById(id: Long): Property? = withContext(ioDispatcher) {
        Log.d("PropertyRepository", "getPropertyById(id=$id) called")
        val result = propertyDao.getPropertyById(id)
        Log.d("PropertyRepository", "getPropertyById(id=$id) result: ${result?.title ?: "null"}")
        result
    }

    suspend fun insertProperty(property: Property): Long = withContext(ioDispatcher) {
        Log.d("PropertyRepository", "insertProperty called for title=${property.title}")
        val id = propertyDao.insertProperty(property)
        Log.d("PropertyRepository", "insertProperty succeeded with id=$id")
        id
    }

    suspend fun insertProperties(properties: List<Property>) = withContext(ioDispatcher) {
        Log.d("PropertyRepository", "insertProperties called with ${properties.size} items")
        propertyDao.insertProperties(properties)
        Log.d("PropertyRepository", "insertProperties inserted ${properties.size} items")
    }

    suspend fun updateProperty(property: Property) = withContext(ioDispatcher) {
        Log.d("PropertyRepository", "updateProperty called for id=${property.id}")
        propertyDao.updateProperty(property)
    }

    suspend fun deleteProperty(property: Property) = withContext(ioDispatcher) {
        Log.d("PropertyRepository", "deleteProperty called for id=${property.id}")
        propertyDao.deleteProperty(property)
    }

    suspend fun clearAll() = withContext(ioDispatcher) {
        Log.d("PropertyRepository", "clearAll() called")
        propertyDao.clearAll()
    }
}

