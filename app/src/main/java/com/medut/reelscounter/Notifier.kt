package com.medut.reelscounter

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object Notifier {
    private const val CHANNEL = "warnings"

    // Her uyarı bir öncekinden daha sert. {n} = şu anki sayı, {k} = kilide kalan.
    private val warnings = listOf(
        "{n}. kaydırma. Dur." to
            "Fark etmeden {n} reels geçtin. Telefonu bırak. Bu ilk ve en nazik uyarı.",
        "{n} oldu. Hâlâ mı?" to
            "Uyarıyı görmezden geldin. {k} kaydırma sonra seni kilitleyeceğim. Şimdi çık.",
        "{n}. Zamanın eriyor." to
            "Kilide {k} kaldı. Devam edersen sonuçlarına katlanırsın, uyardım.",
        "{n}! Sabrım tükeniyor." to
            "Kilide {k} kaldı. Devam edersen gece yarısına kadar Reels'e giremeyeceksin. Pazarlık yok.",
        "{n}. Kapıyı kapatmak üzereyim." to
            "{k} kaydırma sonra yüzüne kapatıyorum. Şimdi bırak, yoksa yarına kadar dışarıda kalırsın.",
        "{n}. SON UYARI." to
            "Sadece {k} kaydırma kaldı. Bir tane daha ilerlersen kilit düşer ve bu sefer geri dönüş yok."
    )

    private fun nm(c: Context) = c.getSystemService(NotificationManager::class.java)

    private fun post(c: Context, id: Int, title: String, text: String) {
        nm(c).createNotificationChannel(
            NotificationChannel(CHANNEL, "Uyarılar", NotificationManager.IMPORTANCE_HIGH)
        )
        val pi = PendingIntent.getActivity(
            c, 0, Intent(c, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = Notification.Builder(c, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        nm(c).notify(id, n)
    }

    fun warn(c: Context, total: Int) {
        val idx = (total / Store.WARN_EVERY - 1).coerceIn(0, warnings.lastIndex)
        val (title, text) = warnings[idx]
        val k = (Store.LOCK_AT - total).toString()
        post(
            c, total,
            title.replace("{n}", total.toString()).replace("{k}", k),
            text.replace("{n}", total.toString()).replace("{k}", k)
        )
    }

    fun locked(c: Context) {
        post(
            c, 9999, "KİLİTLENDİN 🔒",
            "${Store.LOCK_AT} sınırını aştın. Reels ve Shorts gece 12'ye kadar kapalı. Uyarmıştım."
        )
    }
}
