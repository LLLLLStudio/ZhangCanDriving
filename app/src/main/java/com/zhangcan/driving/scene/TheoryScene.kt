package com.zhangcan.driving.scene

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.zhangcan.driving.core.AchievementManager
import com.zhangcan.driving.core.Game
import com.zhangcan.driving.core.ScoreManager
import com.zhangcan.driving.core.Sfx
import com.zhangcan.driving.core.Ui
import com.zhangcan.driving.data.FunnyComments
import com.zhangcan.driving.data.QuestionBank

/**
 * 赛车：理论考试。
 * 随机抽 4 题，每题 25 分，满分 100，≥ 60 解锁实操。
 */
class TheoryScene(nav: Navigator, context: Context, game: Game) : Scene(nav, context, game) {

    private val questions = QuestionBank.draw(QuestionBank.QUESTIONS_PER_RUN)
    private val optionButtons = ArrayList<Ui.Button>()
    private val backRect = RectF()

    private var index = 0
    private var correct = 0
    private var chosen = -1
    private var locked = false
    private var feedback: String? = null
    private var feedbackOk = false
    private var nextDelay = 0f
    private var elapsed = 0f

    private val details = ArrayList<ScoreManager.Entry>()
    private val newAchievements = ArrayList<AchievementManager.Achievement>()

    override fun onViewport(w: Int, h: Int) {
        super.onViewport(w, h)
        backRect.set(Ui.dp(14), Ui.dp(14), Ui.dp(14) + Ui.dp(84), Ui.dp(14) + Ui.dp(40))
        layoutOptions()
    }

    private fun layoutOptions() {
        val q = questions.getOrNull(index) ?: return
        optionButtons.clear()
        val marginX = vw * 0.07f
        val top = vh * 0.42f
        val bh = (vh * 0.072f).coerceAtMost(Ui.dp(70))
        val gap = vh * 0.018f
        var y = top
        for (i in q.options.indices) {
            // 只取 text 字段，绝不把 Option 对象拼进界面
            val b = Ui.Button("opt$i", q.labelOf(i), Ui.PANEL_LIGHT)
            b.textSize = bh * 0.3f
            b.rect.set(marginX, y, vw - marginX, y + bh)
            optionButtons.add(b)
            y += bh + gap
        }
    }

    override fun update(dt: Float) {
        elapsed += dt
        if (locked && nextDelay > 0f) {
            nextDelay -= dt
            if (nextDelay <= 0f) advance()
        }
    }

    override fun draw(canvas: Canvas) {
        Ui.fill(canvas, Ui.BG)

        // 顶部信息条
        Ui.rect(canvas, 0f, 0f, vw.toFloat(), vh * 0.1f, Ui.PANEL)
        Ui.drawBack(canvas, backRect)
        Ui.text(
            canvas, "理论考试  ${index + 1} / ${questions.size}",
            vw / 2f, vh * 0.055f, vh * 0.024f, Ui.TEXT, Paint.Align.CENTER, true
        )
        Ui.text(
            canvas, "已答对 $correct · 答对 +${Game.POINTS_PER_QUESTION} · 答错 -${Game.PENALTY_PER_NORMAL_ANSWER}",
            vw / 2f, vh * 0.085f, vh * 0.017f, Ui.TEXT_DIM, Paint.Align.CENTER
        )
        Ui.text(
            canvas, FunnyComments.ABSURD_RULE,
            vw / 2f, vh * 0.0995f, vh * 0.0155f, Ui.ACCENT, Paint.Align.CENTER, true
        )

        // 进度条
        val pw = vw * 0.86f
        val px = (vw - pw) / 2f
        Ui.rect(canvas, px, vh * 0.108f, px + pw, vh * 0.116f, Ui.withAlpha(0xFFFFFFFF.toInt(), 30), Ui.dp(4))
        Ui.rect(canvas, px, vh * 0.108f, px + pw * (index.toFloat() / questions.size), vh * 0.116f, Ui.ACCENT, Ui.dp(4))

        val q = questions.getOrNull(index) ?: return
        Ui.text(canvas, "第 ${index + 1} 题", px, vh * 0.175f, vh * 0.019f, Ui.ACCENT, Paint.Align.LEFT, true)
        val lines = Ui.wrap(q.text, vh * 0.03f, vw * 0.86f, true)
        var ty = vh * 0.225f
        for (line in lines) {
            Ui.text(canvas, line, vw * 0.07f, ty, vh * 0.03f, Ui.TEXT, Paint.Align.LEFT, true)
            ty += vh * 0.04f
        }

        for ((i, b) in optionButtons.withIndex()) {
            val isChosen = i == chosen
            val isAnswer = locked && i == q.answer
            if (isAnswer) b.fill = Ui.withAlpha(Ui.GREEN, 90)
            else if (isChosen) b.fill = Ui.withAlpha(Ui.RED, 90)
            else b.fill = Ui.PANEL_LIGHT
            Ui.drawButton(canvas, b, input.isHeld(b.id) && !locked)
        }

        val fb = feedback
        if (fb != null) {
            val r = RectF(vw * 0.06f, vh * 0.805f, vw * 0.94f, vh * 0.905f)
            Ui.rect(canvas, r, if (feedbackOk) Ui.withAlpha(Ui.GREEN, 60) else Ui.withAlpha(Ui.RED, 60), Ui.dp(12))
            val size = vh * 0.0195f
            val lines = Ui.wrap(fb, size, r.width() * 0.92f, true).take(2)
            var ly = r.centerY() - (lines.size - 1) * size * 0.62f + size * 0.34f
            for (line in lines) {
                Ui.text(
                    canvas, line, r.centerX(), ly, size,
                    if (feedbackOk) Ui.GREEN else Ui.RED, Paint.Align.CENTER, true
                )
                ly += size * 1.24f
            }
        }
    }

    override fun hitTest(x: Float, y: Float): String? {
        if (hit(backRect, x, y)) return "back"
        return optionButtons.firstOrNull { hit(it.rect, x, y) }?.id
    }

    override fun onTap(id: String) {
        if (id == "back") {
            nav.go(MenuScene(nav, context, game))
            return
        }
        if (locked) return
        val q = questions.getOrNull(index) ?: return
        val i = id.removePrefix("opt").toIntOrNull() ?: return
        if (i !in q.options.indices) return
        chosen = i
        locked = true
        nextDelay = 1.5f

        // 只读取字段做判定：absurd = true 为得分项，absurd = false 为扣分项
        val option = q.options[i]
        val comment = option.comment
        if (option.absurd) {
            // 离谱即正确
            correct++
            Sfx.reward()
            feedbackOk = true
            feedback = buildString {
                append("正确 +${Game.POINTS_PER_QUESTION} 分")
                if (comment.isNotBlank()) append(" —— $comment")
            }
            details.add(
                ScoreManager.Entry(
                    "第 ${index + 1} 题：${comment.ifBlank { "荒诞，且正确" }}",
                    Game.POINTS_PER_QUESTION
                )
            )
        } else {
            feedbackOk = false
            Sfx.penalty()
            // comment 非空时优先展示该选项的吐槽，否则用本题的吐槽
            feedback = buildString {
                append("错误 -${Game.PENALTY_PER_NORMAL_ANSWER} 分")
                append(" —— ")
                append(comment.ifBlank { q.sass })
            }
            details.add(
                ScoreManager.Entry(
                    "第 ${index + 1} 题：你选了「${option.text}」这种正常答案，荒诞答案是「${q.answerText}」",
                    -Game.PENALTY_PER_NORMAL_ANSWER
                )
            )
        }

        // 荒诞答案触发的成就（例如 A1：WRC 物理学家）
        if (option.absurd) {
            q.achievementOnAbsurd?.let { achId ->
                if (game.achievements.unlock(achId)) {
                    newAchievements.add(AchievementManager.of(achId))
                    Sfx.achievement()
                }
            }
        }
    }

    private fun advance() {
        locked = false
        chosen = -1
        feedback = null
        nextDelay = 0f
        index++
        if (index >= questions.size) {
            finish()
        } else {
            layoutOptions()
        }
    }

    private fun finish() {
        // 荒诞答案 +25，正常答案 -10
        val wrong = questions.size - correct
        val score = correct * Game.POINTS_PER_QUESTION - wrong * Game.PENALTY_PER_NORMAL_ANSWER
        game.recordTheory(score)
        val comment = if (score >= Game.THEORY_PASS) {
            FunnyComments.theoryComment(correct, questions.size)
        } else {
            "理论都没过。连荒诞都答不对，实操考试暂时别想了。"
        }
        val result = Game.RunResult(
            title = "赛车 · 理论考试",
            score = score,
            details = details,
            newAchievements = newAchievements,
            comment = comment
        )
        nav.go(ResultScene(nav, context, game, result) { TheoryScene(nav, context, game) })
    }

    override fun onBack(): Boolean {
        nav.go(MenuScene(nav, context, game))
        return true
    }

    /** 当前题目在界面上真正渲染出来的选项文案（供测试断言不泄露数据结构） */
    fun currentOptionLabels(): List<String> = optionButtons.map { it.label }

    /** 当前题目的荒诞选项下标 */
    fun currentAbsurdIndex(): Int = questions.getOrNull(index)?.answer ?: -1

    companion object {
        const val QUESTIONS_PER_RUN = 4
    }
}
