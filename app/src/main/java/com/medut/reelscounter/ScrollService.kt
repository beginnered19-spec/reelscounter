package com.medut.reelscounter

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView

class ScrollService : AccessibilityService() {

    private var lastCount = 0L
    private var lastWidget = 0L
    private var lastLockCheck = 0L
    private var wm: WindowManager? = null
    private var overlay: TextView? = null
    private val handler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable { overlay?.visibility = View.GONE }

    private val igIds = listOf(
        "com.instagram.android:id/clips_viewer_view_pager",
        "com.instagram.android:id/clips_viewer_recycler_view",
        "com.instagram.android:id/root_clips_layout",
        "com.instagram.android:id/clips_swipe_refresh_container"
    )
    private val ytIds = listOf(
        "com.google.android.youtube:id/reel_recycler",
        "com.google.android.youtube:id/reel_player_page_container",
        "com.google.android.youtube:id/shorts_container",
        "com.google.android.youtube:id/reel_watch_fragment_root"
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        createOverlay()
    }

    private fun createOverlay() {
        val tv = TextView(this).apply {
            textSize = 20f
            visibility = View.GONE
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 140
        }
        overlay = tv
        wm?.addView(tv, lp)
    }

    private fun applyTheme(tv: TextView) {
        val t = Themes.current(this)
        val d = resources.displayMetrics.density
        tv.setTextColor(t.accent)
        tv.setShadowLayer(12 * d, 0f, 0f, t.accent)
        tv.background = Themes.glowBg(t.bg, t.accent, 40 * d, (1.5f * d).toInt(), (3 * d).toInt())
        val h = (28 * d).toInt()
        val v = (14 * d).toInt()
        tv.setPadding(h, v, h, v)
    }

    private fun show(text: String, ms: Long = 3000) {
        overlay?.let {
            applyTheme(it)
            it.text = text
            it.visibility = View.VISIBLE
        }
        handler.removeCallbacks(hideRunnable)
        handler.postDelayed(hideRunnable, ms)
    }

    private fun detectType(pkg: String): String? {
        val root = rootInActiveWindow ?: return null
        val isInstagram = pkg == UsageHelper.INSTAGRAM
        val ids = if (isInstagram) igIds else ytIds
        val type = if (isInstagram) "reels" else "shorts"
        for (id in ids) {
            if (root.findAccessibilityNodeInfosByViewId(id).isNotEmpty()) return type
        }
        return null
    }

    private fun enforceLock() {
        show("🔒 Kilitli · gece 12'de açılır", 4000)
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    override fun onAccessibilityEvent(e: AccessibilityEvent) {
        val pkg = e.packageName?.toString() ?: return
        if (pkg != UsageHelper.INSTAGRAM && pkg != UsageHelper.YOUTUBE) return
        val now = SystemClock.elapsedRealtime()

        // Günlük limit dolduysa Reels/Shorts ekranına girmeyi engelle
        if (Store.isLocked(this)) {
            if (now - lastLockCheck > 400) {
                lastLockCheck = now
                if (Store.LOCK_WHOLE_APP || detectType(pkg) != null) enforceLock()
            }
            return
        }

        if (e.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (now - lastWidget > 30_000) {
                lastWidget = now
                CounterWidget.update(this)
            }
            return
        }
        if (e.eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED) return

        val type = detectType(pkg) ?: return
        if (now - lastCount < 700) return
        lastCount = now

        val n = Store.increment(this, type)
        val total = Store.total(this)
        show(if (type == "reels") "Reels: $n" else "Shorts: $n")
        lastWidget = now
        CounterWidget.update(this)

        if (total >= Store.LOCK_AT) {
            Notifier.locked(this)
            handler.postDelayed({ enforceLock() }, 1500)
        } else if (total % Store.WARN_EVERY == 0) {
            Notifier.warn(this, total)
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        handler.removeCallbacks(hideRunnable)
        overlay?.let { wm?.removeView(it) }
        super.onDestroy()
    }
}
