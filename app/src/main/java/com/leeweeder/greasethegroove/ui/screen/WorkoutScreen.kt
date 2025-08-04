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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.leeweeder.greasethegroove.ui.viewmodel.WorkoutViewModel
import org.koin.androidx.compose.koinViewModel

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
                        exerciseName = state.exerciseName,
                        currentReps = state.currentReps,
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
    var restDuration by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(1) }

    val onConfirmClicked = {
        when (step) {
            1 -> if (exerciseName.isNotBlank()) step = 2
            2 -> if (maxReps.toIntOrNull() != null) step = 3
            3 -> {
                val reps = maxReps.toIntOrNull()
                val duration = restDuration.toIntOrNull()
                if (reps != null && duration != null && duration in 30..120) {
                    onConfirm(exerciseName, reps, duration)
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(
                when (step) {
                    1 -> "Track New Exercise"
                    2 -> "Set Max Reps"
                    else -> "Set Rest Time (30-120 mins)"
                }
            )
        },
        text = {
            Column {
                if (step == 1) {
                    OutlinedTextField(
                        value = exerciseName,
                        onValueChange = { exerciseName = it },
                        label = { Text("Exercise Name") })
                }
                if (step == 2) {
                    OutlinedTextField(
                        value = maxReps,
                        onValueChange = { maxReps = it },
                        label = { Text("Max Reps") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                if (step == 3) {
                    OutlinedTextField(
                        value = restDuration,
                        onValueChange = { restDuration = it },
                        label = { Text("Rest in Minutes") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
        },
        confirmButton = { Button(onClick = onConfirmClicked) { Text("Next") } }
    )
}

@Composable
fun WorkoutDisplay(exerciseName: String, currentReps: Int, onSetComplete: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Next Set", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(16.dp))
        Text(exerciseName, style = MaterialTheme.typography.headlineMedium)
        Text("$currentReps reps", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(32.dp))
        Button(onClick = onSetComplete, modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)) {
            Text("I'm Done!")
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