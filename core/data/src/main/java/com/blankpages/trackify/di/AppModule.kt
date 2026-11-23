package com.blankpages.trackify.di

import android.content.Context
import com.blankpages.trackify.data.local.AppPreferences
import com.blankpages.trackify.data.local.TrackifyDao
import com.blankpages.trackify.data.local.TrackifyDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TrackifyDatabase = TrackifyDatabase.get(context)

    @Provides
    @Singleton
    fun provideDao(database: TrackifyDatabase): TrackifyDao = database.dao()

    @Provides
    @Singleton
    fun providePreferences(@ApplicationContext context: Context): AppPreferences = AppPreferences(context)
}
