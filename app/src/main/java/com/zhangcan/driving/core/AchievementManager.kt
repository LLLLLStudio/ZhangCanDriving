package com.zhangcan.driving.core

import android.content.Context

/**
 * 成就系统。
 * 8 个成就，全部通过 SharedPreferences 本地持久化，卸载即失忆。
 */
class AchievementManager(context: Context) {

    data class Achievement(val id: String, val name: String, val condition: String)

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isUnlocked(id: String): Boolean = prefs.getBoolean(KEY_PREFIX + id, false)

    /** 返回 true 表示本次是新解锁（用于结算页高亮） */
    fun unlock(id: String): Boolean {
        if (isUnlocked(id)) return false
        prefs.edit().putBoolean(KEY_PREFIX + id, true).apply()
        return true
    }

    fun unlockedCount(): Int = ALL.count { isUnlocked(it.id) }

    fun resetAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS = "zhangcan_driving"
        private const val KEY_PREFIX = "ach_"

        const val A1 = "A1"
        const val A2 = "A2"
        const val A3 = "A3"
        const val A4 = "A4"
        const val A5 = "A5"
        const val A6 = "A6"
        const val A7 = "A7"
        const val A8 = "A8"

        val ALL: List<Achievement> = listOf(
            Achievement(A1, "WRC 物理学家", "理论考试选“BeamNG 不如 WRC”"),
            Achievement(A2, "从不换挡", "实操考试全程未按换挡键"),
            Achievement(A3, "手刹漂移大师", "曲线行驶全程只用手刹"),
            Achievement(A4, "圈刹信仰", "刹车异响持续 10 秒"),
            Achievement(A5, "让道不让速", "成功让道且未减速"),
            Achievement(A6, "正弦波骑士", "保持 A > 3m 正弦行驶 10 秒"),
            Achievement(A7, "捏死 ABS", "刹车抱死导致摔车"),
            Achievement(A8, "第一人称？不存在的", "尝试切换视角 3 次")
        )

        fun of(id: String): Achievement = ALL.first { it.id == id }
    }
}
