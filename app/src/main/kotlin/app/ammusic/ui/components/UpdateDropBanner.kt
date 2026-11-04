package app.ammusic.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ammusic.data.update.UpdateInfo
import app.ammusic.ui.theme.AmTheme
import app.ammusic.youtube.tr

@Composable
fun UpdateDropBanner(
    info: UpdateInfo?,
    progress: Float?,
    onInstall: (UpdateInfo) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AmTheme.colors

    AnimatedVisibility(
        visible = info != null,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(dampingRatio = 0.82f, stiffness = 400f)
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = spring(dampingRatio = 0.85f, stiffness = 450f)
        ) + fadeOut(),
        modifier = modifier,
    ) {
        if (info == null) return@AnimatedVisibility

        Row(
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth()
                .shadow(elevation = 16.dp, shape = RoundedCornerShape(16.dp), ambientColor = Color(0x66000000), spotColor = colors.accent.copy(alpha = 0.45f))
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surfaceHigh)
                .border(1.dp, colors.accent.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .padding(start = 14.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                AmIcon(AmIcons.Download, tint = colors.accent, size = 22.dp)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                AmText(
                    text = tr("Mise à jour v${info.versionName} disponible !", "Update v${info.versionName} available!"),
                    style = AmTheme.type.headline.copy(fontWeight = FontWeight.Bold, fontSize = 14.5.sp),
                    color = colors.text,
                    maxLines = 1,
                )
                AmText(
                    text = if (progress != null) {
                        tr("Téléchargement : ${(progress * 100).toInt()}%", "Downloading: ${(progress * 100).toInt()}%")
                    } else if (info.changelog.isNotBlank()) {
                        info.changelog.lines().firstOrNull { it.isNotBlank() } ?: tr("Appuyez pour installer maintenant", "Tap to install now")
                    } else {
                        tr("Appuyez pour installer maintenant", "Tap to install now")
                    },
                    style = AmTheme.type.footnote,
                    color = if (progress != null) colors.accent else colors.textSecondary,
                    maxLines = 1,
                )
            }

            if (progress != null) {
                ProgressRing(
                    fraction = progress,
                    color = colors.accent,
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .size(26.dp),
                    stroke = 3.dp,
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.accent)
                            .pressable(haptic = true, label = "Install") { onInstall(info) }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        AmText(
                            text = tr("Installer", "Install"),
                            style = AmTheme.type.footnote.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                        )
                    }

                    Spacer(Modifier.width(4.dp))

                    IconButton(
                        icon = AmIcons.Close,
                        label = tr("Ignorer", "Dismiss"),
                        onClick = onDismiss,
                        tint = colors.textTertiary,
                        iconSize = 16.dp,
                    )
                }
            }
        }
    }
}

