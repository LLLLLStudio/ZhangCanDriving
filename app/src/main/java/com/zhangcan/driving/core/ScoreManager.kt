package com.zhangcan.driving.core

/**
 * 扣分 / 加分账本。
 * 所有荒诞规则的执行结果都记在这里，结算页直接展示明细。
 */
class ScoreManager {

    data class Entry(val reason: String, val delta: Int)

    var score: Int = START
        private set

    private val entries = ArrayList<Entry>()

    val details: List<Entry> get() = entries

    /** delta 为负数即扣分 */
    fun adjust(delta: Int, reason: String) {
        score += delta
        entries.add(Entry(reason, delta))
    }

    fun reset() {
        score = START
        entries.clear()
    }

    fun deducted(): Int = entries.filter { it.delta < 0 }.sumOf { -it.delta }

    fun gained(): Int = entries.filter { it.delta > 0 }.sumOf { it.delta }

    companion object {
        const val START = 100
    }
}
