package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.daos.DriverDao
import com.example.data.local.daos.OrderDao
import com.example.data.local.daos.ProductDao
import com.example.data.local.daos.ShopDao
import com.example.data.local.entities.DriverEntity
import com.example.data.local.entities.OrderEntity
import com.example.data.local.entities.ProductEntity
import com.example.data.local.entities.ShopEntity

@Database(
    entities = [
        ShopEntity::class,
        ProductEntity::class,
        DriverEntity::class,
        OrderEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SourDeliveryDatabase : RoomDatabase() {
    abstract fun shopDao(): ShopDao
    abstract fun productDao(): ProductDao
    abstract fun driverDao(): DriverDao
    abstract fun orderDao(): OrderDao

    companion object {
        @Volatile
        private var INSTANCE: SourDeliveryDatabase? = null

        fun getDatabase(context: Context): SourDeliveryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SourDeliveryDatabase::class.java,
                    "sour_delivery_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
