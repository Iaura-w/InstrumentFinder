package com.example.instrumentfinder

import RetrofitInstance
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
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
    private var _serverResponse by mutableStateOf("")
    val serverResponse: String
        get() = _serverResponse

    fun uploadFile(fileUri: Uri) {
        Log.d(TAG2, "file uri: $fileUri")
        val file = File(fileUri.path.orEmpty())
        Log.d(TAG2, "file name: ${file.name}")
        val requestFile = file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", file.path, requestFile)
        val call = RetrofitInstance.getRetrofitService().uploadFile(body)

        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                val gson = Gson()
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    val responseEntity =
                        gson.fromJson(responseBody?.string() ?: "", ResponseEntity::class.java)
                    _serverResponse = "Result:\n" + responseEntity.result
                    Log.d(TAG2, "response code: ${response.code()}")
                    Log.d(TAG2, "response message: ${responseEntity.message}")
                    Log.d(TAG2, "response result:  ${responseEntity.result}")
                } else {
                    val errorBody = response.errorBody()
                    val responseEntity =
                        gson.fromJson(errorBody?.string() ?: "", ResponseEntity::class.java)
                    _serverResponse = "Upload failed: " + responseEntity.message
                    Log.d(TAG2, "response code: ${response.code()}")
                    Log.d(TAG2, "response message: ${responseEntity.message}")
                    Log.d(TAG2, "response result:  ${responseEntity.result}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                _serverResponse = "Network error: ${t.message}"
            }
        })
    }
}