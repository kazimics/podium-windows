package app.podiumpodcasts.podium.ui.theme

import androidx.compose.ui.graphics.Color

// ── Design System: Surface ──
val PodiumBackground = Color(0xFF090B0E)
val PodiumSurface = Color(0xFF101318)
val PodiumElevated = Color(0xFF171B22)
val PodiumBorder = Color(0xFF252A33)
val PodiumDivider = Color(0x0FFFFFF6)  // rgba(255,255,255,0.06)

// ── Design System: Typography ──
val PodiumTextPrimary = Color(0xFFF6F6F4)
val PodiumTextSecondary = Color(0xFFB7B8BC)
val PodiumTextMuted = Color(0xFF777A82)
val PodiumTextDisabled = Color(0xFF555861)

// ── Design System: Accent ──
val PodiumAccent = Color(0xFFE0B183)
val PodiumAccentHover = Color(0xFFF0C497)
val PodiumAccentPressed = Color(0xFFC89B6A)

// ── Design System: States ──
val PodiumSuccess = Color(0xFF34C759)
val PodiumWarning = Color(0xFFFFB020)
val PodiumDanger = Color(0xFFFF5A5F)
val PodiumInfo = Color(0xFF409CFF)

// ── Material3 Dark Scheme ──
val PrimaryDark = PodiumAccent
val OnPrimaryDark = PodiumBackground
val PrimaryContainerDark = PodiumElevated
val OnPrimaryContainerDark = PodiumTextPrimary

val SecondaryDark = PodiumTextSecondary
val OnSecondaryDark = PodiumBackground
val SecondaryContainerDark = PodiumElevated
val OnSecondaryContainerDark = PodiumTextPrimary

val TertiaryDark = PodiumAccent
val OnTertiaryDark = PodiumBackground
val TertiaryContainerDark = PodiumElevated
val OnTertiaryContainerDark = PodiumTextPrimary

val BackgroundDark = PodiumBackground
val OnBackgroundDark = PodiumTextPrimary

val SurfaceDark = PodiumSurface
val OnSurfaceDark = PodiumTextPrimary

val ErrorDark = PodiumDanger
val OnErrorDark = PodiumTextPrimary
val ErrorContainerDark = PodiumElevated
val OnErrorContainerDark = PodiumTextPrimary

// ── Material3 Light Scheme (fallback) ──
val PrimaryLight = Color(0xFF6750A4)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFEADDFF)
val OnPrimaryContainerLight = Color(0xFF21005D)

val SecondaryLight = Color(0xFF625B71)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFE8DEF8)
val OnSecondaryContainerLight = Color(0xFF1D192B)

val TertiaryLight = Color(0xFF7D5260)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFFFD8E4)
val OnTertiaryContainerLight = Color(0xFF31111D)

val BackgroundLight = Color(0xFFFFFBFE)
val OnBackgroundLight = Color(0xFF1C1B1F)

val SurfaceLight = Color(0xFFFFFBFE)
val OnSurfaceLight = Color(0xFF1C1B1F)

val ErrorLight = Color(0xFFB3261E)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFF9DEDC)
val OnErrorContainerLight = Color(0xFF410E0B)
