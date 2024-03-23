package com.example.instrumentfinder

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.example.instrumentfinder.ui.theme.InstrumentFinderTheme
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

private const val TAG = "APP"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            InstrumentFinderTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column {
                        TopBar()
                        MainApp()
                    }
                }
            }
        }

    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun TopBar() {
        TopAppBar(
            title = { Text(getString(R.string.app_name)) },
            colors = TopAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                actionIconContentColor = MaterialTheme.colorScheme.onSecondary,
                scrolledContainerColor = MaterialTheme.colorScheme.secondary
            )
        )
    }

    @Preview
    @Composable
    fun MainApp() {
        val recordAudioIntent = Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION)

        var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
        val viewModel: FileUploadViewModel by viewModels()
        var serverResponse = ""
        var isInstrumentFound by remember { mutableStateOf(false) }
        val context = LocalContext.current

        val filePickerLauncher: ActivityResultLauncher<String> = rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            selectedFileUri = uri
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilledTonalButton(onClick = { filePickerLauncher.launch("audio/*") }) {
                        Text("Choose File")
                    }

                    Button(onClick = {
                        startActivity(recordAudioIntent)
                    }) {
                        Text("Record")
                    }

                    FilledTonalButton(onClick = {
                        val intent = Intent(context, HistoryActivity::class.java)
                        intent.putExtra("HISTORY_LIST", ArrayList(viewModel.uploadHistory))
                        startActivity(intent)
                    }) {
                        Text("History")
                    }
                }
            }

            selectedFileUri?.let { uri ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val fileName = uriContentToUriFile(
                            this@MainActivity,
                            uri
                        )?.lastPathSegment ?: "Unknown"

                        Text("File Upload", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            fileName,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        if (viewModel.loading) {
                            LinearProgressIndicator(
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { viewModel.cancelUpload() },
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                            ) {
                                Text("Cancel")
                            }
                        } else {
                            ElevatedButton(
                                onClick = {
                                    val uriFile = uriContentToUriFile(this@MainActivity, uri)
                                    Log.d(TAG, "uri file: $uriFile")
                                    serverResponse = "Uploading file..."
                                    if (uriFile != null) {
                                        viewModel.uploadFile(uriFile, fileName, context)
                                    } else {
                                        serverResponse = "File does not exist"
                                    }
                                },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text("Send")
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Result", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))

                        serverResponse = viewModel.serverResponse
                        if (serverResponse.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            if (serverResponse.contains("OK")) {
                                isInstrumentFound = true
                            }
                            Text(
                                serverResponse.lineSequence().drop(1).joinToString("\n"),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
                Log.d(TAG, "is instrument found $isInstrumentFound")

                if (isInstrumentFound) {
                    val highestInstrument = getHighestInstrument(serverResponse)
                    if (highestInstrument.isNotEmpty()) {
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Most Likely Instrument",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    highestInstrument,
                                    style = MaterialTheme.typography.displaySmall,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.align(alignment = Alignment.CenterHorizontally)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

}

private fun getHighestInstrument(serverResponse: String): String {
    if (!serverResponse.startsWith("OK")) return ""

    return serverResponse.lineSequence()
        .drop(1)
        .map { line ->
            val parts = line.split(' ')
            val instrument = parts.dropLast(1).joinToString(" ").removeSuffix(":")
            val percent = parts.last().removeSuffix("%").toDouble()
            instrument to percent
        }
        .maxByOrNull { it.second }
        ?.first
        ?: ""
}

private fun uriContentToUriFile(context: Context, contentUri: Uri): Uri? {
    val contentResolver: ContentResolver = context.contentResolver
    val fileName = getFileNameFromContentUri(context, contentUri)

    try {
        val inputStream: InputStream? = contentResolver.openInputStream(contentUri)
        if (inputStream != null) {
            val outputFile = File(context.cacheDir, fileName.toString())
            val outputStream = FileOutputStream(outputFile)
            val buffer = ByteArray(1024)
            var read: Int

            while (inputStream.read(buffer).also { read = it } > 0) {
                outputStream.write(buffer, 0, read)
            }
            outputStream.flush()
            outputStream.close()
            inputStream.close()

            return outputFile.toUri()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return null
}

private fun getFileNameFromContentUri(context: Context, contentUri: Uri): String? {
    val contentResolver: ContentResolver = context.contentResolver
    val cursor = contentResolver.query(contentUri, null, null, null, null)

    cursor?.use {
        if (it.moveToFirst()) {
            val nameColumnIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameColumnIndex != -1) {
                return it.getString(nameColumnIndex)
            }
        }
    }
    return null
}

