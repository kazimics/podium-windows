# Changelog

All notable changes to podium-windows will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [0.1.0] - 2026-06-19

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
- Custom download path setting in Settings screen
- Settings persistence via `~/.podium/settings.properties`

### Fixed
- Serialization plugin missing in shared module causing Discover page to fail
- VLC path resolution fails in packaged app (URI is not hierarchical)
- JavaFX Media local file path not converted to URI format
- JavaFX Toolkit not initialized before MediaPlayer creation
- JavaFX native library loading from JAR with multiple fallback paths
- Download file naming now uses podcast title as folder and episode title as filename
- Download progress indicator shows progress during download
- Download state persists when navigating away and back (downloadVersion counter)
- Download completed icon now shows CheckCircle instead of Download icon
- Folder picker uses JFileChooser for proper folder selection on Windows
- DownloadManager state recreated when download path setting changes
- isDownloaded cache invalidated after download completes via downloadVersion

### Changed
- Version set to 0.1.0 (pre-release)
- Replaced VLC (vlcj) with JavaFX MediaPlayer for audio playback
- Removed 87MB VLC plugin bundling requirement
- Users no longer need to install VLC separately
- Updated CI workflow to run new test classes
- Removed unused Android deploy workflow (deploy.yaml)
- Removed build artifacts from git tracking
