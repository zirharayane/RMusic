package app.ammusic.data.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import app.ammusic.MainActivity
import app.ammusic.R
import app.ammusic.youtube.tr
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val changelog: String = "",
)

@Singleton
class AppUpdateManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    // Custom OTA updater hosted on your website
    var updateManifestUrl: String = "https://music.rayane.top/rmusic-update.json"

    private val prefs = context.getSharedPreferences("app_update_prefs", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = OkHttpClient.Builder()
        .callTimeout(30, TimeUnit.SECONDS)
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    private val _updateAvailable = MutableStateFlow<UpdateInfo?>(null)
    val updateAvailable: StateFlow<UpdateInfo?> = _updateAvailable.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Float?>(null)
    val downloadProgress: StateFlow<Float?> = _downloadProgress.asStateFlow()

    val currentVersionName: String
        get() = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.5.2"
        } catch (_: Exception) { "1.5.2" }

    val currentVersionCode: Int
        get() = try {
            PackageInfoCompat.getLongVersionCode(context.packageManager.getPackageInfo(context.packageName, 0)).toInt()
        } catch (_: Exception) { 8 }

    init {
        // Automatically check for update on app startup
        scope.launch {
            checkUpdate(silent = true)
        }
    }

    suspend fun checkUpdate(silent: Boolean = false) = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(updateManifestUrl)
                .header("User-Agent", "RMusic-Android/$currentVersionName")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    if (!silent) showToast(tr("Impossible de vérifier les mises à jour", "Could not check for updates"))
                    return@use
                }
                val body = response.body?.string() ?: return@use
                val info = json.decodeFromString<UpdateInfo>(body)

                if (info.versionCode > currentVersionCode) {
                    _updateAvailable.value = info
                    val lastNotified = prefs.getInt("last_notified_version", 0)
                    if (lastNotified != info.versionCode) {
                        showSystemNotification(info)
                        prefs.edit().putInt("last_notified_version", info.versionCode).apply()
                    }
                    if (!silent) {
                        showToast(tr("Mise à jour disponible: v${info.versionName}", "Update available: v${info.versionName}"))
                        showSystemNotification(info)
                    }
                } else {
                    _updateAvailable.value = null
                    if (!silent) {
                        showToast(tr("RMusic est à jour (v$currentVersionName)", "RMusic is up to date (v$currentVersionName)"))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AppUpdateManager", "Check failed: ${e.message}")
            if (!silent) {
                showToast(tr("Impossible de joindre le serveur de mise à jour", "Could not reach update server"))
            }
        }
    }

    fun showSystemNotification(info: UpdateInfo) {
        try {
            val channelId = "app_updates"
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    tr("Mises à jour", "App Updates"),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = tr("Notifications des nouvelles versions de RMusic", "Notifications for new RMusic releases")
                    enableLights(true)
                    enableVibration(true)
                }
                manager.createNotificationChannel(channel)
            }

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            val text = if (info.changelog.isNotBlank()) {
                "${tr("Nouvelle version v${info.versionName} :", "New version v${info.versionName} :")}\n${info.changelog}"
            } else {
                tr("Une nouvelle version de RMusic est prête à être installée.", "A new version of RMusic is ready to install.")
            }

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(tr("Mise à jour RMusic disponible (v${info.versionName})", "RMusic update available (v${info.versionName})"))
                .setContentText(tr("Appuyez pour installer la nouvelle version.", "Tap to install the new version."))
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            manager.notify(1001, builder.build())
        } catch (e: Exception) {
            Log.e("AppUpdateManager", "Failed to show system notification: ${e.message}")
        }
    }

    fun startDownloadAndInstall(info: UpdateInfo) {
        if (_downloadProgress.value != null) return
        scope.launch {
            _downloadProgress.value = 0f
            try {
                val apkFile = downloadApk(info.apkUrl) { fraction ->
                    _downloadProgress.value = fraction
                }
                _downloadProgress.value = null
                if (apkFile != null && apkFile.exists()) {
                    installApk(apkFile)
                } else {
                    showToast(tr("Échec du téléchargement de la mise à jour", "Update download failed"))
                }
            } catch (e: Exception) {
                Log.e("AppUpdateManager", "Download failed: ${e.message}")
                _downloadProgress.value = null
                showToast(tr("Erreur lors de la mise à jour: ${e.message}", "Update error: ${e.message}"))
            }
        }
    }

    private suspend fun downloadApk(url: String, onProgress: (Float) -> Unit): File? = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@use null
            val body = response.body ?: return@use null
            val totalBytes = body.contentLength()

            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            val file = File(dir, "RMusic-update.apk")
            if (file.exists()) file.delete()

            body.byteStream().use { input ->
                FileOutputStream(file).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    var totalRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (totalBytes > 0) {
                            onProgress(totalRead.toFloat() / totalBytes)
                        }
                    }
                    output.flush()
                }
            }
            file
        }
    }

    fun installApk(file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("AppUpdateManager", "Install failed: ${e.message}")
            showToast(tr("Impossible d'installer: ${e.message}", "Cannot install: ${e.message}"))
        }
    }

    fun dismissUpdate() {
        _updateAvailable.value = null
    }

    private fun showToast(msg: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }
}
