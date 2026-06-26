package com.porrawc2026.app.di

import android.content.Context
import androidx.room.Room
import com.porrawc2026.app.BuildConfig
import com.porrawc2026.app.data.local.AppDatabase
import com.porrawc2026.app.data.local.dao.*
import com.porrawc2026.app.data.remote.ApiConfig
import com.porrawc2026.app.data.remote.ApiService
import com.porrawc2026.app.data.remote.EspnConfig
import com.porrawc2026.app.data.remote.EspnService
import com.porrawc2026.app.util.PrefsManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "porra_wc2026.db"
        ).createFromAsset("porrawc2026.db")
        .addMigrations(AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6, AppDatabase.MIGRATION_6_7, AppDatabase.MIGRATION_7_8, AppDatabase.MIGRATION_8_9).build()
    }

    @Provides
    @Singleton
    fun provideTeamDao(db: AppDatabase): TeamDao = db.teamDao()

    @Provides
    @Singleton
    fun provideMatchDao(db: AppDatabase): MatchDao = db.matchDao()

    @Provides
    @Singleton
    fun provideQuestionDao(db: AppDatabase): QuestionDao = db.questionDao()

    @Provides
    @Singleton
    fun providePlayerPredictionDao(db: AppDatabase): PlayerPredictionDao = db.playerPredictionDao()

    @Provides
    @Singleton
    fun provideKnockoutPredictionDao(db: AppDatabase): KnockoutPredictionDao = db.knockoutPredictionDao()

    @Provides
    @Singleton
    fun provideGroupStandingDao(db: AppDatabase): GroupStandingDao = db.groupStandingDao()

    @Provides
    @Singleton
    fun providePrefsManager(@ApplicationContext context: Context): PrefsManager = PrefsManager(context)

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                    else HttpLoggingInterceptor.Level.NONE
        }
    }

    private fun apiKeyInterceptor(headerName: String, apiKey: String): Interceptor {
        return Interceptor { chain ->
            val request = chain.request().newBuilder()
                .header(headerName, apiKey)
                .build()
            chain.proceed(request)
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(logging: HttpLoggingInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("football-data")
    fun provideFootballDataClient(logging: HttpLoggingInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(apiKeyInterceptor("X-Auth-Token", BuildConfig.FOOTBALL_DATA_API_KEY))
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(@Named("football-data") client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }

    @Provides
    @Singleton
    @Named("espn")
    fun provideEspnRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(EspnConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideEspnService(@Named("espn") retrofit: Retrofit): EspnService {
        return retrofit.create(EspnService::class.java)
    }
}
