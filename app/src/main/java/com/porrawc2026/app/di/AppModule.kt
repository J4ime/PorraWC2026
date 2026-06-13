package com.porrawc2026.app.di

import android.content.Context
import androidx.room.Room
import com.porrawc2026.app.data.local.AppDatabase
import com.porrawc2026.app.data.local.dao.*
import com.porrawc2026.app.data.remote.ApiFootballConfig
import com.porrawc2026.app.data.remote.SofascoreConfig
import com.porrawc2026.app.data.remote.ApiFootballService
import com.porrawc2026.app.data.remote.ApiService
import com.porrawc2026.app.data.remote.ZafronixService
import com.porrawc2026.app.data.remote.ZafronixConfig
import com.porrawc2026.app.data.remote.SofascoreApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
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
        ).addMigrations(AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4).build()
    }

    @Provides
    fun provideTeamDao(db: AppDatabase): TeamDao = db.teamDao()

    @Provides
    fun provideMatchDao(db: AppDatabase): MatchDao = db.matchDao()

    @Provides
    fun provideQuestionDao(db: AppDatabase): QuestionDao = db.questionDao()

    @Provides
    fun providePlayerPredictionDao(db: AppDatabase): PlayerPredictionDao = db.playerPredictionDao()

    @Provides
    fun provideKnockoutPredictionDao(db: AppDatabase): KnockoutPredictionDao = db.knockoutPredictionDao()

    @Provides
    fun provideGroupStandingDao(db: AppDatabase): GroupStandingDao = db.groupStandingDao()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.football-data.org/v4/")
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
    @Named("sofascore")
    fun provideSofascoreRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.sofascore.com/api/v1/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideSofascoreApiService(@Named("sofascore") retrofit: Retrofit): SofascoreApiService {
        return retrofit.create(SofascoreApiService::class.java)
    }

    @Provides
    @Singleton
    @Named("apifootball")
    fun provideApiFootballRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(ApiFootballConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideApiFootballService(@Named("apifootball") retrofit: Retrofit): ApiFootballService {
        return retrofit.create(ApiFootballService::class.java)
    }

    @Provides
    @Singleton
    @Named("zafronix")
    fun provideZafronixRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(ZafronixConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideZafronixService(@Named("zafronix") retrofit: Retrofit): ZafronixService {
        return retrofit.create(ZafronixService::class.java)
    }
}
