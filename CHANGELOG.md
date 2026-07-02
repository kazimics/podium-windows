# Changelog

All notable changes to podium-windows will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [0.1.0] - 2026-07-02

### Added
- New i18n string keys for sidebar, title bar, discover screen, player, settings, and error messages
- Chinese translations (zh) for all previously missing UI strings
- `Strings.get()` format-parameter usage for error messages in DiscoverScreen and AddPodcastDialog
- Lightweight subscribe via `PodcastManager.addPodcastFromPreview()` — podcast created from Apple Podcast preview data without downloading RSS feed (instant subscribe)
- `onSubscribed` callback in DiscoverScreen to refresh subscription list immediately
- Retry button on podcast detail screen when episode loading fails
- Automatic RSS refresh when entering podcast detail screen (deferred episode loading)
- Play state initialization in `SubscriptionManager.updatePodcast()` for newly fetched episodes
- Hover cursor pointer for MiniPlayer controls (speed selector, seek, play, volume, queue, expand)
- iTunes episode lookup API (`entity=podcastEpisode`) — lightweight episode metadata fetch without full RSS download
- `PodcastEpisodeLookupResult` model and `lookupLatestEpisodes()` method on `ApplePodcastClient.Lookup`
- `RssConverter.parseFetchResult()` — shared helper for parsing RSS fetch results without exposing rssparser types
- `podcastItunesLookup` database table for persistent `itunes-lookup:xxx → RSS URL` mapping across restarts
- `ItunesLookupDao` for CRUD on the iTunes URL mapping table
- `loading_episodes` i18n string (EN/ZH)
- "Play Latest Episode" button on FeaturedCard — plays via iTunes API, no subscription required
- Entire FeaturedCard cover + text area clickable → navigates to episode list
- Subscription state feedback on FeaturedCard: spinner during subscribing, check icon when subscribed
- In-memory + persistent cache for `itunes-lookup:xxx` subscription status check across sessions
- Podcast artwork fallback: MiniPlayer now shows podcast cover when the episode has no artwork

### Fixed
- MiniPlayer now falls back to podcast cover art when the played episode has no image of its own
- Audio stays paused when playing a new episode after pausing the previous one (mpv retains pause state across `loadfile`; polling thread's transition false no longer triggers `playNext()`)
- Podcast detail page unsubscribe button now correctly removes the podcast from the subscription list (was refreshing the database but not the in-memory list)
- Navigate to other sidebar tabs no longer blocked when inside podcast episode list
- Infinite loading spinner when RSS fetch fails in PodcastDetailScreen
- iTunes lookup URL (`itunes-lookup:`) causing HTTP error during episode refresh — now resolved to real feed URL at subscribe time
- DiscoverScreen subscription status incorrect for iTunes-sourced podcasts after refresh
- FeaturedCard "Latest Episode" button incorrectly called subscribe instead of playing the latest episode
- FeaturedCard right/left navigation arrows blocked by content overlay (z-order issue)
- `onShowDetail` previously auto-subscribed the podcast — now fetches RSS in preview mode only
- Podcast detail screen now handles unsubscribed (preview) mode — fetches RSS directly, hides unsubscribe button
- Subscribed podcast status lost after re-entering DiscoverScreen — now also checks via RSS URL cache
- Subscribed podcast status lost after app restart — now persists iTunes URL → RSS URL mapping in database

### Changed
- Migrated all hardcoded English UI strings to `Strings["key"]` localization system (App, Discover, Player, Settings, History screens)
- Sidebar nav items now use string keys instead of literals for full i18n support
- Error messages in DiscoverScreen and AddPodcastDialog use `Strings.get()` with format parameters
- Window title in Main.kt uses `Strings["app_name"]` instead of hardcoded "Podium"
- All Chinese code comments translated to English
- `FetchPodcastClient.fetch()` made `open` for testability
- `SubscriptionManager.unsubscribe()` now also cleans up the iTunes lookup mapping
- `PodcastDetailScreen` receives `FetchPodcastClient` for unsubscribed preview mode with built-in loading
- FeaturedCard subscribe button now has three visual states: Add (unsubscribed), spinner (subscribing), Check (subscribed)

### Removed
- Ctrl+K search shortcut badge test (UI element was removed in previous refactor)
- Auto-subscribe side effect when navigating to podcast detail from FeaturedCard

## [0.1.0] - 2026-07-01

### Fixed
- MiniPlayer skip step inconsistent with FullPlayer (MiniPlayer 15s/30s → unified to 10s)
- MiniPlayer play button clickable when no track loaded, now disabled
- FullPlayer background not filling content area, switched to PodiumTheme dark background
- FullPlayer sidebar navigation blocked while expanded, clicking nav items now closes FullPlayer
- FullPlayer text and icon colors not applying dark theme palette

### Changed
- Volume button changed from `Icon.clickable` to `IconButton`, hover shape matches queue/expand buttons (circle), icon size reduced from 22dp to 18dp
- MiniPlayer shows podcast name below episode title (added `currentSubtitle` field)
- `QueueItem` data class now includes `subtitle` field
- `MediaPlayerState.play()` accepts new `subtitle` parameter
- Search interaction redesigned: click search icon to trigger search, Enter key triggers search, removed redundant "Search" button and Ctrl+K badge
- Search results only shown after user triggers a search, keeps Top Podcasts view when idle

## [0.1.0] - 2026-06-30

### Added
- Design system (`DesignTokens.kt`) — centralized design tokens for spacing, colors, typography, and component dimensions
- Custom immersive title bar with undecorated window (minimize, maximize, close buttons with hover effects)
- Window dragging via `WindowDraggableArea`
- Premium gradient button system (`Button.Gradient`, `Button.InnerHighlight`)
- Card background gradient (`Card.Gradient` — #1C1C1E → #15171B)
- Sidebar navigation component with accent highlighting and hover cursor
- Featured card on Discover page with cover art, description, and action buttons
- Podcast card horizontal scroll section ("Trending This Week")
- Episode row component for podcast lists
- Queue panel as fixed right-side drawer with cover art per item
- MiniPlayer redesign: cover art, speed selector, 15s/30s skip, volume slider, progress slider

### Changed
- Theme system upgraded: custom `PodiumColors` data class with `LocalPodiumColors` CompositionLocal
- Default screen changed from Home to Discover
- Color palette updated to match design system (warm gold accent, dark surface tones)
- All hardcoded dp/sp values in Sidebar, DiscoverScreen, PlayerUI replaced with `DesignTokens` references
- MiniPlayer layout: single Row with three-section layout (left: cover+title, center: controls, right: time+slider+volume)
- QueueDrawer redesigned with 320px fixed panel, 80px row height, cover thumbnails

### Removed
- TopAppBar navigation from Discover screen (replaced by Sidebar)
- Material3 default color scheme overrides (replaced by design system tokens)

## [0.1.0] - 2026-06-29

### Added
- `AudioPlayerEngine` interface for abstracting audio playback implementations
- `MpvAudioPlayerEngine` — new audio engine using libmpv via JNA
- `MpvApi` — JNA interface for libmpv C API
- `MpvNativeLoader` — native library loader for mpv-1.dll
- `MpvAudioPlayerEngineTest` — unit tests for mpv engine
- `PlaybackState` enum and `PlayerMetadata` data class for engine state tracking

### Changed
- Audio playback engine switched from Java Sound + Rubberband to libmpv
- `MediaPlayerState` now uses `MpvAudioPlayerEngine` as default engine
- Default engine can be overridden via constructor parameter
- Removed JavaFX MediaPlayer dependency (was unused for audio)

### Removed
- `JavaAudioPlayerEngine` (legacy Java Sound + Rubberband implementation)
- `RubberbandApi`, `RubberbandStretcher`, `RubberbandNativeLoader`
- `AudioDecoder`, `Mp3Decoder`, `M4aDecoder`, `SpiDecoder`, `JAADecAudioDecoder`
- `PitchPlayer`, `WsolaTimeStretch`, `JfxMediaPlayer`, `RubberbandPlayer`
- `PlaybackTest` (legacy JavaFX media test)
- Native libraries: rubberband.dll, libfftw3-3.dll, libsamplerate-0.dll, libstdc++-6.dll, libgcc_s_seh-1.dll, libwinpthread-1.dll
- JARs: jlayer.jar, jaad-0.9.4.jar, mp3spi.jar, javafx-*.jar

## [0.1.0] - 2026-06-26

### Added
- Podcast cover images display on Home, Discover, History screens, and episode lists
- Coil 3 image loading library for network image fetching
- Multi-language support (English / Simplified Chinese)
- Language selector in Settings screen
- Unsubscribe functionality with confirmation dialog on podcast detail screen
- Edit mode on Home screen for batch unsubscribe operations
- Subscribe button on Discover page now shows loading spinner during subscription
- Subscribe success icon aligns with add button for consistent layout
- Discover page now correctly shows subscribed podcasts with check icon across sessions

### Fixed
- Download status showing completed when local file no longer exists
- History list now falls back to podcast cover when episode has no image
- All GUI tests now pass with Coil image loading integration
- Discover page subscribe button lacked loading state feedback
- Subscribe success icon misaligned with add button icon
- Already-subscribed podcasts still showing add button on Discover page
- Imported podcasts not recognized as subscribed on Discover page

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
