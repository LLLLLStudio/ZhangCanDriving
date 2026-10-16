# ZhangCan Driving

一个用来讽刺朋友神秘驾驶技术的纯娱乐小游戏。原生 Android + Kotlin + Canvas 伪 3D，单机、离线、无广告、无内购。

> 核心不是画面，而是：荒诞规则、搞笑文案、扣分弹幕、成就系统。

## 截图 / GIF

> 占位：截图待补充
>
> | 主菜单 | 赛车实操 | 自行车 | 结算 |
> |---|---|---|---|
> | ![menu](docs/menu.png) | ![race](docs/race.png) | ![bike](docs/bike.png) | ![result](docs/result.png) |

## 功能

- 赛车：理论考试（随机 4 题 × 25 分，≥ 60 分解锁实操）
- 赛车：实操考试（直线行驶 / 曲线行驶 / 物理模拟 0%）
- 自行车：神秘骑行（正弦轨迹 / 圈刹异响 / 抱死摔车）
- 成就系统（8 个成就，SharedPreferences 本地持久化）
- 伪 3D 俯视滚动道路（梯形透视 + 滚动中心线）
- 结算页：总分 + 扣分明细 + 搞笑评语 + 解锁成就

## 下载

前往 [Releases](../../releases) 下载最新 APK：`ZhangCanDriving-vX.Y.Z.apk`。

## 安装

1. 下载 APK
2. 允许“安装未知来源应用”
3. 安装并开始

支持 Android 8.0（API 26）及以上，竖屏锁定，离线可玩，APK 体积 < 20MB。

## 玩法

### 赛车 · 理论考试

- 随机 4 题，单选 / 判断，每题 25 分，满分 100
- ≥ 60 分解锁「赛车 · 实操考试」

### 赛车 · 实操考试

- **直线行驶**：每 2 秒检测一次方向盘，必须出现「左打 > 90° 且 右打 > 90°」，否则扣 20，弹字：太稳了，这不像你。
- **曲线行驶**：刹车输入禁用，按下扣 50；只能用手刹转向，拉手刹时横向速度 ×2、后轮抓地力归零。提示：禁止刹车。ZhangCan 只用手刹。
- **物理模拟 0**：不做碰撞检测，车直接穿模，角落常驻「物理模拟：0%」「碰撞模拟：已按偏好关闭」。
- **严禁手动换挡**：无换挡 UI；触发隐藏换挡键（点右上角车速表）扣 100。
- **永远第三人称**：视角固定，尝试切换视角 3 次解锁成就。

### 自行车 · 神秘骑行

- **刹车永远不保养**：健康值恒为 0，只用圈刹 / 罗拉刹，刹车时随机「吱——嘎——」，没有 ABS。
- **无法保持直线**：目标轨迹 y = A·sin(ωt)，要求 A > 3m；太直扣 40，振幅不够扣 30。
- **让道不让速**：横向避开障碍且未减速加分，撞上扣分。结算语：让了，但没完全让。
- **永远不会手捏 ABS**：长按刹车直接抱死 → 侧滑 → 摔车。
- **急过弯摔车**：速度 > 阈值 且 转向 > 阈值 且 刹车按下 → 直接摔。结算语：急弯 + 捏死刹车 = 空中转体。

### 成就

| ID | 名称 | 触发条件 |
|----|------|----------|
| A1 | WRC 物理学家 | 理论考试选“BeamNG 不如 WRC” |
| A2 | 从不换挡 | 实操考试全程未按换挡键 |
| A3 | 手刹漂移大师 | 曲线行驶全程只用手刹 |
| A4 | 圈刹信仰 | 刹车异响持续 10 秒 |
| A5 | 让道不让速 | 成功让道且未减速 |
| A6 | 正弦波骑士 | 保持 A > 3m 正弦行驶 10 秒 |
| A7 | 捏死 ABS | 刹车抱死导致摔车 |
| A8 | 第一人称？不存在的 | 尝试切换视角 3 次 |

## 项目结构

```text
ZhangCanDriving/
├── .github/workflows/release.yml      # Tag 触发自动打包 + Release
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/zhangcan/driving/
│       │   ├── MainActivity.kt        # 竖屏 + 全屏 + 返回键
│       │   ├── GameView.kt            # SurfaceView + 渲染线程
│       │   ├── scene/                 # Menu / Theory / Race / Bike / Result / Achievement
│       │   ├── core/                  # GameLoop / InputState / ScoreManager / AchievementManager / Fake3DRoad / Ui / Game
│       │   └── data/                  # QuestionBank / FunnyComments
│       └── res/                       # strings / colors / themes / 图标
├── 需求.md                            # PRD
└── README.md
```

## 构建

本地：

```bash
export JAVA_HOME=/path/to/jdk17
./gradlew assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

测试与静态检查：

```bash
./gradlew testDebugUnitTest   # Robolectric：18 条用例覆盖全部荒诞规则
./gradlew lintDebug           # Android Lint
```

CI：

- 推送 `v*` Tag 触发 `.github/workflows/release.yml`
- 自动编译 debug APK、创建 Release、上传 APK 资产
- 也支持手动 `workflow_dispatch` 只出构建产物

## 发布

```bash
git tag -a v1.0.0 -m "首个版本：理论考试 + 赛车实操 + 自行车 + 成就"
git push origin v1.0.0
```

GitHub Actions 会自动编译并发布 Release，Release 标题为 Tag 名，说明优先使用 Tag 注释。

版本号规则：`v主版本.次版本.修订号`（v1.0.0、v1.0.1、v1.1.0）。

## 免责声明

- 本项目为纯娱乐用途，不针对任何真实个人
- 不使用真实姓名、照片或隐私信息；公开传播前建议将 ZhangCan 替换为化名
- 游戏中的“规则”全部是反讽，**现实中请遵守交通法规，不要危险驾驶**
- 请勿将游戏内容用于公开羞辱或传播他人隐私
