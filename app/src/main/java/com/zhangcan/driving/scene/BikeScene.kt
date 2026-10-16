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
 * 自行车：神秘骑行。
 *
 * 荒诞规则实现：
 *  1. 刹车永远不保养：健康值恒为 0，只有圈刹 / 罗拉刹，刹车时随机“吱——嘎——”，没有 ABS
 *  2. 无法保持直线：轨迹必须是正弦曲线，太直扣 40，振幅不够扣 30
 *  3. 让道不让速：横向避开障碍且不减速加分，撞上扣分
 *  4. 永远不会手捏 ABS：长按刹车直接抱死 → 侧滑 → 摔车
 *  5. 急过弯摔车：速度 > 阈值 且 转向 > 阈值 且 刹车按下 → 直接摔
 */
class BikeScene(nav: Navigator, context: Context, game: Game) : Scene(nav, context, game) {

    private class Popup(val text: String, val color: Int, var life: Float)

    private class Obstacle(var z: Float, val lane: Float, val icon: String, var brakeUsed: Boolean = false)

    private val score = ScoreManager()
    private val road = Fake3DRoad()
    private val newAchievements = ArrayList<AchievementManager.Achievement>()
    private val popups = ArrayList<Popup>()
    private val obstacles = ArrayList<Obstacle>()

    private val leftBtn = Ui.Button("left", "◀ 左转", Ui.PANEL_LIGHT)
    private val rightBtn = Ui.Button("right", "右转 ▶", Ui.PANEL_LIGHT)
    private val brakeBtn = Ui.Button("brake", "捏死刹车", 0xFF4A1A1A.toInt())

    private var rideT = 0f
    private var steer = 0f
    private var lateral = 0f
    private var speed = 0.6f
    private var crashed = false

    // 刹车状态
    private var brakeHoldTime = 0f
    private var squeakT = 0f
    private var lockUp = false
    private var lockTime = 0f
    private var absAchieved = false

    // 振幅统计（4 秒滑动窗口）
    private val samples = ArrayList<Float>()
    private var ampEvalT = 0f
    private var windowIndex = 0
    private var lastAmpMeters = 0f
    private var sineOkTime = 0f

    private var obstacleTimer = 2.5f
    private var shoulderT = 0f
    private var dodgeCount = 0
    private var hitCount = 0
    private var noiseLabel: String? = null
    private var noiseT = 0f

    private val backRect = RectF()

    override fun onViewport(w: Int, h: Int) {
        super.onViewport(w, h)
        val bh = (h * 0.11f).coerceAtMost(Ui.dp(92))
        val by = h * 0.735f
        leftBtn.rect.set(w * 0.045f, by, w * 0.32f, by + bh)
        rightBtn.rect.set(w * 0.68f, by, w * 0.955f, by + bh)
        brakeBtn.rect.set(w * 0.38f, by + bh * 0.5f, w * 0.62f, by + bh * 1.5f)
        for (b in listOf(leftBtn, rightBtn, brakeBtn)) b.textSize = bh * 0.26f
        backRect.set(Ui.dp(14), h * 0.05f, Ui.dp(14) + Ui.dp(84), h * 0.05f + Ui.dp(40))
    }

    override fun onEnter() {
        super.onEnter()
        pushPopup("骑行开始。目标轨迹：y = A·sin(ωt)，A > 3m。", Ui.ACCENT)
    }

    override fun update(dt: Float) {
        if (crashed) return
        rideT += dt
        updatePopups(dt)
        if (noiseT > 0f) {
            noiseT -= dt
            if (noiseT <= 0f) noiseLabel = null
        }

        val left = input.isHeld("left")
        val right = input.isHeld("right")
        val braking = input.isHeld("brake")

        steer = when {
            right && !left -> 1f
            left && !right -> -1f
            else -> 0f
        }

        speed = (speed + dt * 0.03f).coerceAtMost(1f)
        if (lockUp) speed = (speed - dt * 0.35f).coerceAtLeast(0.25f)

        // 道路中心线：y = A·sin(ωt)，A 取 0.85（换算后 > 3m）
        road.update(dt, speed * 60f, (SINE_A * sin(rideT * SINE_W)).toFloat())

        // 车把转向 → 横向位移；抱死时附加侧滑
        lateral = (lateral + steer * 0.78f * speed * dt).coerceIn(-1.25f, 1.25f)
        if (lockUp) lateral = (lateral + steer.coerceAtLeast(0.2f) * 0.5f * dt).coerceIn(-1.25f, 1.25f)
        if (steer == 0f) lateral *= (1f - dt * 0.35f)

        updateBrake(dt, braking)
        updateAmplitude(dt)
        updateObstacles(dt)

        if (rideT > RIDE_SECONDS) {
            finish("")
        }
    }

    private fun updateBrake(dt: Float, braking: Boolean) {
        if (braking) {
            brakeHoldTime += dt
            squeakT -= dt
            if (squeakT <= 0f) {
                squeakT = 0.9f + Random.nextFloat() * 0.6f
                noiseLabel = if (Random.nextBoolean()) "吱——" else "嘎——"
                noiseT = 0.9f
            }
            // 规则 4：长按直接抱死
            if (!lockUp && brakeHoldTime > 1.6f && speed > 0.45f) {
                lockUp = true
                lockTime = 0f
                pushPopup("抱死。ABS 不存在。捏死就完事了。", Ui.RED)
            }
            // 规则 1：异响持续 10 秒
            if (!absAchieved && brakeHoldTime >= 10f) {
                absAchieved = true
                unlock(AchievementManager.A4)
                pushPopup("刹车异响已持续 10 秒。圈刹信仰，达成。", Ui.ACCENT)
            }
        } else {
            brakeHoldTime = 0f
            squeakT = 0f
            lockUp = false
            lockTime = 0f
        }

        if (lockUp) {
            lockTime += dt
            if (lockTime > 1.4f) {
                unlock(AchievementManager.A7)
                crash("刹车抱死 → 侧滑 → 你飞了。")
                return
            }
        }

        // 规则 5：急过弯摔车
        if (speed > 0.72f && abs(steer) > 0.6f && braking) {
            crash("急弯 + 捏死刹车 = 空中转体。")
        }
    }

    private fun updateAmplitude(dt: Float) {
        samples.add(lateral)
        // 保留 4 秒
        val maxSamples = (4f / dt).toInt().coerceIn(30, 600)
        while (samples.size > maxSamples) samples.removeAt(0)

        var mn = Float.MAX_VALUE
        var mx = -Float.MAX_VALUE
        for (s in samples) {
            if (s < mn) mn = s
            if (s > mx) mx = s
        }
        val ampUnits = if (samples.isEmpty()) 0f else (mx - mn) / 2f
        lastAmpMeters = ampUnits * METERS_PER_UNIT

        // 正弦波骑士：A > 3m 持续 10 秒（允许短暂回落）
        if (lastAmpMeters >= REQUIRED_AMPLITUDE_M) {
            sineOkTime += dt
            if (sineOkTime >= 10f) unlock(AchievementManager.A6)
        } else {
            sineOkTime = (sineOkTime - dt * 2f).coerceAtLeast(0f)
        }

        ampEvalT += dt
        if (ampEvalT >= 4f) {
            ampEvalT = 0f
            windowIndex++
            when {
                lastAmpMeters < STRAIGHT_AMPLITUDE_M -> {
                    score.adjust(-40, "第${windowIndex}次检测：轨迹太直")
                    pushPopup("你居然会直线？扣 40。", Ui.RED)
                }
                lastAmpMeters < REQUIRED_AMPLITUDE_M -> {
                    score.adjust(-30, "第${windowIndex}次检测：振幅不足")
                    pushPopup("正弦波不够神秘，扣 30。", Ui.RED)
                }
                else -> pushPopup("正弦波达标：A = %.1f m".format(lastAmpMeters), Ui.GREEN)
            }
        }
    }

    private fun updateObstacles(dt: Float) {
        obstacleTimer -= dt
        if (obstacleTimer <= 0f) {
            obstacleTimer = 3.2f + Random.nextFloat() * 1.5f
            val lanes = floatArrayOf(-0.72f, 0f, 0.72f)
            val icons = arrayOf("🚶", "🛵", "🐕", "🚙")
            obstacles.add(Obstacle(8f, lanes[Random.nextInt(lanes.size)], icons[Random.nextInt(icons.size)]))
        }

        val braking = input.isHeld("brake")
        val it = obstacles.iterator()
        while (it.hasNext()) {
            val o = it.next()
            o.z -= speed * 6.5f * dt
            if (braking) o.brakeUsed = true
            if (o.z <= 0.6f) {
                it.remove()
                val dodged = abs(lateral - o.lane) > 0.5f
                if (dodged) {
                    dodgeCount++
                    if (!o.brakeUsed && speed > 0.6f) {
                        score.adjust(20, "让道不让速（第${dodgeCount}次）")
                        unlock(AchievementManager.A5)
                        pushPopup("让道不让速。路权归你。+20", Ui.GREEN)
                    } else {
                        pushPopup("让了，但没完全让。", Ui.ACCENT)
                    }
                } else {
                    hitCount++
                    score.adjust(-30, "撞上障碍物（第${hitCount}次）")
                    speed = (speed - 0.15f).coerceAtLeast(0.3f)
                    pushPopup("撞上了。让了，但没完全让。扣 30。", Ui.RED)
                }
            }
        }

        if (abs(lateral) > 1.05f) {
            shoulderT += dt
            if (shoulderT > 2f) {
                shoulderT = 0f
                pushPopup("你骑到路肩了。路肩不归你。", Ui.TEXT_DIM)
            }
        } else {
            shoulderT = 0f
        }
    }

    private fun crash(msg: String) {
        if (crashed) return
        crashed = true
        finish(msg)
    }

    private fun unlock(id: String) {
        if (game.achievements.unlock(id)) newAchievements.add(AchievementManager.of(id))
    }

    private fun finish(comment: String) {
        val result = Game.RunResult(
            title = "自行车 · 神秘骑行",
            score = score.score,
            details = score.details,
            newAchievements = newAchievements,
            comment = if (comment.isNotEmpty()) comment else FunnyComments.pick(FunnyComments.BIKE)
        )
        nav.go(ResultScene(nav, context, game, result) { BikeScene(nav, context, game) })
    }

    private fun pushPopup(text: String, color: Int) {
        popups.add(Popup(text, color, 2.2f))
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

    override fun draw(canvas: Canvas) {
        road.draw(canvas, vw, vh)
        drawSineGuide(canvas)
        drawObstacles(canvas)
        drawBike(canvas)
        drawHud(canvas)
        drawControls(canvas)
        drawPopups(canvas)
    }

    /** 把目标正弦轨迹画在路面上，方便“照抄” */
    private fun drawSineGuide(canvas: Canvas) {
        var d = 1.6f
        while (d < 14f) {
            val phase = rideT * SINE_W - d * 0.35f
            val lat = (SINE_A * sin(phase) * 0.9).toFloat()
            val y = road.projectY(d, vh)
            val x = road.projectX(d, lat, vw)
            val size = (vw * 0.05f / d).coerceAtLeast(3f)
            Ui.rect(canvas, x - size, y - size * 0.5f, x + size, y + size * 0.5f, Ui.withAlpha(Ui.ACCENT, (200 / d).toInt().coerceIn(30, 200)), Ui.dp(3))
            d += 0.7f
        }
    }

    private fun drawObstacles(canvas: Canvas) {
        for (o in obstacles) {
            val d = 1f + o.z
            val y = road.projectY(d, vh)
            val x = road.projectX(d, o.lane, vw)
            val size = vw * 0.15f / d
            Ui.text(canvas, o.icon, x, y, size, Ui.TEXT, Paint.Align.CENTER)
            if (abs(lateral - o.lane) <= 0.5f && o.z < 2.2f) {
                Ui.text(canvas, "⚠", x, y - size, size * 0.7f, Ui.RED, Paint.Align.CENTER)
            }
        }
    }

    private fun drawBike(canvas: Canvas) {
        val cx = vw / 2f + lateral * vw * 0.2f
        val cy = vh * 0.79f
        val size = vh * 0.062f
        canvas.save()
        canvas.rotate(steer * 14f + if (lockUp) sin(rideT * 22f) * 10f else 0f, cx, cy)
        Ui.text(canvas, if (crashed) "💥" else "🚲", cx, cy, size, Ui.TEXT, Paint.Align.CENTER)
        canvas.restore()
    }

    private fun drawHud(canvas: Canvas) {
        Ui.drawBack(canvas, backRect)

        Ui.text(canvas, "速度 ${(speed * 28).toInt()} km/h", vw * 0.62f, vh * 0.08f, vh * 0.022f, Ui.ACCENT, Paint.Align.LEFT, true)
        Ui.text(
            canvas, "轨迹振幅 A = %.1f m".format(lastAmpMeters), vw * 0.62f, vh * 0.11f, vh * 0.019f,
            if (lastAmpMeters >= REQUIRED_AMPLITUDE_M) Ui.GREEN else Ui.TEXT_DIM, Paint.Align.LEFT
        )
        Ui.text(
            canvas, "正弦达标 ${sineOkTime.toInt()} / 10 s", vw * 0.62f, vh * 0.137f, vh * 0.017f,
            Ui.TEXT_DIM, Paint.Align.LEFT
        )

        // 规则 1 / 4 常驻状态
        Ui.text(canvas, "刹车健康值：0%（圈刹 / 罗拉刹，永不保养）", vw * 0.035f, vh * 0.945f, vh * 0.017f, Ui.withAlpha(Ui.TEXT_DIM, 210), Paint.Align.LEFT, true)
        Ui.text(canvas, "ABS：不存在", vw * 0.035f, vh * 0.968f, vh * 0.017f, Ui.withAlpha(Ui.TEXT_DIM, 210), Paint.Align.LEFT)
        Ui.text(canvas, "让道 $dodgeCount 次 · 撞击 $hitCount 次", vw * 0.035f, vh * 0.991f, vh * 0.017f, Ui.withAlpha(Ui.TEXT, 210), Paint.Align.LEFT)

        Ui.text(canvas, "当前分数：${score.score}", vw - vw * 0.035f, vh * 0.968f, vh * 0.02f, Ui.ACCENT, Paint.Align.RIGHT, true)

        if (lockUp) {
            Ui.text(canvas, "抱死中……", vw / 2f, vh * 0.21f, vh * 0.028f, Ui.RED, Paint.Align.CENTER, true)
        }
        val noise = noiseLabel
        if (noise != null) {
            Ui.text(canvas, noise, vw / 2f, vh * 0.26f, vh * 0.034f, Ui.withAlpha(Ui.ACCENT, 230), Paint.Align.CENTER, true)
        }
    }

    private fun drawControls(canvas: Canvas) {
        leftBtn.fill = if (input.isHeld("left")) Ui.withAlpha(Ui.BLUE, 120) else Ui.PANEL_LIGHT
        rightBtn.fill = if (input.isHeld("right")) Ui.withAlpha(Ui.BLUE, 120) else Ui.PANEL_LIGHT
        brakeBtn.fill = if (input.isHeld("brake")) Ui.withAlpha(Ui.RED, 150) else 0xFF4A1A1A.toInt()
        brakeBtn.label = if (input.isHeld("brake")) "捏死中" else "捏死刹车"
        Ui.drawButton(canvas, leftBtn, input.isHeld("left"))
        Ui.drawButton(canvas, rightBtn, input.isHeld("right"))
        Ui.drawButton(canvas, brakeBtn, input.isHeld("brake"))
    }

    private fun drawPopups(canvas: Canvas) {
        var y = vh * 0.31f
        for (p in popups) {
            val alpha = (255 * (p.life / 2.2f).coerceIn(0f, 1f)).toInt()
            Ui.rect(
                canvas, vw * 0.07f, y - vh * 0.026f, vw * 0.93f, y + vh * 0.012f,
                Ui.withAlpha(0xFF000000.toInt(), (alpha * 0.55f).toInt().coerceIn(0, 200)), Ui.dp(8)
            )
            Ui.text(canvas, p.text, vw / 2f, y, vh * 0.021f, Ui.withAlpha(p.color, alpha), Paint.Align.CENTER, true)
            y += vh * 0.046f
        }
    }

    override fun hitTest(x: Float, y: Float): String? {
        if (hit(backRect, x, y)) return "back"
        if (hit(leftBtn.rect, x, y)) return "left"
        if (hit(rightBtn.rect, x, y)) return "right"
        if (hit(brakeBtn.rect, x, y)) return "brake"
        return null
    }

    override fun onTap(id: String) {
        if (id == "back") nav.go(MenuScene(nav, context, game))
    }

    override fun onBack(): Boolean {
        nav.go(MenuScene(nav, context, game))
        return true
    }

    companion object {
        const val RIDE_SECONDS = 45f
        const val SINE_A = 0.85                // 道路正弦振幅（路面内单位）
        const val SINE_W = 1.15                // 角频率
        const val METERS_PER_UNIT = 5f         // 路面半宽视为 5 米
        const val REQUIRED_AMPLITUDE_M = 3f    // PRD：A > 3m
        const val STRAIGHT_AMPLITUDE_M = 0.6f  // 低于此值判定为“太直”
    }
}
