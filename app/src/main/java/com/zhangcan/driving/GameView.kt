package com.zhangcan.driving

import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.zhangcan.driving.core.Game
import com.zhangcan.driving.core.GameLoop
import com.zhangcan.driving.scene.MenuScene
import com.zhangcan.driving.scene.Navigator
import com.zhangcan.driving.scene.Scene
import java.util.concurrent.locks.LockSupport

/**
 * SurfaceView + 独立渲染线程。
 * 渲染线程只做 update/draw；触摸在 UI 线程写入 InputState 快照，不做重锁。
 */
class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Navigator, Runnable {

    private val game = Game(context)
    private val loop = GameLoop(TARGET_FPS)

    @Volatile
    private var scene: Scene = MenuScene(this, context, game)

    @Volatile
    private var pending: Scene? = null

    @Volatile
    private var running = false

    @Volatile
    private var paused = false

    private var thread: Thread? = null

    @Volatile
    private var surfaceW = 0

    @Volatile
    private var surfaceH = 0

    init {
        holder.addCallback(this)
        isClickable = true
        isFocusable = true
        isFocusableInTouchMode = true
        keepScreenOn = true
    }

    override fun go(scene: Scene) {
        pending = scene
    }

    private fun applyPending() {
        val next = pending ?: return
        pending = null
        scene.onExit()
        next.onViewport(surfaceW.coerceAtLeast(1), surfaceH.coerceAtLeast(1))
        next.onEnter()
        scene = next
        loop.reset()
    }

    // ---------------- SurfaceHolder.Callback ----------------

    override fun surfaceCreated(holder: SurfaceHolder) {
        startLoop()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        surfaceW = width
        surfaceH = height
        scene.onViewport(width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        stopLoop()
    }

    private fun startLoop() {
        if (running) return
        running = true
        loop.reset()
        val t = Thread(this, "zhangcan-render")
        t.priority = Thread.MAX_PRIORITY
        thread = t
        t.start()
    }

    private fun stopLoop() {
        running = false
        val t = thread
        thread = null
        if (t != null) {
            try {
                t.join(800)
            } catch (_: InterruptedException) {
            }
        }
    }

    fun pause() {
        paused = true
    }

    fun resume() {
        paused = false
        loop.reset()
    }

    fun handleBack(): Boolean = scene.onBack()

    // ---------------- 渲染循环 ----------------

    override fun run() {
        while (running) {
            if (paused) {
                LockSupport.parkNanos(20_000_000L)
                continue
            }
            val frameStart = loop.frameStart()
            val dt = loop.tick()
            applyPending()

            val s = scene
            s.onViewport(surfaceW.coerceAtLeast(1), surfaceH.coerceAtLeast(1))
            try {
                s.update(dt)
            } catch (t: Throwable) {
                // 任何场景异常都不允许崩掉进程
                android.util.Log.e("ZhangCanDriving", "scene update failed", t)
            }

            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) s.draw(canvas)
            } catch (t: Throwable) {
                android.util.Log.e("ZhangCanDriving", "scene draw failed", t)
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (_: Throwable) {
                    }
                }
            }

            loop.idle(frameStart)
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val s = scene
        s.onViewport(surfaceW.coerceAtLeast(1), surfaceH.coerceAtLeast(1))
        try {
            s.handleTouch(event)
        } catch (t: Throwable) {
            android.util.Log.e("ZhangCanDriving", "touch failed", t)
        }
        return true
    }

    companion object {
        const val TARGET_FPS = 60
    }
}
