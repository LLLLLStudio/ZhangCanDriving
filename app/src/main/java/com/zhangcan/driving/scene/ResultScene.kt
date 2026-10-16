package com.zhangcan.driving.scene

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.zhangcan.driving.core.Game
import com.zhangcan.driving.core.Ui

/**
 * 结算页：总分 / 扣分明细 / 搞笑评语 / 解锁成就 / 重玩 / 返回。
 */
class ResultScene(
    nav: Navigator,
    context: Context,
    game: Game,
    val result: Game.RunResult,
    val replay: (() -> Scene)? = null
) : Scene(nav, context, game) {

    private val replayBtn = Ui.Button("replay", "重玩一次", Ui.PANEL_LIGHT)
    private val menuBtn = Ui.Button("menu", "返回主菜单", Ui.PANEL_LIGHT)
    private var t = 0f
    private var shown = 0f

    override fun onViewport(w: Int, h: Int) {
        super.onViewport(w, h)
        val marginX = w * 0.1f
        val bh = (h * 0.075f).coerceAtMost(Ui.dp(70))
        menuBtn.rect.set(marginX, h * 0.87f, w - marginX, h * 0.87f + bh)
        replayBtn.rect.set(marginX, h * 0.87f - bh - h * 0.02f, w - marginX, h * 0.87f - h * 0.02f)
        replayBtn.textSize = bh * 0.32f
        menuBtn.textSize = bh * 0.32f
        replayBtn.enabled = replay != null && result.canReplay
    }

    override fun update(dt: Float) {
        t += dt
        shown = (shown + dt * 6f).coerceAtMost(1f)
    }

    override fun draw(canvas: Canvas) {
        Ui.fill(canvas, Ui.BG)

        Ui.text(canvas, result.title, vw / 2f, vh * 0.075f, vh * 0.028f, Ui.TEXT, Paint.Align.CENTER, true)

        val scoreColor = when {
            result.score >= 90 -> Ui.GREEN
            result.score >= 60 -> Ui.ACCENT
            else -> Ui.RED
        }
        Ui.text(canvas, result.score.toString(), vw / 2f, vh * 0.21f, vh * 0.11f, scoreColor, Paint.Align.CENTER, true)
        Ui.text(canvas, "总分", vw / 2f, vh * 0.245f, vh * 0.019f, Ui.TEXT_DIM, Paint.Align.CENTER)

        // 搞笑评语
        val cr = RectF(vw * 0.07f, vh * 0.27f, vw * 0.93f, vh * 0.335f)
        Ui.rect(canvas, cr, Ui.withAlpha(Ui.ACCENT, 34), Ui.dp(12))
        Ui.text(canvas, result.comment, cr.centerX(), cr.centerY() + vh * 0.008f, vh * 0.022f, Ui.ACCENT, Paint.Align.CENTER, true)

        var y = vh * 0.375f
        Ui.text(canvas, "扣分明细", vw * 0.08f, y, vh * 0.021f, Ui.TEXT, Paint.Align.LEFT, true)
        y += vh * 0.032f

        val deductions = result.details.filter { it.delta < 0 }
        val bonuses = result.details.filter { it.delta > 0 }
        val shownList = ArrayList<String>()
        for (e in deductions) shownList.add("${e.reason}   ${e.delta}")
        if (shownList.size > 7) {
            val extra = shownList.size - 7
            while (shownList.size > 7) shownList.removeAt(shownList.size - 1)
            shownList.add("…… 还有 $extra 条，你真的很稳定。")
        }
        if (shownList.isEmpty()) shownList.add("本次没有任何扣分记录（不太可能）")

        val lineH = vh * 0.028f
        for (s in shownList) {
            Ui.text(canvas, "· $s", vw * 0.09f, y, vh * 0.019f, Ui.withAlpha(Ui.RED, 220), Paint.Align.LEFT)
            y += lineH
        }
        y += vh * 0.008f

        for (e in bonuses) {
            Ui.text(canvas, "· ${e.reason}   +${e.delta}", vw * 0.09f, y, vh * 0.019f, Ui.GREEN, Paint.Align.LEFT)
            y += lineH
            if (y > vh * 0.7f) break
        }

        // 新解锁成就
        y += vh * 0.012f
        Ui.text(canvas, "本次解锁成就", vw * 0.08f, y, vh * 0.021f, Ui.TEXT, Paint.Align.LEFT, true)
        y += vh * 0.032f
        if (result.newAchievements.isEmpty()) {
            Ui.text(canvas, "· 无。稳定也是一种天赋。", vw * 0.09f, y, vh * 0.019f, Ui.TEXT_DIM, Paint.Align.LEFT)
        } else {
            for (a in result.newAchievements) {
                Ui.text(canvas, "🏆 ${a.name}", vw * 0.09f, y, vh * 0.021f, Ui.GREEN, Paint.Align.LEFT, true)
                y += vh * 0.027f
                Ui.text(canvas, "　${a.condition}", vw * 0.09f, y, vh * 0.016f, Ui.TEXT_DIM, Paint.Align.LEFT)
                y += vh * 0.026f
            }
        }

        Ui.drawButton(canvas, replayBtn, input.isHeld("replay"))
        Ui.drawButton(canvas, menuBtn, input.isHeld("menu"))
    }

    override fun hitTest(x: Float, y: Float): String? {
        if (replayBtn.enabled && hit(replayBtn.rect, x, y)) return "replay"
        if (hit(menuBtn.rect, x, y)) return "menu"
        return null
    }

    override fun onTap(id: String) {
        when (id) {
            "replay" -> replay?.invoke()?.let { nav.go(it) }
            "menu" -> nav.go(MenuScene(nav, context, game))
        }
    }

    override fun onBack(): Boolean {
        nav.go(MenuScene(nav, context, game))
        return true
    }
}
