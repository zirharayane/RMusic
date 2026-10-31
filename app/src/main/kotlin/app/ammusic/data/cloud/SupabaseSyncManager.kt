package app.ammusic.data.cloud

import android.content.Context
import android.content.Intent
import android.net.Uri
import app.ammusic.data.library.LibraryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class CloudUser(
    val id: String,
    val email: String? = null,
    val provider: String? = null,
    val avatarUrl: String? = null,
    val name: String? = null,
)

@Serializable
data class CloudPlaylist(
    val id: String,
    val user_id: String,
    val title: String,
    val subtitle: String = "",
    val thumbnail: String? = null,
    val updated_at: Long = System.currentTimeMillis(),
)

@Serializable
data class CloudLiked(
    val track_id: String,
    val user_id: String,
    val added_at: Long,
)

@Singleton
class SupabaseSyncManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val library: LibraryRepository,
) {
    var supabaseUrl: String = "https://upbetcpndwkvkjvfeaqo.supabase.co"
    var supabaseAnonKey: String = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVwYmV0Y3BuZHdrdmtqdmZlYXFvIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTMzMDM1MDksImV4cCI6MjEwODg3OTUwOX0.3W9999_0hlcPPj2R4lXH5JOrg72gedDAUtF1E-_sf2A"

    private val prefs = context.getSharedPreferences("supabase_auth", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val client = OkHttpClient.Builder()
        .callTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val _currentUser = MutableStateFlow<CloudUser?>(null)
    val currentUser: StateFlow<CloudUser?> = _currentUser.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    init {
        restoreSession()
        observeLocalPlaylists()
    }

    private fun observeLocalPlaylists() {
        scope.launch {
            library.playlists.collect { playlists ->
                val user = _currentUser.value ?: return@collect
                val token = prefs.getString("access_token", null) ?: return@collect
                for (p in playlists) {
                    try {
                        val cloudItem = CloudPlaylist(
                            id = p.id,
                            user_id = user.id,
                            title = p.title,
                            subtitle = p.subtitle,
                            thumbnail = p.thumbnail,
                        )
                        val body = json.encodeToString(cloudItem).toRequestBody(jsonMediaType)
                        val request = Request.Builder()
                            .url("$supabaseUrl/rest/v1/playlists")
                            .header("apikey", supabaseAnonKey)
                            .header("Authorization", "Bearer $token")
                            .header("Prefer", "resolution=merge-duplicates")
                            .post(body)
                            .build()
                        client.newCall(request).execute().close()
                    } catch (e: Exception) {
                        // Safe default: continue offline
                    }
                }
            }
        }
    }

    fun restoreSession() {
        val uid = prefs.getString("user_id", null) ?: return
        val email = prefs.getString("email", null)
        val name = prefs.getString("name", null)
        val provider = prefs.getString("provider", null)
        val avatar = prefs.getString("avatar", null)
        _currentUser.value = CloudUser(uid, email, provider, avatar, name)
    }

    fun getOAuthUrl(provider: String, redirectUri: String = "rmusic://auth-callback"): String {
        return "$supabaseUrl/auth/v1/authorize?provider=$provider&redirect_to=${Uri.encode(redirectUri)}"
    }

    fun signInWithDiscord(context: Context) {
        val url = getOAuthUrl("discord")
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun signInWithGoogle(context: Context) {
        val url = getOAuthUrl("google")
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun handleAuthCallback(accessToken: String, refreshToken: String, user: CloudUser) {
        prefs.edit()
            .putString("access_token", accessToken)
            .putString("refresh_token", refreshToken)
            .putString("user_id", user.id)
            .putString("email", user.email)
            .putString("name", user.name)
            .putString("provider", user.provider)
            .putString("avatar", user.avatarUrl)
            .apply()
        _currentUser.value = user
        triggerFullSync()
    }

    fun signOut() {
        prefs.edit().clear().apply()
        _currentUser.value = null
    }

    fun triggerFullSync() {
        val user = _currentUser.value ?: return
        val token = prefs.getString("access_token", null) ?: return

        scope.launch {
            _isSyncing.value = true
            try {
                // Sync Playlists
                val localPlaylists = library.playlists.first()
                for (p in localPlaylists) {
                    val cloudItem = CloudPlaylist(
                        id = p.id,
                        user_id = user.id,
                        title = p.title,
                        subtitle = p.subtitle,
                        thumbnail = p.thumbnail,
                    )
                    val body = json.encodeToString(cloudItem).toRequestBody(jsonMediaType)
                    val request = Request.Builder()
                        .url("$supabaseUrl/rest/v1/playlists")
                        .header("apikey", supabaseAnonKey)
                        .header("Authorization", "Bearer $token")
                        .header("Prefer", "resolution=merge-duplicates")
                        .post(body)
                        .build()
                    client.newCall(request).execute().close()
                }

                // Sync Liked Tracks
                val likedTracks = library.liked.first()
                for (t in likedTracks) {
                    val cloudLiked = CloudLiked(
                        track_id = t.id,
                        user_id = user.id,
                        added_at = System.currentTimeMillis(),
                    )
                    val body = json.encodeToString(cloudLiked).toRequestBody(jsonMediaType)
                    val request = Request.Builder()
                        .url("$supabaseUrl/rest/v1/liked_tracks")
                        .header("apikey", supabaseAnonKey)
                        .header("Authorization", "Bearer $token")
                        .header("Prefer", "resolution=merge-duplicates")
                        .post(body)
                        .build()
                    client.newCall(request).execute().close()
                }
            } catch (e: Exception) {
                android.util.Log.e("RMusicSync", "Sync failed: ${e.message}")
            } finally {
                _isSyncing.value = false
            }
        }
    }
}
