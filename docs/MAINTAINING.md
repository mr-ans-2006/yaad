# Maintaining Yaad

How to change things, test them, and ship a new version.

## 1. One-time setup

- Install **Android Studio** (it includes JDK 17 and the Android SDK). Open the `yaad` folder.
- Or, without Android Studio: install JDK 17 and the Android command-line tools, then create
  `local.properties` with one line: `sdk.dir=/path/to/your/Android/sdk`.
- The first build downloads Gradle and all libraries. That takes a few minutes, once.

## 2. Everyday commands

| Command | What it does |
|---|---|
| `./gradlew assembleDebug` | Builds a test APK: `app/build/outputs/apk/debug/app-debug.apk` |
| `./gradlew installDebug` | Builds and installs it on a phone plugged in by USB (with USB debugging on) |
| `./gradlew :core:test` | Tests the brain: tokenizer, model, search, tags, key line |
| `./gradlew :core:benchmark` | Times search at 500 / 1,000 / 2,000 fake screenshots (not part of `:core:test`) |
| `./gradlew :app:testDebugUnitTest` | Tests auto-pick and draws the screens |
| `./gradlew :app:recordRoborazziDebug` | Re-draws `docs/screenshots/` after you change the design |
| `./gradlew assembleRelease` | Builds the signed release APK (needs your signing key, see below) |

On Windows use `gradlew.bat` instead of `./gradlew`.

**Run the tests before every release.** If a search test fails after you change the ranking,
the test tells you exactly which search got worse.

## 3. Where to change what

| I want to… | Edit |
|---|---|
| Change colours | `app/src/main/java/.../ui/theme/Color.kt` |
| Change fonts or text sizes | `ui/theme/Type.kt` (font files are in `app/src/main/res/font/`) |
| Change how cards/buttons look | `ui/components/Brutal.kt` |
| Change a screen's layout or words | `ui/screens/*.kt` |
| Add or change a tag | `core/src/main/kotlin/.../Tags.kt` + a colour in `tagColor()` in `Color.kt` |
| Change how the key line is chosen | `core/.../KeyLineFinder.kt` |
| Change search ranking | `core/.../SearchRanker.kt` (weights and the 0.27 "meaning floor") |
| Change which folders count as screenshots | `app/.../pickup/ScreenshotFinder.kt` |
| Change background checking | `app/.../pickup/AutoPick.kt` |
| Reword battery-guide steps or buttons | `core/.../BatteryGuides.kt` |
| Change which phone maker gets which guide | `core/.../PhoneBrand.kt` (see "Adding a phone brand" below) |
| Change the battery guide's layout | `ui/screens/BatteryGuideScreen.kt` |
| Change what's stored per screenshot | `app/.../data/Shot.kt` (see "Database changes" below) |

## 4. Database changes (important)

If you add a column to `Shot`, people who already have Yaad installed have the OLD table.
1. Increase `version = 1` to `version = 2` in `data/YaadDatabase.kt`.
2. Add a *migration* that changes the old table, for example:
   ```kotlin
   val MIGRATION_1_2 = object : Migration(1, 2) {
       override fun migrate(db: SupportSQLiteDatabase) {
           db.execSQL("ALTER TABLE shots ADD COLUMN favorite INTEGER NOT NULL DEFAULT 0")
       }
   }
   ```
   and register it: `Room.databaseBuilder(...).addMigrations(MIGRATION_1_2).build()`.

Without a migration the app crashes on update. Never skip this step.

## 4a. Adding a phone brand to the battery guide

Example: adding Huawei / Honor (EMUI / MagicOS).

1. **`core/.../PhoneBrand.kt`**: add an entry to the enum, e.g.
   `HUAWEI("Huawei · Honor", "EMUI / MagicOS", "Huawei and Honor phones"),`
   and add its maker words to `wordsToBrand`: `"huawei" to HUAWEI, "honor" to HUAWEI`.
   (Find the real maker name for a phone by searching "Build.MANUFACTURER <phone>", or install a
   device-info app on that phone.)
2. **`core/.../BatteryGuides.kt`**: write `val huawei = BatteryGuide(PhoneBrand.HUAWEI, "intro…", listOf(GuideStep(…)))`
   and add it to the `all` list. Use [dontkillmyapp.com](https://dontkillmyapp.com) for the steps
   (write them in your own words). For buttons, put the brand's settings screens first with
   `SettingsTarget.component("package", "full.class.Name")`, then end the list with `thenAppInfo`
   or `thenBattery`. The test checks every button ends with those.
3. **`ui/screens/BatteryGuideScreen.kt`**: give it a colour in `PhoneBrand.accent()`. Kotlin will
   refuse to build until you do, because that `when` must cover every brand.
4. **`core/src/test/.../BatteryGuideTest.kt`**: add its maker names to `detectsEachMaker`.
5. Run `./gradlew :core:test`, then `./gradlew :app:recordRoborazziDebug` and add a screenshot test
   for the new brand in `ScreenshotsTest.kt` if you like.
6. Add a row to the battery table in `README.md`.

Component names are unofficial and change between phone versions. If someone reports that a button
opened the wrong page, find the right screen with `adb shell dumpsys activity activities | grep mResumedActivity`
while that page is open on their phone, and add it to the front of the list.

## 5. Your signing key (keep it safe!)

Android only installs an **update** if it is signed with the **same key** as the installed version.
Lose the key and every user has to uninstall Yaad (and lose their library) to get a new version.

- The v1.0 key is `yaad-release.jks` with its passwords in `keystore.properties`. They were made for
  you and are kept **outside** this repo. Back both up somewhere private (a USB stick, a password manager).
- **Never commit them to GitHub.** `.gitignore` already blocks `*.jks` and `keystore.properties`.
- To build a release, put `keystore.properties` in the project root (next to `settings.gradle.kts`):
  ```
  storeFile=/full/path/to/yaad-release.jks
  storePassword=...
  keyAlias=yaad
  keyPassword=...
  ```
- To make a brand-new key (only for a new app, not an update):
  `keytool -genkeypair -keystore yaad-release.jks -alias yaad -keyalg RSA -keysize 4096 -validity 10000`

## 6. Releasing a new version

1. In `app/build.gradle.kts`, increase `versionCode` (2, 3, 4…: must always go up) and set
   `versionName` ("1.1").
2. Run all tests: `./gradlew :core:test :app:testDebugUnitTest`.
3. `./gradlew assembleRelease`
4. Check the privacy promise still holds:
   ```
   $ANDROID_HOME/build-tools/35.0.0/aapt dump permissions app/build/outputs/apk/release/app-release.apk
   ```
   `android.permission.INTERNET` must NOT be in the list. If a new library adds it, remove it in
   `AndroidManifest.xml` with `tools:node="remove"` (see the existing lines there).
5. Rename the APK to `Yaad-v1.1.apk` and attach it to a GitHub Release with a short "what's new".

## 7. Updating libraries

Versions live in `app/build.gradle.kts` and `build.gradle.kts`. Change one, build, run the tests.
Update one library at a time so you know which one broke something. After updating ML Kit or
WorkManager, always re-check permissions (step 6.4).

## 8. Changing the search model

The model files are in `app/src/main/assets/model/`. If you swap in another model:
- it must output `last_hidden_state` with 384 numbers per token (or change `Vectors.DIM`),
- update `vocab.txt` if its vocabulary differs,
- regenerate `core/src/test/resources/reference.json` with the official Python tokenizer and model,
- every saved screenshot needs new embeddings: bump the database version and mark all rows PENDING
  in the migration so they are re-read.

## 9. Testing on a real phone

The automatic tests can't run Google's text reader or the phone makers' battery rules (those only
exist on a phone). Before a release, check on your Reno 6:
- take a screenshot with Yaad closed → open Yaad → it appears, read and tagged,
- share an image from WhatsApp to "Save to Yaad",
- search with words that are *not* in the screenshot,
- tap a link in "All text" and in "Links",
- switch dark mode,
- open the Battery guide: your brand should be listed under YOUR PHONE, and each button should open a
  Settings page (the exact one, or Yaad's app info / the battery list as a backup),
- turn on airplane mode and do everything again: nothing should change.
