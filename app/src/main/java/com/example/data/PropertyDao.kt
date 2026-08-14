package com.example.data

import android.util.Log
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

@Dao
abstract class PropertyDao {
    @Query("SELECT * FROM properties ORDER BY createdAt DESC")
    abstract fun getAllPropertiesRaw(): Flow<List<Property>>

    fun getAllProperties(): Flow<List<Property>> {
        Log.d("PropertyDao", "getAllProperties() query initiated")
        return getAllPropertiesRaw().onEach { list ->
            Log.d("PropertyDao", "getAllProperties() emitted ${list.size} items from Room database")
        }
    }

    @Query("SELECT * FROM properties WHERE id = :id")
    abstract suspend fun getPropertyByIdRaw(id: Long): Property?

    suspend fun getPropertyById(id: Long): Property? {
        Log.d("PropertyDao", "getPropertyById(id=$id) executed")
        val result = getPropertyByIdRaw(id)
        Log.d("PropertyDao", "getPropertyById(id=$id) returned: ${result?.title ?: "null"}")
        return result
    }

    @Query("SELECT * FROM properties WHERE distressStatus = :status ORDER BY createdAt DESC")
    abstract fun getPropertiesByDistressStatusRaw(status: String): Flow<List<Property>>

    fun getPropertiesByDistressStatus(status: String): Flow<List<Property>> {
        Log.d("PropertyDao", "getPropertiesByDistressStatus(status=$status) query initiated")
        return getPropertiesByDistressStatusRaw(status).onEach { list ->
            Log.d("PropertyDao", "getPropertiesByDistressStatus(status=$status) emitted ${list.size} items")
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertPropertyRaw(property: Property): Long

    suspend fun insertProperty(property: Property): Long {
        Log.d("PropertyDao", "insertProperty called for title=${property.title}")
        val id = insertPropertyRaw(property)
        Log.d("PropertyDao", "insertProperty success with id=$id")
        return id
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertPropertiesRaw(properties: List<Property>)

    suspend fun insertProperties(properties: List<Property>) {
        Log.d("PropertyDao", "insertProperties called with ${properties.size} items")
        insertPropertiesRaw(properties)
        Log.d("PropertyDao", "insertProperties inserted ${properties.size} items into Room")
    }

    @Update
    abstract suspend fun updatePropertyRaw(property: Property)

    suspend fun updateProperty(property: Property) {
        Log.d("PropertyDao", "updateProperty called for id=${property.id}")
        updatePropertyRaw(property)
    }

    @Delete
    abstract suspend fun deletePropertyRaw(property: Property)

    suspend fun deleteProperty(property: Property) {
        Log.d("PropertyDao", "deleteProperty called for id=${property.id}")
        deletePropertyRaw(property)
    }

    @Query("DELETE FROM properties")
    abstract suspend fun clearAllRaw()

    suspend fun clearAll() {
        Log.d("PropertyDao", "clearAll() executed on properties table")
        clearAllRaw()
    }
}

