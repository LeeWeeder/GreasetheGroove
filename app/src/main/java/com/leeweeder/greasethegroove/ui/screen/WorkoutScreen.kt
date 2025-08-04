package com.leeweeder.greasethegroove.ui.screen

import android.Manifest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.leeweeder.greasethegroove.data.repository.WorkoutState
import com.leeweeder.greasethegroove.ui.viewmodel.WorkoutViewModel
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import java.time.Duration
import java.time.Instant
import java.util.Locale

private fun formatTime(duration: Duration): String {
    // The Duration class handles negative values correctly, but we'll ensure it's at least zero.
    val d = if (duration.isNegative) Duration.ZERO else duration

    val hours = d.toHours()
    // This subtracts the hours, leaving only the remaining minutes part of the duration.
    val minutes = d.minusHours(hours).toMinutes()
    // This subtracts both hours and minutes, leaving the remaining seconds.
    val seconds = d.minusHours(hours).minusMinutes(minutes).seconds

    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun WorkoutScreen(viewModel: WorkoutViewModel = koinViewModel()) {
    val workoutState by viewModel.workoutState.collectAsState()
    var showRpeDialog by remember { mutableStateOf(false) }

    // Handle Notification Permission for Android 13+
    val notificationPermissionState =
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    LaunchedEffect(Unit) {
        if (!notificationPermissionState.status.isGranted) {
            notificationPermissionState.launchPermissionRequest()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when {
            workoutState == null -> {
                // Loading state
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            workoutState?.isSetupComplete == false -> {
                InitialSetupDialog(
                    onConfirm = { exercise, maxReps, rest ->
                        viewModel.saveInitialSetup(exercise, maxReps, rest)
                    }
                )
            }

            else -> {
                workoutState?.let { state ->
                    WorkoutDisplay(
                        state = workoutState!!,
                        onSetComplete = { showRpeDialog = true }
                    )
                }
            }
        }

        if (showRpeDialog) {
            RpeDialog(
                onDismiss = { showRpeDialog = false },
                onConfirm = { rpe ->
                    viewModel.completeSetAndAdjust(rpe)
                    showRpeDialog = false
                }
            )
        }
    }
}

@Composable
fun InitialSetupDialog(onConfirm: (String, Int, Int) -> Unit) {
    var exerciseName by remember { mutableStateOf("") }
    var maxReps by remember { mutableStateOf("") }
    var restDuration by remember { mutableFloatStateOf(30f) }
    var step by remember { mutableIntStateOf(1) }

    val onConfirmClicked = {
        when (step) {
            1 -> if (exerciseName.isNotBlank()) step = 2
            2 -> if (maxReps.toIntOrNull() != null) step = 3
            3 -> {
                val reps = maxReps.toIntOrNull()
                if (reps != null) {
                    onConfirm(exerciseName, reps, restDuration.toInt())
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(
                when (step) {
                    1 -> "Track new exercise"
                    2 -> "Set max reps"
                    else -> "Set rest time"
                }
            )
        },
        text = {
            Column {
                if (step == 1) {
                    OutlinedTextField(
                        value = exerciseName,
                        onValueChange = { exerciseName = it },
                        label = { Text("Exercise name") })
                }
                if (step == 2) {
                    OutlinedTextField(
                        value = maxReps,
                        onValueChange = { maxReps = it },
                        label = { Text("Max reps") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                if (step == 3) {
                    Text("${restDuration.toInt()} minutes")
                    Slider(
                        value = restDuration,
                        onValueChange = {
                            restDuration = it
                        },
                        valueRange = 30f..120f,
                        steps = 2
                    )
                }
            }
        },
        confirmButton = { Button(onClick = onConfirmClicked) { Text("Next") } }
    )
}

@Composable
fun WorkoutDisplay(
    state: WorkoutState,
    onSetComplete: () -> Unit
) {
    var remainingDuration by remember { mutableStateOf(Duration.ZERO) }

    // The LaunchedEffect is keyed to the Instant timestamp.
    LaunchedEffect(key1 = state.restPeriodEndTime) {
        val endTime = state.restPeriodEndTime
        // Loop while the current time is before the end time.
        while (Instant.now().isBefore(endTime)) {
            // Calculate the remaining duration safely.
            remainingDuration = Duration.between(Instant.now(), endTime)
            delay(1000)
        }
        remainingDuration = Duration.ZERO
    }

    // The timer is running if the duration is positive.
    val isTimerRunning = !remainingDuration.isZero && !remainingDuration.isNegative

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isTimerRunning) "Resting..." else "Ready for next set",
            style = MaterialTheme.typography.labelLarge,
            color = if (isTimerRunning) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))

        Text(state.exerciseName, style = MaterialTheme.typography.displayMedium)
        Text("${state.currentReps} reps", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(32.dp))

        // NEW: Dynamic text block for the timer/ready message
        Box(modifier = Modifier.height(60.dp), contentAlignment = Alignment.Center) {
            if (isTimerRunning) {
                Text(
                    text = "Next set in: ${formatTime(remainingDuration)}",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = "It's time to do your set!",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onSetComplete,
            // The button is disabled while the timer is running
            enabled = !isTimerRunning,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("I'm done!")
        }
    }
}

@Composable
fun RpeDialog(onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var rpe by remember { mutableFloatStateOf(5f) }
    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(Modifier.padding(24.dp)) {
                Text("How difficult was that set?", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(16.dp))
                Text(
                    "RPE: ${rpe.toInt()}",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Slider(
                    value = rpe,
                    onValueChange = { rpe = it },
                    valueRange = 1f..10f,
                    steps = 8
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { onConfirm(rpe.toInt()) },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Confirm")
                }
            }
        }
    }
}