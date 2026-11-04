package app.ammusic.data.cloud

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.util.Log
import app.ammusic.data.library.ArtistRow
import app.ammusic.data.library.LibraryRepository
import app.ammusic.data.library.LikedRow
import app.ammusic.data.library.PlaylistKind
import app.ammusic.data.library.PlaylistRow
import app.ammusic.data.library.PlaylistTrackRow
import app.ammusic.data.library.TrackRow
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
data class CloudTrackItem(
    val id: String,
    val title: String,
    val artists: String = "",
    val album: String? = null,
    val thumbnail: String? = null,
    val durationSeconds: Int? = null,
    val artistId: String? = null,
    val albumId: String? = null,
)

@Serializable
data class CloudPlaylist(
    val id: String,
    val user_id: String,
    val title: String,
    val subtitle: String = "",
    val thumbnail: String? = null,
    val tracks_json: String = "[]",
    val updated_at: Long = System.currentTimeMillis(),
)

@Serializable
data class CloudLiked(
    val user_id: String,
    val track_id: String,
    val title: String = "",
    val artist: String = "",
    val thumbnail: String? = null,
    val duration_seconds: Int = 0,
    val added_at: Long = System.currentTimeMillis(),
)

@Serializable
data class CloudArtist(
    val user_id: String,
    val artist_id: String,
    val name: String,
    val thumbnail: String? = null,
    val added_at: Long = System.currentTimeMillis(),
)

@OptIn(FlowPreview::class)
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
        observeAutoSync()
        if (_currentUser.value != null) {
            triggerFullSync()
        }
    }

    private fun observeAutoSync() {
        // 1. Auto-push Playlists on change (batch upsert, debounced)
        scope.launch {
            library.playlists.debounce(2_000).collect { playlists ->
                val user = _currentUser.value ?: return@collect
                val token = prefs.getString("access_token", null) ?: return@collect
                if (playlists.isEmpty()) return@collect
                for (p in playlists) {
                    try {
                        val tracks = library.dao.playlistTracks(p.id).first()
                        val trackItems = tracks.map {
                            CloudTrackItem(
                                id = it.id,
                                title = it.title,
                                artists = it.artists,
                                album = it.album,
                                thumbnail = it.thumbnail,
                                durationSeconds = it.durationSeconds,
                                artistId = it.artistId,
                                albumId = it.albumId,
                            )
                        }
                        val cloudItem = CloudPlaylist(
                            id = p.id,
                            user_id = user.id,
                            title = p.title,
                            subtitle = p.subtitle,
                            thumbnail = p.thumbnail,
                            tracks_json = json.encodeToString(trackItems),
                            updated_at = System.currentTimeMillis(),
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
                        Log.e("RMusicSync", "Playlist sync failed: ${e.message}")
                    }
                }
            }
        }

        // 2. Auto-push Liked songs on change (single batch upsert, debounced)
        scope.launch {
            library.liked.debounce(2_000).collect { likedTracks ->
                val user = _currentUser.value ?: return@collect
                val token = prefs.getString("access_token", null) ?: return@collect
                if (likedTracks.isEmpty()) return@collect
                try {
                    val items = likedTracks.map { t ->
                        CloudLiked(
                            user_id = user.id,
                            track_id = t.id,
                            title = t.title,
                            artist = t.artists,
                            thumbnail = t.thumbnail,
                            duration_seconds = t.durationSeconds ?: 0,
                            added_at = System.currentTimeMillis(),
                        )
                    }
                    val body = json.encodeToString(items).toRequestBody(jsonMediaType)
                    val request = Request.Builder()
                        .url("$supabaseUrl/rest/v1/liked_tracks")
                        .header("apikey", supabaseAnonKey)
                        .header("Authorization", "Bearer $token")
                        .header("Prefer", "resolution=merge-duplicates")
                        .post(body)
                        .build()
                    client.newCall(request).execute().close()
                    Log.i("RMusicSync", "Auto-synced ${items.size} liked tracks in 1 batch")
                } catch (e: Exception) {
                    Log.e("RMusicSync", "Liked track batch sync failed: ${e.message}")
                }
            }
        }

        // 3. Auto-push Favorite Artists on change (single batch upsert, debounced)
        scope.launch {
            library.artists.debounce(2_000).collect { artistsList ->
                val user = _currentUser.value ?: return@collect
                val token = prefs.getString("access_token", null) ?: return@collect
                if (artistsList.isEmpty()) return@collect
                try {
                    val items = artistsList.map { a ->
                        CloudArtist(
                            user_id = user.id,
                            artist_id = a.id,
                            name = a.title,
                            thumbnail = a.thumbnail,
                            added_at = System.currentTimeMillis(),
                        )
                    }
                    val body = json.encodeToString(items).toRequestBody(jsonMediaType)
                    val request = Request.Builder()
                        .url("$supabaseUrl/rest/v1/fav_artists")
                        .header("apikey", supabaseAnonKey)
                        .header("Authorization", "Bearer $token")
                        .header("Prefer", "resolution=merge-duplicates")
                        .post(body)
                        .build()
                    client.newCall(request).execute().close()
                    Log.i("RMusicSync", "Auto-synced ${items.size} artists in 1 batch")
                } catch (e: Exception) {
                    Log.e("RMusicSync", "Artist batch sync failed: ${e.message}")
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

    fun getOAuthUrl(provider: String, redirectUri: String = "https://music.rayane.top/auth/callback"): String {
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

    fun handleAuthCallback(accessToken: String, refreshToken: String) {
        scope.launch {
            // First decode JWT payload for instant response
            val quickUser = decodeJwtUser(accessToken)
            if (quickUser != null) {
                _currentUser.value = quickUser
                saveUserSession(accessToken, refreshToken, quickUser)
            }

            // Next fetch fresh user info from Supabase auth endpoint
            val fullUser = fetchUserInfo(accessToken) ?: quickUser ?: CloudUser(id = "user", provider = "discord")
            _currentUser.value = fullUser
            saveUserSession(accessToken, refreshToken, fullUser)

            // Trigger full bidirectional sync (Pull first, then Push)
            triggerFullSync()
        }
    }

    private fun saveUserSession(accessToken: String, refreshToken: String, user: CloudUser) {
        prefs.edit()
            .putString("access_token", accessToken)
            .putString("refresh_token", refreshToken)
            .putString("user_id", user.id)
            .putString("email", user.email)
            .putString("name", user.name)
            .putString("provider", user.provider ?: "discord")
            .putString("avatar", user.avatarUrl)
            .apply()
    }

    private fun decodeJwtUser(jwt: String): CloudUser? {
        try {
            val parts = jwt.split(".")
            if (parts.size < 2) return null
            val payload = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP))
            val obj = json.parseToJsonElement(payload).jsonObject
            val sub = obj["sub"]?.jsonPrimitive?.content ?: return null
            val email = obj["email"]?.jsonPrimitive?.contentOrNull
            val meta = obj["user_metadata"]?.jsonObject
            val name = meta?.get("custom_claims")?.jsonObject?.get("global_name")?.jsonPrimitive?.contentOrNull
                ?: meta?.get("global_name")?.jsonPrimitive?.contentOrNull
                ?: meta?.get("full_name")?.jsonPrimitive?.contentOrNull
                ?: meta?.get("name")?.jsonPrimitive?.contentOrNull
                ?: meta?.get("user_name")?.jsonPrimitive?.contentOrNull
                ?: email?.substringBefore("@")
            val avatar = meta?.get("avatar_url")?.jsonPrimitive?.contentOrNull
                ?: meta?.get("picture")?.jsonPrimitive?.contentOrNull
            return CloudUser(
                id = sub,
                email = email,
                provider = "discord",
                avatarUrl = avatar,
                name = name,
            )
        } catch (e: Exception) {
            return null
        }
    }

    private suspend fun fetchUserInfo(token: String): CloudUser? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("$supabaseUrl/auth/v1/user")
                .header("apikey", supabaseAnonKey)
                .header("Authorization", "Bearer $token")
                .get()
                .build()
            client.newCall(req).execute().use { res ->
                val body = res.body?.string() ?: return@use null
                val root = json.parseToJsonElement(body).jsonObject
                val id = root["id"]?.jsonPrimitive?.content ?: return@use null
                val email = root["email"]?.jsonPrimitive?.contentOrNull
                val meta = root["user_metadata"]?.jsonObject
                val name = meta?.get("custom_claims")?.jsonObject?.get("global_name")?.jsonPrimitive?.contentOrNull
                    ?: meta?.get("global_name")?.jsonPrimitive?.contentOrNull
                    ?: meta?.get("full_name")?.jsonPrimitive?.contentOrNull
                    ?: meta?.get("name")?.jsonPrimitive?.contentOrNull
                    ?: meta?.get("user_name")?.jsonPrimitive?.contentOrNull
                    ?: email?.substringBefore("@")
                val avatar = meta?.get("avatar_url")?.jsonPrimitive?.contentOrNull
                    ?: meta?.get("picture")?.jsonPrimitive?.contentOrNull
                CloudUser(
                    id = id,
                    email = email,
                    provider = "discord",
                    avatarUrl = avatar,
                    name = name,
                )
            }
        } catch (e: Exception) {
            Log.e("RMusicSync", "Failed to fetch user from Supabase auth: ${e.message}")
            null
        }
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
                // 1. Pull remote data first so freshly installed apps get all user content
                pullRemoteLikedTracks(token)
                pullRemoteArtists(token)
                pullRemotePlaylists(token)

                // 2. Push any local playlists, liked tracks, or artists up to Supabase
                pushLocalChanges(user, token)
            } catch (e: Exception) {
                Log.e("RMusicSync", "Full sync error: ${e.message}")
            } finally {
                _isSyncing.value = false
            }
        }
    }

    private suspend fun pullRemoteLikedTracks(token: String) = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("$supabaseUrl/rest/v1/liked_tracks?select=*")
                .header("apikey", supabaseAnonKey)
                .header("Authorization", "Bearer $token")
                .get()
                .build()
            client.newCall(req).execute().use { res ->
                if (!res.isSuccessful) {
                    val err = res.body?.string()
                    Log.w("RMusicSync", "Pull liked failed (${res.code}): $err")
                    return@use
                }
                val body = res.body?.string() ?: return@use
                val list = json.decodeFromString<List<CloudLiked>>(body)
                if (list.isEmpty()) return@use

                val tracks = list.map {
                    TrackRow(
                        id = it.track_id,
                        title = it.title.ifBlank { "Titre" },
                        artists = it.artist,
                        album = null,
                        thumbnail = it.thumbnail,
                        durationSeconds = if (it.duration_seconds > 0) it.duration_seconds else null,
                        artistId = null,
                        albumId = null,
                    )
                }
                val likedRows = list.map {
                    LikedRow(
                        trackId = it.track_id,
                        addedAt = it.added_at,
                        local = true,
                    )
                }
                library.dao.upsertTracks(tracks)
                library.dao.insertLiked(likedRows)
                Log.i("RMusicSync", "Restored ${likedRows.size} liked songs from Supabase")
            }
        } catch (e: Exception) {
            Log.e("RMusicSync", "Pull liked exception: ${e.message}")
        }
    }

    private suspend fun pullRemoteArtists(token: String) = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("$supabaseUrl/rest/v1/fav_artists?select=*")
                .header("apikey", supabaseAnonKey)
                .header("Authorization", "Bearer $token")
                .get()
                .build()
            client.newCall(req).execute().use { res ->
                if (!res.isSuccessful) {
                    val err = res.body?.string()
                    Log.w("RMusicSync", "Pull artists failed (${res.code}): $err")
                    return@use
                }
                val body = res.body?.string() ?: return@use
                val list = json.decodeFromString<List<CloudArtist>>(body)
                if (list.isEmpty()) return@use

                val rows = list.map {
                    ArtistRow(
                        id = it.artist_id,
                        name = it.name,
                        thumbnail = it.thumbnail,
                        addedAt = it.added_at,
                        local = true,
                    )
                }
                library.dao.insertArtists(rows)
                Log.i("RMusicSync", "Restored ${rows.size} favorite artists from Supabase")
            }
        } catch (e: Exception) {
            Log.e("RMusicSync", "Pull artists exception: ${e.message}")
        }
    }

    private suspend fun pullRemotePlaylists(token: String) = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("$supabaseUrl/rest/v1/playlists?select=*")
                .header("apikey", supabaseAnonKey)
                .header("Authorization", "Bearer $token")
                .get()
                .build()
            client.newCall(req).execute().use { res ->
                if (!res.isSuccessful) {
                    val err = res.body?.string()
                    Log.w("RMusicSync", "Pull playlists failed (${res.code}): $err")
                    return@use
                }
                val body = res.body?.string() ?: return@use
                val list = json.decodeFromString<List<CloudPlaylist>>(body)
                for (remote in list) {
                    val row = PlaylistRow(
                        id = remote.id,
                        title = remote.title,
                        subtitle = remote.subtitle,
                        thumbnail = remote.thumbnail,
                        addedAt = remote.updated_at,
                        kind = PlaylistKind.Local,
                    )
                    library.dao.upsertPlaylists(listOf(row))

                    if (remote.tracks_json.isNotBlank() && remote.tracks_json != "[]") {
                        try {
                            val tracks = json.decodeFromString<List<CloudTrackItem>>(remote.tracks_json)
                            if (tracks.isNotEmpty()) {
                                library.dao.upsertTracks(tracks.map {
                                    TrackRow(
                                        id = it.id,
                                        title = it.title,
                                        artists = it.artists,
                                        album = it.album,
                                        thumbnail = it.thumbnail,
                                        durationSeconds = it.durationSeconds,
                                        artistId = it.artistId,
                                        albumId = it.albumId,
                                    )
                                })
                                library.dao.upsertPlaylistTracks(tracks.mapIndexed { idx, t ->
                                    PlaylistTrackRow(remote.id, t.id, idx.toLong())
                                })
                            }
                        } catch (e: Exception) {
                            Log.w("RMusicSync", "Failed to parse tracks_json for ${remote.title}: ${e.message}")
                        }
                    }
                    library.refreshSummary(remote.id)
                }
                Log.i("RMusicSync", "Restored ${list.size} playlists from Supabase")
            }
        } catch (e: Exception) {
            Log.e("RMusicSync", "Pull playlists failed: ${e.message}")
        }
    }

    private suspend fun pushLocalChanges(user: CloudUser, token: String) = withContext(Dispatchers.IO) {
        // Push playlists
        val localPlaylists = library.playlists.first()
        for (p in localPlaylists) {
            try {
                val tracks = library.dao.playlistTracks(p.id).first()
                val trackItems = tracks.map {
                    CloudTrackItem(
                        id = it.id,
                        title = it.title,
                        artists = it.artists,
                        album = it.album,
                        thumbnail = it.thumbnail,
                        durationSeconds = it.durationSeconds,
                        artistId = it.artistId,
                        albumId = it.albumId,
                    )
                }
                val cloudItem = CloudPlaylist(
                    id = p.id,
                    user_id = user.id,
                    title = p.title,
                    subtitle = p.subtitle,
                    thumbnail = p.thumbnail,
                    tracks_json = json.encodeToString(trackItems),
                    updated_at = System.currentTimeMillis(),
                )
                val body = json.encodeToString(cloudItem).toRequestBody(jsonMediaType)
                val request = Request.Builder()
                    .url("$supabaseUrl/rest/v1/playlists")
                    .header("apikey", supabaseAnonKey)
                    .header("Authorization", "Bearer $token")
                    .header("Prefer", "resolution=merge-duplicates")
                    .post(body)
                    .build()
                client.newCall(request).execute().use { res ->
                    if (!res.isSuccessful) {
                        Log.w("RMusicSync", "Push playlist failed (${res.code}): ${res.body?.string()}")
                    }
                }
            } catch (e: Exception) {
                Log.e("RMusicSync", "Push playlist exception: ${e.message}")
            }
        }

        // Push Liked Tracks in single batch
        val likedTracks = library.liked.first()
        if (likedTracks.isNotEmpty()) {
            try {
                val items = likedTracks.map { t ->
                    CloudLiked(
                        user_id = user.id,
                        track_id = t.id,
                        title = t.title,
                        artist = t.artists,
                        thumbnail = t.thumbnail,
                        duration_seconds = t.durationSeconds ?: 0,
                        added_at = System.currentTimeMillis(),
                    )
                }
                val body = json.encodeToString(items).toRequestBody(jsonMediaType)
                val request = Request.Builder()
                    .url("$supabaseUrl/rest/v1/liked_tracks")
                    .header("apikey", supabaseAnonKey)
                    .header("Authorization", "Bearer $token")
                    .header("Prefer", "resolution=merge-duplicates")
                    .post(body)
                    .build()
                client.newCall(request).execute().close()
                Log.i("RMusicSync", "Pushed ${items.size} local liked tracks to Supabase in 1 batch")
            } catch (e: Exception) {
                Log.e("RMusicSync", "Push liked tracks batch exception: ${e.message}")
            }
        }

        // Push Favorite Artists in single batch
        val artistsList = library.artists.first()
        if (artistsList.isNotEmpty()) {
            try {
                val items = artistsList.map { a ->
                    CloudArtist(
                        user_id = user.id,
                        artist_id = a.id,
                        name = a.title,
                        thumbnail = a.thumbnail,
                        added_at = System.currentTimeMillis(),
                    )
                }
                val body = json.encodeToString(items).toRequestBody(jsonMediaType)
                val request = Request.Builder()
                    .url("$supabaseUrl/rest/v1/fav_artists")
                    .header("apikey", supabaseAnonKey)
                    .header("Authorization", "Bearer $token")
                    .header("Prefer", "resolution=merge-duplicates")
                    .post(body)
                    .build()
                client.newCall(request).execute().close()
                Log.i("RMusicSync", "Pushed ${items.size} local artists to Supabase in 1 batch")
            } catch (e: Exception) {
                Log.e("RMusicSync", "Push artists batch exception: ${e.message}")
            }
        }
    }
}
