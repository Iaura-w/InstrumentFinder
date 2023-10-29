package com.example.instrumentfinder

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.instrumentfinder.ui.theme.InstrumentFinderTheme
import java.io.File

class MainActivity : ComponentActivity() {

    private val resultLauncherAudio =
        registerForActivityResult(ActivityResultContracts.GetContent()) { audioUri: Uri? ->
            if (audioUri != null) {
                selectedAudioFilePath = File(audioUri.path.toString())
            }
        }

    private var selectedAudioFilePath by mutableStateOf<File?>(null)
    private val serverUrl = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            InstrumentFinderTheme {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ChooseAudioFile()
                }
            }
        }
    }

    @Composable
    private fun ChooseAudioFile() {
        Button(
            onClick = { resultLauncherAudio.launch("audio/*") },
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Choose audio file")
        }

        if (selectedAudioFilePath != null) {
            Button(
                onClick = { uploadAudioFile(selectedAudioFilePath!!) },
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Send")
            }
        }
    }

    private fun uploadAudioFile(audioFilePath: File) {
        Toast.makeText(this, "sending $audioFilePath", Toast.LENGTH_SHORT).show()
    }
}