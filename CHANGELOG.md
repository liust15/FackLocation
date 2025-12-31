# 更新日志 (Changelog)

## [Unreleased] - 2025-12-31

### 修复 (Fixed)

-   **构建系统**:
    -   将 Android Gradle Plugin (AGP) 版本从不稳定的 `8.13.2` 降级到稳定的 `8.7.2`。
    -   将 Gradle Wrapper 版本同步更新至 `8.7`，与 AGP 版本保持兼容。
    -   将 `compileSdk` 和 `targetSdk` 从 `36` 调整为更稳定且广泛兼容的 `34`。
    -   将 Java 编译级别从 `11` 提升至 `17`，以满足新版 AGP 的要求。
-   **依赖管理**:
    -   移除了 `app/build.gradle` 中重复声明的 `androidx.appcompat:appcompat` 依赖，统一通过 `libs.versions.toml` 进行版本管理。
-   **Xposed Hook 稳定性**:
    -   在 `HookMain.java` 中，为 `WifiManager.getScanResults()` 的 Hook 返回一个安全的空列表 (`Collections.emptyList()`) 而不是 `null`，避免了目标应用在遍历结果时发生 `NullPointerException`。
    -   对 `TelephonyManager.getCellLocation()` 的 Hook 逻辑进行了优化，仅记录日志而不返回 `null`，以防止部分未做空检查的应用崩溃。
    -   移除了对 `Location.class` 的重复 Hook，只保留通过类名字符串和 `ClassLoader` 的方式，使逻辑更清晰。
    -   为所有 Hook 方法和文件读写操作增加了 `try-catch` 块，防止因意外异常导致整个 Hook 失效或目标进程崩溃。
-   **代码健壮性**:
    -   在 `RealLocationCollector.java` 中增加了多重安全检查，包括对 `Context`、回调、系统服务 (`LocationManager`, `WifiManager`) 的空指针判断，以及对 API 调用（如 `isProviderEnabled`, `getScanResults`）的异常捕_create_捕捉，显著提升了真实位置采集的稳定性。

### 新增 (Added)

-   **文档**:
    -   创建了 `README.md` 文件，详细说明了应用的功能、前提条件、使用方法和配置文件路径。
    -   创建了本 `CHANGELOG.md` 文件，用于记录版本变更。
