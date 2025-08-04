package com.leeweeder.greasethegroove.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Data class to hold the entire state
data class WorkoutState(
    val exerciseName: String,
    val maxReps: Int,
    val restDuration: Int, // in minutes
    val currentReps: Int,
    val isSetupComplete: Boolean
)

class WorkoutRepository(private val dataStore: DataStore<Preferences>) {

    private object PreferencesKeys {
        val EXERCISE_NAME = stringPreferencesKey("exercise_name")
        val MAX_REPS = intPreferencesKey("max_reps")
        val REST_DURATION = intPreferencesKey("rest_duration")
        val CURRENT_REPS = intPreferencesKey("current_reps")
        val IS_SETUP_COMPLETE = booleanPreferencesKey("is_setup_complete")
    }

    suspend fun saveInitialWorkout(exerciseName: String, maxReps: Int, restDuration: Int, initialReps: Int) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.EXERCISE_NAME] = exerciseName
            prefs[PreferencesKeys.MAX_REPS] = maxReps
            prefs[PreferencesKeys.REST_DURATION] = restDuration
            prefs[PreferencesKeys.CURRENT_REPS] = initialReps
            prefs[PreferencesKeys.IS_SETUP_COMPLETE] = true
        }
    }

    suspend fun updateCurrentReps(newReps: Int) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.CURRENT_REPS] = newReps
        }
    }

    val workoutState: Flow<WorkoutState> = dataStore.data.map { prefs ->
        WorkoutState(
            exerciseName = prefs[PreferencesKeys.EXERCISE_NAME] ?: "",
            maxReps = prefs[PreferencesKeys.MAX_REPS] ?: 0,
            restDuration = prefs[PreferencesKeys.REST_DURATION] ?: 0,
            currentReps = prefs[PreferencesKeys.CURRENT_REPS] ?: 0,
            isSetupComplete = prefs[PreferencesKeys.IS_SETUP_COMPLETE] ?: false
        )
    }
}