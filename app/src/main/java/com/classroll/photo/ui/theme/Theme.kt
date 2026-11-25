package com.classroll.photo.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember

@Composable
fun ClassrollTheme(
    skinId: String = SkinId.Paper.key,
    content: @Composable () -> Unit,
) {
    val skin = remember(skinId) { skinById(skinId) }
    val shapes = remember(skin) {
        Shapes(
            extraSmall = RoundedCornerShape(skin.radii.xs),
            small = RoundedCornerShape(skin.radii.small),
            medium = RoundedCornerShape(skin.radii.medium),
            large = RoundedCornerShape(skin.radii.large),
            extraLarge = RoundedCornerShape(skin.radii.extraLarge),
        )
    }
    CompositionLocalProvider(LocalSkin provides skin) {
        MaterialTheme(
            colorScheme = skin.scheme,
            typography = skin.typography,
            shapes = shapes,
            content = content,
        )
    }
}
