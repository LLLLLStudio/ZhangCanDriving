package com.zhangcan.driving.scene

import kotlin.random.Random

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.zhangcan.driving.core.AchievementManager
import com.zhangcan.driving.core.Fake3DRoad
import com.zhangcan.driving.core.Game
import com.zhangcan.driving.core.ScoreManager
import com.zhangcan.driving.core.Ui
import com.zhangcan.driving.data.FunnyComments
import kotlin.math.abs
import kotlin.math.sin

/**
 * 赛车：实操考试。
 *
 * 荒诞规则实现：
 *  1. 直线行驶：每 2 秒检测方向盘，必须出现左打 > 90° 且 右打 > 90°，否则扣 20
 *  2. 曲线行驶：刹车禁用，按下扣 50；只能用手刹转向，拉手刹横向速度 ×2、后轮抓地力归零
 *  3. 物理模拟 0：不做碰撞检测，车直接穿模，角落常驻状态文案
 *  4. 严禁手动换挡：无换挡 UI，触发隐藏换挡键扣 100
 *  5. 永远第三人称：视角固定，尝试切换 3 次解锁成就
 */
class RaceScene(nav: Navigator, context: Context, game: Game) : Scene(nav, context, game) {

    private enum class Phase { INTRO, STRAIGHT, CURVE, DONE }

    private class Cone(var z: Float, val lateral: Float)

    private class Popup(val text: String, val color: Int, var life: Float)

    private val score = ScoreManager()
    private val road = Fake3DRoad()
    private val newAchievements = ArrayList<AchievementManager.Achievement>()
    private val popups = ArrayList<Popup>()
    private val cones = ArrayList<Cone>()

    private val leftBtn = Ui.Button("left", "◀ 左打", Ui.PANEL_LIGHT)
    private val rightBtn = Ui.Button("right", "右打 ▶", Ui.PANEL_LIGHT)
    private val handbrakeBtn = Ui.Button("handbrake", "手刹", 0xFF4A3A12.toInt())
    private val brakeBtn = Ui.Button("brake", "刹车", Ui.PANEL_LIGHT)
    private val speedRect = RectF()
    private val viewRect = RectF()

    private var phase = Phase.INTRO
    private var phaseT = 0f
    private var totalT = 0f

    private var steerDeg = 0f
    private var lateral = 0f
    private var speed = 0.22f

    // 规则 1 的 2 秒检测窗口
    private var windowT = 0f
    private var minSteer = 0f
    private var maxSteer = 0f
    private var windowIndex = 0

    // 规则 2
    private var curveBrakePresses = 0
    private var curveHandbrakeTime = 0f

    // 规则 4 / 5
    private var gearPressed = false
    private var viewAttempts = 0

    private var coneTimer = 0f
    private var flashT = 0f
    private var flashColor = Ui.RED

    override fun onViewport(w: Int, h: Int) {
        super.onViewport(w, h)
        val bh = (h * 0.1f).coerceAtMost(Ui.dp(88))
        val by = h * 0.74f
        leftBtn.rect.set(w * 0.045f, by, w * 0.30f, by + bh)
        rightBtn.rect.set(w * 0.70f, by, w * 0.955f, by + bh)
        handbrakeBtn.rect.set(w * 0.34f, by + bh * 0.62f, w * 0.66f, by + bh * 1.62f)
        brakeBtn.rect.set(w * 0.34f, by - bh * 0.42f, w * 0.66f, by + bh * 0.58f)
        for (b in listOf(leftBtn, rightBtn, handbrakeBtn, brakeBtn)) b.textSize = bh * 0.26f

        speedRect.set(w * 0.38f, h * 0.055f, w * 0.62f, h * 0.115f)
        viewRect.set(w * 0.03f, h * 0.055f, w * 0.36f, h * 0.115f)
    }

    override fun onEnter() {
        super.onEnter()
        pushPopup("考试开始：请像 ZhangCan 一样驾驶。", Ui.ACCENT)
    }

    override fun update(dt: Float) {
        totalT += dt
        updatePopups(dt)
        if (flashT > 0f) flashT -= dt

        when (phase) {
            Phase.INTRO -> {
                phaseT += dt
                road.update(dt, 18f, 0f)
                if (phaseT > 2.4f) {
                    phase = Phase.STRAIGHT
                    phaseT = 0f
                    resetWindow()
                    pushPopup("第一项：直线行驶。保持“你的风格”。", Ui.ACCENT)
                }
            }
            Phase.STRAIGHT -> updateDriving(dt, handbrakeAllowed = true)
            Phase.CURVE -> updateDriving(dt, handbrakeAllowed = true)
            Phase.DONE -> road.update(dt, 8f, 0f)
        }
    }

    private fun updateDriving(dt: Float, handbrakeAllowed: Boolean) {
        phaseT += dt
        val left = input.isHeld("left")
        val right = input.isHeld("right")
        val handbrake = input.isHeld("handbrake")

        // 方向盘角度：目标 ±540°，转动 560°/s，回正 380°/s
        val target = when {
            right && !left -> 540f
            left && !right -> -540f
            else -> 0f
        }
        val rate = if (target == 0f) 380f else 560f
        steerDeg = moveToward(steerDeg, target, rate * dt)

        speed = (speed + dt * 0.11f).coerceAtMost(1f)

        // 曲线段：后轮抓地力归零，只有手刹能让车横过来，且横向速度 ×2
        val sliding = handbrake
        val grip = if (phase == Phase.CURVE) 0f else 1f
        val steerFactor = when {
            phase == Phase.CURVE -> if (sliding) 1f else 0.12f
            else -> 1f
        }
        val lateralSpeed = (steerDeg / 540f) * 0.5f * speed * steerFactor * (if (sliding) 2f else 1f)
        lateral = (lateral + lateralSpeed * dt).coerceIn(-1.25f, 1.25f)
        // 没有转向输入时缓慢回到路中（物理模拟：无）
        if (abs(steerDeg) < 1f) lateral *= (1f - dt * 0.6f * grip)

        road.update(dt, speed * 70f, steerDeg / 540f)

        if (phase == Phase.STRAIGHT) {
            windowT += dt
            minSteer = minOf(minSteer, steerDeg)
            maxSteer = maxOf(maxSteer, steerDeg)
            if (windowT >= 2f) {
                windowIndex++
                if (minSteer > -90f || maxSteer < 90f) {
                    score.adjust(-20, "直线行驶 第${windowIndex}次检测：太稳了，这不像你")
                    pushPopup("太稳了，这不像你。扣 20。", Ui.RED)
                } else {
                    pushPopup("检测通过：方向确实很随意。", Ui.GREEN)
                }
                resetWindow()
            }
            if (phaseT > STRAIGHT_SECONDS) {
                phase = Phase.CURVE
                phaseT = 0f
                pushPopup("下一项：曲线行驶。禁止刹车。ZhangCan 只用手刹。", Ui.ACCENT)
            }
        } else {
            if (sliding) curveHandbrakeTime += dt
            coneTimer -= dt
            if (coneTimer <= 0f) {
                coneTimer = 1.5f
                val lanes = floatArrayOf(-0.7f, 0f, 0.7f)
                cones.add(Cone(7f + Random.nextFloat() * 2f, lanes[Random.nextInt(lanes.size)]))
            }
            if (phaseT > CURVE_SECONDS) {
                phase = Phase.DONE
                finish()
            }
        }

        // 障碍物靠近（不做碰撞检测，直接穿模）
        val it = cones.iterator()
        while (it.hasNext()) {
            val c = it.next()
            c.z -= speed * 7f * dt
            if (c.z <= 0.15f) {
                it.remove()
                pushPopup("穿模！碰撞模拟已按偏好关闭。", Ui.BLUE)
            }
        }
    }

    private fun resetWindow() {
        windowT = 0f
        minSteer = 0f
        maxSteer = 0f
    }

    private fun moveToward(v: Float, target: Float, step: Float): Float {
        if (v < target) return (v + step).coerceAtMost(target)
        if (v > target) return (v - step).coerceAtLeast(target)
        return v
    }

    private fun pushPopup(text: String, color: Int) {
        popups.add(Popup(text, color, 2.4f))
        if (popups.size > 4) popups.removeAt(0)
    }

    private fun updatePopups(dt: Float) {
        val it = popups.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.life -= dt
            if (p.life <= 0f) it.remove()
        }
    }

    private fun unlock(id: String) {
        if (game.achievements.unlock(id)) newAchievements.add(AchievementManager.of(id))
    }

    private fun finish() {
        if (!gearPressed) unlock(AchievementManager.A2)
        if (curveBrakePresses == 0 && curveHandbrakeTime > 5f) unlock(AchievementManager.A3)
        val comment = when {
            score.score < 0 -> "你的驾驶技术已经超越了物理引擎。"
            curveBrakePresses > 0 -> "刹车片：你已经三个月没碰我了。"
            else -> FunnyComments.pick(FunnyComments.RACE)
        }
        val result = Game.RunResult(
            title = "赛车 · 实操考试",
            score = score.score,
            details = score.details,
            newAchievements = newAchievements,
            comment = comment
        )
        nav.go(ResultScene(nav, context, game, result) { RaceScene(nav, context, game) })
    }

    override fun draw(canvas: Canvas) {
        road.draw(canvas, vw, vh)
        drawCones(canvas)
        drawCar(canvas)
        drawHud(canvas)
        drawControls(canvas)
        drawPopups(canvas)

        if (flashT > 0f) {
            Ui.rect(canvas, 0f, 0f, vw.toFloat(), vh.toFloat(), Ui.withAlpha(flashColor, (70 * (flashT / 0.35f)).toInt().coerceIn(0, 70)))
        }

        if (phase == Phase.INTRO) drawIntro(canvas)
    }

    private fun drawCones(canvas: Canvas) {
        for (c in cones) {
            val d = 1f + c.z
            val y = road.projectY(d, vh)
            val x = road.projectX(d, c.lateral, vw)
            val size = vw * 0.12f / d
            Ui.text(canvas, "🚧", x, y, size, Ui.TEXT, Paint.Align.CENTER)
        }
    }

    private fun drawCar(canvas: Canvas) {
        val cx = vw / 2f + lateral * vw * 0.18f
        val cy = vh * 0.80f
        val size = vh * 0.062f
        canvas.save()
        canvas.rotate(steerDeg / 540f * 10f, cx, cy)
        Ui.text(canvas, "🚗", cx, cy, size, Ui.TEXT, Paint.Align.CENTER)
        // 车辆阴影块（色块占位，符合伪 3D 约定）
        Ui.rect(canvas, cx - size * 0.5f, cy + size * 0.08f, cx + size * 0.5f, cy + size * 0.2f, Ui.withAlpha(0x00000000, 90), Ui.dp(4))
        canvas.restore()
    }

    private fun drawHud(canvas: Canvas) {
        // 左上：视角（规则 5）
        Ui.rect(canvas, viewRect, Ui.withAlpha(0xFFFFFFFF.toInt(), 18), Ui.dp(10))
        Ui.text(canvas, "视角：第三人称", viewRect.left + vw * 0.015f, viewRect.top + vh * 0.022f, vh * 0.019f, Ui.TEXT, Paint.Align.LEFT, true)
        Ui.text(
            canvas, "尝试切换 $viewAttempts 次", viewRect.left + vw * 0.015f, viewRect.top + vh * 0.048f,
            vh * 0.015f, Ui.TEXT_DIM, Paint.Align.LEFT
        )

        // 右上：速度（隐藏换挡键就在这）
        Ui.rect(canvas, speedRect, Ui.withAlpha(0xFFFFFFFF.toInt(), 18), Ui.dp(10))
        Ui.text(canvas, "${(speed * 180).toInt()} km/h", speedRect.centerX(), speedRect.top + vh * 0.028f, vh * 0.024f, Ui.ACCENT, Paint.Align.CENTER, true)
        Ui.text(canvas, "挡位：不可用", speedRect.centerX(), speedRect.top + vh * 0.055f, vh * 0.014f, Ui.TEXT_DIM, Paint.Align.CENTER)

        // 方向盘角度
        val deg = steerDeg.toInt()
        Ui.text(
            canvas, "方向盘：%d°".format(deg), vw / 2f, vh * 0.148f, vh * 0.02f,
            if (abs(steerDeg) > 90f) Ui.GREEN else Ui.TEXT_DIM, Paint.Align.CENTER, true
        )

        // 项目提示
        val phaseName = when (phase) {
            Phase.INTRO -> "准备中"
            Phase.STRAIGHT -> "项目：直线行驶（每 2 秒检测方向盘）"
            Phase.CURVE -> "项目：曲线行驶（禁止刹车 / 只用手刹）"
            Phase.DONE -> "结算中"
        }
        Ui.text(canvas, phaseName, vw / 2f, vh * 0.19f, vh * 0.018f, Ui.TEXT, Paint.Align.CENTER)
        if (phase == Phase.CURVE) {
            Ui.text(
                canvas, "后轮抓地力：0%　横向速度：×2（手刹中）", vw / 2f, vh * 0.217f, vh * 0.016f,
                if (input.isHeld("handbrake")) Ui.ACCENT else Ui.TEXT_DIM, Paint.Align.CENTER
            )
        }

        // 规则 3：角落常驻
        Ui.text(canvas, "物理模拟：0%", vw * 0.035f, vh * 0.965f, vh * 0.017f, Ui.withAlpha(Ui.TEXT_DIM, 200), Paint.Align.LEFT, true)
        Ui.text(canvas, "碰撞模拟：已按偏好关闭", vw * 0.035f, vh * 0.988f, vh * 0.017f, Ui.withAlpha(Ui.TEXT_DIM, 200), Paint.Align.LEFT)

        Ui.text(canvas, "当前分数：${score.score}", vw - vw * 0.035f, vh * 0.965f, vh * 0.02f, Ui.ACCENT, Paint.Align.RIGHT, true)
    }

    private fun drawControls(canvas: Canvas) {
        leftBtn.fill = if (input.isHeld("left")) Ui.withAlpha(Ui.BLUE, 120) else Ui.PANEL_LIGHT
        rightBtn.fill = if (input.isHeld("right")) Ui.withAlpha(Ui.BLUE, 120) else Ui.PANEL_LIGHT
        handbrakeBtn.fill = if (input.isHeld("handbrake")) Ui.withAlpha(Ui.ACCENT, 130) else 0xFF4A3A12.toInt()

        when (phase) {
            Phase.STRAIGHT -> {
                brakeBtn.enabled = false
                brakeBtn.label = "刹车（默认禁用）"
                brakeBtn.fill = Ui.PANEL
            }
            Phase.CURVE -> {
                brakeBtn.enabled = true
                brakeBtn.label = "刹车（禁止）"
                brakeBtn.fill = Ui.withAlpha(Ui.RED, 120)
            }
            else -> {
                brakeBtn.enabled = false
                brakeBtn.label = "刹车"
                brakeBtn.fill = Ui.PANEL
            }
        }

        Ui.drawButton(canvas, leftBtn, input.isHeld("left"))
        Ui.drawButton(canvas, rightBtn, input.isHeld("right"))
        Ui.drawButton(canvas, handbrakeBtn, input.isHeld("handbrake"))
        Ui.drawButton(canvas, brakeBtn, input.isHeld("brake"))
    }

    private fun drawPopups(canvas: Canvas) {
        var y = vh * 0.34f
        for (p in popups) {
            val alpha = (255 * (p.life / 2.4f).coerceIn(0f, 1f)).toInt()
            Ui.rect(
                canvas, vw * 0.08f, y - vh * 0.026f, vw * 0.92f, y + vh * 0.012f,
                Ui.withAlpha(0xFF000000.toInt(), (alpha * 0.55f).toInt().coerceIn(0, 200)), Ui.dp(8)
            )
            Ui.text(canvas, p.text, vw / 2f, y, vh * 0.021f, Ui.withAlpha(p.color, alpha), Paint.Align.CENTER, true)
            y += vh * 0.046f
        }
    }

    private fun drawIntro(canvas: Canvas) {
        Ui.rect(canvas, 0f, 0f, vw.toFloat(), vh.toFloat(), Ui.withAlpha(Ui.BG, 210))
        Ui.text(canvas, "赛车 · 实操考试", vw / 2f, vh * 0.32f, vh * 0.038f, Ui.TEXT, Paint.Align.CENTER, true)
        val lines = listOf(
            "1. 直线行驶：每 2 秒检测，需左右各打 90°",
            "2. 曲线行驶：禁止刹车，只用手刹",
            "3. 物理模拟 0%，碰撞直接穿模",
            "4. 无换挡 UI（真的没有）",
            "5. 视角固定第三人称"
        )
        var y = vh * 0.4f
        for (l in lines) {
            Ui.text(canvas, l, vw / 2f, y, vh * 0.021f, Ui.TEXT_DIM, Paint.Align.CENTER)
            y += vh * 0.038f
        }
        Ui.text(canvas, "开始……", vw / 2f, vh * 0.7f, vh * 0.026f, Ui.ACCENT, Paint.Align.CENTER, true)
    }

    override fun hitTest(x: Float, y: Float): String? {
        if (hit(viewRect, x, y)) return "view"
        if (hit(speedRect, x, y)) return "gear"
        if (hit(leftBtn.rect, x, y)) return "left"
        if (hit(rightBtn.rect, x, y)) return "right"
        if (hit(handbrakeBtn.rect, x, y)) return "handbrake"
        if (hit(brakeBtn.rect, x, y)) return "brake"
        return null
    }

    override fun onTap(id: String) {
        when (id) {
            // 规则 5：视角切换尝试
            "view" -> {
                viewAttempts++
                val msg = if (viewAttempts >= 3) {
                    "视角原教旨主义者，达成。" 
                } else {
                    "视角已锁定：永远第三人称（第 $viewAttempts 次尝试）"
                }
                pushPopup(msg, Ui.ACCENT)
                if (viewAttempts >= 3) unlock(AchievementManager.A8)
            }
            // 规则 4：隐藏换挡键
            "gear" -> {
                if (!gearPressed) {
                    gearPressed = true
                    score.adjust(-100, "触发了隐藏换挡键")
                    flashT = 0.35f
                    flashColor = Ui.RED
                }
                pushPopup("换挡是什么？你不需要知道。扣 100。", Ui.RED)
            }
            // 规则 2：曲线行驶禁止刹车
            "brake" -> {
                when (phase) {
                    Phase.CURVE -> {
                        curveBrakePresses++
                        score.adjust(-50, "曲线行驶第${curveBrakePresses}次踩刹车")
                        flashT = 0.35f
                        flashColor = Ui.RED
                        pushPopup("禁止刹车。ZhangCan 只用手刹。扣 50。", Ui.RED)
                    }
                    Phase.STRAIGHT -> pushPopup("直线段的刹车是装饰品。", Ui.TEXT_DIM)
                    else -> Unit
                }
            }
        }
    }

    override fun onBack(): Boolean {
        nav.go(MenuScene(nav, context, game))
        return true
    }

    companion object {
        const val STRAIGHT_SECONDS = 12f
        const val CURVE_SECONDS = 16f
    }
}
