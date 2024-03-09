package com.example.instrumentfinder

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object History {
    private const val PREFERENCES_FILE_KEY = "com.example.instrumentfinder.HISTORY_PREF"
    private const val HISTORY_KEY = "HISTORY"
    private const val MAX_ITEMS = 30

    fun saveHistory(context: Context, newItem: Pair<String, String>) {
        val sharedPreferences =
            context.getSharedPreferences(PREFERENCES_FILE_KEY, Context.MODE_PRIVATE)
        val history = getHistory(context).toMutableList()
        history.add(0, newItem)
        if (history.size > MAX_ITEMS) {
            history.removeLast()
        }
        sharedPreferences.edit().putString(HISTORY_KEY, Gson().toJson(history)).apply()
    }

    fun getHistory(context: Context): List<Pair<String, String>> {
        val sharedPreferences =
            context.getSharedPreferences(PREFERENCES_FILE_KEY, Context.MODE_PRIVATE)
        val historyJson = sharedPreferences.getString(HISTORY_KEY, "[]")
        val type = object : TypeToken<List<Pair<String, String>>>() {}.type
        return Gson().fromJson(historyJson, type)
    }
}
