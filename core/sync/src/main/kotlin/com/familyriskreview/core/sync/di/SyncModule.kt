package com.familyriskreview.core.sync.di

import com.familyriskreview.core.sync.FakeSyncClient
import com.familyriskreview.core.sync.SupabaseSyncClient
import com.familyriskreview.core.sync.SyncClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SyncModule {

    @Provides
    @Singleton
    @Named("supabaseUrl")
    fun provideSupabaseUrl(): String? = System.getenv("FRR_SUPABASE_URL")

    @Provides
    @Singleton
    @Named("supabaseAnonKey")
    fun provideSupabaseAnonKey(): String? = System.getenv("FRR_SUPABASE_ANON_KEY")

    @Provides
    @Singleton
    fun provideSyncClient(
        @Named("supabaseUrl") supabaseUrl: String?,
        @Named("supabaseAnonKey") supabaseAnonKey: String?,
    ): SyncClient {
        val supabase = SupabaseSyncClient(supabaseUrl, supabaseAnonKey)
        return if (supabase.isConfigured()) supabase else FakeSyncClient()
    }
}
