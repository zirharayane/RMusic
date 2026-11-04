package app.ammusic.ui.settings

import app.ammusic.youtube.tr

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.Arrangement
import app.ammusic.data.cloud.CloudUser
import app.ammusic.playback.SleepTimer
import app.ammusic.ui.AccountViewModel
import app.ammusic.ui.components.AmIcon
import app.ammusic.ui.components.AmIcons
import app.ammusic.ui.components.AmText
import app.ammusic.ui.components.Cover
import app.ammusic.ui.components.IconButton
import app.ammusic.ui.components.LargeTitleScreen
import app.ammusic.ui.components.LocalActions
import app.ammusic.ui.components.MenuAction
import app.ammusic.ui.components.MenuRequest
import app.ammusic.ui.components.RowCoverSize
import app.ammusic.ui.components.SegmentedControl
import app.ammusic.ui.library.storageSize
import app.ammusic.data.EqualizerPresets
import app.ammusic.ui.components.hairlineBelow
import app.ammusic.ui.components.pressable
import app.ammusic.ui.components.slidingPill
import app.ammusic.ui.theme.AccentChoices
import app.ammusic.ui.theme.AmTheme
import app.ammusic.ui.theme.ThemeMode
import app.ammusic.youtube.model.YtAccount
import app.ammusic.youtube.stream.AudioQuality
import kotlinx.coroutines.delay

private val ThemeLabels get() = listOf(tr("Système", "System"), tr("Sombre", "Dark"), tr("Clair", "Light"))
private val LanguageCodes = listOf("", "fr", "en")
private val LanguageLabels get() = listOf(tr("Système", "System"), "Français", "English")
private val QualityLabels get() = listOf("Auto", tr("Haute", "High"), tr("Éco", "Saver"))
private val SizeLabels get() = listOf(tr("Petit", "Small"), tr("Moyen", "Medium"), tr("Grand", "Large"))
private val CrossfadeSeconds = listOf(0, 3, 6, 9, 12)
private val CrossfadeLabels get() = listOf(tr("Non", "Off"), "3 s", "6 s", "9 s", "12 s")
private val SleepMinutes = listOf(null, 15, 30, 60)
private val SleepLabels get() = listOf(tr("Non", "Off"), "15 min", "30 min", "1 h")

@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeMode: (ThemeMode) -> Unit,
    accentIndex: Int,
    onAccent: (Int) -> Unit,
    onBack: () -> Unit,
    onEqualizer: () -> Unit,
    accountViewModel: AccountViewModel = hiltViewModel(),
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val colors = AmTheme.colors
    val context = LocalContext.current
    val cloudUser by viewModel.cloudUser.collectAsStateWithLifecycle()
    val isCloudSyncing by viewModel.isCloudSyncing.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val cacheBytes by viewModel.cacheBytes.collectAsStateWithLifecycle()
    val downloadBytes by viewModel.downloadBytes.collectAsStateWithLifecycle()
    val updateAvailable by viewModel.updateAvailable.collectAsStateWithLifecycle()
    val updateProgress by viewModel.updateProgress.collectAsStateWithLifecycle()

    LargeTitleScreen(
        title = tr("Réglages", "Settings"),
        leading = { IconButton(AmIcons.ChevronLeft, label = tr("Retour", "Back"), onClick = onBack, tint = colors.accent, iconSize = 26.dp) },
    ) {
        item(key = "cloud_account") {
            CloudAccountCard(
                user = cloudUser,
                isSyncing = isCloudSyncing,
                onSignInDiscord = { viewModel.signInWithDiscord(context) },
                onSignInGoogle = { viewModel.signInWithGoogle(context) },
                onSyncNow = viewModel::syncCloud,
                onSignOut = viewModel::signOutCloud,
            )
        }
        item(key = "look") {
            Group(header = tr("Apparence", "Appearance")) {
                Choice(tr("Thème", "Theme"), ThemeLabels, themeMode.ordinal) { onThemeMode(ThemeMode.entries[it]) }
            }
        }

        item(key = "playback") {
            Group(
                header = tr("Lecture", "Playback"),
            ) {
                Choice(tr("Qualité en Wi-Fi", "Quality on Wi-Fi"), QualityLabels, settings.wifiQuality.ordinal) {
                    viewModel.update { copy(wifiQuality = AudioQuality.entries[it]) }
                }
                Divider()
                Choice(tr("Qualité en données mobiles", "Quality on mobile data"), QualityLabels, settings.mobileQuality.ordinal) {
                    viewModel.update { copy(mobileQuality = AudioQuality.entries[it]) }
                }
                Divider()
                Toggle(tr("Enchaîner avec des titres similaires", "Keep playing similar songs"), settings.autoplay) {
                    viewModel.update { copy(autoplay = it) }
                }
                Divider()
                Link(tr("Égaliseur", "Equalizer"), equalizerLabel(settings.eqPreset), onEqualizer)
                Divider()
                Toggle(tr("Volume égal entre les titres", "Even out volume between songs"), settings.normalizeVolume) { viewModel.update { copy(normalizeVolume = it) } }
                Divider()
                Choice(tr("Fondu enchaîné", "Crossfade"), CrossfadeLabels, CrossfadeSeconds.indexOf(settings.crossfadeSeconds).coerceAtLeast(0)) {
                    viewModel.update { copy(crossfadeSeconds = CrossfadeSeconds[it]) }
                }
            }
        }

        item(key = "skip") {
            Group(
                footer = tr("Via SponsorBlock.", "Uses SponsorBlock."),
            ) {
                Toggle(tr("Passer les passages sans musique", "Skip non-music parts"), settings.skipSegments) {
                    viewModel.update { copy(skipSegments = it) }
                }
            }
        }

        item(key = "lyrics") {
            Group(header = tr("Paroles", "Lyrics")) {
                Choice(tr("Taille du texte", "Text size"), SizeLabels, settings.lyricsSize) { viewModel.update { copy(lyricsSize = it) } }
                Divider()
                Toggle(tr("Suivre les paroles mot à mot", "Follow lyrics word by word"), settings.wordByWord) {
                    viewModel.update { copy(wordByWord = it) }
                }
            }
        }

        item(key = "downloads") {
            Group(header = tr("Téléchargements", "Downloads")) {
                Toggle(tr("Télécharger seulement en Wi-Fi", "Download on Wi-Fi only"), settings.downloadOnWifi) { viewModel.update { copy(downloadOnWifi = it) } }
                Divider()
                Value(tr("Espace utilisé", "Space used"), storageSize(downloadBytes))
                Divider()
                Action(tr("Supprimer les téléchargements", "Remove all downloads")) { viewModel.removeDownloads() }
            }
        }

        item(key = "storage") {
            Group(
                header = tr("Stockage", "Storage"),
            ) {
                Value(tr("Limite du cache audio", "Audio cache limit"), tr("Illimité", "Unlimited"))
                Divider()
                Value(tr("Espace utilisé", "Space used"), megabytes((cacheBytes / (1024 * 1024)).toInt()))
                Divider()
                Action(tr("Vider le cache", "Clear cache")) { viewModel.clearCache() }
            }
        }

        item(key = "backup") {
            val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let(viewModel::exportTo) }
            val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(viewModel::importFrom) }
            Group(
                header = tr("Sauvegarde", "Backup"),
                footer = tr("Playlists de ce téléphone, titres aimés sans compte et réglages.", "Playlists on this phone, liked songs and settings."),
            ) {
                Action(tr("Exporter dans un fichier", "Export to a file")) { exporter.launch(viewModel.backupName()) }
                Divider()
                Action(tr("Restaurer une sauvegarde", "Restore a backup")) { importer.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) }
            }
        }

        item(key = "updates") {
            val progress = updateProgress
            val info = updateAvailable
            Group(header = tr("Mises à jour", "Updates")) {
                if (info != null) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        AmText(
                            text = tr("Mise à jour v${info.versionName} disponible !", "Update v${info.versionName} available!"),
                            style = AmTheme.type.headline.copy(fontWeight = FontWeight.Bold),
                            color = colors.accent,
                        )
                        if (info.changelog.isNotBlank()) {
                            AmText(
                                text = info.changelog,
                                style = AmTheme.type.footnote,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                            )
                        }
                        if (progress != null) {
                            AmText(
                                text = tr("Téléchargement : ${(progress * 100).toInt()}%", "Downloading: ${(progress * 100).toInt()}%"),
                                style = AmTheme.type.footnote,
                                color = colors.accent,
                            )
                        } else {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.accent)
                                    .pressable(haptic = true, label = "Update") {
                                        viewModel.startUpdate(info)
                                    },
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AmText(tr("Installer maintenant", "Install now"), style = AmTheme.type.headline.copy(fontWeight = FontWeight.Bold), color = Color.White)
                            }
                        }
                    }
                } else {
                    Action(tr("Rechercher des mises à jour", "Check for updates")) {
                        viewModel.checkForUpdates()
                    }
                }
            }
        }

        if (cloudUser != null) {
            item(key = "bigSignOut") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp)
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFE53935))
                        .pressable(haptic = true, label = tr("Se déconnecter", "Log out")) {
                            viewModel.signOutCloud()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    AmText(
                        text = tr("Se déconnecter", "Log Out"),
                        style = AmTheme.type.headline.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                    )
                }
            }
        }

        item(key = "version") {
            val version = remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty() }
            AmText(
                text = "RMusic (RM) $version",
                style = AmTheme.type.footnote,
                color = colors.textTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 12.dp),
            )
        }
    }
}

private fun megabytes(value: Int): String = when {
    value >= 1024 -> "${(value / 102.4f).toInt() / 10f}".removeSuffix(".0") + tr(" Go", " GB")
    else -> "$value" + tr(" Mo", " MB")
}

@Composable
private fun sleepStatus(sleep: SleepTimer): String? {
    val now = remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(sleep) {
        while (sleep.endsAtMs != null) {
            now.longValue = System.currentTimeMillis()
            delay(5_000)
        }
    }
    val end = sleep.endsAtMs
    return when {
        end != null -> {
            val minutes = ((end - now.longValue + 59_999) / 60_000).coerceAtLeast(1)
            tr("Arrêt dans $minutes min", "Stops in $minutes min")
        }
        else -> null
    }
}

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

@Composable
private fun Group(header: String? = null, footer: String? = null, content: @Composable ColumnScope.() -> Unit) {
    val colors = AmTheme.colors
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = if (header != null) 26.dp else 18.dp)) {
        if (header != null) {
            AmText(
                text = header.uppercase(),
                style = AmTheme.type.footnote.copy(letterSpacing = 0.04.em),
                color = colors.textSecondary,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
            )
        }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.surface), content = content)
        if (footer != null) {
            AmText(
                text = footer,
                style = AmTheme.type.footnote,
                color = colors.textSecondary,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
            )
        }
    }
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).hairlineBelow(AmTheme.colors.hairline, 16.dp))
}

@Composable
private fun Choice(label: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 13.dp, bottom = 14.dp)) {
        AmText(label, maxLines = 1)
        Spacer(Modifier.height(10.dp))
        SegmentedControl(options, selected, onSelect = onSelect)
    }
}

@Composable
private fun Toggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = AmTheme.colors
    val knob by animateFloatAsState(if (checked) 1f else 0f, spring(dampingRatio = 1f, stiffness = 520f), label = "switch")
    Row(
        Modifier
            .fillMaxWidth()
            .pressable(scale = 0.99f, haptic = true, label = label) { onChange(!checked) }
            .heightIn(min = 52.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AmText(label, maxLines = 2, modifier = Modifier.weight(1f).padding(end = 12.dp))
        Canvas(Modifier.size(width = 51.dp, height = 31.dp)) {
            val on = knob.coerceIn(0f, 1f)
            drawRoundRect(lerp(colors.text.copy(alpha = 0.16f), colors.accent, on), cornerRadius = CornerRadius(size.height / 2f))
            val gap = 2.dp.toPx()
            val radius = size.height / 2f - gap
            val x = gap + radius + (size.width - 2f * (gap + radius)) * knob
            drawCircle(Color(0x33000000), radius, Offset(x, size.height / 2f + 1.dp.toPx()))
            drawCircle(Color.White, radius, Offset(x, size.height / 2f))
        }
    }
}

@Composable
private fun Value(label: String, value: String) {
    Row(Modifier.fillMaxWidth().heightIn(min = 50.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        AmText(label, maxLines = 1, modifier = Modifier.weight(1f))
        AmText(value, color = AmTheme.colors.textSecondary, maxLines = 1)
    }
}

@Composable
private fun Action(label: String, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().pressable(scale = 0.99f, haptic = true, label = label, onClick = onClick).heightIn(min = 50.dp).padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        AmText(label, color = AmTheme.colors.accent, maxLines = 1)
    }
}

@Composable
private fun AccentPicker(selected: Int, onSelect: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 13.dp, bottom = 8.dp)) {
        AmText(tr("Couleur d'accent", "Accent color"), maxLines = 1, modifier = Modifier.padding(horizontal = 16.dp))
        Row(Modifier.padding(start = 8.dp, top = 4.dp)) {
            AccentChoices.forEachIndexed { index, color ->
                val ring by animateFloatAsState(
                    targetValue = if (index == selected) 1f else 0f,
                    animationSpec = spring(dampingRatio = 1f, stiffness = 440f),
                    label = "accentRing",
                )
                Box(
                    Modifier.size(48.dp).pressable(scale = 0.85f, haptic = true, label = tr("Couleur ${index + 1}", "Color ${index + 1}")) { onSelect(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .graphicsLayer {
                                alpha = ring
                                scaleX = 0.8f + 0.2f * ring
                                scaleY = scaleX
                            }
                            .border(2.dp, color, CircleShape),
                    )
                    Box(Modifier.size(28.dp).clip(CircleShape).background(color))
                }
            }
        }
    }
}

@Composable
private fun CloudAccountCard(
    user: CloudUser?,
    isSyncing: Boolean,
    onSignInDiscord: () -> Unit,
    onSignInGoogle: () -> Unit,
    onSyncNow: () -> Unit,
    onSignOut: () -> Unit,
) {
    val colors = AmTheme.colors
    Column(
        Modifier
            .padding(start = 20.dp, end = 20.dp, top = 14.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .padding(16.dp),
    ) {
        if (user == null) {
            AmText(
                tr("Sauvegarde dans le Cloud", "Cloud Sync"),
                style = AmTheme.type.section.copy(fontWeight = FontWeight.SemiBold),
                color = colors.text,
            )
            AmText(
                tr("Connectez-vous pour synchroniser vos playlists et titres aimés.", "Sign in to sync your playlists and liked songs."),
                style = AmTheme.type.footnote,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF5865F2))
                    .pressable(haptic = true, label = "Discord") { onSignInDiscord() },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AmText(tr("Se connecter avec Discord", "Sign in with Discord"), style = AmTheme.type.headline.copy(fontWeight = FontWeight.Bold), color = Color.White)
            }
        } else {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (user.avatarUrl != null) {
                    Cover(user.avatarUrl, Modifier.size(50.dp), circle = true, requestSize = RowCoverSize)
                } else {
                    Box(Modifier.size(50.dp).clip(CircleShape).background(colors.surfaceHigh), contentAlignment = Alignment.Center) {
                        AmIcon(AmIcons.User, tint = colors.accent, size = 24.dp)
                    }
                }
                Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                    AmText(user.name ?: user.email ?: "Utilisateur", style = AmTheme.type.section.copy(fontWeight = FontWeight.SemiBold), maxLines = 1)
                    AmText(
                        if (isSyncing) tr("Synchronisation en cours…", "Syncing…") else tr("Playlists & titres synchronisés", "Playlists & tracks synced"),
                        style = AmTheme.type.footnote,
                        color = if (isSyncing) colors.accent else colors.textSecondary,
                        maxLines = 1,
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.accent.copy(alpha = 0.15f))
                        .pressable(haptic = true, label = "Sync", onClick = onSyncNow)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    AmText(
                        text = if (isSyncing) tr("Sync…", "Sync…") else tr("Cloud Actif", "Cloud Synced"),
                        style = AmTheme.type.footnote.copy(fontWeight = FontWeight.Bold),
                        color = colors.accent,
                    )
                }
            }
        }
    }
}

private fun equalizerLabel(preset: String): String = when (preset) {
    "off" -> tr("Désactivé", "Off")
    "custom" -> tr("Personnalisé", "Custom")
    else -> EqualizerPresets.firstOrNull { it.id == preset }?.label ?: tr("Désactivé", "Off")
}

@Composable
private fun Link(label: String, value: String, onClick: () -> Unit) {
    val colors = AmTheme.colors
    Row(
        Modifier.fillMaxWidth().pressable(scale = 0.99f, label = label, onClick = onClick).heightIn(min = 50.dp).padding(start = 16.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AmText(label, maxLines = 1, modifier = Modifier.weight(1f))
        AmText(value, color = colors.textSecondary, maxLines = 1, modifier = Modifier.padding(end = 6.dp))
        AmIcon(AmIcons.ChevronRight, tint = colors.textTertiary, size = 15.dp)
    }
}
