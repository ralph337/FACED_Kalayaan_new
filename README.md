# FACED Kalayaan — Evacuation Center Data Management & Inventory Tracking System

Android Studio project generated from the client requirements document
(*Activity 2: Requirements Analysis and Project Conceptualization*) and the
HTML clickable prototype for the MSWDO / LGU of Kalayaan, Laguna.

## What this is

An **offline-first** native Android app (Kotlin + Jetpack Compose + Room) for
MSWDO/LGU staff to file **FACED** (Family Assistance Card in Emergencies and
Disasters) forms during a disaster, manage evacuation centers and relief
inventory, and generate reports — fully usable with no internet connection,
syncing to a central database once connectivity returns.

## How to open it

1. Install **Android Studio** (Koala/2024.1 or newer recommended).
2. `File → Open...` and select this project's root folder (the one containing
   `settings.gradle.kts`).
3. Let Gradle sync (it will download dependencies from Google's/Maven's
   repositories — this requires internet access in Android Studio itself).
4. Run on an emulator or device with **minSdk 26 (Android 8.0)** or higher.

## Demo accounts (seeded on first launch)

| Role  | Username  | Password  |
|-------|-----------|-----------|
| Admin | `admin`   | `admin123`|
| Staff | `rlazaro` | `staff123`|
| Staff | `amacabuhay` | `staff123` |

There is also **"Continue Offline (Guest Mode)"** right on the login screen —
no account needed, everything is saved to the device, and you'll be offered a
one-time sync into your real account the next time you log in online.

## Feature map (→ requirements doc)

| App area | Requirements covered |
|---|---|
| **Login screen: Log In vs. Continue Offline (Guest Mode)** | Feature #1 (offline mode), Non-Functional Req: Availability |
| **Guest Mode → real-account sync popup on next online login** | Feature #1 ("automatic synchronization when connection returns") |
| **Admin → Manage Staff** (gear icon on Dashboard, Admin only) | Functional Req #4 (admin creates/deletes accounts) |
| **Dashboard** | Feature #7 (statistics & dashboard analytics), Functional Req #6 |
| **FACED tab → grouped by family → File New / Search / Sort** | Features #2, #3, #8, #15; Functional Req #2, #3, #9 |
| **Duplicate warning dialog: Update Existing / File as New / Cancel** | Functional Req #8 |
| **FACED Detail → Assistance tab** | Features #3, #17 (relief distribution, auto inventory deduction) |
| **Centers tab** (Evacuation Centers + Relief Inventory combined) | Feature #11, #14, #16 |
| **Reports tab → FACED Form Report + Disaster Summary, both CSV-exportable** | Feature #12 (audit trail), Feature #18 (DROMIC); Functional Req #7 |
| **Top bar: connectivity status + pending-sync counter + Sync button** | Feature #1, #9; Functional Req #1, #10 |
| **Room local database (works fully offline)** | Feature #1; Objective "Offline Mode with Automatic Syncing" |
| **Password hashing (SHA-256) + offline-capable auth** | Non-Functional Requirement: Security |

## Architecture

```
data/
  entity/       Room @Entity data classes (FacedRecord, HouseholdMember,
                 VulnerableTag, AssistanceRecord, EvacCenter, InventoryItem,
                 User, EventLog)
  dao/           Room @Dao interfaces (Flow-based reactive queries)
  repository/    Business logic: duplicate detection + family grouping, sync
                 simulation (incl. Guest Mode -> account re-homing), stock
                 deduction, session/connectivity state
  AppDatabase.kt Room database + seed data bootstrap
ui/
  login/         Log In + Continue Offline (Guest Mode)
  dashboard/     Stat cards + center occupancy + Admin "Manage Staff" entry
  faced/         FacedListScreen (family groups) -> FamilyRecordsScreen (every
                 filing for one family) -> FacedDetailScreen (profile + assistance);
                 FacedFormScreen (file/edit, with the duplicate-handling dialog)
  evac/          Evacuation Centers + Relief Inventory, combined under one tab
                 (CentersAndInventoryScreen)
  inventory/     Relief Inventory stock + distribution log (used by evac/ above)
  reports/       Reports tab: FACED Form (event log) report + Disaster Summary report
  admin/         Admin-only staff account management
  components/    Shared building blocks (stat cards, status pills, scaffold)
  navigation/    Single-Activity Navigation-Compose graph + manual DI factory
util/            CSV export, date/age/timestamp helpers, password hashing
```

The app is **single-Activity** (`MainActivity`) hosting a Navigation-Compose
graph; there is no XML layout — the whole UI is Jetpack Compose. Bottom
navigation has 4 tabs: **Dashboard, FACED, Centers, Reports**.

## How Offline Guest Mode + sync works

1. On the login screen, tapping **"Continue Offline (Guest Mode)"** signs the
   device into a special local `GUEST_USER` (id `-1`) — no credentials needed.
   `SessionManager.isOnline` is `false` for the whole guest session.
2. Every FACED form filed, check-in/check-out, and assistance record entered
   while a guest is tagged with that guest id (`filedByUserId` /
   `recordedByUserId` / `performedByUserId` = `-1`) and written straight to
   the local Room database, exactly like a normal logged-in session — nothing
   about the UI or data model changes for the guest.
3. The next time someone **logs in with a real account** on this device,
   `SyncManager.hasGuestPendingData()` checks whether any such rows exist. If
   so, a popup asks **"Sync Now"** or **"Not Now"** before entering the app.
   "Sync Now" calls `SyncManager.syncGuestDataToUser(...)`, which re-homes
   every guest-authored FACED record, assistance record, and event-log entry
   to the logged-in account and marks it synced.

## How the duplicate / family-grouping flow works (Change #4)

- Filing a new FACED form checks for an existing filing with the same head
  name + barangay. If found, a dialog offers:
  - **Update That Existing Record** — the new data you typed is saved onto
    the matched record instead of creating a new one; the head-of-family name
    (and barangay) fields lock so the record can't drift to a different family.
  - **File as a New Record for This Family** — saves as a new filing (a new
    disaster event for the same family), still grouped with their history.
  - **Cancel** — go back and edit the form.
- The FACED tab lists one card per **family** (head name + barangay), each
  showing how many filings exist. Tapping a family opens every filing on
  record for them; tapping a filing opens the full profile/assistance view.

## Notes on what's simulated vs. production-ready

- **Sync**: `SyncManager` currently simulates a network push (a short delay,
  then marks local rows `isSynced = true`). To connect it to a real backend,
  add a Retrofit service and replace the simulated delay with actual HTTP
  calls — everything else (the pending count badge, the guest-sync popup, the
  UI) stays the same.
- **Local encryption**: the database uses plain Room/SQLite for buildability.
  For production, swap in `net.zetetic:android-database-sqlcipher`'s
  `SupportFactory` in `AppDatabase.kt` for AES-256 at-rest encryption — noted
  in that file.
- **Export**: uses CSV (via `CsvExporter.kt`) rather than a binary PDF/XLSX
  library, so the project stays dependency-light; CSV opens directly in Excel/
  Sheets for the client's "export for printing/upload" requirement.
- **Background auto-sync**: `androidx.work` is already included as a
  dependency so a `CoroutineWorker` can be added to call `SyncManager` when
  `ConnectivityManager` reports the device is back online, instead of the
  manual "Sync" button.

## Next steps you may want before deployment

1. Add SQLCipher for at-rest DB encryption (see note above).
2. Wire `SyncManager` to a real backend once the LGU provisions one (HTTPS/TLS
   satisfies the "secured online database" requirement).
3. Add a WorkManager job for automatic background sync.
4. Add app icon assets (a placeholder adaptive icon is included).
5. Consider a proper PDF export matching the official DSWD FACED/DROMIC template.
6. Add a "reset password" action to the Admin screen (currently Admin can
   create/deactivate/delete staff but not reset an existing password).
