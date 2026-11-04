package app.ammusic.data.library

import android.util.Base64
import android.util.Log
import app.ammusic.youtube.YouTubeMusic
import app.ammusic.youtube.model.YtSearchFilter
import app.ammusic.youtube.model.YtTrack
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

    suspend fun importFromUrl(url: String, onProgress: (Int, Int) -> Unit = { _, _ -> }): String? = withContext(Dispatchers.IO) {
        val trimmed = url.trim()
        when {
            trimmed.contains("spotify") || trimmed.contains("spoti.fi") -> importSpotify(trimmed, onProgress)
            trimmed.contains("youtube") || trimmed.contains("youtu.be") -> importYouTube(trimmed)
            else -> null
        }
    }

    private suspend fun importSpotify(url: String, onProgress: (Int, Int) -> Unit): String? = withContext(Dispatchers.IO) {
        var resolvedUrl = url

        // 1. Resolve short links (e.g. spotify.link/xyz or spoti.fi/xyz)
        if (resolvedUrl.contains("spotify.link") || resolvedUrl.contains("spoti.fi")) {
            val linkMatcher = Pattern.compile("https?://[^\\s]+").matcher(resolvedUrl)
            val extractedLink = if (linkMatcher.find()) linkMatcher.group(0) else resolvedUrl
            try {
                val req = Request.Builder()
                    .url(extractedLink)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .build()
                client.newCall(req).execute().use { res ->
                    resolvedUrl = res.request.url.toString()
                }
            } catch (e: Exception) {
                Log.w("PlaylistImporter", "Failed to resolve shortlink: ${e.message}")
            }
        }

        // 2. Extract Spotify Playlist ID
        val idMatcher = Pattern.compile("playlist[/:]([a-zA-Z0-9_-]+)").matcher(resolvedUrl)
        val playlistId = if (idMatcher.find()) idMatcher.group(1) else {
            Log.e("PlaylistImporter", "Could not find playlist ID in: $resolvedUrl")
            return@withContext null
        }

        var playlistName: String = "Spotify Playlist"
        val rawTracks = mutableListOf<RawItem>()

        // 3. Primary extraction: Fetch full playlist page and decode initialState base64
        try {
            val pageUrl = "https://open.spotify.com/playlist/$playlistId"
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

        // 4. Secondary fallback: Spotify Embed page (__NEXT_DATA__)
        if (rawTracks.isEmpty()) {
            try {
                val embedUrl = "https://open.spotify.com/embed/playlist/$playlistId"
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

                        val foundName = entity?.get("name")?.jsonPrimitive?.contentOrNull
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
                Log.w("PlaylistImporter", "embed scrape failed: ${e.message}")
            }
        }

        // 5. Tertiary fallback for playlist title: oEmbed
        if (playlistName == "Spotify Playlist") {
            try {
                val oembedUrl = "https://open.spotify.com/oembed?url=https://open.spotify.com/playlist/$playlistId"
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
            Log.e("PlaylistImporter", "No tracks could be found for Spotify playlist: $playlistId")
            return@withContext null
        }

        Log.i("PlaylistImporter", "Found ${rawTracks.size} tracks for '$playlistName', matching on YouTube Music...")

        // 6. Create the playlist locally
        val newPlaylistId = library.createPlaylist(playlistName) ?: return@withContext null
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
        newPlaylistId
    }

    private suspend fun importYouTube(url: String): String? = withContext(Dispatchers.IO) {
        val listMatcher = Pattern.compile("[?&]list=([a-zA-Z0-9_-]+)").matcher(url)
        val listId = if (listMatcher.find()) listMatcher.group(1) else return@withContext null
        val page = YouTubeMusic.playlist(listId).getOrNull() ?: return@withContext null
        val newPlaylistId = library.createPlaylist(page.playlist.title) ?: return@withContext null
        if (page.tracks.isNotEmpty()) {
            library.addAllToPlaylist(newPlaylistId, page.playlist.title, page.tracks)
        }
        newPlaylistId
    }
}
