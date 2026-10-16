package com.zhangcan.driving.core

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path

/**
 * 伪 3D 道路。
 *
 * 不引入任何 3D 引擎，只用 Canvas 画梯形 + 透视缩放：
 *   - 深度 d（1 = 贴近镜头，越大越远），屏幕 y = horizon + (h - horizon) / d
 *   - 路面半宽 = k / d，天然形成近大远小
 *   - 中心虚线与路侧标杆按 speed 滚动，制造速度感
 */
class Fake3DRoad {

    /** -1..1，道路在远端的横向偏移（转向时整体弯过去） */
    var curve = 0f

    /** 已行驶的“世界距离”，用于滚动 */
    var scroll = 0f

    var speed = 0f

    private val path = Path()
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    fun update(dt: Float, speed: Float, steer: Float) {
        this.speed = speed
        scroll += speed * dt
        if (scroll > 100000f) scroll -= 100000f
        val target = steer.coerceIn(-1f, 1f) * 0.92f
        curve += (target - curve) * (dt * 2.2f).coerceAtMost(1f)
    }

    fun horizonY(h: Int): Float = h * 0.33f

    fun depthToY(d: Float, h: Int): Float {
        val hz = horizonY(h)
        return hz + (h - hz) / d.coerceAtLeast(0.15f)
    }

    fun halfWidthAt(d: Float, w: Int): Float = w * 0.62f / d.coerceAtLeast(0.15f)

    fun centerXAt(d: Float, w: Int): Float =
        w / 2f + curve * w * 0.6f * (1f - 1f / d.coerceAtLeast(0.15f))

    /** 把 (深度, 路面内相对横坐标 -1..1) 投影到屏幕 */
    fun projectX(d: Float, lateral: Float, w: Int): Float =
        centerXAt(d, w) + lateral * halfWidthAt(d, w)

    fun projectY(d: Float, h: Int): Float = depthToY(d, h)

    fun draw(canvas: Canvas, w: Int, h: Int) {
        val hz = horizonY(h)

        // 天空 + 地面
        Ui.fill(canvas, Ui.SKY)
        Ui.rect(canvas, 0f, hz, w.toFloat(), h.toFloat(), Ui.GRASS)

        // 路面（远端窄、近端宽 → 梯形）
        val farD = 26f
        val nearD = 1f
        path.reset()
        path.moveTo(centerXAt(farD, w) - halfWidthAt(farD, w), depthToY(farD, h))
        path.lineTo(centerXAt(farD, w) + halfWidthAt(farD, w), depthToY(farD, h))
        path.lineTo(centerXAt(nearD, w) + halfWidthAt(nearD, w), h.toFloat())
        path.lineTo(centerXAt(nearD, w) - halfWidthAt(nearD, w), h.toFloat())
        path.close()
        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.FILL
        p.color = Ui.ROAD
        canvas.drawPath(path, p)

        // 路缘白线
        edgeLine(canvas, w, h, -1f)
        edgeLine(canvas, w, h, 1f)

        // 滚动中心虚线
        drawDashes(canvas, w, h, farD)

        // 路侧标杆：速度感来源
        drawPosts(canvas, w, h, farD)
    }

    private fun edgeLine(canvas: Canvas, w: Int, h: Int, side: Float) {
        val inset = 0.965f
        val farD = 26f
        val nearD = 1f
        path.reset()
        path.moveTo(centerXAt(farD, w) + side * halfWidthAt(farD, w) * inset, depthToY(farD, h))
        path.lineTo(centerXAt(farD, w) + side * halfWidthAt(farD, w), depthToY(farD, h))
        path.lineTo(centerXAt(nearD, w) + side * halfWidthAt(nearD, w), h.toFloat())
        path.lineTo(centerXAt(nearD, w) + side * halfWidthAt(nearD, w) * inset, h.toFloat())
        path.close()
        p.reset()
        p.isAntiAlias = true
        p.color = Ui.withAlpha(Ui.ROAD_LINE, 200)
        canvas.drawPath(path, p)
    }

    private fun drawDashes(canvas: Canvas, w: Int, h: Int, farD: Float) {
        val seg = 4f
        val dashLen = 2f
        val phase = scroll % seg
        var k = 0
        while (true) {
            val d1 = 1f + (k * seg - phase)
            if (d1 > farD) break
            val d2 = (d1 + dashLen).coerceAtMost(farD)
            if (d2 > 1.02f) {
                val y1 = depthToY(d1.coerceAtLeast(1f), h)
                val y2 = depthToY(d2, h)
                val c1 = centerXAt(d1.coerceAtLeast(1f), w)
                val c2 = centerXAt(d2, w)
                val hw1 = halfWidthAt(d1.coerceAtLeast(1f), w) * 0.028f
                val hw2 = halfWidthAt(d2, w) * 0.028f
                path.reset()
                path.moveTo(c1 - hw1, y1)
                path.lineTo(c1 + hw1, y1)
                path.lineTo(c2 + hw2, y2)
                path.lineTo(c2 - hw2, y2)
                path.close()
                p.reset()
                p.isAntiAlias = true
                p.color = Ui.withAlpha(Color.WHITE, (230 * (1f - d2 / farD)).toInt().coerceIn(20, 230))
                canvas.drawPath(path, p)
            }
            k++
        }
    }

    private fun drawPosts(canvas: Canvas, w: Int, h: Int, farD: Float) {
        val seg = 8f
        val phase = scroll % seg
        var k = 0
        while (true) {
            val d = 1f + (k * seg - phase)
            if (d > farD) break
            if (d > 1.05f) {
                val y = depthToY(d, h)
                val hw = halfWidthAt(d, w)
                val cx = centerXAt(d, w)
                val size = (hw * 0.06f).coerceAtLeast(1.2f)
                val alpha = (200 * (1f - d / farD)).toInt().coerceIn(24, 200)
                val col = Ui.withAlpha(Ui.ACCENT, alpha)
                Ui.rect(canvas, cx - hw - size * 2f, y - size * 5f, cx - hw - size, y, col)
                Ui.rect(canvas, cx + hw + size, y - size * 5f, cx + hw + size * 2f, y, col)
            }
            k++
        }
    }
}
