# Buchile Android Beta

This directory contains the native Android test edition of Buchile Censor. It is independent from the Streamlit/Windows runtime and performs inference locally with ONNX Runtime.

## Test-edition scope

- ARM64 phones and tablets running Android 8.0 or later.
- Offline dual-model contour detection at a fixed 640 × 640 inference size.
- Multi-image selection, previous/next preview, region-type filters, confidence control, pixel mosaic, two built-in stickers, custom stickers, preview-only number markers, current-image export, and batch export.
- Chinese and English UI, acknowledgements, project links, and the offline kitty gift.

Limitations: this first test build uses CPU inference for broad compatibility, keeps one global parameter profile for the current session, and has not yet been optimized for every Android chipset. Exported images never include preview number markers.

## Build

The two ONNX model files are kept in `app/src/main/assets/models/`. With JDK 17 and Android SDK 36 installed:

```powershell
$env:ANDROID_HOME = "D:\buchile_android_toolchain\android-sdk"
.\gradlew.bat :app:assembleArm64Debug
```

The installable APK is produced under `app/build/outputs/apk/arm64/debug/`.
