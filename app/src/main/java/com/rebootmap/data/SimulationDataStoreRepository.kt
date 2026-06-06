package com.rebootmap.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rebootmap.data.model.SimulationPersistedState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.simulationDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "simulation_preferences",
)

class SimulationDataStoreRepository(
    private val context: Context,
) : SimulationRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val stateKey = stringPreferencesKey("simulation_state")

    override suspend fun load(): SimulationPersistedState? {
        return context.simulationDataStore.data.map { preferences ->
            preferences[stateKey]?.let { encoded ->
                runCatching { json.decodeFromString<SimulationPersistedState>(encoded) }.getOrNull()
            }
        }.first()
    }

    override suspend fun save(state: SimulationPersistedState) {
        context.simulationDataStore.edit { preferences ->
            preferences[stateKey] = json.encodeToString(state)
        }
    }

    override suspend fun clear() {
        context.simulationDataStore.edit { preferences ->
            preferences.remove(stateKey)
        }
    }
}
