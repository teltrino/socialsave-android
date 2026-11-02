# Building from source

## Requirements

- JDK 17.
- Android SDK platform 35 and build tools 35.0.0 (install with Android Studio's SDK Manager).
- Internet access for the first dependency download. Gradle 8.11.1 is downloaded by the checked-in wrapper and verified against its pinned distribution checksum.

Set `JAVA_HOME` and `ANDROID_HOME` to your installations, or open this project in Android Studio and let it select the SDK. Keep machine-specific SDK paths in ignored `local.properties`.

## Debug build

Windows PowerShell:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

Linux / macOS:

```sh
bash ./gradlew testDebugUnitTest lintDebug assembleDebug
```

The APK is `app/build/outputs/apk/debug/app-debug.apk`. It is signed with a development key and is suitable for local testing. It cannot replace the maintainer-signed release APK; use an emulator or a separate test device, or uninstall the release first if you accept losing its local app data.

The optional Windows helper `./build.ps1` defaults to a debug build and copies the result to `dist/`. It can also reuse ignored runtimes under `.tools/` if present; those runtimes are not needed in a fresh clone when JDK/SDK are configured normally.

## Signed release build

Create your own release keystore with JDK `keytool` or Android Studio. Copy `signing.properties.example` to a private location and fill in its four properties. Use forward slashes in paths, including on Windows. The example contains placeholders only.

```powershell
.\build.ps1 -Configuration Release -SigningProperties C:/private/tellawy-signing.properties
```

Or use Gradle directly:

```sh
bash ./gradlew assembleRelease -PsigningProperties=/private/tellawy-signing.properties
```

Without signing properties, Gradle can create an unsigned release APK; it is not an installable release. The PowerShell release helper fails early if signing is missing. Independently signed builds cannot update the maintainer's APK. For compatible updates, preserve the package ID and signing key and increase `versionCode`.

Never upload keystores, passwords, `.tools/`, `local.properties`, or `.env` files. APKs belong in release assets, rather than Git history. Keep an offline backup of your own signing materials.

## Engine and dependency provenance

The pinned official yt-dlp asset and its SHA-256 manifest are in `app/src/main/assets/engine/`. They allow the app to start with a known engine before checking for updates. Do not replace that asset without updating and verifying the manifest. The wrapper brings Python, QuickJS, and FFmpeg native libraries for the configured ABIs.

See [third-party notices](../THIRD_PARTY_NOTICES.md) for the dependency/source map. If redistributing native binaries, preserve their notices and provide corresponding source as required by their licenses.

## Checks

GitHub Actions runs the same unit tests, lint, and debug build on Ubuntu with JDK 17. It needs no release key. Physical-device checks remain necessary for native execution, conversion, storage, notifications, and manufacturer background restrictions.
