package com.studypartner.planner.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        val DAILY_SUMMARY_ENABLED = booleanPreferencesKey("daily_summary_enabled")
        val DAILY_SUMMARY_TIME = stringPreferencesKey("daily_summary_time")
        val DEFAULT_REMINDER_OFFSET = intPreferencesKey("default_reminder_offset")
        val PARTNER_UPDATES_ENABLED = booleanPreferencesKey("partner_updates_enabled")
        val CURRENT_GROUP_ID = stringPreferencesKey("current_group_id")
    }

    val currentGroupIdFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[CURRENT_GROUP_ID]
    }

    val userPreferencesFlow: Flow<UserPreferences> = dataStore.data.map { preferences ->
        UserPreferences(
            dailySummaryEnabled = preferences[DAILY_SUMMARY_ENABLED] ?: true,
            dailySummaryTime = preferences[DAILY_SUMMARY_TIME] ?: "07:30",
            defaultReminderOffset = preferences[DEFAULT_REMINDER_OFFSET] ?: 15,
            partnerUpdatesEnabled = preferences[PARTNER_UPDATES_ENABLED] ?: true
        )
    }

    suspend fun updateCurrentGroupId(groupId: String?) {
        dataStore.edit { prefs ->
            if (groupId != null) {
                prefs[CURRENT_GROUP_ID] = groupId
            } else {
                prefs.remove(CURRENT_GROUP_ID)
            }
        }
    }

    suspend fun updateDailySummaryEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[DAILY_SUMMARY_ENABLED] = enabled }
    }

    suspend fun updateDailySummaryTime(time: String) {
        dataStore.edit { prefs -> prefs[DAILY_SUMMARY_TIME] = time }
    }

    suspend fun updateDefaultReminderOffset(offset: Int) {
        dataStore.edit { prefs -> prefs[DEFAULT_REMINDER_OFFSET] = offset }
    }

    suspend fun updatePartnerUpdatesEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[PARTNER_UPDATES_ENABLED] = enabled }
    }
}

data class UserPreferences(
    val dailySummaryEnabled: Boolean,
    val dailySummaryTime: String,
    val defaultReminderOffset: Int,
    val partnerUpdatesEnabled: Boolean
)
