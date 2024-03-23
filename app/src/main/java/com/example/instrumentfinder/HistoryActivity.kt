package com.example.instrumentfinder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.instrumentfinder.ui.theme.InstrumentFinderTheme

class HistoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: FileUploadViewModel by viewModels {
                FileUploadViewModelFactory(applicationContext)
            }
            HistoryScreen(viewModel)
        }
    }

    @Composable
    fun HistoryScreen(viewModel: FileUploadViewModel) {
        InstrumentFinderTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    HistoryTopBar()
                    HistoryView(viewModel)
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun HistoryTopBar() {
        TopAppBar(
            title = { Text("${getString(R.string.app_name)} - History") },
            colors = TopAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                actionIconContentColor = MaterialTheme.colorScheme.onSecondary,
                scrolledContainerColor = MaterialTheme.colorScheme.secondary
            )
        )
    }

    @Composable
    fun HistoryView(viewModel: FileUploadViewModel) {
        val history = viewModel.loadHistory().asReversed()

        LazyColumn(modifier = Modifier.padding(8.dp)) {
            items(history) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("File: ${item.filename}", style = MaterialTheme.typography.titleMedium)
                        Text("Date: ${item.date}", style = MaterialTheme.typography.titleSmall)
                        Text(item.result, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}