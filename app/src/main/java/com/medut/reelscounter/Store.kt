package com.medut.reelscounter

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Store {
    // ---- Ayarlar (istersen buradan değiştir) ----
    const val WARN_EVERY = 30          // her 30 kaydırmada uyarı
    const val LOCK_AT = 200            // günlük limit (Reels + Shorts toplamı)
    const val LOCK_WHOLE_APP = false   // true: limit dolunca Instagram/YouTube'un tamamını kilitler

    private fun prefs(c: Context) =
        c.getSharedPreferences("counter", Context.MODE_PRIVATE)

    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun get(c: Context, type: String): Int =
        prefs(c).getInt("${type}_${today()}", 0)

    fun total(c: Context): Int = get(c, "reels") + get(c, "shorts")

    // Gün anahtarı tarihe bağlı olduğu için gece 12'de sayaç ve kilit kendiliğinden sıfırlanır.
    fun isLocked(c: Context): Boolean = total(c) >= LOCK_AT

    fun increment(c: Context, type: String): Int {
        val key = "${type}_${today()}"
        val v = prefs(c).getInt(key, 0) + 1
        prefs(c).edit().putInt(key, v).apply()
        return v
    }

    fun theme(c: Context): Int = prefs(c).getInt("theme", 0)
    fun setTheme(c: Context, i: Int) = prefs(c).edit().putInt("theme", i).apply()
}
