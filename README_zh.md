![podium](/images/title_light.png#gh-light-mode-only)
![podium](/images/title_dark.png#gh-dark-mode-only)
---
![GitHub](https://img.shields.io/github/license/aimok04/podium?style=for-the-badge) ![GitHub release (latest by date)](https://img.shields.io/github/v/release/aimok04/podium?style=for-the-badge)

[English](README.md) | 中文

**podium** 是一个现代化的开源播客应用，面向 **Windows** 平台，使用 Kotlin 和 Compose Multiplatform 开发。
应用采用 **Material 3** 设计，使用 **vlcj** 进行媒体播放。

> [!CAUTION]
> 请注意，*podium* 仍在开发中，功能尚未完善，可能存在 Bug。

> [!NOTE]
> *podium* 仍缺少一些重要功能。
> 如果你有任何想法，请提交 Issue！

## 主要功能

- **下载** 你喜欢的单集，支持离线收听。
- **发现** 新播客 — 在 *Discover* 标签页浏览 *(由 Apple Podcasts 提供支持)*。
- **播放控制** — 播放/暂停、快进快退、播放速度调节。
- **睡眠定时器** — 设定时间后自动暂停播放。
- **队列管理** — 创建和管理你的播放队列。
- **OPML 导入/导出** — 在不同应用之间迁移订阅。
- **播放历史** — 记录你的收听历史。

## 安装

1. 克隆仓库：
   ```
   git clone https://github.com/aimok04/podium.git
   cd podium-windows
   ```

2. 构建并安装：
   ```
   set JAVA_HOME="C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot"
   gradlew.bat :desktop:packageMsi
   ```

3. 运行 MSI 安装包：`desktop/build/compose/binaries/main/msi/`

## 从源码构建

### 环境要求
- JDK 17+
- 安装 VLC Media Player（用于媒体播放）

### 构建命令
```bash
# 直接运行应用
gradlew.bat :desktop:run

# 构建 MSI 安装包
gradlew.bat :desktop:packageMsi

# 运行测试
gradlew.bat :desktop:desktopTest
```

## 技术栈

| 层级 | 技术 |
|------|------|
| UI | Compose Multiplatform + Material 3 |
| 媒体播放 | vlcj (libvlc JVM 绑定) |
| 数据库 | JDBC SQLite |
| 网络 | Ktor |
| RSS 解析 | rssparser |
| 序列化 | kotlinx.serialization |

## 项目结构

```
podium-windows/
├── shared/                    # 共享业务逻辑 (KMP)
├── desktop/                   # Windows 桌面入口
├── feature-home/              # 首页功能模块
├── feature-discover/          # 发现功能模块
├── feature-player/            # 播放器功能模块
├── feature-library/           # 库功能模块
├── feature-settings/          # 设置功能模块
├── build.gradle.kts           # 根构建配置
├── settings.gradle.kts        # 模块配置
└── gradle/                    # Gradle 配置
```

## 许可证

[GNU General Public License v3.0](/LICENSE)
