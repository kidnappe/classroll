package com.classroll.photo.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.classroll.photo.model.Verdict
import com.classroll.photo.ui.theme.corner
import com.classroll.photo.ui.theme.LocalSkin
import com.classroll.photo.ui.theme.PaperCard
import com.classroll.photo.ui.theme.PaperGood
import com.classroll.photo.ui.theme.PaperHighlight
import com.classroll.photo.ui.theme.PaperInk
import com.classroll.photo.ui.theme.PaperLine
import com.classroll.photo.ui.theme.PaperTerracotta
import com.classroll.photo.ui.theme.SkinDecoration

/** 皮肤投影：纸张用柔和阴影，包豪斯用无模糊的实心偏移块。 */
@Composable
fun Modifier.skinShadow(
    elevation: Dp = 8.dp,
    ambient: Float = .08f,
    spot: Float = .18f,
    shape: Shape = MaterialTheme.shapes.medium,
): Modifier {
    val skin = LocalSkin.current
    val hard = skin.hardShadow
    return if (hard != null) {
        drawBehind {
            // 包豪斯皮肤全部使用直角形状，硬投影直接画偏移矩形。
            translate(hard.dx.toPx(), hard.dy.toPx()) { drawRect(hard.color) }
        }
    } else {
        shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = skin.ink.copy(alpha = ambient),
            spotColor = skin.ink.copy(alpha = spot),
        )
    }
}

/** 依据底色亮度选可读的前景色。 */
fun Color.readableInk(): Color = if (luminance() > .5f) Color(0xFF121212) else Color.White

/** 纸张颗粒感：极淡的规则网点；包豪斯皮肤关闭，保持纯色平面。 */
@Composable
fun Modifier.paperGrain(): Modifier {
    val skin = LocalSkin.current
    if (!skin.grain) return this
    val dotColor = skin.ink
    return drawBehind {
        val step = 16.dp.toPx()
        val radius = 0.7.dp.toPx()
        var y = step
        while (y < size.height) {
            var x = step
            while (x < size.width) {
                drawCircle(dotColor.copy(alpha = .022f), radius, Offset(x, y))
                x += step
            }
            y += step
        }
    }
}

/** 卡片顶部装饰：纸张是和纸胶带，包豪斯是三原色拼条。 */
@Composable
fun PaperTape(modifier: Modifier = Modifier) {
    val skin = LocalSkin.current
    when (skin.decoration) {
        SkinDecoration.PaperTape -> Box(
            modifier = modifier
                .width(74.dp)
                .height(18.dp)
                .rotate(-1.5f)
                .background(Color.White.copy(alpha = .55f), RoundedCornerShape(LocalSkin.current.corner(1.dp)))
                .border(0.5.dp, skin.ink.copy(alpha = .10f), RoundedCornerShape(LocalSkin.current.corner(1.dp))),
        )
        SkinDecoration.GeometricBar -> Row(
            modifier = modifier
                .width(66.dp)
                .height(6.dp)
                .border(1.dp, skin.ink),
        ) {
            skin.geometry.forEach { color -> Box(Modifier.weight(1f).height(6.dp).background(color)) }
        }
    }
}

@Composable
fun VerticalDashedDivider(
    height: Dp,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.outline,
) {
    val dashed = LocalSkin.current.dashed
    Box(
        modifier = modifier
            .width(if (dashed) 1.dp else 2.dp)
            .height(height)
            .drawBehind {
                if (dashed) {
                    drawLine(
                        color = color,
                        start = Offset(size.width / 2f, 0f),
                        end = Offset(size.width / 2f, size.height),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.5f, 3.5f), 0f),
                    )
                } else {
                    drawRect(color)
                }
            },
    )
}

@Composable
fun HorizontalDashedDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.outline,
) {
    val dashed = LocalSkin.current.dashed
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(if (dashed) 1.dp else 2.dp)
            .drawBehind {
                if (dashed) {
                    drawLine(
                        color = color,
                        start = Offset(0f, size.height / 2f),
                        end = Offset(size.width, size.height / 2f),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.5f, 3.5f), 0f),
                    )
                } else {
                    drawRect(color)
                }
            },
    )
}

fun formatCount(value: Int): String = "%,d".format(value)

@Composable
fun ClassrollCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val skin = LocalSkin.current
    Surface(
        modifier = modifier.skinShadow(8.dp, .08f, .18f, MaterialTheme.shapes.medium),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = skin.cardBorder,
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    keepPrimaryColor: Boolean = false,
    icon: ImageVector? = null,
) {
    val skin = LocalSkin.current
    Button(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 48.dp)
            .skinShadow(6.dp, .10f, .26f, MaterialTheme.shapes.small),
        enabled = enabled && !loading,
        shape = MaterialTheme.shapes.small,
        border = skin.cardBorder,
        colors = if (keepPrimaryColor) {
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.primary,
                disabledContentColor = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            Spacer(Modifier.width(10.dp))
        } else if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val skin = LocalSkin.current
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 48.dp)
            .then(if (skin.hardShadow != null) Modifier.skinShadow(4.dp, .10f, .26f, MaterialTheme.shapes.small) else Modifier),
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        border = skin.cardBorder,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (skin.hardShadow != null) Color.White else Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun AppIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(44.dp)
            .border(LocalSkin.current.hairline, CircleShape),
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(22.dp))
    }
}

@Composable
fun ScreenTitle(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    val skin = LocalSkin.current
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (skin.titleBlock) {
            val blockColor = skin.geometry[Math.floorMod(title.hashCode(), skin.geometry.size)]
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(blockColor)
                    .border(1.dp, skin.ink),
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
fun SectionHeading(title: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
fun InfoChip(text: String, icon: ImageVector? = null, modifier: Modifier = Modifier) {
    val skin = LocalSkin.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(skin.radii.pill),
        color = MaterialTheme.colorScheme.surface,
        border = skin.hairline,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = PaperGood)
                Spacer(Modifier.width(5.dp))
            }
            Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun StatSummary(items: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    val skin = LocalSkin.current
    Box(modifier = modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .skinShadow(8.dp, .08f, .18f, MaterialTheme.shapes.medium),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            border = skin.cardBorder,
        ) {
            Row(modifier = Modifier.padding(vertical = 16.dp)) {
                items.forEachIndexed { index, (value, label) ->
                    if (index > 0) VerticalDashedDivider(height = 38.dp)
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            value,
                            style = MaterialTheme.typography.titleLarge,
                            color = if (index == 0) skin.accent else MaterialTheme.colorScheme.onSurface,
                        )
                        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        PaperTape(Modifier.align(Alignment.TopCenter))
    }
}

@Composable
fun VerdictBadge(verdict: Verdict, modifier: Modifier = Modifier) {
    val skin = LocalSkin.current
    val color = when (verdict) {
        Verdict.Likely -> PaperGood
        Verdict.Doubtful -> skin.warn
        Verdict.Boundary -> PaperTerracotta
        Verdict.Unlike -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val solid = skin.hardShadow != null
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(skin.radii.pill),
        color = if (solid) color else color.copy(alpha = .12f),
        border = if (solid) skin.hairline else null,
    ) {
        Text(
            verdict.label,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = if (solid) color.readableInk() else color,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun VerdictTabs(
    selected: Verdict,
    counts: Map<Verdict, Int>,
    onSelect: (Verdict) -> Unit,
    modifier: Modifier = Modifier,
) {
    val skin = LocalSkin.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(skin.radii.tabShell))
            .then(if (skin.hardShadow != null) Modifier.border(skin.hairline, RoundedCornerShape(skin.radii.tabShell)) else Modifier)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Verdict.entries.forEach { verdict ->
            val active = verdict == selected
            Surface(
                modifier = Modifier.weight(1f).height(42.dp),
                shape = RoundedCornerShape(skin.radii.tabItem),
                color = if (active) MaterialTheme.colorScheme.primary else Color.Transparent,
                onClick = { onSelect(verdict) },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        verdict.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        (counts[verdict] ?: 0).toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (active) MaterialTheme.colorScheme.onPrimary.copy(alpha = .7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
