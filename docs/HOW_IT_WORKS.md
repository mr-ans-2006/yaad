# How Yaad works: a beginner's tour of the code

This guide walks through Yaad's code in plain English. You don't need to know Kotlin yet;
each idea is explained the first time it shows up, using real lines from this project.

---

## 1. The big picture

```
 You take a screenshot
        │
        ▼
 Android saves it in DCIM/Screenshots  ──►  Yaad notices      (pickup/AutoPick.kt)
                                              │
                                              ▼
                                      ML Kit reads the text     (brain/TextReader.kt)
                                              │
                                              ▼
                     key line · links · tags · meaning-numbers  (core/ + brain/Brain.kt)
                                              │
                                              ▼
                                   saved in the database        (data/Library.kt, data/Shot.kt)
                                              │
 You type "a tool for data recovery"          ▼
        └──────────────────────────────►  search ranks every screenshot  (core/SearchRanker.kt)
                                              │
                                              ▼
                                  the screens show the results  (ui/)
```

The project has two parts ("modules"):

- **`core/`** is plain Kotlin with no Android in it. It's the *brain*: splitting text into word pieces,
  turning text into numbers, ranking search results, choosing tags and the key line. Because it has no
  Android code, it can be tested on any computer with `./gradlew :core:test`.
- **`app/`** is the Android app: screens, database, background jobs, the share button, and ML Kit.

---

## 2. Kotlin in five minutes (with Yaad's own code)

**Variables.** `val` can't change after it's set; `var` can.
```kotlin
val tagger = AutoTagger(embedder)   // fixed
var done = 0                        // changes as we go
done++
```

**Functions** start with `fun`. The type after `:` is what they give back.
```kotlin
fun dot(a: FloatArray, b: FloatArray): Float { ... }
```

**Data classes** are little boxes of related values. Kotlin writes `equals`, `toString` and `copy` for you.
```kotlin
data class OcrLine(val text: String, val height: Float = 0f, val top: Float = 0f, val block: Int = -1)
```
`= 0f` is a *default value*, so you can write `OcrLine("Hello")` and skip the rest.
`shot.copy(status = Shot.STATUS_DONE)` makes a new box with one value changed.

**Null safety.** A type with `?` may be empty (`null`). Kotlin forces you to handle that.
```kotlin
val mediaId: Long? = null              // shared images have no gallery id
shot.mediaId?.let { dao.ignore(Ignored(it)) }   // only runs if there IS an id
val name = c.getString(nameCol) ?: ""  // "?:" means "if it's null, use this instead"
```

**Lambdas** are small functions you pass around, written in `{ }`. `it` is the single input.
```kotlin
found.filter { it.mediaId !in known }   // keep only screenshots we don't know yet
```

**Objects.** `object KeyLineFinder { ... }` is a class with exactly one instance. Good for helpers.

**Enums** are a fixed list of choices: `enum class Tag { CODE, TOOLS, PLACES, ... }`.

**Coroutines (`suspend`).** Reading a screenshot takes time. A `suspend fun` can pause and wait
*without freezing the screen*. `withContext(Dispatchers.IO)` says "do this on a background thread".
```kotlin
suspend fun syncWithGallery(): Int = withContext(Dispatchers.IO) { ... }
```

**Flow** is a stream of values over time. `dao.observeAll()` is a `Flow<List<Shot>>`: every time the
database changes, it sends the new list, and the screen redraws by itself.

---

## 3. The brain: `core/`

### `WordPieceTokenizer.kt`: text → numbers the model knows
AI models don't read letters; they read *token ids*. The model has a dictionary of 30,522 pieces
(`app/src/main/assets/model/vocab.txt`). The tokenizer:
1. lowercases and removes accents ("Café" → "cafe"),
2. splits on spaces and makes each punctuation mark its own piece,
3. breaks each word into the longest pieces in the dictionary: `"tokenization"` → `token`, `##ization`
   (`##` means "glued to the piece before"),
4. adds `[CLS]` at the start and `[SEP]` at the end.

It must match the official tokenizer exactly. `TokenizerTest` checks that against output from the
official Python library.

### `OnnxEmbedder.kt`: numbers → meaning
**ONNX Runtime** runs the model file (`minilm-l6-v2-qint8.onnx`, 23 MB, already shrunk to 8-bit numbers).
The model gives back 384 numbers *for every token*. We **average** them into one list of 384 numbers
for the whole text ("mean pooling") and **normalize** it (make its length 1).
That list is the text's **embedding**: its meaning, as a direction in space.

### `Vectors.kt`: comparing meanings
Two embeddings pointing the same way = similar meaning. The **dot product** measures that
(1.0 = same meaning, about 0 = unrelated). `bestMatch` handles long screenshots, which are split into
chunks (see `Chunker` in `TextEmbedder.kt`): the screenshot's score is its best chunk's score.
`toBytes`/`fromBytes` turn the numbers into bytes so the database can store them.

### `SearchRanker.kt`: hybrid search
For each screenshot:
```
score = meaning similarity
      + 0.35 × (share of your words that appear in the text)
      + 0.15 if your whole phrase appears exactly
      + 0.10 if one of your words is the screenshot's tag
```
Screenshots with weak meaning (below 0.27) AND no matching word are hidden, so random results don't show up.
Meaning finds "restore deleted files" for "data recovery"; the word bonus makes names like "Recuva" win.

### `KeyLineFinder.kt`: the one-line summary
Every line of text gets points: bigger text than the rest, 4–14 words, mostly letters, near the top.
Lines like `9:41`, `87%`, `Reply` or `Directions Start Call Save` are thrown out first.
Headings that OCR split over two lines are glued back (`joinWrapped`). If the winner is very short
(like "Recuva"), the next real sentence is added: "Recuva — Restore deleted files".

### `LinkFinder.kt`: making links tappable
A *regular expression* (a search pattern) finds things like `https://...` and `github.com/x/y`, but
skips email addresses and normal words like "Rs. 6,499".

### `Tags.kt` + `AutoTagger.kt`: the tags
Each tag has **keywords** ("github", "npm" → Code) and **example sentences** ("a GitHub repository with
source code"). A screenshot's score for a tag = keyword points + how close its meaning is to the examples.
Above 0.35 it gets the tag (at most 3, and only ones close to the best).
**To add a tag:** add an entry in `Tags.kt` and a colour in `ui/theme/Color.kt` (`tagColor`). Done.

### `PhoneBrand.kt` + `BatteryGuides.kt`: the battery guide's content
Phone makers (Samsung, Xiaomi, Vivo, Infinix, Tecno, Oppo…) add their own battery savers, and each
one hides the switches somewhere different. These two files hold everything the battery guide says.

- **`PhoneBrand`** is an `enum class`: a fixed list of choices (`SAMSUNG`, `XIAOMI`, `VIVO`,
  `TRANSSION`, `OPPO`, `GENERIC`). Brands that share the same system software share one entry:
  Realme and OnePlus run ColorOS like Oppo, and Infinix, Tecno and itel are all made by Transsion.
- **`PhoneBrand.detect(manufacturer, brand)`** turns Android's maker name into one of those choices.
  Android gives us strings like `"samsung"`, `"INFINIX MOBILITY LIMITED"` or `"Xiaomi"`. We lower-case
  the text, split it into words, and look each word up in a small map (`"tecno" → TRANSSION`).
  Whole words only, so `"itel"` can't match inside `"satellite"`. Unknown makers (Google, Motorola…)
  get `GENERIC`, the plain Android guide.
- **`BatteryGuides`** is an `object` (a single shared instance) holding one `BatteryGuide` per brand.
  A guide is a list of `GuideStep`s (title + instructions + buttons). A `GuideButton` holds a list
  of `SettingsTarget`s: places in Settings to try, in order. A target is either a **component**
  (a specific screen inside another app, written as package + class name, like Xiaomi's
  `com.miui.securitycenter` / `…AutoStartManagementActivity`) or a standard Android **action**.
- Every button's list ends with two screens that exist on every phone: the battery optimisation list
  and Yaad's App info. So even if a brand screen is missing, something useful opens.

Why is this in `core/` and not `app/`? Because it's plain data with no Android code, it can be
tested on a computer (`BatteryGuideTest`). The app turns the descriptions into real Intents.

---

## 4. The Android app: `app/`

### `AndroidManifest.xml`: the app's ID card
Lists the screens, the share target, and permissions. Note the lines with `tools:node="remove"`:
they *remove* the internet permission that a Google library tries to add. That's the privacy promise,
enforced by Android itself.

### `YaadApp.kt`: starts first
Created once when the app starts. It holds the shared pieces (database, settings, brain, library)
and starts auto-pick's background watchers.

### `data/`: storing things
- **`Shot.kt`**: one screenshot = one row. `@Entity` tells Room to make a table from this class.
- **`ShotDao.kt`**: the questions we can ask the database, written in SQL inside `@Query("...")`.
  Room writes the real code for you at build time.
- **`YaadDatabase.kt`**: creates the `yaad.db` file in the app's private storage.
- **`Prefs.kt`**: small settings (theme, onboarding done) saved with `SharedPreferences`.
- **`Library.kt`**: *the* place that changes data. `syncWithGallery()` finds new screenshots,
  `processPending()` reads them, `importShared()` copies shared images in, `delete()` and `search()`.

### `brain/`
- **`TextReader.kt`**: opens the image (shrinking huge ones to save memory) and runs ML Kit.
  Returns lines with their size, position and paragraph number.
- **`Brain.kt`**: loads the search model the first time it's needed, then turns OCR lines into
  key line + tags + links + embedding (`understand()`).

### `pickup/`: auto-pick
- **`ScreenshotFinder.kt`**: asks Android's MediaStore for images whose folder contains "screenshot".
- **`Permissions.kt`**: which photo permission to ask for on which Android version.
- **`AutoPick.kt`**: four safety nets, because Oppo phones kill background apps:
  1. a `ContentObserver` hears gallery changes while Yaad is running,
  2. a WorkManager job with a *content trigger* wakes up when the gallery changes,
  3. an hourly WorkManager job,
  4. a catch-up every time you open the app (`onResume` in the ViewModel).

  `ScanWorker` does the work: sync, then read pending screenshots one by one, reporting progress.
  Android stops background jobs after about 10 minutes, so it stops at 8 and queues itself again.

### `ShareActivity.kt`
Appears in the share sheet as "Save to Yaad". It has no screen: copies the image, shows "Saved", closes.

### `MainActivity.kt`: the window and navigation
The app has one Activity. Inside it, **Navigation Compose** switches between screens by route name:
`"home"`, `"shot/{id}"`, `"settings"`, `"battery"`. It also opens other apps: the browser for links,
the gallery, and the battery guide's Settings pages. It works out the phone's brand once
(`ThisPhone.brand`) and hands it to the battery screen.

### `battery/SettingsDoors.kt`: opening other apps' settings safely
An **Intent** is a message to Android saying "please open this". `SettingsDoors.toIntent()` turns a
`SettingsTarget` from `core/` into an Intent: a component Intent (`setComponent(...)`) or an action
Intent (`Intent("android.settings.…")`). It fills in Yaad's package name where a screen needs to know
which app to show (`package:io.github.mrans2006.yaad`, or Xiaomi's `package_name` extra).

`SettingsDoors.open()` tries the targets one by one inside `try { … } catch`. If a screen doesn't
exist on this phone, Android throws `ActivityNotFoundException`; if it exists but is private, it throws
`SecurityException`. We catch both and move to the next target, so a missing screen can never crash
Yaad. It returns which target opened (0 = the best one), and MainActivity shows a small message if
only a backup page opened. `ThisPhone.brand` reads `Build.MANUFACTURER` and `Build.BRAND` and calls
`PhoneBrand.detect()`; `by lazy` means it's worked out the first time it's used, then remembered.

Note: opening another app's screen with an explicit component needs no permission and no
`<queries>` entry in the manifest. (Only *asking* Android whether an app is installed would.)

### `ui/YaadViewModel.kt`: the bridge
Screens shouldn't talk to the database directly. The ViewModel combines several streams (all
screenshots, search results, progress, permissions) into one `HomeState`, and the screen just draws it.
Searching waits 250 ms after you stop typing (`debounce`) so it doesn't run on every key press.

### `ui/theme/`: the look
- **`Color.kt`**: the palette and one colour per tag. `YaadColors` has a light and a dark version.
- **`Type.kt`**: the fonts. `Inter` for everything; `UrduFont` (Noto Sans Arabic, only the letters of یاد)
  for the small Urdu wordmark, because Inter has no Arabic letters.
- **`Theme.kt`**: `YaadTheme { ... }` picks light or dark and hands the colours to every screen.

### `ui/components/Soft.kt`: the soft-brutalist pieces (v1.1)
Four rules: a thin 1.5 dp outline on everything (`Modifier.outlined(...)`), ONE hard shadow only on
primary things (the search box, primary buttons, the key-line card), colour only on tags (muted
pastels), and black primary buttons (bone-white in dark mode). `Modifier.pressable` makes cards and
buttons shrink a touch while pressed. `TagChip` fades between colours when you select it.
`BottomBar.kt` is the two-tab bar (Search, Folders).

### `ui/YaadNavHost.kt` and `ui/Motion.kt`: screens and animations
`YaadNavHost` is the map of screens: the Search tab (`search` → `shot/{id}`), the Folders tab
(`folders` → `folder/{key}` → `shot/{id}`), plus `settings` and `battery`. Each tab keeps its own back
stack and scroll position. Everything sits inside a `SharedTransitionLayout`: a grid thumbnail and the
detail image share the name `shot-<id>@<list>` (`Modifier.sharedShot`), so opening a screenshot makes
the thumbnail grow into the detail image, and Back shrinks it home. Other screen changes are a gentle
fade plus a slight slide (`enterFor` / `exitFor`). With Android's "Remove animations" setting on
(`LocalReduceMotion`), screens just swap.

### `ui/Folders.kt`: how folders are made
Folders are not stored: they are worked out from the auto-tags every time the list changes. "All" holds
everything; then one folder per tag that has screenshots (in the order of `Tags.kt`); then "Untagged"
(text read, no tag fitted) and "No text" (nothing readable, or reading failed). A screenshot with two
tags is in both folders. Screenshots still being read are in "All" only until their tags arrive.

### `ui/screens/`
`SearchScreen`, `FoldersScreen` (+ `FolderScreen`), `DetailScreen`, `OnboardingScreen`, `SettingsScreen`,
`BatteryGuideScreen`, and `Common.kt` (top bar, search box, `ShotCard`, battery tip, import status).

`BatteryGuideScreen(batteryOk, myBrand, actions)` draws: a green/pink status box, then **YOUR PHONE**
with that brand's steps, then **OTHER PHONES**: one bar per other brand. Tapping a bar opens its steps
(`rememberSaveable` remembers which one is open, even if you rotate the phone). Each brand has its own
accent colour (`PhoneBrand.accent()`). The screen never opens Settings itself: a button calls
`actions.onButton(button)` and MainActivity does the opening. That keeps the screen easy to preview
and screenshot. The Search tab's one-line battery tip (`BatteryTip` in `Common.kt`) uses
`HomeState.phoneBrand` to say "Keep Yaad working on Samsung" (or "your phone" when unknown).
Each one takes a *state* (what to show) and *actions* (what happens on tap). That makes them easy to
preview and test, and is why the screenshot tests can draw them with sample data.

---

## 5. One screenshot's journey

1. You take a screenshot. Android saves `DCIM/Screenshots/Screenshot_2026...jpg`.
2. The gallery changes → `GalleryChangedWorker` (or the `ContentObserver`) calls `AutoPick.scanNow()`.
3. `ScanWorker` → `Library.syncWithGallery()` finds a new id → saves a `Shot` with status PENDING.
   (The home screen already shows a "Reading…" card.)
4. `Library.processPending()` → `TextReader.read()` (ML Kit) → `Brain.understand()`:
   key line, links, tags and embedding.
5. The `Shot` is saved again with status DONE. The `Flow` sends the new list, the card updates by itself.
6. Later you search "data recovery tool" → `Library.search()` embeds your words and calls
   `SearchRanker.rank()` → you tap the result → `DetailScreen`.

---

## 6. The tests

| Test | What it proves |
|---|---|
| `core/.../TokenizerTest` | Our tokenizer gives exactly the same pieces as the official one |
| `core/.../EmbedderTest` | Kotlin + ONNX gives the same embeddings as Python (similarity > 0.995) |
| `core/.../SearchTest` | 13 realistic searches find the right screenshot first |
| `core/.../RealOcrTest` | The same, using text from a real OCR engine (messy on purpose) |
| `core/.../TaggerTest`, `KeyLineTest`, `LinkFinderTest` | Tags, key lines and links |
| `core/.../BatteryGuideTest` | Maker names (`"samsung"`, `"TECNO MOBILE LIMITED"`, `"POCO"`, `"Google"`…) pick the right guide; every guide has steps; every button ends with screens that always exist |
| `core/.../SpeedTest` | Prints how fast embedding and ranking are |
| `app/.../AutoPickTest` | Auto-pick against a pretend Android 13 gallery: which folders, new/deleted/ignored, permissions, jobs |
| `app/.../ScreenshotsTest` | Draws every screen, light and dark, to `app/build/screenshots-v1.1/` |
| `app/.../FoldersTest` | How screenshots are sorted into folders |
| `app/.../MotionRecordingTest` | Opt-in: saves 60 fps frames of open/back and tab switching (`-Pyaad.motion=true`) |

---

## 7. Things to try (to learn by changing)

1. **Change a colour.** In `ui/theme/Color.kt`, change `TagHacks`. Run the app or `./gradlew :app:recordRoborazziDebug`.
2. **Add a tag**, like "Recipes": add it to `Tags.kt` with keywords and 3 example sentences, give it a
   colour in `tagColor()`, and add a test sample in `core/src/test/.../Samples.kt`.
3. **Make the word match stronger.** In `SearchRanker.kt`, change `0.35f` to `0.5f` and run `./gradlew :core:test`.
4. **Reword a battery step.** Edit the text in `core/.../BatteryGuides.kt`, then run
   `./gradlew :app:recordRoborazziDebug` and look at `app/build/screenshots-v1.1/12_battery_guide_light.png`.
5. **Show more of the key line.** In `ui/screens/Common.kt` → `ShotCard`, change `maxLines`.
