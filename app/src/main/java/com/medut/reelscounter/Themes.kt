package com.medut.reelscounter

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable

data class Theme(val name: String, val bg: Int, val accent: Int)

object Themes {
    val all = listOf(
        Theme("Neon Camgöbeği", 0xFF0B1016.toInt(), 0xFF00E5FF.toInt()),
        Theme("Mor Gece", 0xFF0F0B18.toInt(), 0xFFB388FF.toInt()),
        Theme("Gül Neon", 0xFF150B11.toInt(), 0xFFFF4081.toInt()),
        Theme("Zümrüt", 0xFF0A130E.toInt(), 0xFF00E676.toInt()),
        Theme("Kehribar", 0xFF141008.toInt(), 0xFFFFB300.toInt())
    )

    fun current(c: Context): Theme = all[Store.theme(c).coerceIn(0, all.lastIndex)]

    fun withAlpha(color: Int, a: Int): Int = (color and 0x00FFFFFF) or (a shl 24)

    private fun rect(color: Int, radius: Float, stroke: Int = 0, strokeColor: Int = 0): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(color)
        g.cornerRadius = radius
        if (stroke > 0) g.setStroke(stroke, strokeColor)
        return g
    }

    /** Dıştan içe yumuşayan parlama (glow) efektli yuvarlak köşeli arka plan. */
    fun glowBg(bg: Int, accent: Int, radius: Float, stroke: Int, inset: Int): Drawable {
        val outer = rect(withAlpha(accent, 0x22), radius + inset * 2)
        val mid = rect(withAlpha(accent, 0x40), radius + inset)
        val inner = rect(bg, radius, stroke, accent)
        val ld = LayerDrawable(arrayOf<Drawable>(outer, mid, inner))
        ld.setLayerInset(1, inset, inset, inset, inset)
        ld.setLayerInset(2, inset * 2, inset * 2, inset * 2, inset * 2)
        return ld
    }

    fun swatch(accent: Int, selected: Boolean, inset: Int, stroke: Int): Drawable {
        val outer = GradientDrawable()
        outer.shape = GradientDrawable.OVAL
        outer.setColor(withAlpha(accent, 0x44))
        val inner = GradientDrawable()
        inner.shape = GradientDrawable.OVAL
        inner.setColor(accent)
        if (selected) inner.setStroke(stroke, 0xFFFFFFFF.toInt())
        val ld = LayerDrawable(arrayOf<Drawable>(outer, inner))
        ld.setLayerInset(1, inset, inset, inset, inset)
        return ld
    }
}
