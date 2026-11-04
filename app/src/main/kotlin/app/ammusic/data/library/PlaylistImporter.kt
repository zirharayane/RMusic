package app.ammusic.data.library

import android.util.Log
import app.ammusic.youtube.YouTubeMusic
import app.ammusic.youtube.model.YtSearchFilter
import app.ammusic.youtube.model.YtTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
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
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun importFromUrl(url: String, onProgress: (Int, Int) -> Unit = { _, _ -> }): String? = withContext(Dispatchers.IO) {
        val trimmed = url.trim()
        when {
            trimmed.contains("spotify") -> importSpotify(trimmed, onProgress)
            trimmed.contains("youtube") || trimmed.contains("youtu.be") -> importYouTube(trimmed)
            else -> null
        }
    }

    private suspend fun importSpotify(url: String, onProgress: (Int, Int) -> Unit): String? {
        val idMatcher = Pattern.compile("playlist[/:]([a-zA-Z0-9]+)").matcher(url)
        val playlistId = if (idMatcher.find()) idMatcher.group(1) else return null
        val embedUrl = "https://open.spotify.com/embed/playlist/$playlistId"

        val request = Request.Builder()
            .url(embedUrl)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .build()

        val html = try {
            client.newCall(request).execute().use { it.body?.string() } ?: return null
        } catch (e: Exception) {
            Log.e("PlaylistImporter", "Failed to fetch Spotify embed: ${e.message}")
            return null
        }

        val jsonMatcher = Pattern.compile("<script id=\"__NEXT_DATA__\" type=\"application/json\">(.*?)</script>").matcher(html)
        if (!jsonMatcher.find()) return null
        val jsonStr = jsonMatcher.group(1)

        val parsed = try {
            json.parseToJsonElement(jsonStr).jsonObject
        } catch (e: Exception) {
            return null
        }

        val entity = parsed["props"]?.jsonObject
            ?.get("pageProps")?.jsonObject
            ?.get("state")?.jsonObject
            ?.get("data")?.jsonObject
            ?.get("entity")?.jsonObject ?: return null

        val name = entity["name"]?.jsonPrimitive?.content ?: "Spotify Playlist"
        val rawTrackList = entity["trackList"]?.jsonArray ?: return null

        data class RawItem(val title: String, val artist: String)
        val rawTracks = rawTrackList.mapNotNull { element ->
            val obj = element.jsonObject
            val title = obj["title"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val subtitle = obj["subtitle"]?.jsonPrimitive?.content ?: ""
            RawItem(title, subtitle)
        }

        if (rawTracks.isEmpty()) return null

        val newPlaylistId = library.createPlaylist(name) ?: return null
        val matchedTracks = mutableListOf<YtTrack>()

        rawTracks.forEachIndexed { index, raw ->
            onProgress(index + 1, rawTracks.size)
            try {
                val query = "${raw.title} ${raw.artist}".trim()
                val searchResult = YouTubeMusic.search(query, YtSearchFilter.Songs).getOrNull()
                val match = searchResult?.items?.filterIsInstance<YtTrack>()?.firstOrNull()
                if (match != null) {
                    matchedTracks.add(match)
                }
            } catch (e: Exception) {
                // Continue with next track
            }
        }

        if (matchedTracks.isNotEmpty()) {
            library.addAllToPlaylist(newPlaylistId, name, matchedTracks)
        }
        return newPlaylistId
    }

    private suspend fun importYouTube(url: String): String? {
        val listMatcher = Pattern.compile("[?&]list=([a-zA-Z0-9_-]+)").matcher(url)
        val listId = if (listMatcher.find()) listMatcher.group(1) else return null
        val page = YouTubeMusic.playlist(listId).getOrNull() ?: return null
        val newPlaylistId = library.createPlaylist(page.playlist.title) ?: return null
        if (page.tracks.isNotEmpty()) {
            library.addAllToPlaylist(newPlaylistId, page.playlist.title, page.tracks)
        }
        return newPlaylistId
    }
}
