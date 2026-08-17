# Comix Android Wrapper

A minimal Android WebView wrapper that opens https://comix.to/ in a standalone app window.

## Build on GitHub
Push this repository to GitHub. The included GitHub Actions workflow builds a debug APK automatically on pushes to `main`/`master`, or manually from **Actions → Build Android APK → Run workflow**.

After the run finishes, open the workflow run and download the **Comix-Android-APK** artifact. Unzip it and install `app-debug.apk` on Android.

## Notes
- This is an unofficial wrapper and is not affiliated with Comix.
- Website behavior can change and may affect the wrapper.
- Login/session data is stored by Android WebView on the device.
