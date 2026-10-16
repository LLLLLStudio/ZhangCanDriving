package com.zhangcan.driving.data

/**
 * 理论考试题库。
 * 每题 25 分，随机抽 4 题组成一套卷子。
 */
object QuestionBank {

    data class Question(
        val text: String,
        val options: List<String>,
        /** 正确答案下标（本项目的“正确”以公路法规和常识为准） */
        val answer: Int,
        /** 选到这个下标会触发的成就 id（恶搞彩蛋） */
        val achievementOnOption: Map<Int, String> = emptyMap(),
        /** 答错时的吐槽 */
        val sass: String = "答案不对，但很有你的风格。"
    )

    val ALL: List<Question> = listOf(
        Question(
            text = "WRC Generation 的物理是否真实如 BeamNG？",
            options = listOf("是", "不是", "只有不碰撞时是", "我从不开碰撞模拟"),
            answer = 1,
            sass = "你居然承认了？这不像你。"
        ),
        Question(
            text = "BeamNG 和 WRC Generation，哪个物理更真实？",
            options = listOf("BeamNG", "WRC Generation", "都不如我的驾驶", "物理是什么"),
            answer = 0,
            achievementOnOption = mapOf(1 to "A1"),
            sass = "BeamNG 看了都摇头。"
        ),
        Question(
            text = "手动挡起步应该？",
            options = listOf("油离配合", "半联动", "直接 W 到底", "换挡是什么？"),
            answer = 0,
            sass = "起步三点头，坐稳了。"
        ),
        Question(
            text = "正确驾驶视角？",
            options = listOf("第一人称", "第三人称", "引擎盖", "永远第三人称"),
            answer = 0,
            sass = "朋友，第三人称是看电影用的。"
        ),
        Question(
            text = "曲线行驶正确操作？",
            options = listOf("刹车减速", "手刹转向", "松油门", "关闭物理模拟"),
            answer = 2,
            sass = "你选了手刹？请回实操考试。"
        ),
        Question(
            text = "雨天跟车距离应该？",
            options = listOf("加大", "减小", "跟着前车尾灯就行", "车距是什么"),
            answer = 0,
            sass = "你贴得比后视镜还近。"
        ),
        Question(
            text = "遇到停止线红灯应该？",
            options = listOf("停车等待", "减速通过", "手刹漂移过弯", "关掉碰撞模拟"),
            answer = 0,
            sass = "红灯对你来说只是一种装饰。"
        ),
        Question(
            text = "刹车的作用是？",
            options = listOf("减速停车", "转向辅助", "装饰", "我从来不用"),
            answer = 0,
            sass = "曲线行驶里你确实不用。"
        ),
        Question(
            text = "夜间会车应该？",
            options = listOf("切换近光灯", "远光怼回去", "开双闪", "蒙上眼睛"),
            answer = 0,
            sass = "对面的司机已经看不见路了。"
        ),
        Question(
            text = "通过人行横道应该？",
            options = listOf("减速让行", "加速通过", "按喇叭示意", "关掉物理模拟"),
            answer = 0,
            sass = "行人表示很感动。"
        ),
        Question(
            text = "关于换挡，正确的是？",
            options = listOf("按车速换挡", "一直 1 挡", "换挡键是隐藏的", "车会自己换"),
            answer = 0,
            sass = "你不需要知道换挡。"
        ),
        Question(
            text = "车辆失控时应该？",
            options = listOf("松油门、稳住方向", "猛打方向", "拉手刹漂移救车", "打开第三人称"),
            answer = 0,
            sass = "拉手刹救车，经典操作。"
        )
    )

    /** 随机抽 count 题（不重复） */
    fun draw(count: Int): List<Question> = ALL.shuffled().take(count)
}
