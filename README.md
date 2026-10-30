# RuuFlix Android — GitHub Cloud Build

This is an Android WebView client for:

`https://ruuflixserver1.apps.bot-hosting.cloud/`

**You do not need Android Studio or a local Android SDK.** GitHub Actions builds the app in the cloud. The first workflow run creates an installable debug APK. After you add a persistent upload keystore to GitHub Actions Secrets, the same workflow creates a signed Play Store `.aab` file.

## Included features

- RuuFlix site opens inside the app over HTTPS
- JavaScript, DOM storage and cookies for website login/session support
- Native refresh and share controls
- On-device watchlist for saved RuuFlix pages
- Android back navigation
- Full-screen HTML5 video support
- Website file-upload picker support
- External links open outside the app
- Connection error screen with retry
- SSL certificate errors are blocked (never bypassed)
- Android 16 / API 36 target

The app is a website client, not an offline movie downloader. Website/player functionality still depends on the site, its hosting, and the streaming providers.

## 1. Put this project into GitHub

1. Sign in to GitHub and choose **New repository**.
2. Name it `ruuflix-android`. A private repository is fine.
3. Create the repository with a `main` branch.
4. Unzip this project on your computer.
5. In the new GitHub repository, choose **Add file → Upload files**.
6. Upload the *contents* of this folder (including the hidden `.github` folder), then commit to `main`.
7. Open the **Actions** tab. If GitHub asks, enable Actions for the repository.
8. Choose **Build RuuFlix Android → Run workflow**. A push to `main` also starts a build.

## 2. Download and test the APK

When the workflow completes, open its run page. Under **Artifacts**, download `RuuFlix-debug-APK`, unzip it, and install the `.apk` on an Android phone. This debug APK is for testing; it is **not** the Play Store upload file.

## 3. Create your permanent Play upload key in GitHub Codespaces

You must keep the same upload key for future app updates. Do not commit the `.jks` file or passwords into the repository.

1. From the repository page, choose **Code → Codespaces → Create codespace on main**. This project includes a Java 21 Codespaces environment.
2. In the Codespaces terminal, run:

```bash
keytool -genkeypair -v \
  -keystore ruuflix-upload.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias ruuflix-upload
```

3. Choose a strong keystore password and answer the prompts. Keep a secure offline backup of `ruuflix-upload.jks` and its passwords. Losing the upload key can complicate future updates.
4. Encode the keystore for a GitHub secret:

```bash
base64 -w 0 ruuflix-upload.jks > keystore-base64.txt
```

5. In your GitHub repository, open **Settings → Secrets and variables → Actions → New repository secret**. Add these four secrets:

| Secret name | Value |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | Full contents of `keystore-base64.txt` |
| `ANDROID_KEY_ALIAS` | `ruuflix-upload` |
| `ANDROID_KEYSTORE_PASSWORD` | Password you entered for the keystore |
| `ANDROID_KEY_PASSWORD` | Key password from keytool (often the same password) |

6. Delete the temporary text file from the Codespace after copying it to GitHub Secrets:

```bash
rm -f keystore-base64.txt
```

Keep the `.jks` backup somewhere secure and private. Never upload it to a public repository, commit it, or share it in chat.

## 4. Build the Play Store AAB

Go to **Actions → Build RuuFlix Android → Run workflow**. After it succeeds, download the `RuuFlix-Play-Store-AAB` artifact. Inside is the signed `.aab` file for Google Play Console.

If only the debug APK artifact appears, one or more of the four signing secrets is missing or incorrect. Check the workflow log and the secret names.

## 5. Before publishing on Google Play

- Confirm that you own or have written permission to distribute the site and every film/series, poster, subtitle, logo, and stream exposed in the app.
- A website-only wrapper may fail Google Play's Webviews, Functionality, or User Experience reviews. This project adds native navigation, sharing, upload handling, retry/error handling and full-screen playback, but approval is never guaranteed. Add real, useful app-specific features before submission where possible.
- Publish a real privacy policy on a public HTTPS URL. `docs/privacy-policy-template.md` is only a template: complete it to match the data the website and its third parties actually collect.
- Complete the Play Console Data safety form, content rating questionnaire, target audience, ads declaration, and app access instructions truthfully.
- Prepare Play Store screenshots, a feature graphic, app description, support contact, and developer website.
- Test login, search, playback, full-screen, external links, file upload, and network failures on real Android devices.
- Use a unique application ID before the first Play upload if `com.ruuflix.app` is already taken. After publishing, changing the application ID means creating a different Play Store app.
- Google Play currently requires new apps and updates to target API 36 or higher from August 31, 2026.

## Project settings to review before the first Play upload

- Application ID: `com.ruuflix.app`
- App name: `RuuFlix`
- Website: `https://ruuflixserver1.apps.bot-hosting.cloud/`
- Version: `1.0.0` (`versionCode 1`)

Set your final application ID, developer identity, support email and privacy policy before releasing. Increment `versionCode` for every future Play Store update.

## Troubleshooting

- **The site is blank:** open the URL in Chrome on the same phone first. If it also fails there, the issue is likely the website/hosting rather than the Android wrapper.
- **Video does not play:** confirm that the website's player supports Android Chrome/WebView and that the stream provider permits embedded playback. Some DRM or third-party players need additional site-specific integration.
- **Login does not persist:** verify that the site's authentication does not require unsupported third-party browser flows. Cookies are enabled in this app.
- **GitHub build fails:** open the failed workflow step and read the first Gradle/SDK error. The workflow uses Java 17, Gradle 8.13, Android Gradle Plugin 8.13.2, and SDK 36.
