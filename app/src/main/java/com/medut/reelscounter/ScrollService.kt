package com.medut.reelscounter

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
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
            setTextColor(Color.WHITE)
            textSize = 20f
            setPadding(48, 20, 48, 20)
            setBackgroundResource(R.drawable.overlay_bg)
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

    private fun show(text: String) {
        overlay?.apply {
            this.text = text
            visibility = View.VISIBLE
        }
        handler.removeCallbacks(hideRunnable)
        handler.postDelayed(hideRunnable, 3000)
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

    override fun onAccessibilityEvent(e: AccessibilityEvent) {
        val pkg = e.packageName?.toString() ?: return
        if (pkg != UsageHelper.INSTAGRAM && pkg != UsageHelper.YOUTUBE) return
        val now = SystemClock.elapsedRealtime()

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
        show(if (type == "reels") "Reels: $n" else "Shorts: $n")
        lastWidget = now
        CounterWidget.update(this)
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        handler.removeCallbacks(hideRunnable)
        overlay?.let { wm?.removeView(it) }
        super.onDestroy()
    }
}
