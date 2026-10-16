package com.zhangcan.driving.scene

import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import com.zhangcan.driving.core.Game
import com.zhangcan.driving.core.InputState
import com.zhangcan.driving.core.Ui

/** 场景切换回调，由 GameView 实现 */
interface Navigator {
    fun go(scene: Scene)
}

/**
 * 所有场景的基类。
 * 渲染线程调用 update/draw，UI 线程调用 onTouch —— 触摸只写入 InputState 的快照，避免加锁。
 */
abstract class Scene(
    protected val nav: Navigator,
    protected val context: Context,
    protected val game: Game
) {

    protected val input = InputState()

    /** 当前视口尺寸，onViewport 与 draw 时同步 */
    @Volatile
    protected var vw: Int = 1080

    @Volatile
    protected var vh: Int = 1920

    open fun onEnter() {
        input.clear()
        onViewport(vw, vh)
    }

    open fun onExit() {
        input.clear()
    }

    open fun onViewport(w: Int, h: Int) {
        vw = w
        vh = h
    }

    abstract fun update(dt: Float)

    abstract fun draw(canvas: Canvas)

    /** 命中测试：返回控件 id，null 表示没命中任何可交互区域 */
    open fun hitTest(x: Float, y: Float): String? = null

    /** 点击（按下后在同一控件上抬起） */
    open fun onTap(id: String) {}

    /** 返回 true 表示已消费返回键 */
    open fun onBack(): Boolean = false

    fun handleTouch(e: MotionEvent) {
        if (e.actionMasked == MotionEvent.ACTION_DOWN) {
            // 主线程做一次坐标兜底，防止首帧前 vw/vh 为 0
        }
        input.onTouch(e) { x, y -> hitTest(x, y) }
        if (e.actionMasked == MotionEvent.ACTION_UP) {
            var t = input.pollTap()
            while (t != null) {
                onTap(t)
                t = input.pollTap()
            }
        }
    }

    protected fun hit(r: android.graphics.RectF, x: Float, y: Float): Boolean =
        x >= r.left && x <= r.right && y >= r.top && y <= r.bottom

    /** 每次布局时按屏幕比例重置按钮位置 */
    protected fun layoutButton(b: Ui.Button, l: Float, t: Float, r: Float, bt: Float) {
        b.rect.set(l, t, r, bt)
    }
}
