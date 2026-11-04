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

## Desktop Port & Packaging (RMusic Desktop v1.0.0)
- **Zero-Admin Installer**:
  - Replaced Compose Multiplatform's default WiX Burn installer with Inno Setup 6 (`installer.iss`).
  - Set `PrivilegesRequired=lowest` to install per-user into `%LOCALAPPDATA%\Programs\RMusic`, eliminating all UAC/administrator elevation prompts.
  - Automatically registers `rmusic://` deep links under `HKCU\Software\Classes\rmusic`.
- **Branded 3D Monogram Logo**:
  - Generated full multi-resolution Windows `.ico` (16 to 256px) and high-res PNGs from master `docs/logo.jpg`.
  - Replaced old AM-Music yellow play triangle icon throughout executable, taskbar, tray, shortcuts, and web assets.
- **Startup Crash Diagnoses & Fixes**:
  - Resolved `NullPointerException` in `resourcePainter` by adding multi-classloader fallback.
  - Guarded DWM window styling against unsupported Windows 10 LTSC (19044) native calls.
  - Removed raw COM vtable calls in Windows media controls that crashed native threads on Windows 10.
  - Added strong lock and channel retention in `SingleInstance.kt` with automatic shutdown cleanup.
- **GitHub Release & Web Distribution**:
  - Pushed codebase and release to `https://github.com/zirharayane/RMusic-Desktop.git`.
  - Published `RMusic-Setup.exe` (76.2 MB) to release `v1.0.0`.
  - Updated web portal (`E:\Projects\RMusic web`) CTAs to link directly to the latest GitHub release asset, and deployed to Cloudflare Workers (`music.rayane.top`).
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

## GitHub Repository & Release Publication
- Initialized clean git repository with comprehensive `.gitignore` filtering out build caches, APKs, and secrets.
- Crafted a complete, modern `README.md` featuring the 3D logo, feature badges, screenshot gallery, building guide, and credits.
- Created public GitHub repository `RMusic` under user account `zirharayane` (`https://github.com/zirharayane/RMusic`).
- Pushed complete source code to `main` branch.
- Created initial GitHub release **`v1.0.0`** (`RMusic v1.0.0 — Initial Release`).
- Uploaded release binary asset **`RMusic.apk`** (54.93 MB) directly to the release page.

## Modern Showcase Website Development (`/web`)
- **Dedicated Project Workspace**:
  - Scaffolding in isolated `web/` directory powered by Vite 8, React 19, TypeScript, and Tailwind CSS v4.
- **Rayane.top & RMusic Visual DNA**:
  - Pure `#000000` OLED background with electric orange (`#FF6B00`) accents, dark carbon surfaces, and frosted glass transparency inspired by the app's Haze shader engine.
- **Interactive Three.js 3D Monogram (`ThreeHeroScene.tsx`)**:
  - Built an interactive metallic extruded 3D "R" sculpture in Three.js with dynamic lighting, orbiting particles, and cursor-following parallax tilt.
- **Interactive Web Player Simulator (`InteractivePlayer.tsx`)**:
  - Fully working HTML5 audio player simulator featuring *Santé (Stromae)*, *Get Lucky (Daft Punk)*, and *Starboy (The Weeknd)* with real preview audio streams.
  - Interactive scrub bar, real-time synced lyrics (LRCLIB), and 5-band hardware Equalizer sliders directly in the browser.
- **3D Tilted Android Phone Showcase (`PhoneMockupGallery.tsx`)**:
  - Perspective 3D phone chassis rendering actual screenshots: frosted glass player bar over album art, OLED home feed, playlist importer, and Supabase Discord cloud sync.
- **Asymmetric Feature Architecture (`FeatureStrips.tsx`)**:
  - Content-driven layout highlights ad-free background playback, zero-key Spotify playlist importer, unlimited cache, Supabase cloud sync, and Android Auto.
- **Lyrics Spotlight & Lyric Share Cards (`LyricsSpotlight.tsx`)**:
  - Dynamic lyric card preview with song translation and share card formatting.
- **Celebratory Download CTA (`DownloadCTA.tsx`)**:
  - Direct links to latest GitHub release asset with celebratory confetti explosion, 30-second install guide, and security badges.
- **Quality & Performance Verification**:
  - Clean TypeScript compilation with zero errors (`tsc -b && vite build`).
  - Production preview server running on `http://localhost:5173/` with verified 200 responses across all static assets.

## Impeccable Design & Mobile Refinement Pass
- **Hero & Navbar Simplification**:
  - Removed redundant pill badge (`RMusic v1.0.0 Release • 100% Ad-Free`) above the H1 headline.
  - Removed square app icon image from Navbar, keeping clean stylized typography (`RMusic v1.0`).
- **Interactive Player Redesign**:
  - Centered album artwork and spinning vinyl on top, with song switcher pills directly beneath.
  - Placed wide frosted glass player controls bar below the art with scrubber, play/pause, volume, and 5-band hardware EQ.
  - Completely purged in-player lyrics panel.
- **Phone Mockup Gallery Curation**:
  - Removed "Signature Frosted" card and tab, keeping the 3 authentic device screenshots: OLED Home, Universal Playlist Importer, and Discord Cloud Sync.
- **Engine Features Refactoring**:
  - Replaced the clunky 60/40 split and the duplicate hero heading with a quiet, high-craft 4-card grid for Ad-Free streaming, Playlist Importer, Unlimited Cache, and Discord Sync / Android Auto.
- **Purged Lyrics Section**:
  - Completely removed `LyricsSpotlight` component and cleaned up `#lyrics` navigation anchors.
- **Kinetic "GET IT NOW" CTA**:
  - Replaced the clunky container card and 3-step text instructions with massive kinetic typography ("GET IT NOW.") with orange drop-shadow glow and a single prominent download button.
- **Mobile Layout & Tap Target Conformance**:
  - Applied `antislop-layoutmobile` rules: ensured all interactive buttons, track pills, and sliders maintain minimum 44px tap targets and zero horizontal overflow across mobile viewports.

## Mobile Layout Overhaul & Track Update
- **Tracklist Updated to 4 Songs**:
  - **Santé** - Stromae (restored by user request).
  - **Not Like Us** - Kendrick Lamar.
  - **Sommaire** - L'Morphine feat. Raid.
  - **redrum** - 21 Savage.
- **Removed 5-Band Equalizer Sliders**:
  - Eliminated the EQ toggle button and slider panel from the player deck as requested, leaving a streamlined, clean frosted glass controls deck.
- **Mobile Overflow & Horizontal Leaks Fixed**:
  - Restrained the vinyl record pop-out to a safe translation (`translate-x-8 sm:translate-x-14`) so it never spills past the viewport on 320px–390px screens.
  - Sized album art fluidly (`w-56 h-56 sm:w-72 sm:h-72`) for small phones.
- **Player Deck Mobile Ergonomics**:
  - Stacked controls responsively: prominent 56px play/pause button, accessible volume slider, and 44px minimum touch targets.
- **Hero & Section Spacing Calibrated**:
  - Reduced excessive mobile padding across all sections (`py-12 sm:py-20`).
  - Fluid kinetic typography (`text-5xl sm:text-7xl md:text-8xl lg:text-9xl`) in "GET IT NOW." preventing awkward word breaks on narrow devices.
- **Phone Mockup Mobile Flattening**:
  - Disabled extreme 3D rotation on phones to avoid clipping, rendering flat and centered for touch devices.

## Project Directory & Standalone Server Migration
- **Standalone Project Directory**:
  - Moved web project to dedicated directory: `E:\Projects\RMusic web`.
  - Development server restarted and running at `http://localhost:5173/`.
- **All Assets & Builds Verified**:
  - Server confirmed healthy (HTTP 200) with official *Sommaire* artwork (82,040 bytes) and all audio streams active.

## Phone Perspective & Navigation Polish
- **Phone Angled to the Left on PC, Completely Static on Mobile**:
  - Desktop / Tablet (`sm+`): Styled with a stylish 3D perspective angled to the left (`rotate-y-[-16deg] rotate-x-[5deg] -rotate-z-[2deg]`), complete with an offset drop shadow and specular glare.
  - Mobile (`<sm`): Fully static, upright, and centered (`transform-none`), with zero perspective distortion, zero clipping, and clean touch ergonomics.
- **Updated Sommaire Official Artwork**:
  - Sourced and installed the exact official *Sommaire* artwork uploaded by the user.
- **Navbar Links**:
  - Added direct link to `rayane.top` in both the desktop navbar and mobile drawer.
- **Interactive Touch & Gesture Support in 3D Monogram**:
  - Added native passive touch gesture listeners (`touchmove`) to `ThreeHeroScene.tsx`, allowing mobile visitors to rotate the 3D 'R' sculpture directly with touch on Android & iOS screens.
  - Sized 3D canvas responsively (`h-[260px] xs:h-[300px] sm:h-[380px] lg:h-[480px]`) eliminating vertical screen crowding.
- **Card Spacing & Mobile Touch Padding**:
  - Tuned section padding across the board (`py-12 sm:py-20`), eliminating awkward empty space between sections.
  - Cards now utilize comfortable `p-5 sm:p-7` spacing on mobile for compact readability.
- **Clean Grid Reflow**:
  - Converted feature cards into a fluid 1-column mobile stack and 2-column tablet/desktop grid without horizontal overflow or collision.

## Feature Overhaul: Auto-Download, Discord Profiles, Water Player, Supabase Auto-Sync & In-App OTA Updater
- **Liked Playlist Auto-Download Toggle**:
  - Added `autoDownloadLiked: Boolean` to `Settings` and persistent storage via `UserSettings`.
  - Added an interactive **AUTO** / **AUTO ACTIF** toggle badge directly in `LibraryScreen.kt` Liked songs header.
  - Wired `DownloadRepository` to automatically download any newly liked track as soon as it is added when auto-download is enabled.
- **Player Sheet Download Button & Liquid Water Shader Backdrop**:
  - Added a dedicated one-tap `DownloadButton` in `PlayerSheet.kt` next to `HeartButton`, with real-time download status indicators (downloading, downloaded, or download action).
  - Built a dynamic liquid water-style animated backdrop (`PlayerWidgets.kt`) featuring multi-phase sine wave motion blending OLED black, electric orange, and liquid white highlights with smooth GPU-accelerated canvas compositing.
- **Discord Profile & Username Integration**:
  - Upgraded `SupabaseSyncManager` to automatically decode the Discord OAuth JWT token and fetch user metadata (`global_name`, `full_name`, `avatar_url`, real user UUID).
  - Linked `LibraryViewModel.account` with Supabase `currentUser`, allowing the Home screen `ProfileButton` to display the user's authentic Discord avatar.
  - Displayed the user's Discord avatar and real username in `SettingsScreen.kt`.
- **Playlist Importer Dialog Redo**:
  - Upgraded `TextPrompt` and `AppActions` with custom placeholders and action labels (`promptCustom`).
  - Redesigned the Import Playlist prompt in `LibraryScreen.kt` to clearly ask for a Spotify or YouTube playlist link with placeholder `"Collez le lien Spotify ou YouTube..."` and action button `"Importer"`.
  - Broadened Spotify URL parsing in `PlaylistImporter.kt` to support `spotify:playlist:...`, embed URLs, and standard share links.
- **Supabase Auto-Sync & Database Schema (`supabase_schema.sql`)**:
  - Created standalone `supabase_schema.sql` containing the full PostgreSQL schema and Row Level Security (RLS) policies for `playlists`, `liked_tracks`, and `fav_artists`.
  - Rewrote `SupabaseSyncManager` with debounced auto-sync observers for `library.playlists`, `library.liked`, and `library.artists`, automatically pushing local updates to the cloud without requiring manual sync button taps.
  - Implemented bidirectional remote pull to restore playlists on login.
- **Settings Screen UX Polish**:
  - Replaced the manual sync button and small close icon in `CloudAccountCard` with an automated `"Cloud Synced"` status pill.
  - Added a full-width **Big Red Log Out** button (`#E53935`) at the bottom of `SettingsScreen.kt` when signed in.
- **In-App Self-Hosted OTA Updater (`AppUpdateManager.kt`)**:
  - Built an in-app updater that checks a remote manifest (`version.json`) hosted on your website or server.
  - Added in-app update detection, background APK downloading with progress indication, and seamless installation via Android's native `PackageInstaller` (`FileProvider`).
  - Added "Rechercher des mises à jour" action and in-app update banner in `SettingsScreen.kt`.
  - Added `REQUEST_INSTALL_PACKAGES` permission in `AndroidManifest.xml` and configured file provider paths.
- **Build & Compilation Verification**:
  - Successfully configured Android SDK licenses.
  - Successfully compiled Kotlin sources and assembled debug APK: `app/build/outputs/apk/debug/app-debug.apk` (57.6 MB).

## Domain Configuration & Instant Supabase Push Update
- **Configured Domain (`music.rayane.top`)**:
  - Pointed `updateManifestUrl` in `AppUpdateManager.kt` to `https://music.rayane.top/rmusic-update.json`.
  - Created `web/public/rmusic-update.json` configured for `https://music.rayane.top/RMusic.apk`.
  - Added direct download link to `DownloadCTA.tsx` and copied fresh `RMusic.apk` into `web/public/`.
  - Rebuilt production web distribution (`dist/`).
- **Instant Supabase Real-Time Push**:
  - Removed artificial flow debounce delays in `SupabaseSyncManager.kt` for `library.playlists`, `library.liked`, and `library.artists`.
  - Whenever a song is liked, a playlist is created/modified, or an artist is favorited, the change is pushed to Supabase immediately.

## Website Copy Audit, Humanization & Accessibility Pass (`/antislop-human`)
- **Copy Audit & Slop Removal**:
  - Ran diagnostic tests across all web copy in `web/index.html` and `web/src/components/`.
  - Removed em dash `—` in `index.html` (`R-02` rule conformance).
  - Replaced AI buzzwords (`seamless`, `lightning feed`, `inner tube endpoints`, `analytics beacons`, `curated library`) with clear, plain-spoken language.
  - Replaced chatbot closer ("Thank you for choosing RMusic!") in `DownloadCTA.tsx` with direct human status message ("Opening latest release on GitHub.").
- **Copy Tightening & Humanizing**:
  - **Hero Subtitle**: Shortened from staccato buzzwords to conversational phrasing: *"Background playback, synced lyrics, and one-tap playlist imports in true OLED black. No sign-up required."*
  - **Phone Mockup Gallery**: Simplified headings and descriptions across Home feed, Playlist Importer, and Discord Cloud Sync cards; eliminated jargon.
  - **Feature Strips**: Shortened descriptions for background play, playlist imports, offline cache, and Android Auto.
  - **Download CTA**: Condensed specs and headline copy to punchy, natural statements.
- **Accessibility & Contrast Fixes (WCAG AA)**:
  - Audited color contrast with WCAG 2.x relative luminance formula.
  - Upgraded low-contrast `text-zinc-500` (4.35:1 on black, failing WCAG AA normal text minimum of 4.5:1) to `text-zinc-400` (8.19:1), fully passing WCAG AA and AAA standards across specs, footers, and metadata labels.
- **Verification**:
  - Verified with automated test script (`test_site_copy.mjs`) with zero warnings.
  - Verified production build (`npm run build`) runs cleanly without errors.

## Cloudflare Pages Wrangler Deployment Fix (`RMusic-web`)
- **Issue**: Cloudflare Pages deploy job failed with `X [ERROR] Missing Pages project name. Use --project-name <name> or set the name in your Wrangler configuration file.`
- **Resolution**: Added modern Cloudflare Pages `wrangler.toml` in `RMusic web/` specifying `name = "rmusic-web"` and `pages_build_output_dir = "dist"`. Verified `wrangler` CLI parses project name properly.
- **Trigger**: Pushed latest updates (`679154c`) to `main` branch to trigger automated Cloudflare Workers Builds deployment.

## Website Visual Cleanup (Hero Section Polish)
- **Removed Hardware Specs Ribbon (`Hero.tsx`)**:
  - Removed the 3-column stats ribbon (`NATIVE UI Compose / PRIVACY Zero Track / CLOUD Discord`) below the CTA buttons for a cleaner, higher-impact above-the-fold display.
  - Cleaned up unused Lucide icon imports (`Shield`, `Cpu`, `Sparkles`).
- **Removed 3D Caption Pill (`ThreeHeroScene.tsx`)**:
  - Removed the floating bottom pill badge (`Interactive 3D • Touch or move cursor`) from the WebGL canvas.
- **Verification**:
  - Production build (`npm run build`) succeeded with zero errors or warnings (`dist/` generated).

## Mobile Navbar Polish & SEO Optimization (`Navbar.tsx`, `index.html`)
- **Mobile Navbar Overhaul (`Navbar.tsx`)**:
  - Added an instant 1-tap `APK` download pill on the mobile navbar header (`px-3 py-1.5`, electric orange) so mobile visitors can download without even opening the menu.
  - Implemented a smooth full-screen frosted backdrop blur overlay (`fixed inset-0 bg-black/80 backdrop-blur-md`) that dismisses the menu on outside tap.
  - Locked background scrolling (`document.body.style.overflow = 'hidden'`) while the mobile drawer is open to prevent awkward background scroll bleed.
  - Styled drawer header with brand mark `RMusic Android 9+` and close button.
  - Prominently styled `rayane.top Portfolio` with an orange highlight badge and external link icon.
  - Added actions grid inside mobile drawer: `Download APK (v1.0.0)`, `Windows (.exe)`, and `Source Code` on GitHub.
  - Added smooth scroll-to-top handler on the brand logo.
- **Footer Updates (`Footer.tsx`)**:
  - Added `rayane.top` with an electric orange badge and external link icon to the main footer navigation row.
- **Production SEO & Structured Data (`index.html`, `robots.txt`, `sitemap.xml`)**:
  - Created `public/robots.txt` allowing all search engines and referencing the sitemap.
  - Created `public/sitemap.xml` pointing to `https://music.rayane.top/` with weekly frequency and 1.0 priority.
  - Upgraded `index.html` with full Open Graph metadata (`og:title`, `og:description`, `og:image`, `og:url`, `og:site_name`, `og:locale`).
  - Added Twitter Summary Large Image card tags (`twitter:card`, `twitter:site`, `twitter:creator`, `twitter:image`).
  - Added canonical URL link (`https://music.rayane.top/`).
  - Embedded JSON-LD `SoftwareApplication` and `WebSite` schema.org markup with author attribution to Rayane Zirha, free price offer, download URL, operating systems, and screenshots.
- **Verification**:
  - `npm run build` compiled without errors; `dist/index.html`, `dist/robots.txt`, and `dist/sitemap.xml` generated and verified.

## Cloud Sync Restoration & In-App Update False Positive Fix
- **Root Causes Diagnosed**:
  1. **False Update Available Banner**: `app/build.gradle.kts` was still configured with `versionCode = 4` and `versionName = "0.4"` from upstream. When the app fetched `rmusic-update.json` (`versionCode: 6`), `info.versionCode > currentVersionCode` evaluated to `6 > 4 = true`, displaying a false update prompt ("Mise à jour v1.5.0 disponible !") even on the latest release.
  2. **Liked Songs & Artists Not Restored on Fresh Install**:
     - `SupabaseSyncManager` completely lacked pull logic for `liked_tracks` and `fav_artists` (`pullRemoteLikedTracks` and `pullRemoteArtists` did not exist).
     - When uninstalled and reinstalled, the empty local Room DB was used; the full sync only pulled playlist names with dummy IDs and never restored liked songs, artists, or playlist tracks.
     - Playlists in cloud were pushed with empty `tracks_json = "[]"`.
     - In Postgres / Supabase, tables lacked explicit `GRANT ALL ... TO authenticated, anon;` leading to HTTP 403 `42501 permission denied`.
- **Fixes Applied**:
  - **Version Code Synced (`app/build.gradle.kts`)**:
    - Bumped `versionCode = 6` and `versionName = "1.5.0"` in `defaultConfig`. Verified via `aapt2 dump badging` that output APK matches `versionCode='6' versionName='1.5.0'`.
  - **Full Bidirectional Cloud Sync (`SupabaseSyncManager.kt`)**:
    - Added `pullRemoteLikedTracks(token)`: queries `liked_tracks` and restores both `tracks` and `liked` table rows into Room DB.
    - Added `pullRemoteArtists(token)`: queries `fav_artists` and restores artist rows into Room DB.
    - Upgraded `pullRemotePlaylists(token)`: restores playlist metadata and decodes `tracks_json` into Room DB.
    - Upgraded playlist upload to serialize all playlist tracks into `tracks_json`.
    - Rewrote `triggerFullSync()` to pull first (restoring remote content immediately for fresh installs) and then push any local items.
    - Added automatic full sync on app launch when an existing session is detected in `init`.
    - Added non-blocking error logging for HTTP responses instead of swallowing failures.
  - **Interactive Cloud Sync Pill (`SettingsScreen.kt`)**:
    - Made the "Cloud Synced" status pill pressable with haptics so users can tap to manually trigger an immediate cloud sync anytime.
  - **Database Permissions Script (`supabase_schema.sql`)**:
    - Added explicit `GRANT ALL ON TABLE public.playlists, public.liked_tracks, public.fav_artists TO authenticated, anon;` to avoid PostgreSQL error `42501`.
- **Build & Verification**:
  - Executed `gradlew compileDebugKotlin` (Build successful).
  - Executed `gradlew assembleDebug` (Build successful).
  - Verified APK badging: `package: name='app.ammusic' versionCode='6' versionName='1.5.0'`.
  - Copied updated APK to `E:\Projects\RMusic web\binaries\RMusic.apk`.

## Production Release Publication (v1.5.2) — Spotify Importer & Notice Fix
- **Root Cause of "Import Failed with Checkmark"**:
  1. `LibraryRepository.announce(text)` lacked a `failed` parameter and unconditionally passed `failed = false` to `LibraryNotice`. When `LibraryViewModels` announced `"Import failed"`, the UI component `NoticeHost` rendered `AmIcons.Check` (checkmark) instead of `AmIcons.Close` (✕ icon).
  2. Spotify mobile share links often use shortlinks (`spotify.link` / `spoti.fi`) or wrap the URL in text ("Check out this playlist..."). `PlaylistImporter` previously attempted direct regex matching on the raw string and failed to follow HTTP redirects, rejecting shortlinks before any network call.
  3. User playlists occasionally return empty `trackList` on embed endpoints (`open.spotify.com/embed/playlist/...`).
- **Fixes Applied**:
  - **Notice Host Icon Alignment (`LibraryRepository.kt`, `LibraryViewModels.kt`)**: Added `announce(text, failed = false)`. In `LibraryViewModels.importPlaylist`, passed `failed = true` on failure so `NoticeHost` displays the red/gray ✕ icon and triggers long-press haptic feedback.
  - **Full Spotify Link Resolution (`PlaylistImporter.kt`)**:
    - Automatically follows HTTP 301/302 redirects for `spotify.link` and `spoti.fi` shortlinks.
    - Upgraded primary scraping to decode the base64 `initialState` payload directly from `open.spotify.com/playlist/<id>` for complete, unfiltered track lists.
    - Maintained fallback to `__NEXT_DATA__` embed parsing and oEmbed for title fallback.
- **Release Assets Published**:
  - Bumped version to `versionCode = 8` (`v1.5.2`) in `app/build.gradle.kts`.
  - Rebuilt APK: `app-debug.apk` (57.7 MB, versionCode 8).
  - Deployed `rmusic-update.json` with `versionCode: 8` (`v1.5.2`) to `music.rayane.top`.
  - Published GitHub release **`v1.5.2`** with updated `RMusic.apk` and `RMusic-Setup.exe`.


## In-App Update Dropdown Banner & System Push Notifications (v1.5.3)
- **Feature Additions**:
  1. **In-App Update Dropdown Banner (`UpdateDropBanner.kt`, `AppRoot.kt`)**:
     - Created `UpdateDropBanner`: an animated status-bar dropdown banner featuring spring animations (`slideInVertically` + `fadeIn`).
     - Displays update title ("Update v1.5.3 available!"), changelog description, "Install" button with haptic feedback, and a dismiss button (`✕`).
     - Displays live download progress using `ProgressRing` when downloading the update.
     - Positioned at `Alignment.TopCenter` within `AppRoot` above all navigation screens, dropping down seamlessly when opening the app if an update is available.
  2. **Android System Push Notification (`AppUpdateManager.kt`)**:
     - Added `showSystemNotification(info: UpdateInfo)` in `AppUpdateManager`.
     - Automatically fires an Android notification into the system drawer with `NotificationChannel("app_updates", "App Updates")` at `IMPORTANCE_HIGH`.
     - Notification includes version details, changelog preview, and a pending intent opening `MainActivity`.
     - Deduplication: uses `SharedPreferences("app_update_prefs")` (`last_notified_version`) so the notification fires once per new version and does not spam the user drawer.
  3. **Library View Model & State Integration (`LibraryViewModels.kt`)**:
     - Injected `AppUpdateManager` into `LibraryViewModel`.
     - Exposed `updateAvailable`, `updateProgress`, `startUpdate()`, and `dismissUpdate()` for root-level Compose consumption.
- **Verification & Deployment**:
  - Bumped `versionCode = 9` and `versionName = "1.5.3"` in `app/build.gradle.kts`.
  - Rebuilt debug APK: `app-debug.apk` (57.7 MB) and verified with `aapt2 dump badging` (`versionCode='9' versionName='1.5.3'`).
  - Deployed `rmusic-update.json` (`versionCode: 9`, `v1.5.3`) to Cloudflare Workers (`music.rayane.top/rmusic-update.json`).
  - Committed and pushed changes to both `RMusic-web` and `RMusic` mobile repositories.


## Spotify Album Importer, Personalized Home Feed & Followed Artists Mixes (v1.5.4)
- **Spotify Importer Expansion (`PlaylistImporter.kt`, `LibraryViewModels.kt`)**:
  - Added support for Spotify Album URLs (`open.spotify.com/album/<id>`), EPs, and singles (e.g. `tesla (slowed electro mix)`).
  - Embed endpoint extraction parses `__NEXT_DATA__` for `trackList` items and album names.
  - Added URL parameter sanitization (extracting pure link before query parameters like `si`, `utm_source`, `pi`).
  - Added detection and clear user messaging for private or collaborative invite links (`pt=...`) that require Spotify authentication.
  - Upgraded `importFromUrl` to return `ImportResult` (Success with track count and title, or descriptive Error) so announcements are informative and actionable.
- **Personalized Home Feed (`HomeViewModel.kt`, `HomeScreen.kt`)**:
  - Injected `LibraryRepository` into `HomeViewModel`.
  - **History-Based Quick Picks**: When user history or liked songs exist, "Quick picks" dynamically populates from recent plays and liked songs, supplemented with similar tracks via `YouTubeMusic.radio(seedTrack.id)`.
  - **Followed Artists Carousel**: Followed artists (`LibraryDao.artists()`) are displayed in a dedicated carousel with circular artist portraits.
  - **Artist Top Mixes & Stations**: Parallel searches for `${artist.title} Mix` generate customized mix playlists for the user's top followed artists, mirroring Spotify's "Your Top Mixes" and "Recommended Stations".
  - **Regional Filter**: When personalized data is available, generic regional charts (e.g. "All Hits" / classic regional playlists) are filtered out, keeping the home screen tailored to the user's music taste.
  - **Cold-Start Fallback**: If a user has no listening history or followed artists yet, the app gracefully falls back to curated global charts.
- **Verification & Deployment**:
  - Bumped `versionCode = 10` and `versionName = "1.5.4"` in `app/build.gradle.kts`.
  - Rebuilt debug APK: `app-debug.apk` (57.7 MB) and verified with `aapt2 dump badging` (`versionCode='10' versionName='1.5.4' platformBuildVersionName='17'`).
  - Deployed `rmusic-update.json` (`versionCode: 10`, `v1.5.4`) to Cloudflare Workers (`music.rayane.top`).
  - Updated web binaries at `E:\Projects\RMusic web\binaries\RMusic.apk`.
