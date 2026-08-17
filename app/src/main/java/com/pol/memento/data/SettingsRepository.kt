package com.pol.memento.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Inizializza il DataStore a livello di file per assicurare una singola istanza
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    // Definiamo le "chiavi" con cui salveremo i vari dati
    companion object {
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val DEFAULT_PRIORITY = stringPreferencesKey("default_priority")
        val IS_GRID_VIEW = booleanPreferencesKey("is_grid_view")
    }

    // Flusso che emette il valore della Dark Mode (default: false)
    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_DARK_MODE] ?: false
    }

    val isGridView: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_GRID_VIEW] ?: false
    }

    // Flusso per la priorità di default (default: MEDIUM)
    val defaultPriority: Flow<PriorityLevel> = context.dataStore.data.map { preferences ->
        val priorityName = preferences[DEFAULT_PRIORITY] ?: PriorityLevel.MEDIUM.name
        PriorityLevel.valueOf(priorityName)
    }

    // Funzioni per salvare i dati
    suspend fun setDarkMode(isEnabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_DARK_MODE] = isEnabled
        }
    }

    suspend fun setGridView(isEnabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_GRID_VIEW] = isEnabled
        }
    }

    suspend fun setDefaultPriority(priority: PriorityLevel) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_PRIORITY] = priority.name
        }
    }
}
