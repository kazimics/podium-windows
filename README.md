![podium](/images/title_light.png#gh-light-mode-only)
![podium](/images/title_dark.png#gh-dark-mode-only)
---
![GitHub](https://img.shields.io/github/license/aimok04/podium?style=for-the-badge) ![GitHub release (latest by date)](https://img.shields.io/github/v/release/aimok04/podium?style=for-the-badge)

English | [中文](README_zh.md)

**podium** 是一个现代化的开源播客应用，面向 **Windows** 平台，使用 Kotlin 和 Compose Multiplatform 开发。
应用采用 **Material 3** 设计，使用 **vlcj** 进行媒体播放。

> [!CAUTION]
> 请注意，*podium* 仍在开发中，功能尚未完善，可能存在 Bug。

> [!NOTE]
> *podium* 仍缺少一些重要功能。
> 如果你有任何想法，请提交 Issue！

## Notable Features

- **Download** your favorite episodes for offline listening.
- **Discover** new podcasts on the *Discover* tab *(powered by Apple Podcasts)*.
- **Playback controls** - play/pause, seek, playback speed adjustment.
- **Sleep timer** - automatically pause after a set duration.
- **Queue management** - build and manage your playback queue.
- **OPML import/export** - transfer your subscriptions between apps.
- **History** - track your listening history.

## Installation

1. Clone the repository:
   ```
   git clone https://github.com/aimok04/podium.git
   cd podium-windows
   ```

2. Build and install:
   ```
   set JAVA_HOME="C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot"
   gradlew.bat :desktop:packageMsi
   ```

3. Run the MSI installer from `desktop/build/compose/binaries/main/msi/`

## Building from Source

### Prerequisites
- JDK 17+
- VLC Media Player installed (for media playback)

### Build Commands
```bash
# Run the app directly
gradlew.bat :desktop:run

# Build MSI installer
gradlew.bat :desktop:packageMsi

# Run tests
gradlew.bat :desktop:desktopTest
```

## Tech Stack

| Layer | Technology |
|-------|-----------|
| UI | Compose Multiplatform + Material 3 |
| Media | vlcj (libvlc JVM binding) |
| Database | JDBC SQLite |
| Network | Ktor |
| RSS Parsing | rssparser |
| Serialization | kotlinx.serialization |

## Project Structure

```
podium-windows/
├── shared/                    # Shared business logic (KMP)
├── desktop/                   # Windows desktop entry point
├── feature-home/              # Home feature module
├── feature-discover/          # Discover feature module
├── feature-player/            # Player feature module
├── feature-library/           # Library feature module
├── feature-settings/          # Settings feature module
├── build.gradle.kts           # Root build config
├── settings.gradle.kts        # Module config
└── gradle/                    # Gradle config
```

## License

[GNU General Public License v3.0](/LICENSE)
