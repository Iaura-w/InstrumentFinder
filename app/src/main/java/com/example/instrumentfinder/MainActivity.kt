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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

private const val TAG = "APP"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Column {
                FileUpload()
                Recording()
            }
        }
    }

    @Composable
    fun FileUpload() {
        var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
        val viewModel: FileUploadViewModel by viewModels()
        var serverResponse = ""

        val filePickerLauncher: ActivityResultLauncher<String> = rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            selectedFileUri = uri
        }

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Choose audio file")

            Button(
                onClick = {
                    filePickerLauncher.launch("audio/*")
                }
            ) {
                Text("Choose File")
            }

            selectedFileUri?.let { uri ->
                Text(
                    "Selected File: ${
                        uriContentToUriFile(
                            this@MainActivity,
                            uri
                        )?.lastPathSegment ?: "Unknown"
                    }"
                )
                Log.d(TAG, serverResponse)

                if (viewModel.loading) {
                    Button(onClick = { viewModel.cancelUpload() }) {
                        Text("Cancel")
                    }
                    CircularProgressIndicator()
                } else {
                    Button(onClick = {
                        val uriFile = uriContentToUriFile(this@MainActivity, uri)

                        Log.d(TAG, "uri file: $uriFile")

                        serverResponse = "Uploading file..."
                        if (uriFile != null) {
                            viewModel.uploadFile(uriFile)
                        } else {
                            serverResponse = "File does not exist"
                        }

                    }) {
                        Text("Send")
                    }
                }

                serverResponse = viewModel.serverResponse
                Text("Response:\n $serverResponse")
            }
        }
    }


    @Composable
    fun Recording() {
        val recordAudioIntent = Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION)

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Audio Recording")

            Button(
                onClick = {
                    startActivity(recordAudioIntent)
                }
            ) {
                Text("Record Audio")
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