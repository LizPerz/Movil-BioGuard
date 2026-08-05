package com.bioguard.movil.di

import android.content.Context
import com.bioguard.movil.data.local.BioGuardDatabase
import com.bioguard.movil.data.local.CachedDataDao
import com.bioguard.movil.data.local.PendingDataDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DatabaseModule {

    @Provides
    @Singleton
    fun provideBioGuardDatabase(@ApplicationContext context: Context): BioGuardDatabase {
        return BioGuardDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun providePendingDataDao(database: BioGuardDatabase): PendingDataDao {
        return database.pendingDataDao()
    }

    @Provides
    @Singleton
    fun provideCachedDataDao(database: BioGuardDatabase): CachedDataDao {
        return database.cachedDataDao()
    }
}
