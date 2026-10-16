package com.zhangcan.driving

import android.content.Context
import android.graphics.Canvas
import android.os.SystemClock
import android.view.MotionEvent
import androidx.test.core.app.ApplicationProvider
import com.zhangcan.driving.core.AchievementManager
import com.zhangcan.driving.core.Game
import com.zhangcan.driving.scene.AchievementScene
import com.zhangcan.driving.scene.BikeScene
import com.zhangcan.driving.scene.MenuScene
import com.zhangcan.driving.scene.Navigator
import com.zhangcan.driving.scene.RaceScene
import com.zhangcan.driving.scene.ResultScene
import com.zhangcan.driving.scene.Scene
import com.zhangcan.driving.scene.TheoryScene
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 荒诞规则的 JVM 级验证（Robolectric）。
 * 直接驱动 Scene 的 update / draw / 触摸链路，确认每个规则真的会扣分、解锁、结算。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GameRulesTest {

    private class RecordingNavigator : Navigator {
        var current: Scene? = null
        override fun go(scene: Scene) {
            current = scene
        }
    }

    private lateinit var context: Context
    private lateinit var game: Game
    private lateinit var nav: RecordingNavigator

    private val width = 1080
    private val height = 2340
    private val dt = 1f / 60f

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        game = Game(context)
        game.achievements.resetAll()
        nav = RecordingNavigator()
    }

    // ---------------- 工具 ----------------

    /** 扫描视口，找出某个控件 id 的一个可点击坐标 */
    private fun Scene.pointOf(id: String): Pair<Float, Float> {
        onViewport(width, height)
        var y = 0f
        while (y < height) {
            var x = 0f
            while (x < width) {
                if (hitTest(x, y) == id) return x to y
                x += 6f
            }
            y += 6f
        }
        throw AssertionError("找不到控件：$id")
    }

    private fun Scene.down(x: Float, y: Float): MotionEvent {
        val e = MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, x, y, 0)
        handleTouch(e)
        e.recycle()
        return e
    }

    private fun Scene.up(x: Float, y: Float) {
        val e = MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, x, y, 0)
        handleTouch(e)
        e.recycle()
    }

    /** 追加一根手指（多指同时按住，例如“转向 + 刹车”） */
    private fun Scene.downPointer(pointerId: Int, x: Float, y: Float) {
        val props = arrayOf(
            MotionEvent.PointerProperties().apply {
                id = pointerId
                toolType = MotionEvent.TOOL_TYPE_FINGER
            }
        )
        val coords = arrayOf(
            MotionEvent.PointerCoords().apply {
                this.x = x
                this.y = y
                pressure = 1f
                size = 1f
            }
        )
        val e = MotionEvent.obtain(
            SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
            MotionEvent.ACTION_POINTER_DOWN, 1, props, coords,
            0, 0, 1f, 1f, 0, 0, android.view.InputDevice.SOURCE_TOUCHSCREEN, 0
        )
        handleTouch(e)
        e.recycle()
    }

    /** 同时移动/按住多根手指 */
    private fun Scene.movePointers(vararg fingers: Pair<Int, Pair<Float, Float>>) {
        val props = fingers.map { (pid, _) ->
            MotionEvent.PointerProperties().apply {
                id = pid
                toolType = MotionEvent.TOOL_TYPE_FINGER
            }
        }.toTypedArray()
        val coords = fingers.map { (_, pos) ->
            MotionEvent.PointerCoords().apply {
                x = pos.first
                y = pos.second
                pressure = 1f
                size = 1f
            }
        }.toTypedArray()
        val e = MotionEvent.obtain(
            SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
            MotionEvent.ACTION_MOVE, props.size, props, coords,
            0, 0, 1f, 1f, 0, 0, android.view.InputDevice.SOURCE_TOUCHSCREEN, 0
        )
        handleTouch(e)
        e.recycle()
    }

    private fun Scene.cancelTouch() {
        val e = MotionEvent.obtain(
            SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
            MotionEvent.ACTION_CANCEL, 0f, 0f, 0
        )
        handleTouch(e)
        e.recycle()
    }

    /** 点一下（按下 + 抬起，会触发 onTap） */
    private fun Scene.tapAt(id: String) {
        val (x, y) = pointOf(id)
        down(x, y)
        up(x, y)
    }

    private fun Scene.holdAt(id: String): Pair<Float, Float> {
        val (x, y) = pointOf(id)
        down(x, y)
        return x to y
    }

    /** 推进 seconds 秒；每次 update 前后都 draw 一次，顺带验证渲染不崩 */
    private fun Scene.step(seconds: Float, canvas: Canvas? = null) {
        var t = 0f
        while (t < seconds) {
            draw(canvas ?: Canvas())
            update(dt)
            t += dt
        }
    }

    private fun resultFrom(scene: Scene): Game.RunResult {
        scene.step(0.5f)
        val r = nav.current
        assertTrue("应当已进入结算页，实际：$r", r is ResultScene)
        return (r as ResultScene).result
    }

    // ---------------- 主菜单 ----------------

    @Test
    fun menu_理论考试入口_可进入() {
        val menu = MenuScene(nav, context, game)
        menu.onViewport(width, height)
        menu.onTap("theory")
        assertTrue(nav.current is TheoryScene)
    }

    @Test
    fun menu_理论未通过时_实操入口被锁() {
        val menu = MenuScene(nav, context, game)
        menu.onViewport(width, height)
        menu.onTap("race")
        assertNull("理论未过 60 分不应进入实操", nav.current)
    }

    @Test
    fun menu_理论通过后_实操入口解锁() {
        game.recordTheory(75)
        val menu = MenuScene(nav, context, game)
        menu.onViewport(width, height)
        menu.onTap("race")
        assertTrue(nav.current is RaceScene)
    }

    @Test
    fun menu_成就入口_可进入() {
        val menu = MenuScene(nav, context, game)
        menu.onViewport(width, height)
        menu.onTap("ach")
        assertTrue(nav.current is AchievementScene)
    }

    // ---------------- 理论考试 ----------------

    @Test
    fun theory_答题后进入结算并记录分数() {
        val theory = TheoryScene(nav, context, game)
        theory.onViewport(width, height)
        var guard = 0
        while (nav.current == null && guard++ < 12) {
            theory.tapAt("opt0")
            theory.step(2f)
        }
        val result = resultFrom(theory)
        assertEquals("赛车 · 理论考试", result.title)
        assertTrue("每题 25 分", result.score % Game.POINTS_PER_QUESTION == 0)
        assertEquals(theoryScoreOf(result), game.theoryBest)
    }

    private fun theoryScoreOf(result: Game.RunResult): Int =
        result.details.count { it.delta > 0 } * Game.POINTS_PER_QUESTION

    // ---------------- 赛车：规则 1 / 2 / 4 / 5 ----------------

    @Test
    fun race_规则1_直线太稳扣20() {
        val race = RaceScene(nav, context, game)
        race.onViewport(width, height)
        race.step(2.6f) // 熬过 INTRO
        race.step(32f)  // 全程不碰方向盘，直线段每 2 秒检测一次
        val result = resultFrom(race)
        val deducts = result.details.filter { it.delta == -20 }
        assertTrue("直线段每 2 秒检测，不左右打满应多次扣 20", deducts.size >= 3)
        assertTrue(result.details.any { it.reason.contains("太稳了") })
    }

    @Test
    fun race_规则1_左右各打90度可通过检测() {
        val race = RaceScene(nav, context, game)
        race.onViewport(width, height)
        race.step(2.5f) // 熬过 INTRO

        // 先把方向盘打到右侧 ≈ +220°，再以 0.6 秒为半周期左右摆动：
        // 每个 2 秒检测窗口内都会出现 < -90° 和 > +90°
        val (rx0, ry0) = race.holdAt("right")
        race.step(0.4f)
        race.up(rx0, ry0)
        repeat(10) {
            val (lx, ly) = race.holdAt("left")
            race.step(0.6f)
            race.up(lx, ly)
            val (rx, ry) = race.holdAt("right")
            race.step(0.6f)
            race.up(rx, ry)
        }

        race.step(20f) // 跑完剩余赛程
        val result = resultFrom(race)
        assertTrue(
            "左右各打 90° 的窗口不应被扣分，实际：${result.details.filter { it.delta < 0 }}",
            result.details.none { it.reason.contains("太稳了") }
        )
    }

    @Test
    fun race_规则2_曲线段踩刹车扣50() {
        val race = RaceScene(nav, context, game)
        race.onViewport(width, height)
        race.step(15f) // 进入曲线段
        race.tapAt("brake")
        race.step(18f) // 跑完剩余赛程
        val result = resultFrom(race)
        assertTrue(result.details.any { it.delta == -50 && it.reason.contains("曲线行驶") })
    }

    @Test
    fun race_规则4_隐藏换挡键扣100并阻止A2() {
        val race = RaceScene(nav, context, game)
        race.onViewport(width, height)
        race.tapAt("gear")
        race.step(32f)
        val result = resultFrom(race)
        assertTrue(result.details.any { it.delta == -100 })
        assertTrue(result.details.any { it.reason.contains("换挡") })
        assertFalse(game.achievements.isUnlocked(AchievementManager.A2))
    }

    @Test
    fun race_规则5_尝试切换视角3次解锁A8() {
        val race = RaceScene(nav, context, game)
        race.onViewport(width, height)
        repeat(3) {
            race.tapAt("view")
            race.step(0.2f)
        }
        assertTrue(game.achievements.isUnlocked(AchievementManager.A8))
    }

    @Test
    fun race_完整跑完进入结算页() {
        val race = RaceScene(nav, context, game)
        race.onViewport(width, height)
        race.step(32f)
        val result = resultFrom(race)
        assertEquals("赛车 · 实操考试", result.title)
        assertNotNull(result.comment)
    }

    // ---------------- 自行车：规则 1~5 ----------------

    @Test
    fun bike_规则2_一直直行扣40() {
        val bike = BikeScene(nav, context, game)
        bike.onViewport(width, height)
        bike.step(47f) // 骑完全程（含两个以上 4 秒振幅检测窗口）
        val result = resultFrom(bike)
        assertTrue(result.details.any { it.delta == -40 && it.reason.contains("太直") })
    }

    @Test
    fun bike_规则4_长按刹车抱死摔车并解锁A7() {
        val bike = BikeScene(nav, context, game)
        bike.onViewport(width, height)
        val (bx, by) = bike.holdAt("brake")
        bike.step(6f)
        bike.up(bx, by)
        val result = resultFrom(bike)
        assertTrue("应摔车并给出结算", result.title == "自行车 · 神秘骑行")
        assertTrue(game.achievements.isUnlocked(AchievementManager.A7))
    }

    @Test
    fun bike_规则5_急弯捏刹车摔车() {
        val bike = BikeScene(nav, context, game)
        bike.onViewport(width, height)
        // 一路贴右路肩：既避开障碍（不减速），又把速度拉到阈值以上
        val (rx, ry) = bike.holdAt("right")
        bike.step(10f)
        val (bx, by) = bike.pointOf("brake")
        bike.movePointers(0 to (rx to ry), 1 to (bx to by)) // 第二根手指：同时捏死刹车
        bike.step(1.2f)
        bike.cancelTouch()
        val result = resultFrom(bike)
        assertTrue("急弯 + 捏死刹车应摔车", result.comment.contains("急弯") || result.comment.contains("转体"))
    }

    @Test
    fun bike_规则3_撞上障碍物扣30() {
        val bike = BikeScene(nav, context, game)
        bike.onViewport(width, height)
        bike.step(47f) // 全程直线骑行，必然撞上前方障碍
        val result = resultFrom(bike)
        assertTrue(result.details.any { it.delta == -30 && it.reason.contains("撞上") })
    }

    // ---------------- 成就持久化 ----------------

    @Test
    fun achievements_可持久化() {
        assertTrue(game.achievements.unlock(AchievementManager.A1))
        assertEquals(false, game.achievements.unlock(AchievementManager.A1))
        val reloaded = Game(context)
        assertTrue(reloaded.achievements.isUnlocked(AchievementManager.A1))
        assertEquals(1, reloaded.achievements.unlockedCount())
    }

    @Test
    fun achievements_共8个_全部有名称与条件() {
        assertEquals(8, AchievementManager.ALL.size)
        assertTrue(AchievementManager.ALL.all { it.name.isNotBlank() && it.condition.isNotBlank() })
        val ids = AchievementManager.ALL.map { it.id }.toSet()
        assertEquals(setOf("A1", "A2", "A3", "A4", "A5", "A6", "A7", "A8"), ids)
    }

    // ---------------- 渲染 ----------------

    @Test
    fun 所有场景_连续绘制不崩溃() {
        val canvas = Canvas()
        val scenes = listOf(
            MenuScene(nav, context, game),
            TheoryScene(nav, context, game),
            RaceScene(nav, context, game),
            BikeScene(nav, context, game),
            AchievementScene(nav, context, game),
            ResultScene(
                nav, context, game,
                Game.RunResult("测试", 42, emptyList(), AchievementManager.ALL.take(1), "评语")
            )
        )
        for (s in scenes) {
            s.onViewport(width, height)
            s.onEnter()
            var t = 0f
            while (t < 3f) {
                s.draw(canvas)
                s.update(dt)
                t += dt
            }
            s.onExit()
        }
    }
}

