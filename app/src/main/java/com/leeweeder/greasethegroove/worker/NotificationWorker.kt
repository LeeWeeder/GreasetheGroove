package com.leeweeder.greasethegroove.worker

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.leeweeder.greasethegroove.GtGApplication
import com.leeweeder.greasethegroove.MainActivity
import com.leeweeder.greasethegroove.R

class NotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val exerciseName = inputData.getString(KEY_EXERCISE_NAME) ?: "your exercise"
        val reps = inputData.getInt(KEY_REPS, 1)

        showNotification(exerciseName, reps)

        return Result.success()
    }

    private fun showNotification(exerciseName: String, reps: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent =
            PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, GtGApplication.CHANNEL_ID)
            .setSmallIcon(R.drawable.exercise_24px)
            .setContentTitle("Time for your next set!")
            .setContentText("Perform $reps reps of $exerciseName.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        }
    }

    companion object {
        const val WORK_NAME = "gtg_workout_notification"
        const val KEY_EXERCISE_NAME = "exercise_name"
        const val KEY_REPS = "reps"
        const val NOTIFICATION_ID = 1
    }
}