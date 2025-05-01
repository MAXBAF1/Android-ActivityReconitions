package com.example.first

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.first.ActivityTransitionData.TRANSITIONS_EXTRA
import com.example.first.ActivityTransitionData.TRANSITIONS_RECEIVER_ACTION
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionEvent
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity

class ActivityTransitionsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("ActivityTransitionReceiver", "Transition Event Received")

        when (intent.action) {
            TRANSITIONS_RECEIVER_ACTION -> {
                if (ActivityTransitionResult.hasResult(intent)) {
                    val result: ActivityTransitionResult = ActivityTransitionResult.extractResult(intent) ?: return

                    for (event in result.transitionEvents) {
                        val transitionInfo = getTransitionInfo(event)

                        sendTransitionInfo(transitionInfo, context)

                        val serviceIntent = Intent(context, ActivityTransitionService::class.java).apply {
                            action = TRANSITIONS_EXTRA
                            putExtra(
                                TRANSITIONS_EXTRA,
                                transitionInfo
                            )
                        }
                        ContextCompat.startForegroundService(context, serviceIntent)
                        Toast.makeText(context, transitionInfo, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun getTransitionInfo(event: ActivityTransitionEvent): String {
        val activityType = activityType(event.activityType)
        val transitionType = transitionType(event.transitionType)
        Log.d("ActivityRecognition", "Activity Type: $activityType, Transition Type: $transitionType")

        return "$transitionType $activityType"
    }

    private fun sendTransitionInfo(transitionInfo: String, context: Context) {
        val intent = Intent(context, MainActivity::class.java)
        intent.putExtra(TRANSITIONS_EXTRA, transitionInfo)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        context.startActivity(intent)
    }


    private fun activityType(activityType: Int): String {
        return when (activityType) {
            DetectedActivity.IN_VEHICLE -> "IN_VEHICLE"
            DetectedActivity.ON_BICYCLE -> "ON_BICYCLE"
            DetectedActivity.RUNNING -> "RUNNING"
            DetectedActivity.STILL -> "STILL"
            DetectedActivity.WALKING -> "WALKING"
            else -> "OTHER activityType"
        }
    }

    private fun transitionType(transitionType: Int): String {
        return when (transitionType) {
            ActivityTransition.ACTIVITY_TRANSITION_ENTER -> "ENTER"
            ActivityTransition.ACTIVITY_TRANSITION_EXIT -> "EXIT"
            else -> "OTHER transitionType"
        }
    }
}

