# Changes Log

## Initial Project Audit & Exploration
- Explored complete repository structure and code architecture across both modules (`:app` and `:youtube`).
- Investigated dependencies, design patterns, playback pipeline, data storage, and external integrations.
- Identified all project credits, licenses, third-party libraries, and upstream code origins (Metrolist, innertubex, SimpMusic, NewPipeExtractor, LRCLIB, LyricsPlus, BetterLyrics, SponsorBlock, Inter, Haze, etc.).
- Prepared detailed documentation summarizing how the application works, its technical architecture, and its full attribution history.

## Development Environment Check
- Verified current environment:
  - OS: Windows
  - Java: JDK 27 detected (`C:\Program Files\Java\jdk-27`)
  - Android Studio: Detected running (`studio64.exe` active at `C:\Program Files\Android\Android Studio`).
  - Android SDK: Located at `C:\Users\Programming\AppData\Local\Android\Sdk`. Setup wizard currently in progress.
- Updated Rebranding specifications:
  - Full App Name: `RMusic`
  - Short App Name: `RM`
- Outlined step-by-step setup plan to complete wizard, launch standalone AVD on screen, and proceed with custom UI & feature modifications.



## Feature Implementation & Design Overhaul (RMusic / RM)
- **App Rebranding**:
  - Updated app name to `RMusic` (Short: `RM`) in `app/src/main/res/values/strings.xml`, `settings.gradle.kts`, and Settings UI.
- **Rayane.top Color Palette**:
  - Configured `Theme.kt` with electric orange (`#FF6B00`) as primary accent, pure black (`#000000`) OLED background, dark charcoal surfaces (`#100F12`), and pure white text (`#FFFFFF`).
- **One-Click Download All**:
  - Added a dedicated one-click `Download All` icon action to the Liked Songs view in `LibraryScreen.kt` calling `actions.download(liked)`.
  - Confirmed and linked one-click `DownloadButton` across all playlist headers in `CollectionScreen.kt`.
- **Settings Streamlining**:
  - Removed Sleep Timer section and badges from both `SettingsScreen.kt` and `PlayerSheet.kt` ("App never sleeps").
  - Removed History management controls and YouTube sign-in dependency.
  - Made audio caching unlimited in `PlaybackService.kt` via `LeastRecentlyUsedCacheEvictor(Long.MAX_VALUE)`.
- **Supabase Cloud Sync & Discord / Google Login**:
  - Created `SupabaseSyncManager` (`app/src/main/kotlin/app/ammusic/data/cloud/SupabaseSyncManager.kt`) supporting Discord and Google OAuth authentication.
  - Implemented cloud sync logic to push and pull user playlists and liked songs to/from Supabase.
  - Connected `CloudAccountCard` in `SettingsScreen.kt` with one-tap Discord and Google sign-in buttons, real-time sync indicators, and sign-out actions.
  - Added `rmusic://auth-callback` deep link intent-filter in `AndroidManifest.xml` and OAuth callback handling in `MainActivity.kt`.

- **Build & Compilation Verification**:
  - Successfully compiled with Kotlin 2.4 and Jetpack Compose (`compileDebugKotlin`).
  - Successfully packaged into release-ready debug APK: `app/build/outputs/apk/debug/app-debug.apk` (57.4 MB).

## UI Polish & Customizations (RMusic Visual Updates)
- **Library Screen**:
  - Completely removed the YouTube Music sign-in prompt card (`GuestCard`). The library operates 100% standalone and clean.
- **Home Screen**:
  - Removed the large "Home" title text.
  - Added a stylized branded header at the top left: "**R**Music" with "**R**" in Electric Orange (`#FF6B00`) and "**Music**" in high-contrast text (`colors.text` - White in dark mode, Black in light mode).
  - Enlarged the Profile icon button at the top right from 32dp to 44dp with a 24dp icon for a much bolder look and easier tap target.
- **Settings Screen**:
  - Removed the `AccentPicker` and the "App language" segmented selector from the Appearance section, locking in the sleek Electric Orange theme.
- **Night Theme Polish**:
  - Completely eliminated the subtle purple/pink hue in dark mode.
  - Replaced with 100% pure neutral charcoal/carbon tones: `surface` = `#141414`, `surfaceHigh` = `#222222`, `textSecondary` = `#8E8E93`.
- **Live Emulator Deployment**:
  - Built and deployed fresh APK directly to `emulator-5554` with successful install and live launch.

## App Icon, Instant Launch, Supabase Discord Auth & Playlist Importer
- **App Icon Overhaul**:
  - Generated new Android launcher icons (`ic_launcher.png`, `ic_launcher_round.png`, `ic_launcher_foreground.webp`) across `mipmap-xxhdpi` and `mipmap-xxxhdpi` from the user's 3D orange "R" audio-jack design on OLED black background.
- **Instant Seamless Launch**:
  - Removed Android 12+ splash screen hang/delay; configured `android:windowSplashScreenAnimatedIcon` to `@android:color/transparent` and set `splash.remove()` instantly on initial frame render for zero icon zoom delay.
- **Supabase Cloud Sync (Discord Only)**:
  - Wired client credentials to project `upbetcpndwkvkjvfeaqo` with anon key.
  - Disabled Google button in `SettingsScreen.kt` for a focused, full-width "Sign in with Discord" card.
  - Kept Supabase service secret private and out of client code.
- **Playlist Importer (Spotify & YouTube)**:
  - Built `PlaylistImporter` supporting Spotify public embed links (no developer API key required) and YouTube playlist links.
  - Added "Import playlist (Spotify, YouTube)..." row in Library Playlists subscreen with interactive prompt and auto-sync to Supabase.
- **Live Device Verification**:
  - Rebuilt APK and deployed to `emulator-5554`.
  - Verified RMusic 3D launcher icon in app drawer, instant launch, top-left "RMusic" branding, enlarged profile button, carbon OLED theme, Discord login card in Settings, clean Library without YouTube prompts, and the Playlist Importer dialog.

## Physical Device Transfer (Samsung Galaxy A32)
- Detected connected Samsung Galaxy A32 (`Rayane's A32`) via MTP storage.
- Transferred `RMusic.apk` (54.9 MB) directly to the phone's **Download** folder (`Internal storage\Download\RMusic.apk`).
- Ready for immediate one-tap installation on the device.
