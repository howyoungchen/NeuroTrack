# NeuroTrack / 心迹

<div align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" width="96" alt="NeuroTrack 应用图标">
  <p><strong>看见变化，早点照顾自己。</strong></p>
  <p>一个安静、本地优先的 Android 自我观察工具，陪我留意康复期里的压力与睡眠变化。</p>
  <p><a href="README.md">English</a></p>
  <p>
    <a href="https://github.com/howyoungchen/NeuroTrack/releases/latest"><img src="https://img.shields.io/github/v/release/howyoungchen/NeuroTrack?style=flat-square&color=4B5290" alt="最新版本"></a>
    <img src="https://img.shields.io/badge/Android-8.0%2B-2F806D?style=flat-square" alt="Android 8.0 及以上">
    <img src="https://img.shields.io/badge/data-local--first-2F806D?style=flat-square" alt="数据本地优先">
  </p>
</div>

![NeuroTrack：观察压力趋势、睡眠节奏，并把数据留在本机](docs/images/neurotrack-readme-hero.svg)

<div align="center">
  <strong><a href="https://github.com/howyoungchen/NeuroTrack/releases/latest">下载最新版本</a></strong>
  ·
  <a href="https://github.com/howyoungchen/NeuroTrack/releases">查看全部版本</a>
</div>

## 实机界面

下面三张图来自 NeuroTrack v1.4 的实际运行界面，使用 Android 模拟器和虚构自测数据截取，不是设计稿，也不包含个人信息。

<table>
  <tr>
    <td width="33%"><img src="docs/images/screenshots/status.png" alt="NeuroTrack 状态页：压力等级与睡眠概览"></td>
    <td width="33%"><img src="docs/images/screenshots/assessment.png" alt="NeuroTrack 自测页：逐题完成过去一周自测"></td>
    <td width="33%"><img src="docs/images/screenshots/settings.png" alt="NeuroTrack 设置页：语言、主题、提醒与权限"></td>
  </tr>
  <tr>
    <td align="center"><strong>状态概览</strong><br><sub>压力、睡眠与趋势放在一起</sub></td>
    <td align="center"><strong>逐题自测</strong><br><sub>一次只关注一个问题</sub></td>
    <td align="center"><strong>设置与权限</strong><br><sub>每项本地数据权限都可见</sub></td>
  </tr>
</table>

## 我为什么做它

如果你经历过焦虑症或类似的神经症，你可能也有过这种感受：最难的不只是症状严重的那段时间，而是好起来以后，仍然不知道自己会不会又在不知不觉中滑回去。

对我来说，状态变差很少是一夜之间发生的。它通常早有迹象：连续几晚睡不好，身体莫名紧绷，又开始反复琢磨一些事，日常节奏一点点被打乱。可当我身处其中时，这些信号反而很容易被忽略。等我真正意识到，压力往往已经积累了一段时间。

我想要一个工具，在这些变化还很小的时候帮我把它们留下来：

- 偶尔问问我最近过得怎么样；
- 把主观感受和睡眠变化放到一起看；
- 只有当趋势真的值得注意时，再提醒我停一下、休息一下。

我没有找到完全符合这个想法的产品，所以自己写了 NeuroTrack。

我一直坚持一件事：**这个 App 本身不能成为新的压力来源。** 它不要求我每天打卡，不用连续天数绑住我，也不会用焦虑推动我打开它。它应该安静地待在手机里，在我需要看清状态时提供一点依据。

## 如果你也有这些需要

NeuroTrack 也许会适合你，如果你：

- 已经度过最难熬的阶段，但仍想更早察觉状态变化；
- 常常要到连续失眠、明显疲惫以后，才发现压力已经积累；
- 想看长期趋势，又不想每天维护一套复杂记录；
- 不愿把敏感的自测、睡眠和情绪相关数据交给云端账号；
- 希望提醒是克制的，而不是另一种催促。

它不会替你判断“有没有复发”，也不会告诉你该接受什么治疗。它只做一件更朴素的事：**帮你把容易忽略的变化放到眼前。**

## 它怎样陪我观察状态

| 我关心的事 | NeuroTrack 做什么 |
| --- | --- |
| 最近主观感受怎么样 | 用 10 道题完成一次过去一周自测，通常一两分钟 |
| 压力是不是在积累 | 把自测与可用的睡眠信号合成 0–10 的压力等级，并展示月度趋势 |
| 作息有没有悄悄变化 | 根据屏幕交互时间戳推断睡眠时长、入睡和起床时间，观察近一周与近一个月的节奏 |
| 什么时候需要提醒 | 只有压力等级超过 5 时才发出预警；最近 7 天没有自测时，最多按设定每周提醒一次 |
| 我能不能带走数据 | 日志和睡眠原始数据都由我主动选择时间范围后手动导出 |

支持中文与 English，也支持跟随系统、浅色和深色主题。

## 实际用起来很简单

```mermaid
flowchart LR
    A["每周花一两分钟<br/>完成一次自测"] --> B["可选开启睡眠观察<br/>只读取必要时间信号"]
    B --> C["在状态页查看<br/>压力、睡眠与月度趋势"]
    C --> D{"压力是否明显升高？"}
    D -- "没有" --> E["保持安静<br/>继续生活"]
    D -- "超过阈值" --> F["温和提醒<br/>给自己留出恢复时间"]
```

1. 安装并打开 App，在 **自测** 页完成第一次记录。
2. 如果希望观察睡眠，在 **设置** 中授予使用情况访问权限；定位权限只是可选的辅助信号。
3. 回到 **状态** 页查看压力等级、最近睡眠和较长时间的趋势。
4. 然后去过自己的生活。需要注意时，它再出声。

它不是一件要每天“经营”的东西。对我来说，用得越省心，才越可能长期留下来。

## 我刻意没有做的事

- **没有账号系统**：不需要手机号、邮箱或登录。
- **没有联网功能**：应用清单没有申请互联网权限，App 本身不会把记录发到服务器。
- **没有打卡排名**：没有连续天数、积分、排行榜或催促式文案。
- **没有医疗结论**：分数只用于自我观察，不代表诊断，也不能替代专业判断。
- **没有默认读取一切**：睡眠观察和定位辅助都由你决定是否开启。

## 隐私与权限

这类记录很敏感，所以我把功能做成本地优先：

- 自测、睡眠记录、屏幕事件和运行日志保存在本机数据库中；
- 睡眠观察使用的是系统提供的屏幕交互时间戳，不读取屏幕内容、消息或其他 App 里的内容；
- 可选的粗略定位只读取设备已有的本地移动信号，用来辅助判断起床边界；NeuroTrack 不保存原始位置轨迹；
- NeuroTrack 本身不会上传记录；只有你主动点击导出时，App 才会把所选数据交给系统分享面板。Android 的系统备份行为仍取决于你的设备设置；
- App 不申请互联网权限，也没有分析统计、广告 SDK 或云端账号。

为了实现这些功能，Android 可能会询问以下权限：

| 权限 | 用途 | 是否可选 |
| --- | --- | --- |
| 通知 | 每周自测提醒与压力偏高预警 | 可选 |
| 使用情况访问 | 回查屏幕交互时间，推断过去 24 小时的睡眠状态 | 睡眠观察需要 |
| 粗略定位 | 用本地移动信号辅助判断起床边界，不保存轨迹 | 可选 |
| 开机启动 | 手机重启后恢复本地定时任务 | 自动使用 |
| 忽略电池优化 / 精确闹钟 | 提高后台分析和提醒的稳定性 | 可选 |

## 下载与安装

1. 前往 [Releases](https://github.com/howyoungchen/NeuroTrack/releases/latest) 下载最新的 <code>.apk</code>。
2. 如果 Android 提示，请允许浏览器或文件管理器“安装未知应用”。
3. 打开 APK 完成安装。

NeuroTrack 支持 Android 8.0（API 26）及以上版本。GitHub Release 中提供的 APK 使用项目发布密钥签名；从旧版本升级时请继续使用这里发布的安装包。

## 一个重要的提醒

NeuroTrack 是我用来观察自身变化的辅助工具，**不是医疗软件，也不能替代医生、心理咨询师或紧急援助。**

如果你正在经历严重症状、状态持续恶化，或者有伤害自己的想法，请不要等待 App 给出提示，尽快联系专业人士、可信任的人或当地紧急救助服务。照顾好自己永远比记录完整更重要。

## 从源码构建

需要 Android Studio 2025.3+、JDK 17+（AGP 9.2.1，Min SDK 26 / Target SDK 36）：

```powershell
.\gradlew.bat assembleDebug
```

提 PR 前建议运行：

```powershell
.\gradlew.bat :app:compileDebugKotlin
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:testDebugUnitTest
```

## 谢谢你，也欢迎你

谢谢每一个用过这个 App、分享过感受、提过建议，或者只是读到这里的人。

如果你也关心恢复期的自我观察、隐私友好的工具，或者愿意写一点 Android 代码，非常欢迎参与。你可以[提交问题或建议](https://github.com/howyoungchen/NeuroTrack/issues)，也可以改文案、优化睡眠推断、补测试或完善翻译。

我只有一个请求：这个项目关乎正在努力恢复的人。请保持善意，避免污名化表达，也请谨慎对待任何听起来像医疗建议的说法。

## 开源协议

本项目使用 [NeuroTrack Noncommercial License](LICENSE)。

个人使用、学习、研究、修改与非商业分发均被允许；商业使用需要事先获得书面授权。
