package com.example.instrumentfinder

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class FileUploadViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FileUploadViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FileUploadViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}