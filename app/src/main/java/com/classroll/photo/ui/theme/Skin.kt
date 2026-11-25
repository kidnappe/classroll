package com.classroll.photo.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 硬投影：无模糊，纯色偏移块。 */
@Immutable
data class HardShadow(val dx: Dp, val dy: Dp, val color: Color)

@Immutable
data class SkinRadii(
    val xs: Dp,
    val small: Dp,
    val medium: Dp,
    val large: Dp,
    val extraLarge: Dp,
    val pill: Dp,
    val tabShell: Dp,
    val tabItem: Dp,
    val iconTile: Dp,
    val bar: Dp,
    val sheet: Dp,
)

enum class SkinDecoration { PaperTape, GeometricBar }

@Immutable
data class Skin(
    val id: String,
    val label: String,
    val tagline: String,
    // 颜色
    val background: Color,
    val card: Color,
    val cardAlt: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val accent: Color,
    val onAccent: Color,
    val highlight: Color,
    val onHighlight: Color,
    val good: Color,
    val warn: Color,
    val bad: Color,
    val blue: Color,
    val purple: Color,
    val geometry: List<Color>,
    // 结构
    val radii: SkinRadii,
    val cardBorder: BorderStroke,
    val hairline: BorderStroke,
    val hardShadow: HardShadow?,
    val grain: Boolean,
    val dashed: Boolean,
    val titleBlock: Boolean,
    val decoration: SkinDecoration,
    val navSelected: Color,
    val snackbarContainer: Color,
    val snackbarContent: Color,
    val scheme: ColorScheme,
    val typography: Typography,
)

private val PaperRadii = SkinRadii(
    xs = 8.dp,
    small = 12.dp,
    medium = 18.dp,
    large = 26.dp,
    extraLarge = 32.dp,
    pill = 999.dp,
    tabShell = 14.dp,
    tabItem = 11.dp,
    iconTile = 10.dp,
    bar = 25.dp,
    sheet = 26.dp,
)

private val SharpRadii = SkinRadii(
    xs = 0.dp,
    small = 0.dp,
    medium = 0.dp,
    large = 0.dp,
    extraLarge = 0.dp,
    pill = 0.dp,
    tabShell = 0.dp,
    tabItem = 0.dp,
    iconTile = 0.dp,
    bar = 0.dp,
    sheet = 0.dp,
)

private val PaperScheme = lightColorScheme(
    primary = PaperInkValue,
    onPrimary = PaperCardValue,
    primaryContainer = PaperHighlightValue,
    onPrimaryContainer = PaperInkValue,
    secondary = PaperTerracottaValue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF7D8C8),
    onSecondaryContainer = PaperInkValue,
    tertiary = PaperGoodValue,
    onTertiary = Color.White,
    background = PaperBackgroundValue,
    onBackground = PaperInkValue,
    surface = PaperCardValue,
    onSurface = PaperInkValue,
    surfaceVariant = PaperCardAltValue,
    onSurfaceVariant = PaperMutedValue,
    outline = PaperLineValue,
    outlineVariant = PaperLineValue,
    error = PaperBadValue,
    onError = Color.White,
)

private val BauhausScheme = lightColorScheme(
    primary = BauhausRed,
    onPrimary = Color.White,
    primaryContainer = BauhausYellow,
    onPrimaryContainer = BauhausBlack,
    secondary = BauhausBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3E8F6),
    onSecondaryContainer = BauhausBlack,
    tertiary = BauhausBlack,
    onTertiary = Color.White,
    background = BauhausPaper,
    onBackground = BauhausBlack,
    surface = Color.White,
    onSurface = BauhausBlack,
    surfaceVariant = BauhausPanel,
    onSurfaceVariant = BauhausMuted,
    outline = BauhausBlack,
    outlineVariant = BauhausBlack,
    error = BauhausRed,
    onError = Color.White,
)

val PaperSkin = Skin(
    id = SkinId.Paper.key,
    label = "纸张",
    tagline = "原版纸感",
    background = PaperBackgroundValue,
    card = PaperCardValue,
    cardAlt = PaperCardAltValue,
    ink = PaperInkValue,
    muted = PaperMutedValue,
    line = PaperLineValue,
    accent = PaperTerracottaValue,
    onAccent = Color.White,
    highlight = PaperHighlightValue,
    onHighlight = PaperInkValue,
    good = PaperGoodValue,
    warn = PaperWarnValue,
    bad = PaperBadValue,
    blue = PaperBlueValue,
    purple = PaperPurpleValue,
    geometry = listOf(PaperTerracottaValue, PaperHighlightValue, PaperBlueValue),
    radii = PaperRadii,
    cardBorder = BorderStroke(1.dp, PaperLineValue),
    hairline = BorderStroke(1.dp, PaperLineValue.copy(alpha = .55f)),
    hardShadow = null,
    grain = true,
    dashed = true,
    titleBlock = false,
    decoration = SkinDecoration.PaperTape,
    navSelected = PaperInkValue,
    snackbarContainer = PaperInkValue,
    snackbarContent = PaperBackgroundValue,
    scheme = PaperScheme,
    typography = PaperTypography,
)

val BauhausSkin = Skin(
    id = SkinId.Bauhaus.key,
    label = "包豪斯",
    tagline = "几何 · 三原色",
    background = BauhausPaper,
    card = Color.White,
    cardAlt = BauhausPanel,
    ink = BauhausBlack,
    muted = BauhausMuted,
    line = BauhausBlack,
    accent = BauhausRed,
    onAccent = Color.White,
    highlight = BauhausYellow,
    onHighlight = BauhausBlack,
    good = BauhausBlue,
    warn = BauhausOchre,
    bad = BauhausRed,
    blue = BauhausBlue,
    purple = BauhausBlack,
    geometry = listOf(BauhausRed, BauhausYellow, BauhausBlue),
    radii = SharpRadii,
    cardBorder = BorderStroke(2.dp, BauhausBlack),
    hairline = BorderStroke(2.dp, BauhausBlack),
    hardShadow = HardShadow(5.dp, 5.dp, BauhausBlack),
    grain = false,
    dashed = false,
    titleBlock = true,
    decoration = SkinDecoration.GeometricBar,
    navSelected = BauhausRed,
    snackbarContainer = BauhausBlack,
    snackbarContent = Color.White,
    scheme = BauhausScheme,
    typography = BauhausTypography,
)

enum class SkinId(val key: String, val label: String) {
    Paper("paper", "纸张"),
    Bauhaus("bauhaus", "包豪斯"),
}

val AllSkins = listOf(PaperSkin, BauhausSkin)

fun skinById(id: String?): Skin = when (id) {
    SkinId.Bauhaus.key -> BauhausSkin
    else -> PaperSkin
}

val LocalSkin = staticCompositionLocalOf { PaperSkin }

/** 包豪斯皮肤把所有圆角压平；纸张皮肤保留组件原本的圆角值。 */
fun Skin.corner(value: Dp): Dp = if (hardShadow != null) 0.dp else value

/** 课程色按课程名归类；非 @Composable，需在组合内传入当前皮肤。 */
fun Skin.courseColor(name: String): Color = when {
    name.contains("数学") -> accent
    name.contains("物理") -> blue
    name.contains("程序设计") -> if (this.id == SkinId.Bauhaus.key) BauhausBlack else purple
    else -> if (this.id == SkinId.Bauhaus.key) BauhausOchre else good
}
