# 🎬 CastFlix (URL Stream)

A native Android streaming app built with Kotlin, Jetpack Compose and Media3 ExoPlayer. It plays movies and web series from direct links, with a Node.js backend and a password-protected admin panel.

## 📥 Download

[

![Download APK](https://img.shields.io/badge/Download-APK-00E676?style=for-the-badge&logo=android&logoColor=black)

](https://github.com/Mahabul29/CastFlix/releases/latest/download/CastFlix.apk)

**Direct link:** https://github.com/Mahabul29/CastFlix/releases/latest/download/CastFlix.apk

All versions: [Releases](https://github.com/Mahabul29/CastFlix/releases)

### How to install
1. Tap the **Download** button above (turn off your VPN if the download stalls).
2. Open the downloaded `CastFlix.apk`.
3. If Android blocks it, allow **Install from this source**.
4. If Play Protect warns you, tap **Install anyway**.
5. Open the app and allow the permissions it asks for.

Requires Android 7.0 or newer.

### Download not working?
- Open [Releases](https://github.com/Mahabul29/CastFlix/releases), pick the latest version, and tap the `.apk` under **Assets**.
- If there is no release yet, open the [Actions](https://github.com/Mahabul29/CastFlix/actions) tab and wait for the latest build to finish with a green check.
- A download stuck at 0 B means the file was not published yet or the connection dropped. Delete it and try again.
- "App not installed" usually means the file is incomplete, or an older build with a different signature is installed. Download it again, or uninstall the old app first.
- The repository must be **public** for other people to download the APK.

## ✨ Features

- Home: banner carousel, Recently Played, Categories, Recently Added, Movies and Web Series
- Search with live results and a random mix of movies and series
- Streams direct links: MP4, MKV, HLS (m3u8) and more
- Player: fullscreen, speed control, audio and subtitle tracks, resume position
- Series: episode lists with next episode
- Downloads and local files
- Settings: Appearance, Audio, Screen, Playback, Download and Privacy
- Profile: custom avatar, name and username
- Admin panel: add, edit and delete movies, series and episodes by link only

## 🖥 Backend and admin panel

The server lives in [`backend/`](backend) (Node.js, Express, MongoDB) and deploys on Koyeb.

1. Create a free MongoDB Atlas cluster and copy its connection string.
2. On Koyeb, deploy this repo with the Dockerfile at `backend/Dockerfile` and add these variables:
   - `MONGODB_URL`
   - `ADMIN_PASSWORD`
   - `JWT_SECRET`
3. Open `https://YOUR-APP.koyeb.app/admin`, enter the password, and add movies and series links.
4. Put your Koyeb URL in `app/src/main/java/com/example/data/remote/ApiConstants.kt` (`DEFAULT_KOYEB_FQDN`).

## 🛠 Built with

- Kotlin
- Jetpack Compose and Material 3
- Media3 ExoPlayer
- Room
- Retrofit and Moshi
- Node.js, Express and MongoDB

## 🔄 Auto build

Every push to `main` builds a new APK with GitHub Actions and publishes it as the **latest** release in [Releases](https://github.com/Mahabul29/CastFlix/releases).

---

Made by [Mahabul29](https://github.com/Mahabul29)
