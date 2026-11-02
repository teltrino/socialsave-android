# Privacy and network behavior

There is no analytics SDK, advertising SDK, app account, or application backend in this source tree.

- Source websites receive extraction/download requests, including normal request information such as the device's public IP address. YouTube previews use YouTube's oEmbed endpoint.
- The app checks GitHub for official yt-dlp releases when opened, with a five-minute cooldown for rapid reopens. Newer engines and checksums come from the official project. yt-dlp may fetch its official EJS components when required.
- Preview metadata, completed-file names, engine version, download state, and recent error details are stored locally. Media is saved to `Downloads/Tellawy downloader`.
- Copy details places diagnostic text on the clipboard only when requested. Error output can include source URLs; review it before sharing publicly.
- Notifications, foreground-service permissions, and a wake lock support ongoing downloads. Broad file-storage permission is not requested; MediaStore publishes completed files.

This describes this app's code, not the practices of third-party websites or the device's operating system.
