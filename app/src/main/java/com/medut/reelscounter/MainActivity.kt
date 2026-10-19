package com.medut.reelscounter

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var info: TextView
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
        }
        val title = TextView(this).apply {
            text = "Kaydırma Sayacı"
            textSize = 26f
        }
        info = TextView(this).apply { textSize = 18f; setPadding(0, 32, 0, 32) }
        status = TextView(this).apply { textSize = 15f; setPadding(0, 0, 0, 24) }

        fun button(label: String, action: () -> Unit): Button {
            val b = Button(this)
            b.text = label
            b.setOnClickListener { action() }
            return b
        }

        box.addView(title)
        box.addView(info)
        box.addView(status)
        box.addView(button("1) Uygulama bilgisi (kısıtlı ayarlar için)") {
            startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:$packageName")
                )
            )
        })
        box.addView(button("2) Erişilebilirlik ayarlarını aç") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        box.addView(button("3) Kullanım erişimini aç") {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        })
        box.addView(button("Yenile") { refresh() })

        val sv = ScrollView(this)
        sv.addView(box)
        setContentView(sv)
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun serviceEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val me = ComponentName(this, ScrollService::class.java).flattenToString()
        return enabled.split(":").any { it.equals(me, ignoreCase = true) }
    }

    private fun refresh() {
        val usage = UsageHelper.hasAccess(this)
        val ig = if (usage) UsageHelper.format(UsageHelper.todayMillis(this, UsageHelper.INSTAGRAM)) else "-"
        val yt = if (usage) UsageHelper.format(UsageHelper.todayMillis(this, UsageHelper.YOUTUBE)) else "-"
        info.text = "Bugün\n\nReels: ${Store.get(this, "reels")}\nShorts: ${Store.get(this, "shorts")}\n\n" +
            "Instagram süresi: $ig\nYouTube süresi: $yt"
        status.text = "Erişilebilirlik servisi: " + (if (serviceEnabled()) "AÇIK ✓" else "KAPALI ✗") +
            "\nKullanım erişimi: " + (if (usage) "VAR ✓" else "YOK ✗")
        CounterWidget.update(this)
    }
}
