package com.example.instrumentfinder

import RetrofitInstance
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
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

private const val TAG2 = "APP2"

class FileUploadViewModel : ViewModel() {
    private var currentCall: Call<ResponseBody>? = null
    private var _serverResponse by mutableStateOf("")
    private var _loading by mutableStateOf(false)

    val serverResponse: String
        get() = _serverResponse
    val loading: Boolean
        get() = _loading

    fun uploadFile(fileUri: Uri) {
        _loading = true
        val startTime = System.currentTimeMillis()
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(fileUri.path.orEmpty())
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
                    handleResponse(response)
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    _loading = false
                    _serverResponse = "Network error: ${t.message}"
                    Log.d(TAG2, "Upload error: ${t.message}")
                }
            })
            currentCall = call
        }
    }

    fun cancelUpload() {
        currentCall?.cancel()
        _loading = false
        _serverResponse = "Upload cancelled"
    }

    private fun handleResponse(response: Response<ResponseBody>) {
        _loading = false
        val gson = Gson()
        if (response.isSuccessful) {
            response.body()?.let {
                val responseEntity = gson.fromJson(it.string(), ResponseEntity::class.java)
                _serverResponse = "\n" + responseEntity.result
                logResponse(response, responseEntity)
            }
        } else {
            response.errorBody()?.let {
                val responseEntity = gson.fromJson(it.string(), ResponseEntity::class.java)
                _serverResponse = "Upload failed: " + responseEntity.message
                logResponse(response, responseEntity)
            }
        }
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
}