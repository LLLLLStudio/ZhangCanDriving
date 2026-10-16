package com.zhangcan.driving.core

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface

/**
 * 统一绘制工具。
 * 整个项目只有一个 Paint 实例反复复用，保证 60FPS 下不产生垃圾对象。
 */
object Ui {

    // 主题色
    const val BG = 0xFF10131A.toInt()
    const val SKY = 0xFF16202C.toInt()
    const val GRASS = 0xFF1E2A22.toInt()
    const val ROAD = 0xFF2E3440.toInt()
    const val ROAD_LINE = 0xFFE8E8E8.toInt()
    const val PANEL = 0xFF1B2130.toInt()
    const val PANEL_LIGHT = 0xFF232B3D.toInt()
    const val ACCENT = 0xFFFFC107.toInt()
    const val TEXT = 0xFFECECEC.toInt()
    const val TEXT_DIM = 0xFF9AA4B2.toInt()
    const val GREEN = 0xFF4CAF50.toInt()
    const val RED = 0xFFE53935.toInt()
    const val BLUE = 0xFF42A5F5.toInt()

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val boldTypeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

    fun dp(v: Float): Float = v * Resources.getSystem().displayMetrics.density

    fun dp(v: Int): Float = dp(v.toFloat())

    fun fill(canvas: Canvas, color: Int) {
        paint.reset()
        paint.style = Paint.Style.FILL
        paint.color = color
        canvas.drawPaint(paint)
    }

    fun rect(canvas: Canvas, r: RectF, color: Int, radius: Float = 0f) {
        paint.reset()
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
        paint.color = color
        if (radius > 0f) canvas.drawRoundRect(r, radius, radius, paint) else canvas.drawRect(r, paint)
    }

    fun rect(canvas: Canvas, l: Float, t: Float, rr: Float, b: Float, color: Int, radius: Float = 0f) {
        tmpRect.set(l, t, rr, b)
        rect(canvas, tmpRect, color, radius)
    }

    private val tmpRect = RectF()

    fun stroke(canvas: Canvas, r: RectF, color: Int, width: Float, radius: Float = 0f) {
        paint.reset()
        paint.isAntiAlias = true
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = width
        paint.color = color
        if (radius > 0f) canvas.drawRoundRect(r, radius, radius, paint) else canvas.drawRect(r, paint)
    }

    fun text(
        canvas: Canvas,
        s: String,
        x: Float,
        y: Float,
        size: Float,
        color: Int = TEXT,
        align: Paint.Align = Paint.Align.LEFT,
        isBold: Boolean = false
    ) {
        paint.reset()
        paint.isAntiAlias = true
        paint.color = color
        paint.textSize = size
        paint.textAlign = align
        paint.typeface = if (isBold) boldTypeface else Typeface.DEFAULT
        canvas.drawText(s, x, y, paint)
    }

    fun textWidth(s: String, size: Float, isBold: Boolean = false): Float {
        paint.reset()
        paint.isAntiAlias = true
        paint.textSize = size
        paint.typeface = if (isBold) boldTypeface else Typeface.DEFAULT
        return paint.measureText(s)
    }

    /** 中文没有空格，按字符宽度硬换行 */
    fun wrap(s: String, size: Float, maxWidth: Float, isBold: Boolean = false): List<String> {
        val out = ArrayList<String>()
        val sb = StringBuilder()
        for (ch in s) {
            if (ch == '\n') {
                out.add(sb.toString())
                sb.setLength(0)
                continue
            }
            sb.append(ch)
            if (textWidth(sb.toString(), size, isBold) > maxWidth && sb.length > 1) {
                sb.setLength(sb.length - 1)
                out.add(sb.toString())
                sb.setLength(0)
                sb.append(ch)
            }
        }
        if (sb.isNotEmpty()) out.add(sb.toString())
        return out
    }

    fun blend(c1: Int, c2: Int, t: Float): Int {
        val r = (Color.red(c1) * (1 - t) + Color.red(c2) * t).toInt()
        val g = (Color.green(c1) * (1 - t) + Color.green(c2) * t).toInt()
        val b = (Color.blue(c1) * (1 - t) + Color.blue(c2) * t).toInt()
        return Color.argb(255, r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
    }

    fun withAlpha(color: Int, alpha: Int): Int = (color and 0x00FFFFFF) or (alpha.coerceIn(0, 255) shl 24)

    /** 通用按钮：场景负责布局（rect），这里只负责画 */
    class Button(
        val id: String,
        var label: String,
        var fill: Int = PANEL_LIGHT,
        var textColor: Int = TEXT,
        var sub: String? = null,
        var enabled: Boolean = true
    ) {
        val rect = RectF()
        var textSize = 0f
    }

    fun drawButton(canvas: Canvas, b: Button, pressed: Boolean) {
        val r = b.rect
        val base = when {
            !b.enabled -> 0xFF171C26.toInt()
            pressed -> blend(b.fill, Color.WHITE, 0.2f)
            else -> b.fill
        }
        rect(canvas, r, base, dp(14))
        stroke(canvas, r, withAlpha(Color.WHITE, if (b.enabled) 28 else 12), dp(1), dp(14))
        val size = if (b.textSize > 0f) b.textSize else (r.height() * 0.3f).coerceIn(dp(15), dp(30))
        val cy = r.centerY()
        val color = if (b.enabled) b.textColor else TEXT_DIM
        if (b.sub != null) {
            text(canvas, b.label, r.centerX(), cy + size * 0.02f, size, color, Paint.Align.CENTER, true)
            text(canvas, b.sub!!, r.centerX(), cy + size * 0.92f, size * 0.52f, TEXT_DIM, Paint.Align.CENTER)
        } else {
            text(canvas, b.label, r.centerX(), cy + size * 0.36f, size, color, Paint.Align.CENTER, true)
        }
    }

    /** 左上角小返回按钮的通用绘制 */
    fun drawBack(canvas: Canvas, r: RectF, label: String = "← 返回") {
        rect(canvas, r, withAlpha(Color.WHITE, 20), dp(10))
        text(canvas, label, r.centerX(), r.centerY() + dp(6), dp(15), TEXT, Paint.Align.CENTER, true)
    }
}
