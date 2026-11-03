# Taybii Video Player V1

Original Android Kotlin app for playing local video and audio files. No PLAYit code, logos, or copyrighted assets are used.

## Build from your Android phone with GitHub Actions

1. Create a new GitHub repository (for example `taybii-video-player`).
2. Extract this ZIP. Upload **the contents inside** `TaybiiVideoPlayer`, not the ZIP itself, to the repository root. Keep `.github/workflows/android-build.yml` and all nested directories intact. GitHub's web uploader may not preserve all folder structures; using GitHub's mobile-friendly file upload, Git client or git push is more reliable.
3. Open the repository **Actions** tab, choose **Build Taybii APK**, and click **Run workflow** (or push to `main`).
4. After a successful run, open the run page and download the `TaybiiVideoPlayer-debug-apk` artifact. Extract that download ZIP to find `app-debug.apk`.
5. Install the APK on your Android phone and grant access to videos and audio files.

## Tech stack

Kotlin; Android Gradle Plugin 8.13.2; Gradle 8.13; JDK 17; Media3 1.11.1; Android SDK 36. Uses Android platform views, no Compose dependencies. Min Android 8, target Android 16.

## Implemented in V1 source

- Video and Audio bottom tabs; local files listed from Android MediaStore; video thumbnails, file names, durations, file sizes.
- Full-screen video and audio playback with Media3 ExoPlayer; play/pause, seek and next.
- Gesture on left half controls current app window brightness; right half controls device media stream volume.
- Player settings: supported embedded audio track selection, system equalizer presets, bass boost, favorite, repeat modes, next.
- Favorites persisted locally. This first iteration saves favorites; a separate favorites browsing screen is **not** included.
- No ads, no game tab, no user account and no downloader.

## Known limitations / test points

- Build has not been compiled or installed in this authoring environment: GitHub Actions will be the first real compile test.
- Equalizer/BassBoost use Android audio effects and can be unsupported or vary by Android manufacturer.
- Local audio stops when leaving the player screen (no background playback service in V1).
- Audio track selection works only for tracks exposed by Media3, and decoder support varies.
- Modern Android permissions may expose only user-selected videos on versions/configurations with partial media access.
- For release publishing, add release signing, Play policy compliance, testing, and an AAB workflow. This workflow intentionally builds only a debug APK.
- Video resolution badges and pixel-by-pixel matched PLAYit layout are not in this initial release.

## Privacy

All file scanning and favorites are local. There is no network permission or analytics SDK.
