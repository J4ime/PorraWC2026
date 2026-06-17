package com.porrawc2026.app.util

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "porra_prefs")

class PrefsManager(private val context: Context) {

    companion object {
        val EXCEL_FILE_NAME = stringPreferencesKey("excel_filename")
        val AUTO_REFRESH = booleanPreferencesKey("auto_refresh")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_POSITION = intPreferencesKey("user_position")
        val PREVIOUS_POSITION = intPreferencesKey("previous_position")
        val CACHE_TIMESTAMP = longPreferencesKey("cache_timestamp")
        val LAST_CACHE_VERSION = intPreferencesKey("last_cache_version")
        val PROCESSED_GOAL_KEYS = stringPreferencesKey("processed_goal_keys")
    }

    val excelFileName: Flow<String?> = context.dataStore.data.map { it[EXCEL_FILE_NAME] }
    val autoRefresh: Flow<Boolean> = context.dataStore.data.map { it[AUTO_REFRESH] ?: true }
    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { it[NOTIFICATIONS_ENABLED] ?: true }
    val userName: Flow<String?> = context.dataStore.data.map { it[USER_NAME] }
    val userPosition: Flow<Int?> = context.dataStore.data.map { it[USER_POSITION] }
    val previousPosition: Flow<Int?> = context.dataStore.data.map { it[PREVIOUS_POSITION] }

    suspend fun setExcelFileName(name: String?) {
        context.dataStore.edit { it[EXCEL_FILE_NAME] = name ?: "" }
    }

    suspend fun setAutoRefresh(enabled: Boolean) {
        context.dataStore.edit { it[AUTO_REFRESH] = enabled }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setUserName(name: String?) {
        context.dataStore.edit { it[USER_NAME] = name ?: "" }
    }

    suspend fun setUserPosition(pos: Int?) {
        context.dataStore.edit {
            if (pos != null) it[USER_POSITION] = pos
            else it.remove(USER_POSITION)
        }
    }

    suspend fun setPreviousPosition(pos: Int?) {
        context.dataStore.edit {
            if (pos != null) it[PREVIOUS_POSITION] = pos
            else it.remove(PREVIOUS_POSITION)
        }
    }

    suspend fun getAutoRefreshSync(): Boolean = autoRefresh.first()
    suspend fun getNotificationsSync(): Boolean = notificationsEnabled.first()
    suspend fun getExcelFileNameSync(): String? = excelFileName.first()?.takeIf { it.isNotBlank() }
    suspend fun getUserNameSync(): String? = userName.first()?.takeIf { it.isNotBlank() }
    suspend fun getUserPositionSync(): Int? = userPosition.first()
    suspend fun getPreviousPositionSync(): Int? = previousPosition.first()

    val cacheTimestamp: Flow<Long?> = context.dataStore.data.map { it[CACHE_TIMESTAMP] }
    val lastCacheVersion: Flow<Int?> = context.dataStore.data.map { it[LAST_CACHE_VERSION] }

    suspend fun setCacheTimestamp(ts: Long) {
        context.dataStore.edit { it[CACHE_TIMESTAMP] = ts }
    }

    suspend fun getCacheTimestampSync(): Long? = cacheTimestamp.first()

    suspend fun setLastCacheVersion(version: Int) {
        context.dataStore.edit { it[LAST_CACHE_VERSION] = version }
    }

    suspend fun getLastCacheVersionSync(): Int? = lastCacheVersion.first()

    suspend fun setProcessedGoalKeys(keys: Set<String>) {
        context.dataStore.edit { it[PROCESSED_GOAL_KEYS] = keys.joinToString(",") }
    }

    suspend fun getProcessedGoalKeys(): Set<String> {
        val raw = context.dataStore.data.first()[PROCESSED_GOAL_KEYS] ?: ""
        return raw.split(",").filter { it.isNotBlank() }.toSet()
    }

    suspend fun clearProcessedGoalKeys() {
        context.dataStore.edit { it.remove(PROCESSED_GOAL_KEYS) }
    }
}
