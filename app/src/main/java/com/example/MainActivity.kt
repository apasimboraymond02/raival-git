package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.MainAppScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.NotificationHelper
import com.example.ui.viewmodel.RaivalViewModel

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("MainActivity", "Notification permission granted")
        } else {
            Log.w("MainActivity", "Notification permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Validate intent - only allow expected actions
        val intent = intent
        if (intent.action != null && intent.action != Intent.ACTION_MAIN && 
            intent.action != Intent.ACTION_VIEW) {
            Log.w("MainActivity", "Unexpected intent action: ${intent.action}")
        }

        // Create the high-priority notification channel
        NotificationHelper.createNotificationChannel(this)

        // Request POST_NOTIFICATIONS permission dynamically on Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            val viewModel: RaivalViewModel = viewModel()
            val currentUser by viewModel.currentUser.collectAsState()
            val systemInDark = androidx.compose.foundation.isSystemInDarkTheme()
            val darkTheme = when (currentUser?.profileTheme) {
                "light" -> false
                "dark" -> true
                else -> systemInDark
            }

            MyApplicationTheme(darkTheme = darkTheme) {
                androidx.compose.material3.Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainAppScreen(viewModel = viewModel, innerPadding = innerPadding)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Validate new intent
        if (intent.action != null && intent.action != Intent.ACTION_MAIN && 
            intent.action != Intent.ACTION_VIEW) {
            Log.w("MainActivity", "Unexpected intent action: ${intent.action}")
        }
        setIntent(intent)
    }
}
