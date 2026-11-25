package com.classroll.photo.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/** 让 Material3 的日期选择器固定显示简体中文，不受系统语言影响。 */
@Composable
fun ChineseLocale(content: @Composable () -> Unit) {
    val base = LocalConfiguration.current
    val context = LocalContext.current
    val configuration = remember(base) {
        Configuration(base).apply {
            setLocale(Locale.SIMPLIFIED_CHINESE)
            setLayoutDirection(Locale.SIMPLIFIED_CHINESE)
        }
    }
    val localizedContext = remember(context, configuration) {
        context.createConfigurationContext(configuration)
    }
    CompositionLocalProvider(
        LocalConfiguration provides configuration,
        LocalContext provides localizedContext,
        content = content,
    )
}
