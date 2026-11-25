package com.classroll.photo.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// 纸张皮肤原始色（0.3.x 原版配色，保持不变）
internal val PaperBackgroundValue = Color(0xFFF8F2E6)
internal val PaperCardValue = Color(0xFFFBF6EA)
internal val PaperCardAltValue = Color(0xFFF1E8D6)
internal val PaperInkValue = Color(0xFF1C1A15)
internal val PaperMutedValue = Color(0xFF6F6759)
internal val PaperLineValue = Color(0xFFDED3C1)
internal val PaperTerracottaValue = Color(0xFFD6542E)
internal val PaperHighlightValue = Color(0xFFFFE066)
internal val PaperGoodValue = Color(0xFF2F6A4F)
internal val PaperWarnValue = Color(0xFFA76A00)
internal val PaperBadValue = Color(0xFFB4432B)
internal val PaperBlueValue = Color(0xFF3564A8)
internal val PaperPurpleValue = Color(0xFF7B5AA6)

// 包豪斯皮肤原始色（取自 superdesign.dev/library/bauhaus 的实际色值）
internal val BauhausPaper = Color(0xFFF0F0F0)
internal val BauhausPanel = Color(0xFFE6E6E6)
internal val BauhausRed = Color(0xFFD02020)
internal val BauhausBlue = Color(0xFF1040C0)
internal val BauhausYellow = Color(0xFFF0C020)
internal val BauhausBlack = Color(0xFF121212)
internal val BauhausMuted = Color(0xFF52525B)
internal val BauhausOchre = Color(0xFF8A6300)

// 旧名保留为当前皮肤的语义色，切换皮肤时全局跟随。
val PaperBackground: Color @Composable @ReadOnlyComposable get() = LocalSkin.current.background
val PaperCard: Color @Composable @ReadOnlyComposable get() = LocalSkin.current.card
val PaperCardAlt: Color @Composable @ReadOnlyComposable get() = LocalSkin.current.cardAlt
val PaperInk: Color @Composable @ReadOnlyComposable get() = LocalSkin.current.ink
val PaperMuted: Color @Composable @ReadOnlyComposable get() = LocalSkin.current.muted
val PaperLine: Color @Composable @ReadOnlyComposable get() = LocalSkin.current.line
val PaperTerracotta: Color @Composable @ReadOnlyComposable get() = LocalSkin.current.accent
val PaperHighlight: Color @Composable @ReadOnlyComposable get() = LocalSkin.current.highlight
val PaperGood: Color @Composable @ReadOnlyComposable get() = LocalSkin.current.good
val PaperWarn: Color @Composable @ReadOnlyComposable get() = LocalSkin.current.warn
val PaperBad: Color @Composable @ReadOnlyComposable get() = LocalSkin.current.bad
val PaperBlue: Color @Composable @ReadOnlyComposable get() = LocalSkin.current.blue
val PaperPurple: Color @Composable @ReadOnlyComposable get() = LocalSkin.current.purple
