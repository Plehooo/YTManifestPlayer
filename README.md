# YT Manifest Player

Aplikasi Android Kotlin untuk mengambil URL stream publik YouTube dengan `yt-dlp` Android wrapper, lalu memutarnya langsung memakai Jetpack Media3/ExoPlayer. Tidak menggunakan WebView atau embed player.

## Fitur

- URL video YouTube biasa dan live publik.
- Prioritas format HLS (`m3u8`) jika yt-dlp menyediakannya.
- Fallback ke format tunggal terbaik yang bisa diputar langsung.
- Pemutaran langsung di ExoPlayer.
- Salin URL stream hasil ekstraksi.
- Tidak memakai cookies, login, atau browser session.
- Tidak mengunduh file video.

## Batasan

- URL khusus `kids.youtube.com` sengaja tidak diekstrak; gunakan aplikasi YouTube Kids resmi agar pengaturan dan perlindungan orang tua tetap berlaku.
- Video privat, membutuhkan login, age-restricted, DRM, atau yang tidak tersedia untuk ekstraksi publik tidak dipaksa terbuka.
- URL stream YouTube bersifat sementara dan dapat kedaluwarsa. Ekstrak ulang bila pemutaran gagal.
- Tidak semua video menyediakan manifest HLS. Untuk video biasa, hasilnya dapat berupa URL media langsung, bukan file `.m3u8`.
- Format adaptif yang memisahkan audio dan video tidak digabung atau diunduh. Aplikasi memilih satu format yang dapat diputar langsung.

## Build di GitHub Actions

1. Buat repository GitHub kosong.
2. Upload seluruh isi folder ini ke branch `main`.
3. Buka tab **Actions** dan jalankan workflow **Build YT Manifest Player APK**.
4. Unduh artifact `YTManifestPlayer-debug` dari hasil workflow.

## Build lokal

Memerlukan JDK 17, Android SDK platform 35, Build Tools 35.0.0, dan Gradle 8.9.

```bash
gradle --no-daemon :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## Komponen sumber

- yt-dlp Android wrapper: https://github.com/yausername/youtubedl-android
- yt-dlp: https://github.com/yt-dlp/yt-dlp
- AndroidX Media3: https://developer.android.com/media/media3

Distribusikan dan gunakan hanya untuk stream yang memang boleh diakses dan diputar dengan cara ini. Periksa lisensi serta ketentuan layanan sumber sebelum distribusi.
