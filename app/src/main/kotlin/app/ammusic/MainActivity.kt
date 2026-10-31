package app.ammusic

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Intent
import app.ammusic.data.UserSettings
import app.ammusic.data.cloud.CloudUser
import app.ammusic.data.cloud.SupabaseSyncManager
import app.ammusic.ui.AppRoot
import app.ammusic.ui.theme.AccentChoices
import app.ammusic.ui.theme.AmTheme
import app.ammusic.ui.theme.ThemeMode
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var cloudSync: SupabaseSyncManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleDeepLink(intent)
        if (Build.VERSION.SDK_INT >= 31) {
            splashScreen.setOnExitAnimationListener { splash ->
                splash.remove()
            }
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {}.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            val settings by UserSettings.state.collectAsStateWithLifecycle()
            val themeMode = ThemeMode.entries[settings.theme.coerceIn(0, ThemeMode.entries.lastIndex)]
            val accentIndex = settings.accent.coerceIn(0, AccentChoices.lastIndex)
            val dark = when (themeMode) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Dark -> true
                ThemeMode.Light -> false
            }

            DisposableEffect(dark) {
                val style = if (dark) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }

            AmTheme(dark = dark, accent = AccentChoices[accentIndex]) {
                AppRoot(
                    themeMode = themeMode,
                    onThemeMode = { mode -> UserSettings.update { copy(theme = mode.ordinal) } },
                    accentIndex = accentIndex,
                    onAccent = { index -> UserSettings.update { copy(accent = index) } },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "rmusic" && uri.host == "auth-callback") {
            val raw = uri.fragment ?: uri.query ?: return
            val params = raw.split("&").mapNotNull {
                val parts = it.split("=")
                if (parts.size == 2) parts[0] to parts[1] else null
            }.toMap()

            val accessToken = params["access_token"]
            val refreshToken = params["refresh_token"] ?: ""
            if (accessToken != null) {
                cloudSync.handleAuthCallback(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    user = CloudUser(
                        id = params["user_id"] ?: "user",
                        provider = params["provider"] ?: "oauth",
                    )
                )
            }
        }
    }
}
