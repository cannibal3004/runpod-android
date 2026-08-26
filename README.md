# RunPod Android

A native Android client for [RunPod](https://www.runpod.io), built with Kotlin and Jetpack Compose. Manage pods, storage, secrets, and billing from your phone.

## Features

- **Pods** — list, detail view, start/stop/restart actions, and live streaming logs (SSE)
- **Create pod** — template or custom image, GPU picker with availability/pricing, cloud (Secure/Community) and data center selection, exposed ports, SSH and JupyterLab toggles, and environment variables
- **Storage** — network volume management (create, rename, resize, delete) and per-pod data storage choice: volume disk (persistent, host-local) or network volume
- **Secrets** — manage account secrets (create/delete) and reference them in pod environment variables as `{{ RUNPOD_SECRET_<name> }}` via a per-variable secret picker
- **Billing** — current credit balance, spend per hour and spend limit, spend summary by category, spend over time chart, and per-pod breakdown
- **Settings** — API key management; the key is stored encrypted on-device and never leaves the app except to call the RunPod API

## Tech stack

- Kotlin, Jetpack Compose (Material 3)
- Hilt for dependency injection
- Retrofit + OkHttp (REST) with `kotlinx.serialization`
- OkHttp SSE for live pod logs
- Navigation Compose
- Android Keystore-backed encrypted storage for the API key

Targets `minSdk 26` / `targetSdk 36`, `compileSdk 37`, Java 21.

## API

Talks to the RunPod REST API v2 (`https://api.runpod.io/v2`) with a Bearer API key. A few account resources (secrets, credit balance) are only available via the GraphQL API (`https://api.runpod.io/graphql`), so the app uses both.

## Building

Requires the Android SDK (a `local.properties` with `sdk.dir` is created by Android Studio, or set it manually):

```sh
./gradlew :app:assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Project structure

```
app/src/main/java/com/canni/runpod/
├── data/
│   ├── api/          # Retrofit + GraphQL interfaces and DTOs
│   ├── auth/         # Encrypted API key storage
│   ├── logs/         # SSE log streamer
│   └── repo/         # Repositories (pods, catalog, volumes, secrets, billing, account)
├── di/               # Hilt modules
└── ui/
    ├── billing/      # Billing page
    ├── components/   # Shared composables
    ├── create/       # Create pod (form + GPU picker)
    ├── logs/         # Live logs
    ├── nav/          # Routes, drawer, shared scaffold
    ├── pod/          # Pod detail
    ├── pods/         # Pod list
    ├── secrets/      # Secrets management
    ├── settings/     # Settings / API key
    ├── setup/        # First-run API key entry
    └── storage/      # Network volume management
```
