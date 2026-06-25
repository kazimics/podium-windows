# Changelog

All notable changes to podium-windows will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [0.1.0] - 2026-06-25

### Fixed
- Progress bar position calculation: `totalInputSamples` unit mismatch caused position to advance at half speed for stereo audio
- Progress bar not syncing to total duration when playback finishes naturally
- Volume control now functional (was a no-op in custom audio pipeline)
- M4aDecoder resume: `seekToMs` was a no-op, now restarts playback from correct position
- Queue always showing empty: episodes now auto-added to queue when played
- Auto-advance to next queue item when track ends naturally

### Added
- Mute toggle: clicking volume icon mutes/unmutes, restoring previous volume level
- Queue button on mini player for quick access to playlist
- QueueSheet with selection mode, batch delete, and per-item download
- "Add to Queue" button on episode lists (podcast detail + history)

### Changed
- Player thread safety: added `@Volatile` to `isStopped`, `currentVolume`, `isPlaying`, `currentPosition`
- Player performance: pre-allocated buffers in playback loop (planar, byteOut) to reduce GC pressure
- Position callback throttled to 300ms intervals to reduce UI refresh overhead

## [0.1.0] - 2026-06-19

### Added
- Custom download path setting in Settings screen
- Settings persistence via `~/.podium/settings.properties`
- Download progress indicator with determinate progress
- Download state persists across navigation
- Download completed icon shows CheckCircle

### Fixed
- WSOLA time-stretch crash: IndexOutOfBounds when frame size < 2048
- WsolaTimeStretch now uses actual frame size instead of fixed 2048
- Download records persisted in database — download status survives path changes
- isDownloaded checks completedDownloads set (no file.exists() fallback)
- Play button checks database for actual file path before playing
- Download progress shows real-time incremental progress
- Download state persists when navigating away and back

### Changed
- Replaced VLC (vlcj) with JavaFX MediaPlayer for audio playback
- Removed 87MB VLC plugin bundling requirement
- Users no longer need to install VLC separately

## [0.1.0] - 2026-06-18

### Added
- Initial release of podium-windows
- Podcast subscription via RSS feed URL
- Discover page with Apple Podcasts search and top podcasts
- Podcast detail page with episode list
- Mini player and full player UI
- Playback queue management
- Sleep timer
- Listening history tracking
- OPML import/export
- Episode download for offline listening
- Debug logging module (`~/.podium/debug.log`) for troubleshooting
- CHANGELOG.md
- Comprehensive test suite (67+ tests across 8 test classes)

### Fixed
- Serialization plugin missing in shared module causing Discover page to fail
- VLC path resolution fails in packaged app (URI is not hierarchical)
- JavaFX Media local file path not converted to URI format
- JavaFX Toolkit not initialized before MediaPlayer creation
- JavaFX native library loading from JAR with multiple fallback paths

### Added
- Pitch-preserving speed control using WSOLA time-stretching algorithm
- When speed != 1.0x, switches to PitchPlayer (JLayer decode + WSOLA + javax.sound output)
- When speed == 1.0x, uses JavaFX MediaPlayer normally
- Updated CI workflow to run new test classes
- Removed unused Android deploy workflow (deploy.yaml)
- Removed build artifacts from git tracking
