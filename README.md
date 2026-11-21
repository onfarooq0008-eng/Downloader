# SaveFlow – Android media downloader

Kotlin · Jetpack Compose · MVVM · Room · DataStore · Retrofit/OkHttp · Coil · Media3 ExoPlayer.
Minimum Android 7.0 (API 24), target 34.

## Get the debug APK (GitHub Actions)

1. Create a GitHub repo and push this folder to `main`.
2. Open **Actions → Build Debug APK**. It runs automatically on every push, pull request and manually via *Run workflow*.
3. When it finishes, open the run and download the **SaveFlow-debug-apk** artifact (zip containing the `.apk`).
4. Pushing a tag such as `v1.0.0` also attaches the APK to a GitHub Release:
   `git tag v1.0.0 && git push origin v1.0.0`

Workflow file: `.github/workflows/android-debug.yml` (JDK 17, Gradle 8.9 provisioned by the workflow, no wrapper needed).

## Build locally
Open the folder in Android Studio (Koala or newer) and run, or `gradle assembleDebug`.
Output: `app/build/outputs/apk/debug/app-debug.apk`.

## Release build
Create `keystore.properties` in the project root (git-ignored):

```
storeFile=/absolute/path/release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

Then `gradle assembleRelease`. Without that file the release build is signed with the debug key.

## Changing the backend
All backend knowledge is in `data/api/ApiConfig.kt` (URL, parameter names, supported qualities and platforms,
JSON key candidates) and `data/api/ResponseParser.kt`. The UI never talks to the network directly.

The only response behavior confirmed against your endpoint is: it returns the media file itself
(`Content-Type: video/mp4`). JSON handling is a best-effort fallback driven by the key lists in `ApiConfig`;
adjust them to your real JSON if the API returns any.
