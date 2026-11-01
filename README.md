<p align="center">
  <img src="assets/branding/app_icon.png" width="96" alt="花迹应用图标" />
</p>

# 花迹 · SpendTrace

一个极简的 Android 原生支出记账 App。打开就能记一笔，数据只留在本机：没有账号、没有网络请求、没有云同步。

- 三个页签：账单（按月看每日记录）、统计（分类占比）、设置（主题、备份、关于）。
- 数据保存在本机 Room 数据库（SQLite），金额全链路以整数「分」存储。
- 备份是一份纯文本 JSON，导出/导入都走系统文件选择器，不申请存储权限。
- 应用**未声明任何权限**，Manifest 里没有 `INTERNET` 等条目。

作者 [sifan077](https://github.com/sifan077) · 仓库 [github.com/sifan077/SpendTrace](https://github.com/sifan077/SpendTrace)

## 功能

### 账单页（首页）

- 顶部品牌头（花迹 / 记录每一笔花销的痕迹）与设置入口。
- 月度汇总卡：本月总支出（大号金额）、今日支出、日均支出；查看历史月份时「今日支出」换成「笔数」。
- 记录列表按天倒序分组，组头显示「今天 / 昨天 / 10月28日 周三」与当天合计；单条显示分类图标、分类名、备注（无备注时显示时间）与前置减号的金额。
- 月份胶囊 `‹ 2026年10月 ›`，不能切到未来月份；查看历史月时胶囊内出现「本月」快捷返回。
- 空状态对当前月与历史月给出不同文案；数据库读取失败时显示错误卡片 + 「重试」。
- 点击记录进入编辑，长按或弹窗内「删除这笔记录」可删除，均需二次确认。
- 右下角悬浮按钮「＋ 记一笔」。

### 记一笔 / 编辑

- 底部弹窗内一次完成，不跳页；编辑态表单预填原值并额外提供删除入口。
- 金额居中大号输入，`¥` 与数字整体居中，实时过滤非法字符；校验「大于 0、最多两位小数、整数位最多 9 位」。
- 分类为 4 列分类格，新增时默认选中「上一次使用的分类」（跨进程记忆）。
- 日期 / 时间默认当前时间，可分别用 Material 3 日期选择器、时间选择器修改。
- 备注可选，最多 60 字。
- 保存后自动关闭弹窗；若记录落在其他月份，首页会自动切到该月份。
- 表单用 `rememberSaveable` 保存，旋转屏幕不丢输入。

### 统计页

- 本月累计支出 + 「共 N 笔 · 日均 ¥x」。
- 分类占比按金额倒序，每行显示图标、分类名、占比（一位小数）、金额，以及自绘进度条（未引入任何图表库）。
- 与账单页共用同一个月份状态，切月两边同步。

### 设置页

- 外观：主题模式「跟随系统 / 浅色 / 深色」，即时生效并持久化，状态栏与导航栏图标明暗同步。
- 分类：7 个默认分类一览（只读），说明暂不支持自定义。
- 本地备份：导出 / 导入 JSON 备份，见[备份格式](#备份格式)。
- 关于：版本、数据存储、作者、开源仓库（可点击跳转）、版权行。

### 备份与恢复

- 导出：把全部分类与记录序列化成 `spendtrace-backup-yyyyMMdd-HHmm.json`，通过 `CreateDocument` 让用户自选保存位置。
- 导入：`OpenDocument` 选文件 → 完整校验 → 弹窗展示备份时间、记录数、分类数与合计金额 → 确认后整体覆盖。
- 校验先于写入，任一项不合法直接报错，现有数据不受影响。
- 恢复在同一个 Room 事务内完成（先删记录再删分类，避开外键 RESTRICT），失败自动回滚，并清掉「上次使用的分类」记忆。
- 单文件上限 16 MB、最多 20 万条记录，读取时边读边判大小，避免超大文件占满内存。

## 技术栈

| 用途 | 方案 |
|---|---|
| 语言 | Kotlin 2.2.10（AGP 内置，不再 apply `kotlin-android` 插件） |
| UI | Jetpack Compose + Material 3（BOM 2026.09.00，material3 1.4.0） |
| 状态 | ViewModel + StateFlow / SharedFlow（lifecycle 2.11.0） |
| 异步 | Coroutines + Flow（`flatMapLatest` 切月，旧查询自动取消） |
| 持久化 | Room 2.8.5 + KSP 生成代码 |
| 构建 | AGP 9.3.3 + Gradle 9.5.0 + JDK 17 |
| 文件导入导出 | 系统文件选择器（SAF，`CreateDocument` / `OpenDocument`） |
| 测试 | JUnit 4.13.2 + AndroidX Test 1.3.0 |

刻意**没有**引入：Hilt、Retrofit、Navigation-Compose、图表库、material-icons-extended。依赖注入用一个十几行的 `AppContainer` 解决，页面切换用一个 `selectedTab` 状态解决，分类图标是自绘 vector。

## 项目结构

```text
app/src/main/java/com/sifan/spendtrace/
  AppInfo.kt                     作者 / 仓库 / 版权信息
  MainActivity.kt                setContent、edge-to-edge、主题模式入口
  SpendTraceApplication.kt       Application + AppContainer（手写依赖容器）
  core/
    Money.kt                     金额解析 / 格式化（以「分」为单位）
    DateFormats.kt               日期文案、月份区间
  data/
    AppPreferences.kt            上次使用的分类、主题模式
    ThemeMode.kt                 主题枚举
    backup/
      BackupService.kt           备份 JSON 序列化、校验、事务式恢复
      BackupFiles.kt             SAF 文件读写（Uri → bytes / text）
    local/
      SpendTraceDatabase.kt      Room 数据库定义
      DefaultCategories.kt       7 个默认分类
      SeedDefaultCategoriesCallback.kt
      entity/                    ExpenseEntity / CategoryEntity
      dao/                       ExpenseDao / CategoryDao
    model/Models.kt              Category / Expense / ExpenseDraft
    repository/                  ExpenseRepository / CategoryRepository
  domain/
    ExpenseStats.kt              月度统计纯函数（总额 / 日均 / 分组 / 分类占比）
  ui/
    SpendTraceApp.kt             Scaffold、底部三页签、FAB、弹窗挂载
    home/                        HomeScreen + SpendTraceViewModel
    stats/                       StatsScreen
    settings/                    SettingsScreen
    components/                  品牌头、月份胶囊、汇总卡、记账弹窗、分类图标、空状态
    theme/                       Color / Theme / Type

app/src/main/res/
  drawable/                      分类图标、空状态、页签、设置项图标（全部自绘 vector）
  mipmap-anydpi/                 自适应启动图标（背景 / 前景 / monochrome）
  values/                        中文字符串、主题、颜色
app/src/test/                    单元测试（金额、统计）
app/src/androidTest/             仪器测试（Room CRUD、默认分类）
assets/branding/                 README 用图标（由自适应图标导出）
gradle/libs.versions.toml        版本目录
```

分层是 UI → ViewModel → Repository → Room，依赖单向向下：`SpendTraceViewModel` 持有唯一的月份状态，`ExpenseStats` 是无 Android 依赖的纯函数模块，Repository 只做 Entity ↔ 领域模型映射和月份区间计算。

## 数据模型

数据库 `spendtrace.db`（version 1，`exportSchema = false`）。

`expenses` — 支出记录：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | INTEGER PK AUTOINCREMENT | 主键 |
| `amountMinor` | INTEGER NOT NULL | 金额，单位「分」，避免浮点误差 |
| `categoryId` | INTEGER NOT NULL | 外键 → `categories.id`，`onDelete = RESTRICT` |
| `note` | TEXT NULL | 备注，空串写入前统一转成 NULL |
| `spentAt` | INTEGER NOT NULL | 消费时间（epoch millis） |
| `createdAt` / `updatedAt` | INTEGER NOT NULL | 创建 / 最后修改时间 |

索引：`spentAt`（按月区间查询）、`categoryId`（外键）。

`categories` — 分类：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | INTEGER PK | 默认分类固定为 1..7 |
| `name` | TEXT | 分类名 |
| `icon` | TEXT | 图标标识（映射到内置矢量图） |
| `color` | INTEGER | ARGB 颜色值 |
| `sortOrder` | INTEGER | 展示顺序 |
| `isActive` | INTEGER(bool) | 预留：停用分类而不删除 |

分类是独立表而不是枚举，`sortOrder` / `isActive` 已预留，后续加「自定义分类」只需增加写入入口，不用改表结构。首次建库时由 `SeedDefaultCategoriesCallback` 以 `INSERT OR IGNORE` 写入 7 个默认分类（餐饮 / 交通 / 购物 / 日用 / 娱乐 / 医疗 / 其他），保证幂等。

## 计算规则

```text
月份区间   = [当月 1 日 00:00, 次月 1 日 00:00)（左闭右开）
日均支出   = 当月总支出 / 天数
             当前月按「已过天数」，历史月按「整月天数」，四舍五入到分
今日支出   = 本地日期等于今天的记录金额之和
按天分组   = 先按本地日期分组，天内按时间倒序，再按日期倒序；组内合计同步算出
分类占比   = 该分类金额 / 当月总支出（显示保留一位小数）
分类排序   = 金额倒序，金额相同按 sortOrder
```

- 月份区间左闭右开，避免月末边界重复或遗漏。
- 所有统计在读取时基于数据库真实记录动态计算，不写入数据库。
- 金额全链路使用 `Long` 存「分」，只在输入 / 展示时转换；解析与格式化集中在 `core/Money.kt`。

## 备份格式

备份是 UTF-8 JSON，默认文件名 `spendtrace-backup-yyyyMMdd-HHmm.json`：

```json
{
  "format": "spendtrace-backup",
  "formatVersion": 1,
  "appVersion": "1.0",
  "exportedAt": "2026-11-01T07:30:00Z",
  "categoryCount": 7,
  "expenseCount": 2,
  "totalAmountMinor": 4500,
  "categories": [
    { "id": 1, "name": "餐饮", "icon": "food", "color": -2060956, "sortOrder": 0, "isActive": true }
  ],
  "expenses": [
    { "id": 1, "amountMinor": 2500, "categoryId": 1, "note": "午饭",
      "spentAt": 1761982200000, "createdAt": 1761982200000, "updatedAt": 1761982200000 }
  ]
}
```

导入时的保护：

- 限制单文件 16 MB、分类 200 条、记录 20 万条，边读边判大小。
- 校验格式标识、版本号范围、清单数量与实际条数是否一致。
- 校验分类 ID 唯一性、名称 / 图标长度、颜色与排序值。
- 校验记录的分类外键存在、金额范围、备注长度、时间戳是否合法。
- 任一校验失败都不会触碰现有数据；通过后在同一事务内全量覆盖（不合并）。

## 运行

环境要求：JDK 17、Android SDK（compileSdk / targetSdk 37，minSdk 30）、Android 设备或模拟器（API 30+）。

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk

./gradlew assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
```

### 发布签名

签名信息放在根目录 `keystore.properties`，该文件已在 `.gitignore` 中，不会进仓库：

```properties
storeFile=/absolute/path/to/your.jks
storePassword=******
keyAlias=******
keyPassword=******
```

文件存在时 release 自动使用该签名，产物为 `app-release.apk`；文件不存在时输出 `app-release-unsigned.apk`，不影响其他构建。

### 包体积

| 构建类型 | 大小 | 说明 |
| --- | --- | --- |
| `debug` | ≈ 31 MB | 含调试工具、未压缩资源，仅用于开发 |
| `release` | **2.6 MB** | R8 代码压缩 + 资源压缩 + 语言过滤 |

release 包构成：dex ≈ 2.4 MB（Compose 运行时为主）、`resources.arsc` ≈ 300 KB、`res/` ≈ 40 KB、native 库 ≈ 37 KB（4 个 ABI）。压缩手段是 `optimization.enable = true`（R8）、`isShrinkResources = true`、`androidResources.localeFilters = ["zh"]`（剔除三方库其他语言翻译，省约 280 KB）、`dependenciesInfo.includeInApk = false`。

### AGP 9 注意事项

1. AGP 9 默认启用内置 Kotlin，工程里**不要**再 apply `org.jetbrains.kotlin.android`，否则报 extension 冲突。
2. Compose 编译器插件仍需显式 apply（`org.jetbrains.kotlin.plugin.compose`，版本与 Kotlin 一致）。
3. KSP 需 **≥ 2.3.12**：低版本通过 `kotlin.sourceSets` 注册生成源码，会触发 `Using kotlin.sourceSets DSL ... not allowed with built-in Kotlin`。
4. 低内存机器可调小 `gradle.properties` 里的 `kotlin.daemon.jvmargs`（默认已设为 `-Xmx1536m`）。

## 测试

| 文件 | 类型 | 覆盖点 |
| --- | --- | --- |
| `MoneyTest` | JVM 单测 | 金额解析（0、负数、三位小数、多位整数、空串）、输入过滤、千分位格式化 |
| `ExpenseStatsTest` | JVM 单测 | 月度合计、今日合计、当前月与历史月日均、按天分组排序、分类占比与排序 |
| `ExpenseDaoTest` | 仪器测试 | 新增并带分类查询、月份区间过滤、更新、删除 |
| `SpendTraceDatabaseTest` | 仪器测试 | 首次建库写入 7 个默认分类 |

```bash
./gradlew testDebugUnitTest          # 13 个 JVM 单测
./gradlew connectedDebugAndroidTest  # 5 个仪器测试（需连接设备/模拟器）
```

## 构建与运行验证

在本开发环境（Windows + JDK 17）实际执行结果：

```text
./gradlew testDebugUnitTest   13 tests passed（MoneyTest 5 + ExpenseStatsTest 8）
./gradlew assembleDebug      成功，app-debug.apk ≈ 31 MB
./gradlew assembleRelease    成功，app-release.apk 2.6 MB（已签名）
```

仪器测试需要设备 / 模拟器，未在本机执行。

## 已实现与未实现

已实现：

- 三个页签：账单、统计、设置。
- 记账、编辑、删除（二次确认），按天分组与月度汇总。
- 月份切换（不可切到未来），当前月与历史月的日均口径区分。
- 分类占比统计与自绘进度条。
- 7 个默认分类、上次使用分类记忆。
- 浅色 / 深色 / 跟随系统主题并持久化。
- JSON 备份导出 / 导入，完整性校验 + 事务式恢复。
- JVM 单测与 Room 仪器测试。

第一版明确不做：

- 自定义分类的增删改（数据表已预留 `sortOrder` / `isActive`）。
- 收入记账、预算、多币种、账单导出（CSV / Excel）。
- 跨设备同步、账号体系（按设计不做云端）。
- 月份状态持久化：冷启动总是回到当前月。
- 侧滑删除（目前只有长按列表与编辑弹窗两个入口）。
- 图表库、趋势分析、订阅 / 周期性支出、通知提醒。

## 隐私

应用没有后端和账号系统，也不调用任何远程接口，Manifest 中未声明任何权限（包括 `INTERNET`）。支出记录、分类和主题偏好全部保存在本机；备份文件只在用户主动选择保存位置时写入，导入时也只读取用户选中的那个文件。
