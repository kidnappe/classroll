package com.classroll.photo

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classroll.photo.data.ThemeSettingsStore
import com.classroll.photo.ui.AppShell
import com.classroll.photo.ui.theme.ClassrollTheme
import com.classroll.photo.ui.theme.SkinId
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            val context = LocalContext.current
            val store = remember { ThemeSettingsStore(context) }
            val skinId by store.skinId.collectAsStateWithLifecycle(initialValue = SkinId.Paper.key)
            val scope = rememberCoroutineScope()
            ClassrollTheme(skinId = skinId) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppShell(
                        skinId = skinId,
                        onSkinChange = { id -> scope.launch { store.saveSkin(id) } },
                    )
                }
            }
        }
    }
}
