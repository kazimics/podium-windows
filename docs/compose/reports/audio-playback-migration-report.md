# Audio Playback Module — Migration Analysis Report

**Date:** 2026-06-29  
**Scope:** desktop/src/desktopMain/kotlin/app/podiumpodcasts/podium/desktop/player/  
**Goal:** Migrate from Java Sound + Rubberband engine to libmpv

---

## 1. All Audio-Playback-Related Classes

| # | Class | File | Role |
|---|-------|------|------|
| 1 | `MediaPlayerState` | `MediaPlayerState.kt` | Compose 状态管理器，UI 层唯一入口 |
| 2 | `AudioPlayer` | `AudioPlayer.kt` | **当前主播放引擎** — Rubberband + Java Sound |
| 3 | `RubberbandPlayer` | `RubberbandPlayer.kt` | 替代播放引擎（被 JfxMediaPlayer 包装） |
| 4 | `JfxMediaPlayer` | `JfxMediaPlayer.kt` | RubberbandPlayer 的包装器（遗留代码） |
| 5 | `PitchPlayer` | `PitchPlayer.kt` | 已废弃 — 使用 WSOLA 变速 |
| 6 | `AudioDecoder` | `AudioDecoder.kt` | 解码器接口 |
| 7 | `Mp3Decoder` | `Mp3Decoder.kt` | MP3 解码（JLayer） |
| 8 | `M4aDecoder` | `M4aDecoder.kt` | M4A/AAC 解码（FFmpeg 子进程） |
| 9 | `SpiDecoder` | `SpiDecoder.kt` | 多格式解码（Java Sound SPI + JAADec） |
| 10 | `JAADecAudioDecoder` | `JAADecAudioDecoder.kt` | AAC 解码（JAADec 反射调用） |
| 11 | `RubberbandApi` | `RubberbandApi.kt` | JNA 接口 → rubberband.dll |
| 12 | `RubberbandStretcher` | `RubberbandStretcher.kt` | RubberbandApi 的 Kotlin 封装 |
| 13 | `RubberbandNativeLoader` | `RubberbandNativeLoader.kt` | Rubberband 原生库加载器 |
| 14 | `WsolaTimeStretch` | `WsolaTimeStretch.kt` | WSOLA 变速算法（被 PitchPlayer 使用） |
| 15 | `PlayerUI` | `PlayerUI.kt` | Compose UI 播放器组件 |

---

## 2. Dependency Graph

```
┌─────────────────────────────────────────────────────────────────────┐
│                         UI Layer                                    │
│  App.kt ──► MediaPlayerState ◄── HistoryScreen.kt                  │
│                    │                   PlayerUI.kt                   │
└────────────────────┼───────────────────────────────────────────────┘
                     │ owns
                     ▼
┌─────────────────────────────────────────────────────────────────────┐
│                     AudioPlayer  (主播放器)                          │
│  ┌──────────┐    ┌────────────────┐    ┌─────────────────────┐      │
│  │ AudioDecoder│   │Rubberband-     │    │ javax.sound.sampled │      │
│  │ (interface)│   │Stretcher       │    │ SourceDataLine       │      │
│  └─────┬────┘    └───────┬────────┘    └─────────────────────┘      │
│        │                 │                                          │
│   ┌────┴──────────┐      │ JNA                                      │
│   │               │      ▼                                          │
│   ▼               ▼  ┌──────────┐                                   │
│ ┌─────────┐ ┌────────┐│Rubberband│                                   │
│ │Mp3Decoder│ │M4aDecoder││Api      │                                   │
│ └────┬────┘ └────┬───┘ └──────────┘                                   │
│      │           │                                                   │
│      ▼           ▼                                                   │
│ ┌─────────┐ ┌──────────────┐                                        │
│ │ JLayer  │ │ FFmpeg (exe) │                                        │
│ └─────────┘ └──────────────┘                                        │
└─────────────────────────────────────────────────────────────────────┘

遗留/替代路径:
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│JfxMediaPlayer│────►│Rubberband-   │────►│  Java Sound  │
│ (未激活)      │     │Player        │     │  SourceDataLine│
└──────────────┘     └──────┬───────┘     └──────────────┘
                            │
                     ┌──────┴───────┐
                     │SpiDecoder    │
                     └──────┬───────┘
                            │
                     ┌──────┴───────┐
                     │JAADecAudio-  │──► JAADec (反射)
                     │Decoder       │
                     └──────────────┘

已废弃:
┌──────────────┐     ┌──────────────┐
│ PitchPlayer  │────►│WsolaTimeStretch│
│ (@Deprecated)│     └──────────────┘
└──────────────┘
```

---

## 3. Current Playback Mechanism

### 3.1 调用链路

```
UI (Compose)
  └─► MediaPlayerState.play(url, title, artworkUrl, durationMs)
        └─► AudioPlayer.play(url, speed, startPositionMs, durationMs)
              │
              ├─ 1. RubberbandNativeLoader.load()          // 加载 rubberband.dll + 依赖
              ├─ 2. openInputStream(url)                    // HTTP/本地文件 → InputStream
              ├─ 3. 读取文件头 12 字节，判断格式
              │      ├─ "ftyp" → M4aDecoder (FFmpeg)
              │      ├─ "ID3" / 0xFF sync word → Mp3Decoder (JLayer)
              │      └─ 其他 → M4aDecoder (FFmpeg, fallback)
              ├─ 4. decoder.open(inputStream / url)
              ├─ 5. startPlaybackLoop(decoder, speed, startPositionMs)
```

### 3.2 播放循环 (startPlaybackLoop)

```kotlin
// 伪代码流程
1. 从 decoder 获取 sampleRate, channels
2. 创建 javax.sound.sampled.AudioFormat (PCM_SIGNED, 16-bit)
3. 获取 SourceDataLine 并 start
4. 创建 RubberbandStretcher (realtime + elastic + formant-preserved)
5. 设置速度, 写入 prePad 静音
6. seek 到 startPositionMs

while (!isStopped) {
    frame = decoder.readFrame()           // ShortArray (interleaved PCM)
    convert interleaved → planar float[][] // 归一化到 [-1.0, 1.0]
    stretcher.process(planar)             // Rubberband 时间拉伸
    output = stretcher.retrieve()         // 拉伸后的 float[][]
    convert planar → interleaved byte[]   // 应用音量, 转换为 16-bit PCM
    line.write(byteOut)                   // 写入 Java Sound SourceDataLine
    update currentPosition               // 基于已处理的输入采样数计算
}
```

### 3.3 核心特征

| 特征 | 实现方式 |
|------|----------|
| **音频输出** | `javax.sound.sampled.SourceDataLine` (Java Sound API) |
| **MP3 解码** | JLayer (`javazoom.jl.decoder`) — 逐帧解码 |
| **M4A/AAC 解码** | FFmpeg 外部进程 (`pipe:1` → PCM) |
| **变速变调** | Rubberband (JNA 绑定, realtime + elastic + formant-preserved) |
| **格式检测** | 魔数文件头检测 (12 字节) |
| **Seek 实现** | 停止 → 重新打开 → 跳转到目标位置 (非精确 seek) |
| **音量控制** | PCM 采样值乘以 volumeFactor (软件混音) |
| **线程模型** | 单独的 daemon 线程 (`audio-player`) |

### 3.4 MediaPlayerState 功能

- 播放队列管理 (`QueueItem`, `queueIndex`)
- 上/下一曲 (`playNext`, `playPrevious`)
- 睡眠定时器 (`setSleepTimer`)
- Compose 响应式状态 (`mutableStateOf`)
- 自动播放下一首 (在 `onPlayStateChanged` 回调中)

---

## 4. External Libraries

### 4.1 编译时依赖 (build.gradle.kts)

| Library | Artifact | Version | Purpose |
|---------|----------|---------|---------|
| **JavaFX Media** | `javafx-media-21.0.2-win.jar` | 21.0.2 | 声明引入但播放代码中未直接使用 |
| **JavaFX Base** | `javafx-base-21.0.2-win.jar` | 21.0.2 | 同上 |
| **JavaFX Graphics** | `javafx-graphics-21.0.2-win.jar` | 21.0.2 | 同上 |
| **JLayer** | `jlayer.jar` | — | MP3 解码 (javazoom.jl.decoder) |
| **JNA** | `jna-5.6.0.jar` | 5.6.0 | Rubberband JNI 桥接 |
| **JNA Platform** | `jna-platform-5.6.0.jar` | 5.6.0 | JNA 平台扩展 |
| **JAADec** | `jaad-0.9.4.jar` | 0.9.4 | AAC/M4A 解码 (反射调用) |
| **Java Sound SPI** | `mp3spi.jar` / SPI 文件 | — | MP3 SPI 支持 |
| **SQLite JDBC** | `org.xerial:sqlite-jdbc` | 3.46.1.0 | 数据库 (非播放) |

### 4.2 原生依赖 (runtime)

| DLL | Purpose |
|-----|---------|
| `rubberband.dll` | Rubberband 时间拉伸核心 |
| `libfftw3-3.dll` | FFT 库 (Rubberband 依赖) |
| `libsamplerate-0.dll` | 采样率转换 (Rubberband 依赖) |
| `libstdc++-6.dll` | C++ 运行时 |
| `libgcc_s_seh-1.dll` | GCC 运行时 |
| `libwinpthread-1.dll` | POSIX 线程 (Windows) |

### 4.3 运行时外部进程

| Process | Purpose |
|---------|---------|
| `ffmpeg.exe` | M4A/AAC 解码 (自动下载到 `~/.podium/ffmpeg/`) |

---

## 5. Coupling Analysis

### 5.1 Java Sound (`javax.sound.sampled`)

**耦合位置：**
- `AudioPlayer.kt:9-11` — import AudioFormat, AudioSystem, SourceDataLine
- `AudioPlayer.kt:106-115` — 创建 AudioFormat, 获取 SourceDataLine, 写入 PCM 数据
- `RubberbandPlayer.kt:8-10` — 同样的 Java Sound 依赖
- `PitchPlayer.kt:12-14` — 同样的 Java Sound 依赖
- `SpiDecoder.kt:6-7` — 使用 AudioSystem.getAudioInputStream() 做 SPI 解码

**耦合深度：** 严重 — 所有播放器实现都直接硬编码了 Java Sound 的 SourceDataLine 作为音频输出端。每个播放器都独立地创建 AudioFormat 和 SourceDataLine。

### 5.2 RubberBand (`rubberband.dll` via JNA)

**耦合位置：**
- `RubberbandApi.kt` — JNA 接口定义，直接绑定 rubberband.dll C API
- `RubberbandStretcher.kt` — RubberbandApi 的 Kotlin 封装
- `RubberbandNativeLoader.kt` — 原生 DLL 加载逻辑
- `AudioPlayer.kt:51,117-122` — 调用 RubberbandStretcher
- `RubberbandPlayer.kt:43,73-78` — 同上

**耦合深度：** 严重 — 变速功能完全依赖 Rubberband。AudioPlayer 和 RubberbandPlayer 都直接实例化 RubberbandStretcher。Rubberband 提供的功能（变速、保持音调）在 libmpv 中是内置的。

### 5.3 JLayer (`javazoom.jl.decoder`)

**耦合位置：**
- `Mp3Decoder.kt:3-6` — import Bitstream, Decoder, Header, SampleBuffer
- `Mp3Decoder.kt:19-29` — 创建 Bitstream 和 Decoder
- `Mp3Decoder.kt:32-37` — 逐帧解码 MP3
- `PitchPlayer.kt:4-7` — 同样的 JLayer 依赖 (已废弃)

**耦合深度：** 中等 — 仅 Mp3Decoder 和已废弃的 PitchPlayer 使用。解码逻辑被 AudioDecoder 接口隔离，但接口本身不提供 libmpv 替换点。

### 5.4 JAADec (`net.sourceforge.jaad`)

**耦合位置：**
- `JAADecAudioDecoder.kt:44-149` — 通过反射调用 JAADec 类
- `SpiDecoder.kt:13,59-71` — 调用 JAADecAudioDecoder

**耦合深度：** 中等 — 通过反射调用，耦合较松散。但仅用于 M4A 格式解码，且在 AudioPlayer 的主路径中已被 FFmpeg 子进程替代。

### 5.5 FFmpeg (外部进程)

**耦合位置：**
- `M4aDecoder.kt:14-17` — 定义下载 URL 和路径常量
- `M4aDecoder.kt:34-104` — 通过 ProcessBuilder 启动 FFmpeg
- `M4aDecoder.kt:206-271` — 自动下载 FFmpeg

**耦合深度：** 中等 — M4aDecoder 完全依赖 FFmpeg 外部进程。libmpv 内置 FFmpeg，可消除此依赖。

### 5.6 耦合矩阵

```
                Java Sound  Rubberband  JLayer  JAADec  FFmpeg
AudioPlayer        ★★★         ★★★        -       -      ★★★
RubberbandPlayer   ★★★         ★★★        -       -       -
SpiDecoder         ★★          -          -       ★★      -
Mp3Decoder         -           -          ★★★      -       -
M4aDecoder         -           -          -       -       ★★★
PitchPlayer        ★★★         -          ★★★      -       -
JfxMediaPlayer      -          ★★★(间接)  -       -       -
MediaPlayerState    -          -(间接)     -       -       -

★★★ = 强耦合  ★★ = 中等耦合  ★ = 弱耦合  - = 无耦合
```

---

## 6. libmpv Migration Architecture

### 6.1 libmpv 能力映射

| Current Component | libmpv Equivalent | Notes |
|-------------------|-------------------|-------|
| Java Sound SourceDataLine | libmpv audio output | 内置，无需额外配置 |
| Rubberband 变速 | `mpv_set_property("speed", ...)` | 内置，支持 0.25x-16x |
| Rubberband 保持音调 | `mpv_set_property("audio-pitch-correction", true)` | 内置 |
| Mp3Decoder (JLayer) | libmpv 内置 FFmpeg | 自动支持 |
| M4aDecoder (FFmpeg 进程) | libmpv 内置 FFmpeg | 消除外部进程 |
| SpiDecoder (Java Sound SPI) | libmpv 内置解码器 | 自动支持 |
| JAADec (AAC) | libmpv 内置 FFmpeg | 自动支持 |
| 音量控制 | `mpv_set_property("volume", ...)` | 内置 |
| Seek | `mpv_command("seek", ...)` | 精确 seek |
| HTTP 流 | libmpv 内置 | 自动支持 |
| 播放状态回调 | mpv 事件系统 | observe-property |

### 6.2 Proposed Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     UI Layer (UNCHANGED)                     │
│  App.kt ──► MediaPlayerState ◄── PlayerUI.kt               │
└────────────────────┬────────────────────────────────────────┘
                     │  interface AudioPlaybackEngine
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                AudioPlaybackEngine (interface)               │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ play(url, speed, positionMs)                          │   │
│  │ pause() / resume() / stop()                          │   │
│  │ seek(positionMs)                                     │   │
│  │ setSpeed(speed) / setVolume(vol)                     │   │
│  │ onPlayStateChanged / onPositionChanged / onError     │   │
│  └──────────────────────────────────────────────────────┘   │
└──────────┬──────────────────────────┬───────────────────────┘
           │                          │
           ▼                          ▼
┌──────────────────┐    ┌──────────────────────┐
│ LibMpvEngine     │    │ LegacyEngine         │
│ (NEW - libmpv)   │    │ (existing AudioPlayer)│
└──────────────────┘    └──────────────────────┘
```

### 6.3 Implementation Plan

#### Phase 1: 引入 AudioPlaybackEngine 接口

```kotlin
interface AudioPlaybackEngine {
    var onPlayStateChanged: ((Boolean) -> Unit)?
    var onPositionChanged: ((Long, Long) -> Unit)?
    var onError: ((String) -> Unit)?

    val isPlaying: Boolean
    val currentPosition: Long
    val duration: Long

    fun play(url: String, speed: Float = 1.0f, startPositionMs: Long = 0L, durationMs: Long = 0L)
    fun pause()
    fun resume()
    fun stop()
    fun seek(positionMs: Long)
    fun setSpeed(speed: Float)
    fun setVolume(vol: Int)
    fun release()
}
```

- AudioPlayer 实现此接口 (现有代码包装)
- MediaPlayerState 依赖此接口而非 AudioPlayer 具体类

#### Phase 2: 实现 LibMpvEngine

```kotlin
class LibMpvEngine : AudioPlaybackEngine {
    private var mpvHandle: Pointer? = null  // mpv_create() 返回

    init {
        mpvHandle = mpv_create()
        mpv_initialize(mpvHandle)
        mpv_set_option_string(mpvHandle, "audio-pitch-correction", "yes")
        // 观察属性变化
        mpv_observe_property(mpvHandle, 0, "time-pos", MPV_FORMAT_DOUBLE)
        mpv_observe_property(mpvHandle, 0, "duration", MPV_FORMAT_DOUBLE)
        mpv_observe_property(mpvHandle, 0, "pause", MPV_FORMAT_FLAG)
        mpv_observe_property(mpvHandle, 0, "eof-reached", MPV_FORMAT_FLAG)
    }

    override fun play(url: String, speed: Float, startPositionMs: Long, durationMs: Long) {
        mpv_set_property_string(mpvHandle, "speed", speed.toString())
        mpv_command(mpvHandle, arrayOf("loadfile", url, "replace",
            "start=${startPositionMs / 1000.0}"))
    }

    override fun setSpeed(speed: Float) {
        mpv_set_property_string(mpvHandle, "speed", speed.toString())
    }

    // ... 事件循环在单独线程中运行，分发回调
}
```

#### Phase 3: JNA 绑定层

需要的 JNA 绑定：
- `mpv_create`, `mpv_initialize`, `mpv_destroy`
- `mpv_command`, `mpv_command_async`
- `mpv_set_option_string`, `mpv_set_property_string`
- `mpv_get_property`
- `mpv_observe_property`, `mpv_unobserve_property`
- `mpv_wait_event`
- `mpv_request_log_messages`

#### Phase 4: 清理

移除以下组件：
- `RubberbandApi.kt`, `RubberbandStretcher.kt`, `RubberbandNativeLoader.kt`
- `Mp3Decoder.kt`, `M4aDecoder.kt`, `SpiDecoder.kt`, `JAADecAudioDecoder.kt`
- `AudioDecoder.kt` (接口)
- `RubberbandPlayer.kt`, `JfxMediaPlayer.kt`, `PitchPlayer.kt`, `WsolaTimeStretch.kt`
- `libs/rubberband.dll` 及所有 rubberband 依赖 DLL
- `libs/jlayer.jar`, `libs/jaad-0.9.4.jar`, `libs/mp3spi.jar`
- `libs/javafx-media-21.0.2-win.jar` (如果确实未使用)

### 6.4 Benefits Summary

| Metric | Before | After |
|--------|--------|-------|
| 外部原生 DLL | 6 个 (rubberband + 5 依赖) | 1 个 (libmpv) |
| 外部进程 | 1 个 (ffmpeg.exe) | 0 个 |
| Java JAR 依赖 | 5 个 (jlayer, jaad, jna×2, mp3spi) | 1 个 (jna) |
| 解码格式支持 | 手动判断 + 多解码器 | libmpv 自动支持所有格式 |
| Seek 精度 | 近似 (停止重开) | 精确 (libmpv 原生 seek) |
| 播放器类数量 | 8 个 (含遗留) | 2 个 (接口 + 实现) |
| 代码行数 (player/) | ~1700 行 | 预估 ~400 行 |

### 6.5 Risks & Considerations

1. **libmpv DLL 大小** — 约 50-80MB，需评估分发影响
2. **JNA 绑定维护** — libmpv C API 较稳定，但需持续跟踪版本
3. **FFmpeg 内置版本** — libmpv 内置的 FFmpeg 版本可能与系统 FFmpeg 不同
4. **音频延迟** — libmpv 默认缓冲可能影响交互式 seek 体验，需调优 `audio-buffer` 参数
5. **线程安全** — libmpv 的 mpv_handle 非线程安全，需确保所有 API 调用在同一线程
6. **测试策略** — 现有测试 (RubberbandPlayerTest, AudioPlayerTest) 需要适配到新接口
