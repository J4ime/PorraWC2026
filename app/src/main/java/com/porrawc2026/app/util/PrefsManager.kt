package com.porrawc2026.app.util

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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
    }

    val excelFileName: Flow<String?> = context.dataStore.data.map { it[EXCEL_FILE_NAME] }
    val autoRefresh: Flow<Boolean> = context.dataStore.data.map { it[AUTO_REFRESH] ?: true }
    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { it[NOTIFICATIONS_ENABLED] ?: true }

    suspend fun setExcelFileName(name: String?) {
        context.dataStore.edit { it[EXCEL_FILE_NAME] = name ?: "" }
    }

    suspend fun setAutoRefresh(enabled: Boolean) {
        context.dataStore.edit { it[AUTO_REFRESH] = enabled }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun getAutoRefreshSync(): Boolean = autoRefresh.first()
    suspend fun getNotificationsSync(): Boolean = notificationsEnabled.first()
    suspend fun getExcelFileNameSync(): String? = excelFileName.first()?.takeIf { it.isNotBlank() }
}
