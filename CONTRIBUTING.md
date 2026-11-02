# Contributing

Bug reports, documentation improvements, and focused pull requests are welcome.

1. Read [the build guide](docs/BUILDING.md) and [architecture notes](docs/ARCHITECTURE.md).
2. Make one focused change. Keep UI text, documentation, issues, and commit messages in English.
3. Run `./gradlew testDebugUnitTest lintDebug assembleDebug` (or `gradlew.bat` on Windows).
4. Explain the problem, resulting behavior, and how you verified it in your pull request.

For download failures, provide app/engine versions, Android/device details, source website, format/quality, and redacted error output. Site support can change independently of this app.

Never commit signing keys, passwords, access tokens, local SDK paths, personal media, or private error logs.

Native download changes should be tested on a physical device. Unit tests and desktop extractor checks do not verify Android process execution, MP3 conversion, or MediaStore saving.
