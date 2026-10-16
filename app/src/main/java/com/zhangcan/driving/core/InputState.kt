package com.zhangcan.driving.core

import android.view.MotionEvent

/**
 * 触摸输入状态机。
 *
 * 支持多指：每个 pointer 映射到一个控件 id（如 left / right / brake / handbrake / 菜单按钮）。
 * 渲染线程只读取 [heldSnapshot]（volatile 快照），避免跨线程遍历 HashSet 崩溃。
 */
class InputState {

    private val pointers = HashMap<Int, String>()

    @Volatile
    private var snapshot: Set<String> = emptySet()

    private val taps = ArrayList<String>()

    val held: Set<String> get() = snapshot

    fun isHeld(id: String): Boolean = snapshot.contains(id)

    fun pollTap(): String? {
        synchronized(taps) {
            if (taps.isEmpty()) return null
            return taps.removeAt(0)
        }
    }

    /** 场景切换时清空，防止“按钮卡住” */
    fun clear() {
        synchronized(pointers) { pointers.clear() }
        snapshot = emptySet()
        synchronized(taps) { taps.clear() }
    }

    fun onTouch(e: MotionEvent, hitTest: (Float, Float) -> String?) {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val i = e.actionIndex
                val id = hitTest(e.getX(i), e.getY(i))
                if (id != null) synchronized(pointers) { pointers[e.getPointerId(i)] = id }
            }

            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until e.pointerCount) {
                    val pid = e.getPointerId(i)
                    val id = hitTest(e.getX(i), e.getY(i))
                    synchronized(pointers) {
                        if (id == null) pointers.remove(pid)
                        else pointers[pid] = id
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val i = e.actionIndex
                val pid = e.getPointerId(i)
                val id = synchronized(pointers) { pointers.remove(pid) }
                if (id != null && hitTest(e.getX(i), e.getY(i)) == id) {
                    synchronized(taps) { taps.add(id) }
                }
            }

            MotionEvent.ACTION_CANCEL -> {
                synchronized(pointers) { pointers.clear() }
            }
        }
        snapshot = synchronized(pointers) { HashSet(pointers.values) }
    }
}
