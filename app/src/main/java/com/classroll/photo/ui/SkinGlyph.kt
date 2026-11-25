package com.classroll.photo.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.classroll.photo.ui.theme.LocalSkin
import com.classroll.photo.ui.theme.Skin

/** 皮肤图标：纸张画叠纸与胶带，包豪斯画四色方块。默认使用当前皮肤。 */
@Composable
fun SkinGlyph(
    skin: Skin = LocalSkin.current,
    glyphSize: Dp = 20.dp,
    modifier: Modifier = Modifier,
) {
    if (skin.hardShadow != null) {
        Row(
            modifier = modifier
                .size(glyphSize)
                .background(Color.White)
                .border(1.dp, skin.ink),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Box(Modifier.weight(1f).fillMaxWidth().background(skin.geometry[0]))
                Box(Modifier.weight(1f).fillMaxWidth().background(skin.geometry[2]))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Box(Modifier.weight(1f).fillMaxWidth().background(skin.geometry[1]))
                Box(Modifier.weight(1f).fillMaxWidth().background(skin.ink))
            }
        }
    } else {
        val sheetColor = skin.card
        val backColor = skin.cardAlt
        val edgeColor = skin.line
        val lineColor = skin.accent
        val tapeColor = skin.accent
        Canvas(modifier = modifier.size(glyphSize)) {
            val w = size.width
            val h = size.height
            val corner = CornerRadius(1.5.dp.toPx())
            val backTopLeft = Offset(w * .27f, h * .19f)
            val backSize = Size(w * .42f, h * .74f)
            rotate(degrees = -8f, pivot = Offset(w * .48f, h * .56f)) {
                drawRoundRect(backColor, backTopLeft, backSize, corner)
                drawRoundRect(edgeColor, backTopLeft, backSize, corner, style = Stroke(1.dp.toPx()))
            }
            val frontTopLeft = Offset(w * .23f, h * .14f)
            val frontSize = Size(w * .52f, h * .78f)
            drawRoundRect(sheetColor, frontTopLeft, frontSize, corner)
            drawRoundRect(edgeColor, frontTopLeft, frontSize, corner, style = Stroke(1.dp.toPx()))
            repeat(3) { index ->
                val y = h * (.48f + index * .13f)
                drawLine(
                    lineColor.copy(alpha = .45f),
                    Offset(w * .34f, y),
                    Offset(w * .64f, y),
                    strokeWidth = 1.3.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            rotate(degrees = -12f, pivot = Offset(w * .5f, h * .17f)) {
                drawRect(
                    tapeColor.copy(alpha = .30f),
                    topLeft = Offset(w * .15f, h * .09f),
                    size = Size(w * .70f, h * .16f),
                )
                drawRect(
                    tapeColor.copy(alpha = .45f),
                    topLeft = Offset(w * .15f, h * .09f),
                    size = Size(w * .70f, h * .16f),
                    style = Stroke(0.6.dp.toPx()),
                )
            }
        }
    }
}
