# Comix Android

Unofficial Android WebView wrapper for Comix.

## V2 behavior
- Top-level navigation is locked to Comix (`comix.to`, `comix.ws`, and their subdomains) and Google domains required for sign-in.
- External sites and custom URI schemes are blocked. The app does not hand them to Chrome/Brave.
- Conservative WebView ad/tracker blocking is included.
- Third-party *subresources* such as image/CDN/API resources are still permitted because Comix may need them to render chapters.
- System-bar layout is configured so the Comix header does not sit underneath Android status icons.
- GitHub Actions builds `Comix-Android.apk` and publishes it as a GitHub Release asset.

> This is an unofficial wrapper. Google may refuse OAuth inside embedded WebViews depending on its current authentication policy.
