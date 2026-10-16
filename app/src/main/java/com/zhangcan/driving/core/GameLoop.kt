package com.zhangcan.driving.core

import java.util.concurrent.locks.LockSupport

/**
 * 固定帧率的游戏主循环计时器。
 * 由渲染线程驱动：每帧调用 tick() 拿到 dt（秒），结束时调用 idle() 睡到下一帧。
 */
class GameLoop(val targetFps: Int = 60) {

    private val frameNs: Long = 1_000_000_000L / targetFps
    private var lastNs = 0L

    fun reset() {
        lastNs = 0L
    }

    /** 返回本帧 dt（秒），最大限制 50ms，防止后台切回时的瞬移 */
    fun tick(): Float {
        val now = System.nanoTime()
        if (lastNs == 0L) {
            lastNs = now
            return 1f / targetFps
        }
        var dt = (now - lastNs) / 1_000_000_000f
        lastNs = now
        if (dt > 0.05f) dt = 0.05f
        if (dt < 0f) dt = 1f / targetFps
        return dt
    }

    fun idle(frameStartNs: Long) {
        val spent = System.nanoTime() - frameStartNs
        val rest = frameNs - spent
        if (rest > 0L) LockSupport.parkNanos(rest)
    }

    fun frameStart(): Long = System.nanoTime()
}
