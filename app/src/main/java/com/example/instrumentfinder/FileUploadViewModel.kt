package com.example.instrumentfinder

import RetrofitInstance
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG2 = "APP2"

class FileUploadViewModel(private val context: Context) : ViewModel() {
    private var currentCall: Call<ResponseBody>? = null
    private var _serverResponse by mutableStateOf("")
    private var _loading by mutableStateOf(false)

    companion object {
        private const val HISTORY_PREFS = "HistoryPrefs"
        private const val HISTORY_KEY = "UploadHistory"
        private const val MAX_HISTORY_SIZE = 10
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(HISTORY_PREFS, Context.MODE_PRIVATE)
    }

    val serverResponse: String
        get() = _serverResponse
    val loading: Boolean
        get() = _loading

    fun uploadFile(fileUri: Uri, fileName: String) {
        _loading = true
        val startTime = System.currentTimeMillis()
        viewModelScope.launch(Dispatchers.IO) {
            val originalFile = File(fileUri.path.orEmpty())
            val file = createFileWithChangedName(originalFile, context)

            if (file.extension.lowercase(Locale.getDefault()) != "mp3") {
                Log.d(TAG2, "Converting file to mp3")

                convertToMp3(fileUri.path.toString(), context) { tempFile ->
                    tempFile?.let {
                        uploadFile(it, startTime, fileName, file, it)
                    } ?: run {
                        _loading = false
                        _serverResponse = "File conversation error. Changing filename may help."
                        Log.d(TAG2, "Conversation error")
                    }
                }
            } else {
                uploadFile(file, startTime, fileName, file, null)
            }
        }
    }

    private fun uploadFile(
        file: File,
        startTime: Long,
        fileName: String,
        sanitizedFile: File,
        tempFile: File?
    ) {
        Log.d(TAG2, "Sending file: $file")
        val requestFile = file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
        val call = RetrofitInstance.getRetrofitService().uploadFile(body)

        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {
                val endTime = System.currentTimeMillis()
                logResponseTime(startTime, endTime)
                handleResponse(response, fileName, sanitizedFile, tempFile)
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                _loading = false
                _serverResponse = "Network error: ${t.message}"
                Log.d(TAG2, "Upload error: ${t.message}")
            }
        })
        currentCall = call
    }

    fun cancelUpload() {
        currentCall?.cancel()
        _loading = false
        _serverResponse = "Upload canceled"
    }

    private fun handleResponse(
        response: Response<ResponseBody>, fileName: String,
        sanitizedFile: File, tempFile: File?
    ) {
        _loading = false
        val gson = Gson()
        if (response.isSuccessful) {
            response.body()?.let {
                val responseEntity = gson.fromJson(it.string(), ResponseEntity::class.java)
                val formattedResponse = reformatResponse(responseEntity)
                _serverResponse = "OK\n$formattedResponse"
                logResponse(response, responseEntity)
                saveToHistory(fileName, "$formattedResponse\n")
            }
        } else {
            response.errorBody()?.let {
                val responseEntity = gson.fromJson(it.string(), ResponseEntity::class.java)
                _serverResponse = "Upload failed: " + responseEntity.message
                logResponse(response, responseEntity)
            }
        }
        if (sanitizedFile.exists()) {
            Log.d(TAG2, "deleting: $sanitizedFile")
            sanitizedFile.delete()
        }
        if (tempFile != null) {
            if (tempFile.exists()) {
                Log.d(TAG2, "deleting: $tempFile")
                tempFile.delete()
            }
        }
    }

    private fun reformatResponse(responseEntity: ResponseEntity) =
        responseEntity.result.lines()
            .filter { it.isNotBlank() }
            .joinToString("\n") { line ->
                val parts = line.split(' ')
                val instrument = parts.dropLast(1).joinToString(" ").uppercase()
                val percentage =
                    "%.2f".format(parts.last().removeSuffix("%").toDouble()) + "%"
                "$instrument: $percentage"
            }

    private fun logResponse(response: Response<*>, responseEntity: ResponseEntity) {
        Log.d(TAG2, "response code: ${response.code()}")
        Log.d(TAG2, "response message: ${responseEntity.message}")
        Log.d(TAG2, "response result: ${responseEntity.result}")
    }

    private fun logResponseTime(startTime: Long, endTime: Long) {
        val responseTime = endTime - startTime
        Log.d(TAG2, "Response time: $responseTime ms")
    }

    private fun saveToHistory(filename: String, result: String) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        val historyItem = HistoryItem(filename, currentDate, result)

        val history = loadHistory().toMutableList()
        if (history.size == MAX_HISTORY_SIZE) {
            history.removeAt(0)
        }
        history.add(historyItem)

        val editor = prefs.edit()
        editor.putString(HISTORY_KEY, Gson().toJson(history))
        editor.apply()
    }

    fun loadHistory(): List<HistoryItem> {
        val historyJson = prefs.getString(HISTORY_KEY, "")
        return if (historyJson.isNullOrEmpty()) {
            emptyList()
        } else {
            val type = object : TypeToken<List<HistoryItem>>() {}.type
            Gson().fromJson(historyJson, type)
        }
    }

    private fun convertToMp3(inputFilePath: String, context: Context, onComplete: (File?) -> Unit) {
        val tempFile = File.createTempFile("converted_", ".mp3", context.cacheDir)
        val filePath = inputFilePath.replace(Regex("[- ]"), "_")
        Log.d(TAG2, "temp file absolute path: ${tempFile.absolutePath}")
        Log.d(TAG2, "input file path: $filePath")
        val command =
            "-y -v debug -i $filePath -c:a libmp3lame -b:a 128k -ac 2 -ar 44100 -vn ${tempFile.absolutePath}"
        FFmpegKit.executeAsync(command) { session ->
            val returnCode = session.returnCode
            if (ReturnCode.isSuccess(returnCode)) {
                Log.d(TAG2, "file converted: $tempFile")
                onComplete(tempFile)
            } else {
                Log.d(TAG2, "return code: ${returnCode}")
                Log.d(
                    TAG2,
                    String.format(
                        "Command failed with state %s and rc %s, %s, %s",
                        session.state,
                        session.returnCode,
                        session.output,
                        session.logs
                    )
                );
                onComplete(null)
            }
        }
    }

    private fun changeFileName(fileName: String): String {
        return fileName.replace(Regex("[- ]"), "_")
    }

    private fun createFileWithChangedName(originalFile: File, context: Context): File {
        Log.d(TAG2, "creating file with changed name: $originalFile")
        val changedFileName = changeFileName(originalFile.name)
        val changedFile = File(originalFile.parent, changedFileName)

        if (!changedFile.exists()) {
            originalFile.copyTo(changedFile)
        }

        return changedFile
    }
}