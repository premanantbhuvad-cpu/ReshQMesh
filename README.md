# ReshQMesh

**Offline Emergency Communication Prototype using Android and ESP32**

## Overview

ReshQMesh is an academic proof of concept exploring local emergency communication when conventional internet-based services are unavailable or unreliable. The Android application provides emergency SOS, messaging, location-coordinate display, Bluetooth connectivity, emergency contacts, and profile screens.

The location feature displays latitude and longitude; this version does not include an integrated offline map.

## Android application contents

- `app/src/main/java/` — Java application code and helper classes
- `app/src/main/res/layout/` — XML layouts for the application screens
- `app/src/main/res/` — icons, colors, strings, themes, and other resources
- `app/src/main/AndroidManifest.xml` — declared app components and permissions
- `app/build.gradle.kts` and root Gradle files — build configuration
- `gradle/wrapper/` and `gradlew` / `gradlew.bat` — Gradle wrapper

## Open and build

1. Install Android Studio with a compatible Android SDK.
2. Extract this project folder.
3. In Android Studio, choose **Open** and select the `ReshQMesh` folder containing `settings.gradle.kts`.
4. Allow Gradle synchronization to complete, then build and run the app.
5. Pair and test the required ESP32 hardware according to the project's existing configuration.

Android Studio may generate a new local `local.properties` file automatically. That file is intentionally not included because its SDK path is specific to the original computer.

## Current scope and limitations

- Location is shown as coordinates; no integrated offline map is included in this version.
- The demonstrated hardware setup uses two ESP32 nodes.
- Portable battery power and measured battery runtime are not included in this Android source package.
- Communication range, large-scale routing, and field reliability need further validation before any real-world emergency deployment.

## Hardware note

This archive contains the Android application project that was provided for cleanup. Include the separate ESP32 firmware/source files in the repository as well if they are part of your final submission; they were not present in this Android app ZIP.

## Project status

Academic prototype, not a certified replacement for emergency services.
