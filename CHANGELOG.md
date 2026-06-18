# Changelog

All notable changes to podium-windows will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [0.1.0] - 2026-06-18

### Added
- Initial release of podium-windows
- Podcast subscription via RSS feed URL
- Discover page with Apple Podcasts search and top podcasts
- Podcast detail page with episode list
- Media playback with VLC (play/pause, seek, volume, speed control)
- Mini player and full player UI
- Playback queue management
- Sleep timer
- Listening history tracking
- OPML import/export
- Episode download for offline listening

### Fixed
- Serialization plugin missing in shared module causing Discover page to fail with "Serializer not found" error

### Changed
- Version set to 0.1.0 (pre-release)
- Removed build artifacts from git tracking
- Removed unused Android deploy workflow
