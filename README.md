# OpenDownloader

OpenDownloader is an open-source, native Android TV application optimized for browsing the web, downloading files, and installing APK packages seamlessly with a standard TV remote control. It bridges the gap between TV layouts and modern web applications by incorporating a virtual cursor and custom components tailored for Leanback launcher compatibility.

---

## Features

- **Web Browser with Virtual Cursor:** Easily navigate any website using a standard TV remote control d-pad. Toggle the cursor on/off with a single click to switch between virtual pointer mode and standard focus-based navigation.
- **Robust Download Engine:**
  - Supports background downloading.
  - Allows pausing, resuming, and cancelling active downloads.
  - Automatically calculates and verifies **SHA-256 checksums** upon completion to ensure package integrity.
- **Downloads Manager:** Keep track of downloaded files, view download speeds/progress, and launch APK installations directly.
- **Bookmarks Manager:** Bookmark your favorite websites and navigate to them in a single click.
- **GitHub Releases Explorer:** Directly search public GitHub repositories (e.g., `Kodi-Game/game.libretro`), view their release history, and download specific release assets/APKs instantly.
- **Self-Updater:** Check for application updates from GitHub releases and download them automatically, with a reliable offline/mock update fallback for testing environments.
- **TV Optimized Launcher Design:** Uses both standard launcher icons and Leanback-compatible banners (`android:banner`) optimized across multiple screen densities (mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi) to fit beautifully on Android TV launchers (including customized ones like AT4K).

---

## Technical Specifications

- **Target/Compile SDK:** 34 (Android 14)
- **Minimum SDK:** 21 (Android 5.0)
- **Language:** Kotlin / Java 17
- **Database:** Room Database (version 2.6.1) with KAPT for storing downloads and bookmarks.
- **Network Library:** OkHttp 4.12.0 for API requests, downloads, and range/resume support.
- **Permissions:**
  - `android.permission.INTERNET`: For web browsing and file downloading.
  - `android.permission.ACCESS_NETWORK_STATE`: To detect network connectivity changes.
  - `android.permission.REQUEST_INSTALL_PACKAGES`: To install downloaded APKs directly (prompts user to grant via system settings on Android Oreo/API 26 and above).
  - `android.permission.READ_EXTERNAL_STORAGE` & `android.permission.WRITE_EXTERNAL_STORAGE` (max SDK 28): For storage access on legacy devices.

---

## Architecture and Project Structure

```
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml       # Main manifest declaring activities, file provider, and TV features
│   │   │   ├── kotlin/com/opendedownloader/app/
│   │   │   │   ├── MainActivity.kt        # Sidebar navigation and core coordinator
│   │   │   │   ├── bookmarks/             # Bookmarks list, adapter, and UI
│   │   │   │   ├── browser/               # WebView wrapper with custom virtual cursor control
│   │   │   │   ├── db/                    # Room Database (AppDatabase, DAOs, and entities)
│   │   │   │   ├── download/              # Multi-threaded download engine with pause/resume & SHA-256 calculation
│   │   │   │   ├── downloads/             # Download progress monitoring & installation UI
│   │   │   │   ├── github/                # GitHub release fetcher and search adapter
│   │   │   │   ├── updater/               # Self-updater check and local mock fallback system
│   │   │   │   └── utils/                 # Package installer helpers (unknown sources permission handling)
│   │   │   └── res/                       # TV layout resources, themes, drawables, and multi-density mipmaps
│   │   └── test/                          # Unit and integration tests
│   └── build.gradle                       # App-level build configurations
├── build.gradle                           # Project-level build configurations
├── settings.gradle                        # Project settings
└── gradle.properties                      # Gradle configurations
```

---

## Installation & Setup

To install OpenDownloader on your Android TV:
1. Go to the **Releases** section on this GitHub repository.
2. Download the latest pre-compiled release APK (`opendedownloader-vX.Y.Z.apk`).
3. Sideload the APK onto your Android TV device using your preferred method (e.g., via USB drive or ADB).
4. Launch the application from your TV's app drawer or Leanback launcher.

---

## How to Use OpenDownloader on Android TV

### 1. Navigating the UI with a D-Pad Remote
OpenDownloader features a sidebar navigation menu. Use your remote control's **Left/Right** d-pad buttons to move between the sidebar menu options and the main content area, and **Up/Down** buttons to move within the menus.

### 2. Using the Web Browser & Virtual Cursor
- Select **Browser** from the sidebar.
- Enter a web URL or a search query in the address bar using the on-screen keyboard. Press **Go** or Enter on your keyboard.
- **Virtual Cursor:** When active, a blue cursor pointer appears on screen. Use the remote control's **D-pad (Up, Down, Left, Right)** to move the cursor smoothly across the webpage. Press the **Center/D-pad Center** button to perform a click on whatever the cursor is pointing to.
- Toggle the virtual cursor on or off using the **Cursor On / Cursor Off** button in the top browser bar depending on whether you want a virtual mouse pointer or standard focus-based navigation.

### 3. Downloading Files & APKs
- Navigate to an APK download link inside the browser.
- Clicking the link triggers a system popup dialog asking if you want to download the file.
- Confirming will initiate the download in the background. You can check the download progress or status in the **Downloads** section.

### 4. Installing APKs and Unknown Sources Permission
- Open the **Downloads** screen.
- Select any completed download and press **Install**.
- **Important Permission Notice:** If this is your first time installing an app via OpenDownloader on Android 8.0 (Oreo) or newer, you will be redirected to the Android system settings. You must toggle/enable **Install Unknown Apps** or **Request Install Packages** permission for **OpenDownloader** to continue. Once granted, press back and click **Install** again to successfully install the APK.

### 5. Managing Bookmarks
- While on a webpage, click the **Star Icon** in the top browser bar to add the page to your bookmarks.
- Navigate to the **Bookmarks** tab in the sidebar to view, delete, or quickly jump to any of your saved links

### 6. Self-Updater
- Select **Updater** from the sidebar.
- Click **Check for Updates** to query the latest official GitHub releases for OpenDownloader.
- If a newer release is found, view the release notes and click **Download Update** to download the new version.
- *Note for developers:* If no official release is currently published or if there's no internet connection, the system will trigger a **Mock Offline Update (v1.1.0)** to allow thorough end-to-end testing of the update and installation flows.

---

## License

This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for more details.
