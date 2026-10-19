package com.medut.reelscounter

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Store {
    private fun prefs(c: Context) =
        c.getSharedPreferences("counter", Context.MODE_PRIVATE)

    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun get(c: Context, type: String): Int =
        prefs(c).getInt("${type}_${today()}", 0)

    fun increment(c: Context, type: String): Int {
        val key = "${type}_${today()}"
        val v = prefs(c).getInt(key, 0) + 1
        prefs(c).edit().putInt(key, v).apply()
        return v
    }
}
