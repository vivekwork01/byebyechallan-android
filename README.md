# ByeByeChallan - Android App

A Kotlin + Jetpack Compose Android app for tracking vehicle document expiry
(insurance, RC, PUC, permits, etc.) across multiple profiles and vehicles.

This README is written assuming **zero prior Android experience**. Follow it
top to bottom the first time.

---

## 1. One-time setup

1. Install **Android Studio**: https://developer.android.com/studio
   - Run the installer, choose the **"Standard"** setup when prompted.
   - This installs the Android SDK and an emulator automatically. Takes 15-20 min.
2. Open Android Studio → **Open** → select the `byebyechallan` folder (the one
   containing this README and the `settings.gradle.kts` file).
3. Android Studio will show "Gradle sync" in the bottom status bar - wait for
   it to finish (first time can take a few minutes, it's downloading
   dependencies). If it asks to use a JDK, accept the bundled one it suggests.

If sync fails, see **Troubleshooting** at the bottom before anything else.

---

## 2. Point the app at YOUR backend

Your backend runs locally on your laptop (`localhost:8081`), but a phone or
emulator can't reach "localhost" on your laptop directly - that word means
"this device" to them, not your laptop. So:

**Open `app/build.gradle.kts`** and find this line near the top:

```kotlin
buildConfigField("String", "BASE_URL", "\"http://localhost:8081/\"")
```

- **Testing on the Android Emulator (virtual phone on your laptop):** leave
  this as-is. `10.0.2.2` is the emulator's special alias for "the machine
  running the emulator" (i.e. your laptop). No change needed.

- **Testing on your real Android phone:** you need your laptop's local
  network IP instead.
  1. Connect your phone and laptop to the **same Wi-Fi network**.
  2. Find your laptop's local IP:
     - Windows: open Command Prompt, run `ipconfig`, look for "IPv4 Address"
       (e.g. `192.168.1.42`)
     - Mac: System Settings → Wi-Fi → Details, or run `ifconfig` in Terminal
  3. Change the line to: `"http://192.168.1.42:8081/"` (use YOUR ip)
  4. Make sure your backend (Spring Boot) is configured to listen on
     `0.0.0.0`, not just `127.0.0.1`, so it accepts connections from other
     devices on the network. Also check your laptop's firewall isn't
     blocking port 8081.

After changing this line, click the elephant/sync icon in Android Studio (or
**File → Sync Project with Gradle Files**) so the change takes effect.

---

## 3. Run the app

1. **Start your backend** first (it must be running before you open the app,
   or every screen will show network errors).
2. In Android Studio's toolbar, pick a device from the dropdown:
   - **Emulator**: click "Device Manager" (phone icon in the right sidebar) →
     create a device if none exists → it'll appear in the dropdown → select it.
   - **Real phone**: enable Developer Options on your phone (Settings → About
     Phone → tap "Build Number" 7 times), then enable "USB Debugging" inside
     Developer Options, then plug in via USB. Your phone should appear in
     the dropdown (accept the "Allow USB debugging" prompt on the phone).
3. Click the green **Run ▶** button. First build takes a few minutes.

---

## 4. App flow (what's built)

Splash (checks login) → Login / Register → Home (profile list, each showing
its soonest-expiring document) → Profile Detail (vehicle list) → Add Vehicle
(registration no + country/state/registration type/vehicle type) → Vehicle
Detail (document checklist: mandatory/optional, uploaded/pending, expiry
dates, red if expired) → Document Upload (pick file, set expiry date,
notification preferences) → saves back to backend.

---

## 5. IMPORTANT - Backend gaps found while building this

While mapping your Swagger spec to this app, a few things came up that don't
have a backend answer yet. The app has working fallbacks for all of them so
it's usable today, but please read this section - some of these are worth
fixing backend-side before you go further:

### 5a. File upload endpoint doesn't exist yet (you confirmed: simple multipart)
The app calls this endpoint, which needs to be added to your Spring Boot app:
```
POST /api/v1/document/upload
Content-Type: multipart/form-data
Body: file (binary, field name "file")
Returns: { "s3Link": "https://..." }
```
The app uploads the file here first, gets back `s3Link`, then sends that
into your existing `saveDocument` endpoint. See
`data/repository/DocumentRepository.kt` → `uploadFile()`.

### 5b. Login response has no `userId`
`AuthResponse` only returns `{token, refreshToken, message}`, but every other
endpoint needs a `userId` path parameter. The app currently **decodes the JWT
token client-side** to pull out a `userId`/`id`/`sub` claim (see
`util/JwtUtils.kt`). This works but is fragile - **recommended fix**: have
your login endpoint return `userId` directly in the JSON response, then
delete the JWT-decoding workaround.

### 5c. No endpoint to list vehicles under a profile
Your Swagger spec lets you fetch documents for a vehicle you already know the
registration number of, but there's no way to ask "what vehicles exist under
this profile?". Since you confirmed vehicles are created implicitly (on
first document upload), the app currently **caches the vehicle list locally
on the phone** (`data/local/VehicleLocalStore.kt`). This means: if the user
reinstalls the app or switches phones, they'd need to re-add their vehicles
(their actual document data on your backend is safe either way).
**Recommended fix**: add `GET /api/v1/user/{userId}/profile/{profileId}/vehicles`
returning distinct registration numbers + their stored type info.

### 5d. No endpoint for vehicle types
`GET /api/v1/document/list` requires a `vehicle_type` query param, and
`CoreVehicleTypeEntity` exists as a schema, but there's no master-data
endpoint to fetch the list of valid vehicle types. The app currently uses a
**hardcoded placeholder list** ("Two Wheeler", "Car", "Commercial Vehicle",
"Truck", "Bus") - see `PLACEHOLDER_VEHICLE_TYPES` in
`ui/screens/vehicle/AddVehicleViewModel.kt`. **Recommended fix**: add a
`GET /api/v1/master/vehicle-type` endpoint and the app can swap to it with a
one-function change.

### 5e. `docId` vs `docTemplateId` ambiguity
`DocumentRequestDto` has both `docTemplateId` and `docId` fields. The app
assumes `docTemplateId` = the checklist item's id (from `CoreDocumentEntity`),
and generates a fresh `docId` per upload instance. This assumption hasn't
been confirmed against actual backend behavior - worth double-checking once
you can test end-to-end.

### 5f. Login field naming
Register collects `email`; Login's `LoginRequest` field is called `username`.
Per your confirmation, the app sends the email value into that field. No
action needed, just documented here for clarity.

---

## 6. Project structure (for learning)

```
app/src/main/java/com/byebyechallan/app/
├── data/
│   ├── model/        - Plain data classes matching your API JSON exactly
│   ├── remote/        - Retrofit API interface, auth token storage, HTTP client
│   ├── local/          - On-device vehicle cache (see gap 5c above)
│   └── repository/   - Wraps API calls, exposes clean Success/Error results
├── ui/
│   ├── screens/        - One folder per screen, each with a Screen.kt (UI)
│   │                     and a ViewModel.kt (state + logic)
│   ├── navigation/   - All screen routes and how they connect
│   ├── theme/          - Colors, fonts, Material3 theme
│   └── components/   - Reusable buttons/banners used across screens
└── util/                  - Date formatting, JWT decoding, file handling helpers
```

Each screen follows the same pattern: a `ViewModel` holds the screen's state
and talks to repositories; the `Screen.kt` Composable just displays that
state and forwards user actions back to the ViewModel. This separation means
you can change how a screen *looks* without touching how it *works*, and
vice versa.

---

## 7. Troubleshooting

**Android Studio shows "Gradle wrapper is not configured" or similar on first
open**: this is expected and easy to fix - Android Studio will show a banner
or dialog offering to set it up automatically (it downloads one small file
using your internet connection). Click the suggested fix/OK button and let
it sync again. This only happens once.

**Gradle sync fails on first open**: usually a JDK mismatch. In Android
Studio: File → Settings → Build, Execution, Deployment → Build Tools →
Gradle → make sure "Gradle JDK" is set to the bundled JDK 17 (should be
default on a fresh install).

**App shows "Network error" on every screen**: backend isn't running, or the
`BASE_URL` doesn't match how you're testing (emulator vs real device - see
Section 2).

**Real device upload fails / "cleartext not permitted"**: shouldn't happen -
this project already enables cleartext HTTP for local development via
`android:usesCleartextTraffic="true"` in the manifest. Remove that line once
your backend has a real HTTPS URL in production.

**"Couldn't load document checklist"** on Vehicle Detail screen: your backend
may not have any `CoreDocumentEntity` records yet for the country/state/
registration type/vehicle type you picked. This is backend seed-data, not
an app bug.

---

## 8. What's intentionally NOT built yet (easy to add later)

- Forgot password
- Edit/delete profile or vehicle
- Push notifications (the email/WhatsApp/SMS toggles are saved per-document,
  but actually *sending* those is a backend job, not something this app does)
- Pre-signed S3 direct upload (you chose simple multipart for v1 - see
  Section 5a if you want to upgrade later)
