package com.zhangcan.driving.scene

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.zhangcan.driving.core.AchievementManager
import com.zhangcan.driving.core.Game
import com.zhangcan.driving.core.Ui

/** 成就列表 */
class AchievementScene(nav: Navigator, context: Context, game: Game) : Scene(nav, context, game) {

    private val backRect = RectF()
    private var t = 0f

    override fun onViewport(w: Int, h: Int) {
        super.onViewport(w, h)
        backRect.set(Ui.dp(14), Ui.dp(14), Ui.dp(14) + Ui.dp(84), Ui.dp(14) + Ui.dp(40))
    }

    override fun update(dt: Float) {
        t += dt
    }

    override fun draw(canvas: Canvas) {
        Ui.fill(canvas, Ui.BG)
        Ui.rect(canvas, 0f, 0f, vw.toFloat(), vh * 0.1f, Ui.PANEL)
        Ui.drawBack(canvas, backRect)
        Ui.text(
            canvas,
            "成就  ${game.achievements.unlockedCount()} / ${AchievementManager.ALL.size}",
            vw / 2f, vh * 0.062f, vh * 0.026f, Ui.TEXT, Paint.Align.CENTER, true
        )

        val marginX = vw * 0.07f
        val top = vh * 0.12f
        val rowH = vh * 0.082f
        val gap = vh * 0.012f
        var y = top
        for (a in AchievementManager.ALL) {
            val unlocked = game.achievements.isUnlocked(a.id)
            val r = RectF(marginX, y, vw - marginX, y + rowH)
            Ui.rect(canvas, r, if (unlocked) Ui.withAlpha(Ui.GREEN, 40) else Ui.withAlpha(0xFFFFFFFF.toInt(), 14), Ui.dp(12))
            val icon = if (unlocked) "🏆" else "🔒"
            Ui.text(canvas, icon, r.left + vw * 0.07f, r.top + rowH * 0.42f, rowH * 0.36f, Ui.TEXT, Paint.Align.CENTER)
            Ui.text(
                canvas, a.name, r.left + vw * 0.13f, r.top + rowH * 0.4f, rowH * 0.27f,
                if (unlocked) Ui.GREEN else Ui.TEXT_DIM, Paint.Align.LEFT, true
            )
            Ui.text(canvas, a.condition, r.left + vw * 0.13f, r.top + rowH * 0.75f, rowH * 0.2f, Ui.TEXT_DIM, Paint.Align.LEFT)
            y += rowH + gap
        }

        Ui.text(
            canvas,
            "成就保存在本机，卸载即失忆",
            vw / 2f, vh * 0.97f, vh * 0.017f, Ui.withAlpha(Ui.TEXT_DIM, 160), Paint.Align.CENTER
        )
    }

    override fun hitTest(x: Float, y: Float): String? = if (hit(backRect, x, y)) "back" else null

    override fun onTap(id: String) {
        if (id == "back") nav.go(MenuScene(nav, context, game))
    }

    override fun onBack(): Boolean {
        nav.go(MenuScene(nav, context, game))
        return true
    }
}
