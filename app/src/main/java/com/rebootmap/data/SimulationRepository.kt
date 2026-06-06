package com.rebootmap.data

import com.rebootmap.data.model.SimulationPersistedState

interface SimulationRepository {
    suspend fun load(): SimulationPersistedState?
    suspend fun save(state: SimulationPersistedState)
    suspend fun clear()
}
