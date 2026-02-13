# YT Downloader

A simple Android app to download YouTube videos as MP4 or MP3.

## Features

- 📥 Download YouTube videos as MP4
- 🎵 Extract audio as MP3
- 📋 Paste URLs from clipboard
- 📤 Share URLs directly from YouTube app
- 📊 Download progress tracking
- 🌙 Light/Dark theme support

## Tech Stack

- **Kotlin** + **Jetpack Compose** for modern Android UI
- **NewPipe Extractor** for YouTube stream extraction
- **mobile-ffmpeg** for audio conversion
- **OkHttp** for downloads

## Setup

1. Open in Android Studio (Arctic Fox or newer)
2. Let Gradle sync dependencies
3. Run on device/emulator (API 24+)

## Usage

1. Copy a YouTube URL
2. Open the app and paste (or share directly from YouTube)
3. Select MP4 or MP3 format
4. Tap "Fetch Video Info"
5. Tap "Download"
6. Find your file in the Downloads folder

## Permissions

- **Internet**: Required for downloading
- **Storage**: Required for saving files (Android 9 and below)
- **Notifications**: For download progress (Android 13+)

## Disclaimer

This app is for personal/educational use only. Downloading YouTube content may violate YouTube's Terms of Service.

## License

MIT
