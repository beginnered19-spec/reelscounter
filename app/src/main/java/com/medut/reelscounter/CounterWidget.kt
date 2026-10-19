package com.medut.reelscounter

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class CounterWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        for (id in ids) manager.updateAppWidget(id, build(context))
    }

    companion object {
        private val layouts = listOf(
            R.layout.widget_counter_0,
            R.layout.widget_counter_1,
            R.layout.widget_counter_2,
            R.layout.widget_counter_3,
            R.layout.widget_counter_4
        )

        fun build(c: Context): RemoteViews {
            val idx = Store.theme(c).coerceIn(0, layouts.lastIndex)
            val v = RemoteViews(c.packageName, layouts[idx])
            v.setTextViewText(R.id.w_reels, "Reels: ${Store.get(c, "reels")}")
            v.setTextViewText(R.id.w_shorts, "Shorts: ${Store.get(c, "shorts")}")
            v.setTextViewText(
                R.id.w_total,
                if (Store.isLocked(c)) "🔒 Kilitli · gece 12'de açılır"
                else "Toplam: ${Store.total(c)} / ${Store.LOCK_AT}"
            )
            if (UsageHelper.hasAccess(c)) {
                v.setTextViewText(
                    R.id.w_ig_time,
                    "Instagram: " + UsageHelper.format(UsageHelper.todayMillis(c, UsageHelper.INSTAGRAM))
                )
                v.setTextViewText(
                    R.id.w_yt_time,
                    "YouTube: " + UsageHelper.format(UsageHelper.todayMillis(c, UsageHelper.YOUTUBE))
                )
            } else {
                v.setTextViewText(R.id.w_ig_time, "Kullanım erişimi izni yok")
                v.setTextViewText(R.id.w_yt_time, "")
            }
            val pi = PendingIntent.getActivity(
                c, 0, Intent(c, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            v.setOnClickPendingIntent(R.id.widget_root, pi)
            return v
        }

        fun update(c: Context) {
            val m = AppWidgetManager.getInstance(c)
            val ids = m.getAppWidgetIds(ComponentName(c, CounterWidget::class.java))
            for (id in ids) m.updateAppWidget(id, build(c))
        }
    }
}
