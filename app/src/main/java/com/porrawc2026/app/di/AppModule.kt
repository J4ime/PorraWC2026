package com.porrawc2026.app.di

import android.content.Context
import androidx.room.Room
import com.porrawc2026.app.BuildConfig
import com.porrawc2026.app.data.local.AppDatabase
import com.porrawc2026.app.data.local.dao.*
import com.porrawc2026.app.data.remote.*
import com.porrawc2026.app.util.PrefsManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.ResponseBody.Companion.toResponseBody
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
        ).addMigrations(AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5).build()
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
    fun providePrefsManager(@ApplicationContext context: Context): PrefsManager = PrefsManager(context)

    private fun loggingInterceptor(): HttpLoggingInterceptor {
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
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor())
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("football-data")
    fun provideFootballDataClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(apiKeyInterceptor("X-Auth-Token", BuildConfig.FOOTBALL_DATA_API_KEY))
            .addInterceptor(loggingInterceptor())
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("api-sports")
    fun provideApiSportsClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(apiKeyInterceptor("x-apisports-key", BuildConfig.API_SPORTS_KEY))
            .addInterceptor(loggingInterceptor())
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
    @Named("sofascore")
    fun provideSofascoreRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(SofascoreConfig.BASE_URL)
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
    fun provideApiFootballRetrofit(@Named("api-sports") client: OkHttpClient): Retrofit {
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
    @Named("worldcup26")
    fun provideWorldCup26Client(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val response = chain.proceed(chain.request())
                val raw = response.body?.string() ?: return@addInterceptor response
                val fixed = fixWorldCup26Json(raw)
                response.newBuilder()
                    .body(fixed.toResponseBody(response.body?.contentType()))
                    .build()
            }
            .addInterceptor(loggingInterceptor())
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private fun fixWorldCup26Json(json: String): String {
        var result = json
        for (field in listOf("home_scorers", "away_scorers")) {
            val key = "\"$field\":\""
            var idx = result.indexOf(key)
            while (idx >= 0) {
                val start = idx + key.length
                val braceIdx = result.indexOfAny(charArrayOf('{', '['), start)
                if (braceIdx < 0 || braceIdx > start + 5) break
                val openChar = result[braceIdx]
                val closeChar = if (openChar == '{') '}' else ']'
                var pos = braceIdx
                var depth = 0
                var endPos = -1
                while (pos < result.length) {
                    when (result[pos]) {
                        openChar -> depth++
                        closeChar -> {
                            depth--
                            if (depth == 0) {
                                if (pos + 1 < result.length && result[pos + 1] == '"') {
                                    endPos = pos + 1
                                }
                                break
                            }
                        }
                        '\\' -> pos++
                    }
                    pos++
                }
                if (endPos < 0) break
                val content = result.substring(braceIdx + 1, pos)
                if (content.contains("\"") && !content.contains("\\\"")) {
                    val escaped = content.replace("\"", "\\\"")
                    result = result.substring(0, braceIdx + 1) + escaped + result.substring(pos)
                }
                idx = result.indexOf(key, idx + 1)
            }
        }
        return result
    }

    @Provides
    @Singleton
    @Named("worldcup26")
    fun provideWorldCup26Retrofit(@Named("worldcup26") client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(WorldCup26Config.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideWorldCup26Service(@Named("worldcup26") retrofit: Retrofit): WorldCup26Service {
        return retrofit.create(WorldCup26Service::class.java)
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
