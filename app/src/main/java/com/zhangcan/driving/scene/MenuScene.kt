package com.zhangcan.driving.scene

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.zhangcan.driving.core.Fake3DRoad
import com.zhangcan.driving.core.Game
import com.zhangcan.driving.core.Ui

/** 主菜单：竖屏、大按钮、顶部标题 */
class MenuScene(nav: Navigator, context: Context, game: Game) : Scene(nav, context, game) {

    private val buttons = listOf(
        Ui.Button("theory", "赛车 · 理论考试", Ui.PANEL_LIGHT, sub = "单选 / 判断 · 每题 25 分"),
        Ui.Button("race", "赛车 · 实操考试", Ui.PANEL_LIGHT, sub = "直线 / 曲线 / 物理模拟 0"),
        Ui.Button("bike", "自行车 · 神秘骑行", Ui.PANEL_LIGHT, sub = "正弦曲线 · 无 ABS"),
        Ui.Button("ach", "🏆 成就", Ui.PANEL_LIGHT),
        Ui.Button("exit", "退出", 0xFF2A1A1C.toInt())
    )

    private val road = Fake3DRoad()
    private var t = 0f
    private var toast: String? = null
    private var toastT = 0f

    override fun onViewport(w: Int, h: Int) {
        super.onViewport(w, h)
        val marginX = w * 0.1f
        var y = h * 0.3f
        val bh = (h * 0.082f).coerceAtMost(Ui.dp(74))
        val gap = h * 0.022f
        for (b in buttons) {
            b.rect.set(marginX, y, w - marginX, y + bh)
            b.textSize = bh * 0.3f
            y += bh + gap
        }
        buttons[1].enabled = game.theoryPassed
        buttons[1].sub = if (game.theoryPassed) "直线 / 曲线 / 物理模拟 0" else "🔒 理论考试 ≥ 60 分解锁"
        buttons[1].label = if (game.theoryPassed) "赛车 · 实操考试" else "赛车 · 实操考试 🔒"
    }

    override fun update(dt: Float) {
        t += dt
        if (toastT > 0f) {
            toastT -= dt
            if (toastT <= 0f) toast = null
        }
        road.update(dt, 26f, kotlin.math.sin(t * 0.45f).toFloat() * 0.5f)
    }

    override fun draw(canvas: Canvas) {
        road.draw(canvas, vw, vh)

        // 顶部压暗，保证标题可读
        Ui.rect(canvas, 0f, 0f, vw.toFloat(), vh * 0.3f, Ui.withAlpha(Ui.BG, 190))

        Ui.text(canvas, "ZhangCan Driving", vw / 2f, vh * 0.13f, vh * 0.045f, Ui.TEXT, Paint.Align.CENTER, true)
        Ui.text(canvas, "神秘驾驶技术模拟器", vw / 2f, vh * 0.175f, vh * 0.021f, Ui.ACCENT, Paint.Align.CENTER, true)
        Ui.text(
            canvas,
            "物理模拟 0% · 碰撞模拟已按偏好关闭",
            vw / 2f, vh * 0.212f, vh * 0.017f, Ui.TEXT_DIM, Paint.Align.CENTER
        )

        for (b in buttons) Ui.drawButton(canvas, b, input.isHeld(b.id))

        val unlocked = game.achievements.unlockedCount()
        Ui.text(
            canvas,
            "成就 $unlocked / ${com.zhangcan.driving.core.AchievementManager.ALL.size}",
            vw / 2f, vh * 0.945f, vh * 0.02f, Ui.TEXT_DIM, Paint.Align.CENTER
        )
        Ui.text(canvas, "纯属娱乐 · 请勿对号入座", vw / 2f, vh * 0.975f, vh * 0.016f, Ui.withAlpha(Ui.TEXT_DIM, 150), Paint.Align.CENTER)

        val msg = toast
        if (msg != null) {
            val r = RectF(vw * 0.08f, vh * 0.255f, vw * 0.92f, vh * 0.295f)
            Ui.rect(canvas, r, Ui.withAlpha(0xFF000000.toInt(), 190), Ui.dp(10))
            Ui.text(canvas, msg, vw / 2f, r.centerY() + vh * 0.007f, vh * 0.019f, Ui.ACCENT, Paint.Align.CENTER, true)
        }
    }

    override fun hitTest(x: Float, y: Float): String? =
        buttons.firstOrNull { hit(it.rect, x, y) }?.id

    override fun onTap(id: String) {
        when (id) {
            "theory" -> nav.go(TheoryScene(nav, context, game))
            "race" -> {
                if (game.theoryPassed) nav.go(RaceScene(nav, context, game))
                else showToast("先去考理论。规则是规则。")
            }
            "bike" -> nav.go(BikeScene(nav, context, game))
            "ach" -> nav.go(AchievementScene(nav, context, game))
            "exit" -> (context as? android.app.Activity)?.finish()
        }
    }

    private fun showToast(msg: String) {
        toast = msg
        toastT = 2.2f
    }
}
