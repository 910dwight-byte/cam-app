package com.example.cam

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import java.util.Locale

@Composable
fun ChatScreen(onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val viewModel: ChatViewModel = viewModel()
    val prefs = remember { context.getSharedPreferences("cam_prefs", 0) }
    val apiKey = remember { prefs.getString("api_key", "") ?: "" }
    var input by remember { mutableStateOf("") }
    var imageMode by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        val t = TextToSpeech(context) { }
        tts = t
        onDispose { t.shutdown() }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val text = results?.firstOrNull()
            if (!text.isNullOrBlank()) input = text
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Cam...")
            }
            speechLauncher.launch(intent)
        }
    }

    LaunchedEffect(viewModel.messages.size) {
        if (viewModel.messages.isNotEmpty()) {
            listState.animateScrollToItem(viewModel.messages.size - 1)
            val last = viewModel.messages.last()
            if (last.role == "assistant" && last.imageUrl == null) {
                tts?.speak(last.text, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cam") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        bottomBar = {
            Column(Modifier.padding(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (imageMode) "Image mode" else "Chat mode", modifier = Modifier.weight(1f))
                    Switch(checked = imageMode, onCheckedChange = { imageMode = it })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                    }) {
                        Icon(Icons.Default.Mic, contentDescription = "Speak")
                    }
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(if (imageMode) "Describe the photo..." else "Message Cam...") }
                    )
                    TextButton(onClick = {
                        val text = input
                        input = ""
                        if (imageMode) {
                            viewModel.sendImageRequest(text)
                        } else {
                            viewModel.sendTextMessage(text, apiKey)
                        }
                    }) {
                        Text("Send")
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(viewModel.messages) { msg ->
                MessageBubble(msg)
            }
            if (viewModel.isLoading.value) {
                item { Text("Cam is thinking...") }
            }
        }
    }
}

@Composable
fun MessageBubble(msg: ChatMessage) {
    val isUser = msg.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.padding(4.dp)
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(msg.text)
                if (msg.imageUrl != null) {
                    AsyncImage(
                        model = msg.imageUrl,
                        contentDescription = "Generated image",
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .fillMaxWidth()
                    )
                }
            }
        }
    }
}
