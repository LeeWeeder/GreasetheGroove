package com.leeweeder.greasethegroove.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

// Data class now uses the modern, type-safe Instant class
data class WorkoutState(
    val exerciseName: String,
    val maxReps: Int,
    val restDuration: Int, // in minutes
    val currentReps: Int,
    val isSetupComplete: Boolean,
    val restPeriodEndTime: Instant // CHANGED: Using Instant instead of Long
)

class WorkoutRepository(private val dataStore: DataStore<Preferences>) {

    private object PreferencesKeys {
        val EXERCISE_NAME = stringPreferencesKey("exercise_name")
        val MAX_REPS = intPreferencesKey("max_reps")
        val REST_DURATION = intPreferencesKey("rest_duration")
        val CURRENT_REPS = intPreferencesKey("current_reps")
        val IS_SETUP_COMPLETE = booleanPreferencesKey("is_setup_complete")
        // CHANGED: Storing the timestamp as a String
        val REST_PERIOD_END_TIME_STRING = stringPreferencesKey("rest_period_end_time_string")
    }

    suspend fun saveInitialWorkout(exerciseName: String, maxReps: Int, restDuration: Int, initialReps: Int) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.EXERCISE_NAME] = exerciseName
            prefs[PreferencesKeys.MAX_REPS] = maxReps
            prefs[PreferencesKeys.REST_DURATION] = restDuration
            prefs[PreferencesKeys.CURRENT_REPS] = initialReps
            prefs[PreferencesKeys.IS_SETUP_COMPLETE] = true
            // Set the end time to the beginning of the epoch, meaning no rest period.
            prefs[PreferencesKeys.REST_PERIOD_END_TIME_STRING] = Instant.EPOCH.toString()
        }
    }

    suspend fun updateAfterSetCompletion(newReps: Int, newRestEndTime: Instant) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.CURRENT_REPS] = newReps
            // Store the Instant as an ISO-8601 string (e.g., "2025-08-05T10:15:30.00Z")
            prefs[PreferencesKeys.REST_PERIOD_END_TIME_STRING] = newRestEndTime.toString()
        }
    }

    val workoutState: Flow<WorkoutState> = dataStore.data.map { prefs ->
        // Read the string and parse it back into an Instant.
        val endTimeString = prefs[PreferencesKeys.REST_PERIOD_END_TIME_STRING]
        val endTime = if (endTimeString != null) Instant.parse(endTimeString) else Instant.EPOCH

        WorkoutState(
            exerciseName = prefs[PreferencesKeys.EXERCISE_NAME] ?: "",
            maxReps = prefs[PreferencesKeys.MAX_REPS] ?: 0,
            restDuration = prefs[PreferencesKeys.REST_DURATION] ?: 0,
            currentReps = prefs[PreferencesKeys.CURRENT_REPS] ?: 0,
            isSetupComplete = prefs[PreferencesKeys.IS_SETUP_COMPLETE] ?: false,
            restPeriodEndTime = endTime
        )
    }
}