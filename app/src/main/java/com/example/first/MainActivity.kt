package com.example.first

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.example.first.ActivityTransitionData.TRANSITIONS_EXTRA
import com.example.first.ActivityTransitionData.TRANSITIONS_RECEIVER_ACTION
import com.example.first.ui.theme.FirstTheme
import com.example.first.ui.theme.gray
import com.example.first.ui.theme.line
import com.example.first.ui.theme.topcolor
import com.example.first.ui.theme.white
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityRecognitionClient
import com.google.android.gms.location.ActivityTransitionRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var activityClient: ActivityRecognitionClient
    private lateinit var request: ActivityTransitionRequest
    private lateinit var pendingIntent: PendingIntent

    private val activityTransitionReceiver by lazy { ActivityTransitionsReceiver() }

    private var currentActivity by mutableStateOf("Запуск")
    private var activityChangeTime by mutableStateOf(getCurrentFormattedTime())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        foregroundService()

        activityClient = ActivityRecognition.getClient(this)

        if (!checkRecognitionPermissionIfLaterVersionQ()) {
            requestRecognitionPermission()
        }

        setContent {
            FirstTheme {
                StatusScreen(
                    currentActivity = currentActivity,
                    activityChangeTime = activityChangeTime,
                    getCurrentTime = { getCurrentFormattedTime() })
            }
        }

        initPendingIntent()

        if (checkRecognitionPermissionIfLaterVersionQ()) {
            registerActivityTransitionUpdates()
        }
    }

    private fun foregroundService() {

        /*Intent(this, ActivityTransitionService::class.java).run {
            startForegroundService(this)
            Log.d("ActivityTransition", "Foreground Service 시작됨 (Android O 이상)")
        }*/
    }

    override fun onStart() {
        super.onStart()
        val intentFilter = IntentFilter(TRANSITIONS_RECEIVER_ACTION)

        registerReceiver(
            activityTransitionReceiver, intentFilter, RECEIVER_NOT_EXPORTED
        )
    }

    private fun checkRecognitionPermissionIfLaterVersionQ(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val activityRecognitionGranted =
                PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.ACTIVITY_RECOGNITION
                )

            val locationPermissionGranted =
                PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_FINE_LOCATION
                ) || PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_COARSE_LOCATION
                )

            activityRecognitionGranted && locationPermissionGranted
        } else {
            true
        }
    }

    private fun requestRecognitionPermission() {
        ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.ACTIVITY_RECOGNITION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ), 0
        )
    }

    private fun initPendingIntent() {
        val intent = Intent(TRANSITIONS_RECEIVER_ACTION)
        intent.setPackage(this.packageName)

        pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent, PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun registerActivityTransitionUpdates() {
        request = ActivityTransitionRequest(ActivityTransitionData.getActivityTransitionList())
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        activityClient.requestActivityTransitionUpdates(request, pendingIntent)
            .addOnSuccessListener {
                currentActivity = "START activity recognition"
            }
            .addOnFailureListener { exception ->
                currentActivity = exception.localizedMessage ?: "ERROR"
            }
    }

    // From ActivityTransitionReceiver
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d("Activity Recognition", "onNewIntent")
        checkIntentData(intent)
    }

    private fun checkIntentData(intent: Intent) {
        val activityTransition = intent.getStringExtra(TRANSITIONS_EXTRA)

        if (activityTransition != null) {
            currentActivity = activityTransition
            activityChangeTime = getCurrentFormattedTime()
        }
    }

    private fun getCurrentFormattedTime(): String {
        return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    }
}

@Composable
fun StatusScreen(
    currentActivity: String, activityChangeTime: String, getCurrentTime: () -> String,
) {
    var time by remember { mutableStateOf(getCurrentTime()) }
    val mutableStatusList = remember { mutableStateListOf<String>() }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(mutableStatusList.size) {
        coroutineScope.launch {
            listState.scrollToItem(mutableStatusList.size - 1)
        }
    }

    LaunchedEffect(currentActivity) {
        while (true) {
            time = getCurrentTime()

            mutableStatusList.add(currentActivity)

            delay(1000L)
        }
    }

    Scaffold(
        topBar = { CustomCardTopAppBar(currentActivity, activityChangeTime) },
        content = { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(color = white)
            ) {
                Column {
                    StatusList(mutableStatusList, listState, getCurrentTime)
                }
            }
        },
    )
}

@Composable
fun CustomCardTopAppBar(status: String, activityChangeTime: String) {
    ElevatedCard(
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(147.dp),
        shape = RectangleShape,
        colors = CardDefaults.cardColors(containerColor = topcolor),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = status,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 24.sp, fontWeight = FontWeight.Bold
                    ),
                    color = Black,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = activityChangeTime,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 20.sp
                    ),
                    color = gray,
                )
            }
        }
    }
}

@Composable
fun StatusList(statusList: List<String>, listState: LazyListState, getCurrentTime: () -> String) {
    LazyColumn(
        state = listState, reverseLayout = false, modifier = Modifier.fillMaxSize()
    ) {
        items(statusList) {
            StatusCardItem(it, getCurrentTime)
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp), thickness = 1.dp, color = line
            )
        }
    }
}

@Composable
fun StatusCardItem(status: String, getCurrentTime: () -> String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = white
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 0.1.dp, horizontal = 0.dp),
        shape = RectangleShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = getCurrentTime(),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(start = 35.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = status,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
    }
}

fun getCurrentFormattedTime(): String {
    return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
}

@Preview(showBackground = true)
@Composable
fun PreviewStatusScreen() {
    FirstTheme {
        StatusScreen(
            currentActivity = "Старт",
            activityChangeTime = getCurrentFormattedTime(),
            getCurrentTime = { getCurrentFormattedTime() })
    }
}
