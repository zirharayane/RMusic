package app.ammusic.data.library

import android.util.Base64
import android.util.Log
import app.ammusic.youtube.YouTubeMusic
import app.ammusic.youtube.model.YtSearchFilter
import app.ammusic.youtube.model.YtTrack
import app.ammusic.youtube.tr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

sealed interface ImportResult {
    data class Success(val playlistId: String, val trackCount: Int, val title: String) : ImportResult
    data class Error(val message: String) : ImportResult
}

@Singleton
class PlaylistImporter @Inject constructor(
    private val library: LibraryRepository,
) {
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    data class RawItem(val title: String, val artist: String)

    suspend fun importFromUrl(url: String, onProgress: (Int, Int) -> Unit = { _, _ -> }): ImportResult = withContext(Dispatchers.IO) {
        val trimmed = url.trim()
        when {
            trimmed.contains("spotify") || trimmed.contains("spoti.fi") -> importSpotify(trimmed, onProgress)
            trimmed.contains("youtube") || trimmed.contains("youtu.be") -> importYouTube(trimmed)
            else -> ImportResult.Error(tr("Lien non supporté (Spotify ou YouTube requis)", "Unsupported link (Spotify or YouTube required)"))
        }
    }

    private suspend fun importSpotify(url: String, onProgress: (Int, Int) -> Unit): ImportResult = withContext(Dispatchers.IO) {
        var resolvedUrl = url
        val hadInviteParam = url.contains("pt=") || url.contains("pi=")

        // 1. Extract pure URL from surrounding text if any
        val linkMatcher = Pattern.compile("https?://[^\\s]+").matcher(resolvedUrl)
        if (linkMatcher.find()) {
            resolvedUrl = linkMatcher.group(0) ?: resolvedUrl
        }

        // 2. Resolve short links (e.g. spotify.link/xyz or spoti.fi/xyz)
        if (resolvedUrl.contains("spotify.link") || resolvedUrl.contains("spoti.fi")) {
            try {
                val req = Request.Builder()
                    .url(resolvedUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .build()
                client.newCall(req).execute().use { res ->
                    resolvedUrl = res.request.url.toString()
                }
            } catch (e: Exception) {
                Log.w("PlaylistImporter", "Failed to resolve shortlink: ${e.message}")
            }
        }

        // 3. Extract Spotify Playlist or Album ID
        val idMatcher = Pattern.compile("(?:playlist|album)[/:]([a-zA-Z0-9_-]+)").matcher(resolvedUrl)
        if (!idMatcher.find()) {
            Log.e("PlaylistImporter", "Could not find playlist or album ID in: $resolvedUrl")
            return@withContext ImportResult.Error(tr("Lien Spotify non reconnu (playlist ou album requis)", "Unrecognized Spotify link (playlist or album required)"))
        }

        val entityId = idMatcher.group(1) ?: return@withContext ImportResult.Error(tr("Identifiant Spotify manquant", "Missing Spotify ID"))
        val isAlbum = resolvedUrl.contains("/album") || resolvedUrl.contains(":album:")
        val entityType = if (isAlbum) "album" else "playlist"

        var playlistName: String = if (isAlbum) "Spotify Album" else "Spotify Playlist"
        val rawTracks = mutableListOf<RawItem>()

        // 4. If Album, primary extraction is via Spotify Embed page (__NEXT_DATA__)
        if (isAlbum) {
            try {
                val embedUrl = "https://open.spotify.com/embed/album/$entityId"
                val req = Request.Builder()
                    .url(embedUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .build()

                val html = client.newCall(req).execute().use { it.body?.string() }
                if (html != null) {
                    val jsonMatcher = Pattern.compile("<script id=\"__NEXT_DATA__\"[^>]*>(.*?)</script>").matcher(html)
                    if (jsonMatcher.find()) {
                        val parsed = json.parseToJsonElement(jsonMatcher.group(1)).jsonObject
                        val entity = parsed["props"]?.jsonObject
                            ?.get("pageProps")?.jsonObject
                            ?.get("state")?.jsonObject
                            ?.get("data")?.jsonObject
                            ?.get("entity")?.jsonObject

                        val foundName = entity?.get("title")?.jsonPrimitive?.contentOrNull
                            ?: entity?.get("name")?.jsonPrimitive?.contentOrNull
                        if (!foundName.isNullOrBlank()) {
                            playlistName = foundName
                        }

                        val trackList = entity?.get("trackList")?.jsonArray
                        if (trackList != null) {
                            for (element in trackList) {
                                val obj = element.jsonObject
                                val title = obj["title"]?.jsonPrimitive?.contentOrNull ?: continue
                                val subtitle = obj["subtitle"]?.jsonPrimitive?.contentOrNull ?: ""
                                rawTracks.add(RawItem(title, subtitle))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("PlaylistImporter", "Album embed scrape failed: ${e.message}")
            }
        } else {
            // 5. If Playlist, primary extraction: Fetch clean playlist page and decode initialState base64
            try {
                val pageUrl = "https://open.spotify.com/playlist/$entityId"
                val req = Request.Builder()
                    .url(pageUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .build()

                val html = client.newCall(req).execute().use { it.body?.string() }
                if (html != null) {
                    val stateMatcher = Pattern.compile("<script id=\"initialState\"[^>]*>(.*?)</script>").matcher(html)
                    if (stateMatcher.find()) {
                        val base64Data = stateMatcher.group(1)
                        val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
                        val jsonString = String(decodedBytes, Charsets.UTF_8)
                        val root = json.parseToJsonElement(jsonString).jsonObject

                        val itemsObj = root["entities"]?.jsonObject?.get("items")?.jsonObject
                        val playlistObj = itemsObj?.values?.firstOrNull()?.jsonObject

                        val foundName = playlistObj?.get("name")?.jsonPrimitive?.contentOrNull
                        if (!foundName.isNullOrBlank()) {
                            playlistName = foundName
                        }

                        val contentItems = playlistObj?.get("content")?.jsonObject?.get("items")?.jsonArray
                        if (contentItems != null) {
                            for (item in contentItems) {
                                val data = item.jsonObject["itemV2"]?.jsonObject?.get("data")?.jsonObject
                                val title = data?.get("name")?.jsonPrimitive?.contentOrNull
                                if (!title.isNullOrBlank()) {
                                    val artistsArray = data["artists"]?.jsonObject?.get("items")?.jsonArray
                                    val artist = artistsArray?.mapNotNull {
                                        it.jsonObject["profile"]?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull
                                    }?.joinToString(", ") ?: ""
                                    rawTracks.add(RawItem(title, artist))
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("PlaylistImporter", "initialState scrape failed: ${e.message}")
            }

            // 6. Secondary fallback for Playlist: Spotify Embed page (__NEXT_DATA__)
            if (rawTracks.isEmpty()) {
                try {
                    val embedUrl = "https://open.spotify.com/embed/playlist/$entityId"
                    val req = Request.Builder()
                        .url(embedUrl)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                        .build()

                    val html = client.newCall(req).execute().use { it.body?.string() }
                    if (html != null) {
                        val jsonMatcher = Pattern.compile("<script id=\"__NEXT_DATA__\"[^>]*>(.*?)</script>").matcher(html)
                        if (jsonMatcher.find()) {
                            val parsed = json.parseToJsonElement(jsonMatcher.group(1)).jsonObject
                            val entity = parsed["props"]?.jsonObject
                                ?.get("pageProps")?.jsonObject
                                ?.get("state")?.jsonObject
                                ?.get("data")?.jsonObject
                                ?.get("entity")?.jsonObject

                            val foundName = entity?.get("title")?.jsonPrimitive?.contentOrNull
                                ?: entity?.get("name")?.jsonPrimitive?.contentOrNull
                            if (!foundName.isNullOrBlank()) {
                                playlistName = foundName
                            }

                            val trackList = entity?.get("trackList")?.jsonArray
                            if (trackList != null) {
                                for (element in trackList) {
                                    val obj = element.jsonObject
                                    val title = obj["title"]?.jsonPrimitive?.contentOrNull ?: continue
                                    val subtitle = obj["subtitle"]?.jsonPrimitive?.contentOrNull ?: ""
                                    rawTracks.add(RawItem(title, subtitle))
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w("PlaylistImporter", "Playlist embed scrape failed: ${e.message}")
                }
            }
        }

        // 7. Tertiary fallback for title: oEmbed
        if (playlistName == "Spotify Playlist" || playlistName == "Spotify Album") {
            try {
                val oembedUrl = "https://open.spotify.com/oembed?url=https://open.spotify.com/$entityType/$entityId"
                val req = Request.Builder().url(oembedUrl).build()
                client.newCall(req).execute().use { res ->
                    val body = res.body?.string()
                    if (body != null) {
                        val obj = json.parseToJsonElement(body).jsonObject
                        val oembedTitle = obj["title"]?.jsonPrimitive?.contentOrNull
                        if (!oembedTitle.isNullOrBlank()) playlistName = oembedTitle
                    }
                }
            } catch (_: Exception) {}
        }

        if (rawTracks.isEmpty()) {
            Log.e("PlaylistImporter", "No tracks found for Spotify $entityType: $entityId")
            return@withContext if (hadInviteParam) {
                ImportResult.Error(tr("Cette playlist Spotify est privée ou une invitation collaborative (connexion Spotify requise)", "This Spotify playlist is private or a collaborative invite (requires Spotify login)"))
            } else {
                ImportResult.Error(tr("Aucun titre trouvé dans ce lien Spotify", "No tracks found in this Spotify link"))
            }
        }

        Log.i("PlaylistImporter", "Found ${rawTracks.size} tracks for '$playlistName', matching on YouTube Music...")

        // 8. Create the playlist locally
        val newPlaylistId = library.createPlaylist(playlistName)
            ?: return@withContext ImportResult.Error(tr("Impossible de créer la playlist locale", "Could not create local playlist"))
        val matchedTracks = mutableListOf<YtTrack>()

        for ((index, raw) in rawTracks.withIndex()) {
            onProgress(index + 1, rawTracks.size)
            try {
                val query = "${raw.title} ${raw.artist}".trim()
                val searchResult = YouTubeMusic.search(query, YtSearchFilter.Songs).getOrNull()
                val match = searchResult?.items?.filterIsInstance<YtTrack>()?.firstOrNull()
                if (match != null) {
                    matchedTracks.add(match)
                }
            } catch (e: Exception) {
                Log.w("PlaylistImporter", "Match failed for '${raw.title}': ${e.message}")
            }
        }

        if (matchedTracks.isNotEmpty()) {
            library.addAllToPlaylist(newPlaylistId, playlistName, matchedTracks)
        }

        Log.i("PlaylistImporter", "Successfully imported ${matchedTracks.size}/${rawTracks.size} tracks into '$playlistName'")
        ImportResult.Success(newPlaylistId, matchedTracks.size, playlistName)
    }

    private suspend fun importYouTube(url: String): ImportResult = withContext(Dispatchers.IO) {
        val listMatcher = Pattern.compile("[?&]list=([a-zA-Z0-9_-]+)").matcher(url)
        val listId = if (listMatcher.find()) listMatcher.group(1) ?: return@withContext ImportResult.Error(tr("Lien de playlist YouTube invalide", "Invalid YouTube playlist link")) else return@withContext ImportResult.Error(tr("Lien de playlist YouTube invalide", "Invalid YouTube playlist link"))
        val page = YouTubeMusic.playlist(listId).getOrNull() ?: return@withContext ImportResult.Error(tr("Playlist YouTube introuvable", "YouTube playlist not found"))
        val newPlaylistId = library.createPlaylist(page.playlist.title) ?: return@withContext ImportResult.Error(tr("Impossible de créer la playlist", "Could not create playlist"))
        if (page.tracks.isNotEmpty()) {
            library.addAllToPlaylist(newPlaylistId, page.playlist.title, page.tracks)
        }
        ImportResult.Success(newPlaylistId, page.tracks.size, page.playlist.title)
    }
}
