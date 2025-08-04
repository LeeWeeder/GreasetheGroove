package com.leeweeder.greasethegroove.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.leeweeder.greasethegroove.data.repository.WorkoutRepository
import com.leeweeder.greasethegroove.data.repository.WorkoutState
import com.leeweeder.greasethegroove.domain.usecase.AdjustRepsUseCase
import com.leeweeder.greasethegroove.domain.usecase.CalculateRepsUseCase
import com.leeweeder.greasethegroove.worker.NotificationWorker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

class WorkoutViewModel(
    private val repository: WorkoutRepository,
    private val calculateRepsUseCase: CalculateRepsUseCase,
    private val adjustRepsUseCase: AdjustRepsUseCase,
    private val workManager: WorkManager
) : ViewModel() {

    val workoutState: StateFlow<WorkoutState?> = repository.workoutState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun saveInitialSetup(exerciseName: String, maxReps: Int, restDuration: Int) {
        viewModelScope.launch {
            val initialReps = calculateRepsUseCase(maxReps, restDuration)
            repository.saveInitialWorkout(exerciseName, maxReps, restDuration, initialReps)
        }
    }

    fun completeSetAndAdjust(rpe: Int) {
        viewModelScope.launch {
            val currentState = workoutState.value ?: return@launch
            val newReps = adjustRepsUseCase(currentState.currentReps, rpe)

            // Use java.time.Duration for type-safe calculation.
            val restDuration = Duration.ofMinutes(currentState.restDuration.toLong())
            // Calculate the future Instant.
            val newRestEndTime = Instant.now().plus(restDuration)

            repository.updateAfterSetCompletion(newReps, newRestEndTime)

            scheduleNextNotification(currentState.restDuration, currentState.exerciseName, newReps)
        }
    }

    private fun scheduleNextNotification(delayInMinutes: Int, exerciseName: String, reps: Int) {
        // ... (This function remains unchanged as WorkManager uses Long)
        val data = Data.Builder()
            .putString(NotificationWorker.KEY_EXERCISE_NAME, exerciseName)
            .putInt(NotificationWorker.KEY_REPS, reps)
            .build()

        val notificationWorkRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(delayInMinutes.toLong(), TimeUnit.MINUTES)
            .setInputData(data)
            .build()

        workManager.enqueueUniqueWork(
            NotificationWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            notificationWorkRequest
        )
    }
}