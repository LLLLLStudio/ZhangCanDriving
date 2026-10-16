package com.zhangcan.driving.scene

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.zhangcan.driving.core.AchievementManager
import com.zhangcan.driving.core.Game
import com.zhangcan.driving.core.ScoreManager
import com.zhangcan.driving.core.Ui
import com.zhangcan.driving.data.FunnyComments
import com.zhangcan.driving.data.QuestionBank

/**
 * 赛车：理论考试。
 * 随机抽 4 题，每题 25 分，满分 100，≥ 60 解锁实操。
 */
class TheoryScene(nav: Navigator, context: Context, game: Game) : Scene(nav, context, game) {

    private val questions = QuestionBank.draw(QUESTIONS_PER_RUN)
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
            val b = Ui.Button("opt$i", "${('A' + i)}. ${q.options[i]}", Ui.PANEL_LIGHT)
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
            canvas, "已答对 $correct · 每题 ${Game.POINTS_PER_QUESTION} 分",
            vw / 2f, vh * 0.085f, vh * 0.017f, Ui.TEXT_DIM, Paint.Align.CENTER
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
            val r = RectF(vw * 0.07f, vh * 0.82f, vw * 0.93f, vh * 0.9f)
            Ui.rect(canvas, r, if (feedbackOk) Ui.withAlpha(Ui.GREEN, 60) else Ui.withAlpha(Ui.RED, 60), Ui.dp(12))
            Ui.text(
                canvas, fb, r.centerX(), r.centerY() + vh * 0.008f, vh * 0.021f,
                if (feedbackOk) Ui.GREEN else Ui.RED, Paint.Align.CENTER, true
            )
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
        val q = questions[index]
        val i = id.removePrefix("opt").toIntOrNull() ?: return
        chosen = i
        locked = true
        nextDelay = 1.5f

        if (i == q.answer) {
            correct++
            feedbackOk = true
            feedback = "正确 +${Game.POINTS_PER_QUESTION} 分"
            details.add(ScoreManager.Entry("第 ${index + 1} 题：答对", Game.POINTS_PER_QUESTION))
        } else {
            feedbackOk = false
            feedback = "错误。${q.sass}"
            details.add(ScoreManager.Entry("第 ${index + 1} 题：答错（正确：${q.options[q.answer]}）", 0))
        }

        q.achievementOnOption[i]?.let { achId ->
            if (game.achievements.unlock(achId)) {
                newAchievements.add(AchievementManager.of(achId))
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
        val score = correct * Game.POINTS_PER_QUESTION
        game.recordTheory(score)
        val comment = when {
            score >= 100 -> "满分。看来你只在考试里守规矩。"
            score >= Game.THEORY_PASS -> FunnyComments.pick(FunnyComments.THEORY)
            else -> "理论都没过。实操考试暂时别想了。"
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

    companion object {
        const val QUESTIONS_PER_RUN = 4
    }
}
