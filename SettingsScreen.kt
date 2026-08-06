package com.example.cam

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("cam_prefs", 0) }
    var apiKey by remember { mutableStateOf(prefs.getString("api_key", "") ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Anthropic API Key")
            Text(
                "Get one at console.anthropic.com. It's stored only on your phone.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                prefs.edit().putString("api_key", apiKey).apply()
                onBack()
            }) {
                Text("Save")
            }
        }
    }
}
