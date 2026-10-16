package com.zhangcan.driving.data

import kotlin.random.Random

/**
 * 理论考试题库。
 *
 * 判定标准只有一条：**离谱即正确**。
 * 每题有且只有一个「ZhangCan 式荒诞答案」，它就是唯一正确选项；
 * 真实交规、正常驾驶知识一律算错。
 */
object QuestionBank {

    /**
     * @param absurd 是否为 ZhangCan 式荒诞答案（也就是本题唯一正确答案）
     * @param comment 选中该选项时给出的搞笑评语
     */
    data class Option(
        val text: String,
        val absurd: Boolean = false,
        val comment: String = ""
    )

    data class Question(
        val text: String,
        val options: List<Option>,
        /** 选到荒诞答案时解锁的成就 id */
        val achievementOnAbsurd: String? = null,
        /** 选了“正常答案”时的吐槽 */
        val sass: String = "太正常了，这里不考这个。"
    ) {
        /** 唯一正确选项 = 荒诞选项 */
        val answer: Int get() = options.indexOfFirst { it.absurd }

        val answerText: String get() = options.getOrNull(answer)?.text ?: ""

        fun isAbsurd(index: Int): Boolean = index == answer

        /** 打乱选项顺序（荒诞标记跟随选项一起移动） */
        fun shuffledOptions(): Question = copy(options = options.shuffled())
    }

    /** 每套卷子固定 4 题，每题 25 分 */
    const val QUESTIONS_PER_RUN = 4

    val ALL: List<Question> = listOf(
        Question(
            text = "WRC Generation 的物理是否真实如 BeamNG？",
            options = listOf(
                Option("是，比 BeamNG 还真实", absurd = true, comment = "立场正确：WRC 的物理宇宙第一。"),
                Option("不是，BeamNG 更真实"),
                Option("只有不碰撞的时候是"),
                Option("我不关心物理，我只关心帧数")
            ),
            sass = "你居然承认 BeamNG 更真实？叛徒。"
        ),
        Question(
            text = "BeamNG 和 WRC Generation，哪个物理更真实？",
            options = listOf(
                Option("WRC Generation", absurd = true, comment = "BeamNG 看了都摇头。"),
                Option("BeamNG"),
                Option("两个都不如真车"),
                Option("没研究过")
            ),
            achievementOnAbsurd = "A1",
            sass = "物理学家，你走错考场了。"
        ),
        Question(
            text = "手动挡起步应该？",
            options = listOf(
                Option("直接 W 到底", absurd = true, comment = "起步三点头，坐稳了。"),
                Option("油离配合"),
                Option("半联动缓抬离合"),
                Option("先给一点油再抬离合")
            ),
            sass = "标准操作答得这么溜，可惜这里不考。"
        ),
        Question(
            text = "正确驾驶视角？",
            options = listOf(
                Option("永远第三人称", absurd = true, comment = "第三人称才能看清自己撞了什么。"),
                Option("第一人称"),
                Option("引擎盖视角"),
                Option("按路况切换视角")
            ),
            sass = "第一人称？你连自己车头在哪都看不到。"
        ),
        Question(
            text = "曲线行驶正确操作？",
            options = listOf(
                Option("手刹转向", absurd = true, comment = "手刹是你唯一的转向助力。"),
                Option("刹车减速"),
                Option("松油门，稳住方向"),
                Option("提前减速并保持车道")
            ),
            sass = "刹车在曲线段只是装饰品，答案是手刹。"
        ),
        Question(
            text = "碰撞模拟应该怎么设置？",
            options = listOf(
                Option("从不开", absurd = true, comment = "开了还怎么穿模？"),
                Option("打开"),
                Option("按需打开"),
                Option("保持默认")
            ),
            sass = "你居然开碰撞？那你没法穿模了。"
        ),
        Question(
            text = "关于换挡，正确的是？",
            options = listOf(
                Option("换挡是什么？", absurd = true, comment = "对，你不需要知道。"),
                Option("按车速逐级换挡"),
                Option("上坡降挡，下坡用低挡"),
                Option("自动挡不用管挡位")
            ),
            sass = "答得太专业了，这里禁止换挡。"
        ),
        Question(
            text = "刹车的作用是？",
            options = listOf(
                Option("我从来不用", absurd = true, comment = "刹车片表示很感动。"),
                Option("减速停车"),
                Option("紧急情况下制动"),
                Option("控制下坡车速")
            ),
            sass = "太绝对了。你应该说：我几乎不用。"
        ),
        Question(
            text = "雨天跟车距离应该？",
            options = listOf(
                Option("跟着前车尾灯就行", absurd = true, comment = "尾灯就是前车给你的承诺。"),
                Option("加大到平时的两倍"),
                Option("降低车速并拉开车距"),
                Option("保持三秒以上")
            ),
            sass = "答得这么安全，安全不属于这个考场。"
        ),
        Question(
            text = "遇到红灯应该？",
            options = listOf(
                Option("关掉碰撞模拟，直接过", absurd = true, comment = "没有碰撞，就没有违规。"),
                Option("停车等待"),
                Option("减速观察后停车"),
                Option("允许右转时右转")
            ),
            sass = "红灯对你来说只是一种装饰。"
        ),
        Question(
            text = "夜间会车应该？",
            options = listOf(
                Option("远光怼回去", absurd = true, comment = "谁先眨眼谁输。"),
                Option("切换近光灯"),
                Option("减速靠右行驶"),
                Option("关闭远光并观察右侧")
            ),
            sass = "近光灯？你太客气了。"
        ),
        Question(
            text = "通过人行横道应该？",
            options = listOf(
                Option("加速通过，路权在我", absurd = true, comment = "行人会理解你的。"),
                Option("减速让行"),
                Option("停车等行人通过"),
                Option("提前观察两侧")
            ),
            sass = "你居然让行？这里没有这一题。"
        ),
        Question(
            text = "车辆失控时应该？",
            options = listOf(
                Option("拉手刹漂移救车", absurd = true, comment = "漂移是唯一的救车方式。"),
                Option("松油门并稳住方向"),
                Option("点刹逐步减速"),
                Option("保持冷静，缓慢回正")
            ),
            sass = "冷静？这里不需要冷静，需要手刹。"
        ),
        Question(
            text = "什么是 ABS？",
            options = listOf(
                Option("不存在，捏死就完事了", absurd = true, comment = "自行车上从来没有 ABS，以前没有，以后也没有。"),
                Option("防抱死制动系统"),
                Option("自动刹车辅助"),
                Option("车身稳定系统的一部分")
            ),
            sass = "你答对了教科书，但答错了世界观。"
        ),
        Question(
            text = "方向盘的正确握法是？",
            options = listOf(
                Option("单手搭在 12 点，另一只手回消息", absurd = true, comment = "12 点握法，视野最好。"),
                Option("双手 9 点 3 点"),
                Option("双手 10 点 2 点"),
                Option("双手握持，不打手")
            ),
            sass = "标准握法？你太专业了。"
        )
    )

    /** 随机抽 count 题（不重复），并打乱每题选项顺序 */
    fun draw(count: Int): List<Question> =
        ALL.shuffled().take(count).map { it.shuffledOptions() }

    /** 占位用：理论上不会取到 null（每题都保证有荒诞选项） */
    fun fallbackComment(): String = "离谱即正确，你居然答得很稳。"

    @Suppress("unused")
    private fun randomIndex(size: Int): Int = Random.nextInt(size)
}
