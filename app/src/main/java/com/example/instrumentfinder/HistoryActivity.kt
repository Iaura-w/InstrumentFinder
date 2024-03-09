package com.example.instrumentfinder

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.activity.ComponentActivity

class HistoryActivity : ComponentActivity() {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        val historyList = History.getHistory(this)
        val listView = findViewById<ListView>(R.id.history_list_view)

        val adapter = historyList?.let { it ->
            ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                it.map { "File: ${it.first} - ${it.second}" })
        }
        listView.adapter = adapter
    }
}