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
        fun build(c: Context): RemoteViews {
            val v = RemoteViews(c.packageName, R.layout.widget_counter)
            v.setTextViewText(R.id.w_reels, "Reels: ${Store.get(c, "reels")}")
            v.setTextViewText(R.id.w_shorts, "Shorts: ${Store.get(c, "shorts")}")
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
