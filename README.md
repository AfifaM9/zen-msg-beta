# zen-msg

A fully functional SMS messaging app for Android, built with Jetpack Compose.

- **Package**: `com.zenlabs.msg`
- **Version**: `1.0.0-beta`
- **minSdk 24 / targetSdk 34 / compileSdk 34**
- **Build system**: Kotlin DSL Gradle (version catalog in `gradle/libs.versions.toml`)

## What it does

Real SMS send/receive via `SmsManager` + `SMS_RECEIVED` broadcast — the same
primitives Google Messages uses. Messages are stored in a Room database and
rendered in a Compose UI (conversation list, thread view, composer).

## TRNG

The `trng` module produces unique message IDs and per-message nonces from a
combination of:

1. Hardware sensor jitter (accelerometer / magnetometer / gyroscope),
2. Android's `SecureRandom` (kernel CSPRNG),
3. Nanosecond scheduler-timing jitter.

IDs are Crockford base32, timestamp-prefixed so they're sortable and
collision-free.

## Build

```bash
export ANDROID_HOME=/path/to/Android/Sdk
./gradlew assembleRelease
# APK: app/build/outputs/apk/release/app-release.apk
```

The release build is signed with the keystore at `app/zen-release.jks`
(alias `zen`, password `zenlabs`). For production, replace with your own.

## Permissions

`SEND_SMS`, `RECEIVE_SMS`, `READ_SMS`, `RECEIVE_MMS`, `READ_CONTACTS`,
`READ_PHONE_STATE`, `POST_NOTIFICATIONS`, `WAKE_LOCK`.

Runtime permissions are requested at first launch from `MainActivity`.

> [!CAUTION]  
> This is a **beta pre-release**. Certain things will be buggy. Visit the stable branch at [AfifaM9/zen-msg](https://github.com/AfifaM9/zen-msg)
