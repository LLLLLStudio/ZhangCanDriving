package com.zhangcan.driving.core

import android.content.Context

/**
 * 全局存档：成就 + 解锁进度。
 * 理论考试 ≥ 60 分才解锁赛车实操考试。
 */
class Game(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("zhangcan_driving", Context.MODE_PRIVATE)

    val achievements = AchievementManager(context)

    /** 一次跑图/一局考试的结果，交给结算页展示 */
    data class RunResult(
        val title: String,
        val score: Int,
        val details: List<ScoreManager.Entry>,
        val newAchievements: List<AchievementManager.Achievement>,
        val comment: String,
        val canReplay: Boolean = true
    )

    var theoryPassed: Boolean = prefs.getBoolean("theory_passed", false)
        private set

    var theoryBest: Int = prefs.getInt("theory_best", 0)
        private set

    fun recordTheory(score: Int) {
        if (score > theoryBest) {
            theoryBest = score
            prefs.edit().putInt("theory_best", score).apply()
        }
        if (score >= THEORY_PASS && !theoryPassed) {
            theoryPassed = true
            prefs.edit().putBoolean("theory_passed", true).apply()
        }
    }

    companion object {
        const val THEORY_PASS = 60
        const val POINTS_PER_QUESTION = 25
    }
}
