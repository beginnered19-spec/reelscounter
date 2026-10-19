package com.medut.reelscounter

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import java.util.Calendar

object UsageHelper {
    const val INSTAGRAM = "com.instagram.android"
    const val YOUTUBE = "com.google.android.youtube"

    fun hasAccess(c: Context): Boolean {
        val ops = c.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = ops.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), c.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun todayMillis(c: Context, pkg: String): Long {
        val usm = c.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val end = System.currentTimeMillis()
        val events = usm.queryEvents(cal.timeInMillis, end)
        val e = UsageEvents.Event()
        var total = 0L
        var resumedAt = -1L
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            if (e.packageName != pkg) continue
            when (e.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    if (resumedAt < 0) resumedAt = e.timeStamp
                }
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED -> {
                    if (resumedAt >= 0) {
                        total += e.timeStamp - resumedAt
                        resumedAt = -1
                    }
                }
            }
        }
        if (resumedAt >= 0) total += end - resumedAt
        return total
    }

    fun format(ms: Long): String {
        val m = ms / 60000
        return if (m >= 60) "${m / 60} sa ${m % 60} dk" else "$m dk"
    }
}
