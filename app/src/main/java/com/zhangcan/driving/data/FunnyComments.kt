package com.zhangcan.driving.data

import kotlin.random.Random

/** 结算页与各场景的搞笑评语 */
object FunnyComments {

    /** 理论考试的判定标准，展示在答题界面上 */
    const val ABSURD_RULE = "判定标准：离谱即正确，真实交规一律算错。"

    val GENERAL = listOf(
        "你的驾驶技术已经超越了物理引擎。",
        "建议直接把车推着走。",
        "这不是驾驶，这是行为艺术。",
        "BeamNG 看了都摇头。",
        "物理引擎已向平台投诉。",
        "交警正在加载中……",
        "副驾的安全带自己系上了。",
        "建议报考飞行器设计专业。",
        "你这不叫开车，叫随机漫步。",
        "轮胎表示：我尽力了。"
    )

    val RACE = listOf(
        "手刹是你唯一信任的部件。",
        "刹车片：你已经三个月没碰我了。",
        "第三人称视角看得很清楚，就是不看路。",
        "物理模拟 0%，责任感也是 0%。",
        "碰撞模拟关了，责任也关了。"
    )

    val BIKE = listOf(
        "让了，但没完全让。",
        "急弯 + 捏死刹车 = 空中转体。",
        "刹车健康值 0%，信仰值 100%。",
        "正弦波骑得很有诗意，方向毫无意义。",
        "圈刹异响已经传遍整条街。"
    )

    val THEORY = listOf(
        "理论满分，实操为零，非常典型的配置。",
        "这题你都会，那路上怎么不是这么开的？",
        "看来交通法规你只用在考试里。",
        "建议把驾照和这张卷子一起裱起来。"
    )

    /** 理论考试结算评语：按“答对了几道荒诞答案”给 */
    fun theoryComment(absurdCorrect: Int, total: Int): String = when {
        absurdCorrect >= total -> "满分。你和 ZhangCan 用同一个物理引擎思考。"
        absurdCorrect * 1.0 >= total * 0.75 -> "及格。荒诞已经很稳了，脑子里还剩一点正常知识，建议删掉。"
        absurdCorrect >= 1 -> "只答对 $absurdCorrect 道。你一半站在物理世界，一半站在 ZhangCan 世界。"
        else -> "零分。你太正常了，这个考场不属于你。"
    }

    fun pick(list: List<String>): String = list[Random.nextInt(list.size)]
}
