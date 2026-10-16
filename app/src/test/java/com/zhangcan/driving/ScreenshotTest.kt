package com.zhangcan.driving

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.test.core.app.ApplicationProvider
import com.zhangcan.driving.core.AchievementManager
import com.zhangcan.driving.core.Game
import com.zhangcan.driving.core.ScoreManager
import com.zhangcan.driving.core.Sfx
import com.zhangcan.driving.scene.AchievementScene
import com.zhangcan.driving.scene.BikeScene
import com.zhangcan.driving.scene.MenuScene
import com.zhangcan.driving.scene.Navigator
import com.zhangcan.driving.scene.RaceScene
import com.zhangcan.driving.scene.ResultScene
import com.zhangcan.driving.scene.Scene
import com.zhangcan.driving.scene.TheoryScene
import java.io.File
import java.io.FileOutputStream
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 用真机渲染管线（Robolectric NATIVE 图形）导出 README 截图 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotTest {

    private class Nav : Navigator {
        var current: Scene? = null
        override fun go(scene: Scene) {
            current = scene
        }
    }

    private val w = 1080
    private val h = 2340

    private fun render(name: String, scene: Scene, warmupFrames: Int = 30, pre: (Scene) -> Unit = {}) {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        scene.onViewport(w, h)
        scene.onEnter()
        pre(scene)
        repeat(warmupFrames) {
            scene.draw(canvas)
            scene.update(1f / 60f)
        }
        scene.draw(canvas)
        val dir = File("../docs").also { it.mkdirs() }
        FileOutputStream(File(dir, name)).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        println("SHOT $name -> ${File(dir, name).absolutePath} ${File(dir, name).length()} bytes")
    }

    // 手动运行：./gradlew testDebugUnitTest --tests "*ScreenshotTest*" --rerun-tasks
    // 默认 @Ignore，避免 CI 里写文件
    @Ignore("手动触发生成 README 截图")
    @Test
    fun exportScreenshots() {
        Sfx.enabled = false
        val context: Context = ApplicationProvider.getApplicationContext()
        val game = Game(context)
        game.achievements.resetAll()
        game.recordTheory(100)
        val nav = Nav()

        render("menu.png", MenuScene(nav, context, game), 120)

        render("theory.png", TheoryScene(nav, context, game), 40)

        render("race.png", RaceScene(nav, context, game), 300)

        render("bike.png", BikeScene(nav, context, game), 240)

        val details = listOf(
            ScoreManager.Entry("直线行驶 第1次检测：太稳了，这不像你", -20),
            ScoreManager.Entry("直线行驶 第2次检测：太稳了，这不像你", -20),
            ScoreManager.Entry("曲线行驶第1次踩刹车", -50),
            ScoreManager.Entry("触发了隐藏换挡键", -100)
        )
        val result = Game.RunResult(
            title = "赛车 · 实操考试",
            score = -90,
            details = details,
            newAchievements = listOf(
                AchievementManager.of(AchievementManager.A8),
                AchievementManager.of(AchievementManager.A2)
            ),
            comment = "你的驾驶技术已经超越了物理引擎。"
        )
        render("result.png", ResultScene(nav, context, game, result), 30)

        render("achievements.png", AchievementScene(nav, context, game), 30)
    }
}
