package com.zhangcan.driving.core

import android.media.AudioManager
import android.media.ToneGenerator

/**
 * 极简音效：不引入任何音频资源，全部用 [ToneGenerator] 现场合成，
 * 所以 APK 体积不受影响；任何设备异常都被吞掉，绝不影响游戏运行。
 */
object Sfx {

    private var tone: ToneGenerator? = null
    private var broken = false

    @Volatile
    var enabled: Boolean = true

    private fun generator(): ToneGenerator? {
        if (broken || !enabled) return null
        tone?.let { return it }
        return try {
            ToneGenerator(AudioManager.STREAM_MUSIC, 70).also { tone = it }
        } catch (t: Throwable) {
            broken = true
            null
        }
    }

    private fun play(type: Int, durationMs: Int) {
        try {
            generator()?.startTone(type, durationMs)
        } catch (_: Throwable) {
            broken = true
        }
    }

    /** 扣分：低沉短促的“咚” */
    fun penalty() = play(ToneGenerator.TONE_DTMF_1, 120)

    /** 加分 / 通过检测 */
    fun reward() = play(ToneGenerator.TONE_DTMF_9, 90)

    /** 圈刹异响：吱—— */
    fun squeak() = play(ToneGenerator.TONE_CDMA_ABBR_ALERT, 200)

    /** 圈刹异响：嘎—— */
    fun creak() = play(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 220)

    /** 解锁成就 */
    fun achievement() = play(ToneGenerator.TONE_PROP_BEEP2, 250)

    /** 摔车 / 碰撞 */
    fun crash() = play(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 400)

    /** 换挡隐藏键：更重的惩罚音 */
    fun heavyPenalty() = play(ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE, 300)

    fun release() {
        try {
            tone?.release()
        } catch (_: Throwable) {
        }
        tone = null
    }
}
