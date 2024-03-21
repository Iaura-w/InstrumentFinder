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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
                        FileUpload()
                    }
                }
            }
        }

    }

    @Preview
    @Composable
    fun FileUpload() {
        var selectedOption by remember { mutableStateOf(0) }
        val recordAudioIntent = Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION)

        var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
        val viewModel: FileUploadViewModel by viewModels()
        var serverResponse = ""
        val context = LocalContext.current

        val filePickerLauncher: ActivityResultLauncher<String> = rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            selectedFileUri = uri
        }

        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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

                    OutlinedButton(onClick = {
                        val intent = Intent(context, HistoryActivity::class.java)
                        intent.putExtra("HISTORY_LIST", ArrayList(viewModel.uploadHistory))
                        startActivity(intent)
                    }) {
                        Text("History")
                    }
                }


                selectedFileUri?.let { uri ->
                    val fileName = uriContentToUriFile(
                        this@MainActivity,
                        uri
                    )?.lastPathSegment ?: "Unknown"
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Selected File: $fileName",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Log.d(TAG, serverResponse)

                    if (viewModel.loading) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = { viewModel.cancelUpload() }) {
                            Text("Cancel")
                        }
                    } else {
                        ElevatedButton(onClick = {
                            val uriFile = uriContentToUriFile(this@MainActivity, uri)

                            Log.d(TAG, "uri file: $uriFile")

                            serverResponse = "Uploading file..."
                            if (uriFile != null) {
                                viewModel.uploadFile(uriFile, fileName, context)
                            } else {
                                serverResponse = "File does not exist"
                            }

                        }) {
                            Text("Send")
                        }
                    }

                    serverResponse = viewModel.serverResponse
                    Text("Result:\n $serverResponse")

                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
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
}
