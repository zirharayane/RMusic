<p align="center">
  <img src="docs/logo.jpg" width="160" height="160" alt="RMusic Logo" style="border-radius: 32px;" />
</p>

<h1 align="center">RMusic (RM)</h1>

<p align="center">
  <strong>A fast, sleek, ad-free music streaming app for Android.</strong><br>
  Built with Jetpack Compose & Kotlin • OLED Black & Electric Orange • Supabase Cloud Sync
</p>

<p align="center">
  <a href="https://github.com/zirharayane/RMusic/releases/latest"><img src="https://img.shields.io/github/v/release/zirharayane/RMusic?style=for-the-badge&color=FF6B00" alt="Latest Release"></a>
  <img src="https://img.shields.io/badge/Platform-Android_9+-000000?style=for-the-badge&logo=android" alt="Android 9+">
  <img src="https://img.shields.io/badge/UI-Jetpack_Compose-000000?style=for-the-badge&logo=jetpackcompose" alt="Compose">
  <img src="https://img.shields.io/badge/Sync-Supabase_|_Discord-5865F2?style=for-the-badge&logo=discord&logoColor=white" alt="Discord Sync">
</p>

---

## ⚡ Highlights

- 🍊 **Signature Rayane.top Design**: Pure `#000000` OLED carbon theme with electric orange accents and ultra-responsive custom Compose UI.
- 🚀 **Instant Launch**: Optimized launch experience with zero splash lag.
- ☁️ **Discord Cloud Sync**: Connect with Discord to automatically sync your playlists and liked songs to the cloud via Supabase.
- 📥 **Playlist Importer**: Import public Spotify playlists or YouTube playlists straight into your library without needing developer API keys.
- 🎵 **Ad-Free Audio Streaming**: Clean, uninterrupted background playback, lock screen controls, and notification drawer media sessions.
- 🎤 **Time-Synced Lyrics**: Line-by-line and word-by-word synced lyrics with translations, plus shareable lyric cards.
- 💾 **Offline Listening**: Unlimited audio caching and one-tap **Download All** for songs, albums, and playlists.
- 🎚️ **Audio Enhancements**: Built-in 5-band Equalizer, crossfade up to 12s, and automatic loudness volume leveling.
- 🚗 **Android Auto**: Full Android Auto in-car support and home screen widgets.

---

## 📱 Screenshots

<p align="center">
  <img src="docs/screenshots/home.png" width="30%" alt="RMusic Home">
  <img src="docs/screenshots/settings.png" width="30%" alt="RMusic Settings">
  <img src="docs/screenshots/playlists.png" width="30%" alt="RMusic Playlists">
</p>

<p align="center">
  <img src="docs/screenshots/player.jpg" width="22%" alt="Now Playing">
  <img src="docs/screenshots/lyrics.jpg" width="22%" alt="Synced Lyrics">
  <img src="docs/screenshots/equalizer.jpg" width="22%" alt="Equalizer">
  <img src="docs/screenshots/share.jpg" width="22%" alt="Share Lyrics">
</p>

---

## 📥 Installation

Grab the latest APK directly from the **[Releases](https://github.com/zirharayane/RMusic/releases)** page:

1. Download **`RMusic.apk`**.
2. Open the APK on your Android device (Android 9.0+ required).
3. If prompted, allow *Install unknown apps* for your browser or file manager.

---

## 🛠️ Building from Source

### Prerequisites
- JDK 17 or JDK 21+
- Android SDK (API 34+ recommended)

```bash
# Clone the repository
git clone https://github.com/zirharayane/RMusic.git
cd RMusic

# Build debug APK
./gradlew :app:assembleDebug

# Install directly to a connected device or emulator
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 🔒 Privacy & Permissions

- **Zero Ads & Zero Tracking**: No analytics trackers, no ad networks.
- **Direct Streaming**: All stream audio queries run directly through YouTube Music endpoints.
- **Secure Cloud Sync**: Cloud authentication is handled through Discord OAuth via Supabase without storing passwords.

---

## 📜 Credits & Acknowledgments

RMusic builds upon open-source contributions from the Android and music communities:
- [Metrolist](https://github.com/mostafa-a-al/metrolist) & [innertubex](https://github.com/mostafa-a-al/innertubex) for YouTube Music API mapping.
- [LRCLIB](https://lrclib.net/) and [LyricsPlus](https://github.com/surfbouncer/lyricsplus) for synced lyrics.
- [Haze](https://github.com/chrisbanes/haze) for frosted glass blur effects.
- [SponsorBlock](https://sponsor.ajay.app/) for optional sponsor segment skipping.
