# Third-party notices and provenance

SocialSave / Tellawy downloader uses the following upstream projects. Their original copyright notices and license terms continue to apply. This file is a source map, not a replacement for upstream license texts or a claim that every native dependency uses one license.

| Component | Version / origin | License and source |
| --- | --- | --- |
| youtubedl-android library, common, FFmpeg wrapper | `io.github.junkfood02.youtubedl-android:*:0.18.1`, Maven Central | GPL-3.0 as declared by the published POM. [Project and build instructions](https://github.com/yausername/youtubedl-android), [license](https://github.com/yausername/youtubedl-android/blob/master/LICENSE). |
| yt-dlp | Official platform-independent release `2026.10.26`, bundled in `app/src/main/assets/engine/yt-dlp` | [Release assets/checksums](https://github.com/yt-dlp/yt-dlp/releases/tag/2026.10.26), [source at the release tag](https://github.com/yt-dlp/yt-dlp/tree/2026.10.26), [license](https://github.com/yt-dlp/yt-dlp/blob/2026.10.26/LICENSE). The official executable includes additional upstream components such as yt-dlp-ejs; retain their bundled notices. |
| FFmpeg and linked native codecs/libraries | Bundled by the wrapper's `ffmpeg:0.18.1` AAR | [FFmpeg legal/source information](https://ffmpeg.org/legal.html), [FFmpeg source](https://git.ffmpeg.org/ffmpeg.git), and wrapper runtime/build references above. The inspected arm64 executable enables GPL and version-3 components; do not describe this binary as LGPL-only. Individual linked libraries have their own terms. |
| CPython and QuickJS | Native runtimes bundled by `library:0.18.1` | [Python licensing](https://docs.python.org/3/license.html), [QuickJS source/license](https://bellard.org/quickjs/), and wrapper runtime/build references. |
| Kotlin standard library | Resolved by the pinned Kotlin Gradle plugin | [JetBrains Kotlin source/license](https://github.com/JetBrains/kotlin). |
| AndroidX | Transitive wrapper dependencies, pinned through Gradle resolution | [AndroidX source and notices](https://android.googlesource.com/platform/frameworks/support/). |
| Jackson 2.11.1 and Apache Commons IO 2.5 | Transitive wrapper dependencies declared by its POM | [Jackson source/license](https://github.com/FasterXML/jackson-databind/tree/jackson-databind-2.11.1), [Commons IO source/license](https://commons.apache.org/proper/commons-io/). |
| JUnit 4.13.2 | Test-only dependency | [Source/license](https://github.com/junit-team/junit4/tree/r4.13.2). |

The original engine manifest records its SHA-256. At runtime the app may install a newer official yt-dlp release; the About screen shows the installed version. Corresponding source for that version is available from the matching upstream tag. The app's Kotlin source, build files, tests, and documentation are maintained in this repository.

For native binary redistribution, use the exact wrapper runtime/build sources and preserve the notices of its linked dependencies; a link to the current FFmpeg main branch alone is not a substitute for the corresponding source of an exact redistributed binary.
