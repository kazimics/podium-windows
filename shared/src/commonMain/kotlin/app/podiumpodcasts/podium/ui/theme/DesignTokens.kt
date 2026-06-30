package app.podiumpodcasts.podium.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object DesignTokens {

    // ── Spacing ──
    object Spacing {
        val xs = 4.dp
        val sm = 8.dp
        val md = 16.dp
        val lg = 24.dp
        val xl = 32.dp
    }

    // ── Common Border ──
    object Border {
        val Width = 1.dp
        val SecondaryColor = Color(0x14FFFFFF)
    }

    // ── Button: primary ──
    object Button {
        val Height = 40.dp
        val Radius = 10.dp
        val IconSize = 20.dp
        val TextSize = 14.sp
        val PaddingHorizontal = 16.dp
        val Gradient = Brush.verticalGradient(
            colors = listOf(Color(0xFFC5976F), Color(0xFFBF936C), Color(0xFFB1845F)),
            startY = 0f, endY = 40f
        )
        val InnerHighlight = Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.12f), Color.Transparent),
            startY = 0f, endY = 40f
        )
        val TextColor = Color(0xFFFFF8F3)
        val IconColor = Color.White
        val ShadowElevation = 10.dp
        val ShadowColor = Color.Black.copy(alpha = 0.25f)
    }

    // ── Button: icon (circular secondary) ──
    object IconButton {
        val Size = 40.dp
        val IconSize = 20.dp
    }

    // ── MiniPlayer ──
    object MiniPlayer {
        val Height = 88.dp
        val PaddingHorizontal = 20.dp
        object Speed { val Size = 36.dp; val TextSize = 14.sp }
        object RewindForward { val Size = 40.dp; val IconSize = 22.dp }
        object PlayPause { val Size = 56.dp; val IconSize = 26.dp }
        object Volume { val IconSize = 22.dp }
        object QueueFullscreen { val Size = 32.dp; val IconSize = 20.dp }
        object Time { val TextSize = 12.sp }
        object Slider { val Height = 20.dp }
    }

    // ── Sidebar ──
    object Sidebar {
        val Width = 240.dp
        val PaddingVertical = 20.dp
        val PaddingHorizontal = 20.dp
        val NavItemHeight = 48.dp
        val NavItemPadding = 12.dp
        val NavIconSize = 20.dp
        val NavTextSize = 14.sp
        val NavSpacing = 10.dp
        val LogoSize = 32.dp
        val LogoRadius = 8.dp
        val LogoIconSize = 18.dp
        val LogoTextSize = 17.sp
        val DividerPadding = 20.dp
    }

    // ── Search Bar ──
    object SearchBar {
        val Width = 320.dp
        val Height = 40.dp
        val Radius = 10.dp
        val PaddingHorizontal = 12.dp
        val IconSize = 16.dp
        val TextSize = 13.sp
        val Gap = 8.dp
        val ShortcutRadius = 5.dp
        val ShortcutTextSize = 11.sp
        val ShortcutPaddingH = 6.dp
        val ShortcutPaddingV = 2.dp
        val ClearIconSize = 14.dp
    }

    // ── Featured Card ──
    object FeaturedCard {
        val Height = 280.dp
        val Radius = 18.dp
        val Padding = 20.dp
        val CoverRadius = 12.dp
        val ContentGap = 24.dp
        val TextGap = 10.dp
        val ButtonGap = 10.dp
        val NavButtonSize = 32.dp
        val NavButtonGap = 6.dp
        val NavIconSize = 16.dp
        val NavPadding = 12.dp
        val ShadowElevation = 8.dp
        val ShadowColor = Color.Black.copy(alpha = 0.4f)
    }

    // ── Podcast Card ──
    object PodcastCard {
        val Width = 150.dp
        val ImageSize = 150.dp
        val ImageRadius = 14.dp
        val Spacing = 12.dp
        val Gap = 16.dp
        val TitleSize = 13.sp
        val AuthorSize = 11.sp
    }

    // ── Episode Row ──
    object EpisodeRow {
        val Height = 88.dp
        val PaddingHorizontal = 32.dp
        val PaddingVertical = 8.dp
        val CoverSize = 64.dp
        val CoverRadius = 10.dp
        val Spacing = 14.dp
        val TitleSize = 14.sp
        val AuthorSize = 12.sp
        val DescSize = 11.sp
        val IconSize = 20.dp
    }

    // ── Queue Panel ──
    object QueuePanel {
        val Width = 320.dp
        val PaddingTop = 20.dp
        val PaddingHorizontal = 24.dp
        val RowHeight = 80.dp
        val CoverSize = 56.dp
        val CoverRadius = 8.dp
        val Spacing = 12.dp
        val TitleSize = 13.sp
        val HeaderTitleSize = 18.sp
        val ClearTextSize = 13.sp
        val DragHandleSize = 16.dp
        val ActiveCoverBadge = 20.dp
    }

    // ── Page Header ──
    object PageHeader {
        val TitleSize = 32.sp
        val SubtitleSize = 14.sp
        val PaddingHorizontal = 32.dp
        val PaddingTop = 28.dp
        val Gap = 4.dp
    }

    // ── Section Header ──
    object SectionHeader {
        val TitleSize = 20.sp
        val LinkSize = 13.sp
        val PaddingHorizontal = 32.dp
    }

    // ── Card Background ──
    object Card {
        val Gradient = Brush.linearGradient(
            colors = listOf(Color(0xFF1C1C1E), Color(0xFF15171B)),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    }
}
