package com.medut.reelscounter

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var bigCount: TextView
    private lateinit var sub: TextView
    private lateinit var card: TextView
    private lateinit var status: TextView

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun glow(tv: TextView, color: Int, r: Int) {
        tv.setShadowLayer(dp(r).toFloat(), 0f, 0f, color)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun buildUi() {
        val t = Themes.current(this)
        val match = ViewGroup.LayoutParams.MATCH_PARENT
        val wrap = ViewGroup.LayoutParams.WRAP_CONTENT

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(56), dp(24), dp(72))
        }

        val title = TextView(this).apply {
            text = "KAYDIRMA SAYACI"
            textSize = 13f
            letterSpacing = 0.25f
            setTextColor(t.accent)
        }
        glow(title, t.accent, 10)

        bigCount = TextView(this).apply {
            textSize = 84f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-thin", Typeface.NORMAL)
            setPadding(0, dp(12), 0, 0)
        }
        glow(bigCount, t.accent, 24)

        sub = TextView(this).apply {
            textSize = 14f
            setTextColor(0xFFB8C0CC.toInt())
        }

        card = TextView(this).apply {
            textSize = 16f
            setTextColor(Color.WHITE)
            setLineSpacing(0f, 1.25f)
            background = Themes.glowBg(t.bg, t.accent, dp(20).toFloat(), dp(1), dp(4))
            setPadding(dp(24), dp(22), dp(24), dp(22))
        }

        status = TextView(this).apply {
            textSize = 13f
            setTextColor(0xFFB8C0CC.toInt())
            setLineSpacing(0f, 1.2f)
        }

        box.addView(title)
        box.addView(bigCount)
        box.addView(sub)
        box.addView(card, LinearLayout.LayoutParams(match, wrap).apply { topMargin = dp(28) })
        box.addView(status, LinearLayout.LayoutParams(match, wrap).apply { topMargin = dp(20) })

        fun button(label: String, action: () -> Unit) {
            val b = Button(this)
            b.text = label
            b.isAllCaps = false
            b.textSize = 14f
            b.setTextColor(t.accent)
            glow(b, t.accent, 8)
            b.background = Themes.glowBg(t.bg, t.accent, dp(14).toFloat(), dp(1), dp(3))
            b.setOnClickListener { action() }
            box.addView(b, LinearLayout.LayoutParams(match, dp(56)).apply { topMargin = dp(14) })
        }

        button("1) Uygulama bilgisi (kısıtlı ayarlar)") {
            startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:$packageName")
                )
            )
        }
        button("2) Erişilebilirlik ayarlarını aç") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        button("3) Kullanım erişimini aç") {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
        button("Yenile") { refresh() }

        val themeLabel = TextView(this).apply {
            text = "TEMA · ${t.name}"
            textSize = 12f
            letterSpacing = 0.2f
            setTextColor(t.accent)
        }
        glow(themeLabel, t.accent, 8)
        box.addView(themeLabel, LinearLayout.LayoutParams(wrap, wrap).apply { topMargin = dp(32) })

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.START
        }
        val selected = Store.theme(this).coerceIn(0, Themes.all.lastIndex)
        Themes.all.forEachIndexed { i, th ->
            val sw = View(this)
            sw.background = Themes.swatch(th.accent, i == selected, dp(9), dp(2))
            sw.setOnClickListener {
                Store.setTheme(this, i)
                buildUi()
                refresh()
            }
            row.addView(sw, LinearLayout.LayoutParams(dp(52), dp(52)).apply { rightMargin = dp(10) })
        }
        box.addView(row, LinearLayout.LayoutParams(wrap, wrap).apply { topMargin = dp(14) })

        val sv = ScrollView(this)
        sv.setBackgroundColor(t.bg)
        sv.addView(box)
        setContentView(sv)
    }

    private fun serviceEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val me = ComponentName(this, ScrollService::class.java).flattenToString()
        return enabled.split(":").any { it.equals(me, ignoreCase = true) }
    }

    private fun notificationsOk(): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun refresh() {
        val usage = UsageHelper.hasAccess(this)
        val ig = if (usage) UsageHelper.format(UsageHelper.todayMillis(this, UsageHelper.INSTAGRAM)) else "-"
        val yt = if (usage) UsageHelper.format(UsageHelper.todayMillis(this, UsageHelper.YOUTUBE)) else "-"
        val total = Store.total(this)

        bigCount.text = total.toString()
        sub.text = if (Store.isLocked(this)) "🔒 Kilitli · gece 12'de açılır"
        else "/ ${Store.LOCK_AT} · kilide ${Store.LOCK_AT - total} kaldı"
        card.text = "Reels      ${Store.get(this, "reels")}\nShorts     ${Store.get(this, "shorts")}\n\n" +
            "Instagram  $ig\nYouTube    $yt"
        status.text = "Erişilebilirlik: " + (if (serviceEnabled()) "AÇIK ✓" else "KAPALI ✗") +
            "\nKullanım erişimi: " + (if (usage) "VAR ✓" else "YOK ✗") +
            "\nBildirim izni: " + (if (notificationsOk()) "VAR ✓" else "YOK ✗")
        CounterWidget.update(this)
    }
}
