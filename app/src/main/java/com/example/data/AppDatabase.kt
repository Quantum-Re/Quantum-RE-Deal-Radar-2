package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PropertyDeal::class, Property::class, ScraperSource::class, PriceHistory::class, InvestorProfile::class, MapTileCache::class, MapOfflineRegion::class, DistressedProperty::class, RecentSearchQuery::class],
    version = 20,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun propertyDealDao(): PropertyDealDao
    abstract fun propertyDao(): PropertyDao
    abstract fun scraperSourceDao(): ScraperSourceDao
    abstract fun priceHistoryDao(): PriceHistoryDao
    abstract fun investorProfileDao(): InvestorProfileDao
    abstract fun mapTileCacheDao(): MapTileCacheDao
    abstract fun distressedPropertyDao(): DistressedPropertyDao
    abstract fun recentSearchDao(): RecentSearchDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "quantum_deal_radar.db"
                )
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun seedDatabaseIfEmpty(context: Context): SeedResult {
            val db = getDatabase(context)
            return DatabaseSeeder.seedDatabaseIfEmpty(db)
        }
    }

    suspend fun seedIfEmpty(): SeedResult {
        return DatabaseSeeder.seedDatabaseIfEmpty(this)
    }
}
