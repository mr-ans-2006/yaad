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
the gallery, and the battery settings (with several ColorOS-specific guesses for "auto launch").

### `ui/YaadViewModel.kt`: the bridge
Screens shouldn't talk to the database directly. The ViewModel combines several streams (all
screenshots, search results, progress, permissions) into one `HomeState`, and the screen just draws it.
Searching waits 250 ms after you stop typing (`debounce`) so it doesn't run on every key press.

### `ui/theme/`: the look
- **`Color.kt`**: the palette and one colour per tag. `YaadColors` has a light and a dark version.
- **`Type.kt`**: the two fonts. `PixelFont` (Pixelify Sans) only for the logo and big headings;
  `BodyFont` (Space Grotesk) for everything you read.
- **`Theme.kt`**: `YaadTheme { ... }` picks light or dark and hands the colours to every screen.

### `ui/components/Brutal.kt`: the neo-brutalist pieces
The whole style is one `Modifier.brutal(...)`: draw a solid shadow shifted down-right, then the fill,
then a thick outline. `BrutalButton` slides into its shadow when pressed. `FolderCard` adds the
coloured tab on top, like a file folder.

### `ui/screens/`
`HomeScreen`, `DetailScreen`, `OnboardingScreen`, `SettingsScreen`, `BatteryGuideScreen`.
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
| `core/.../SpeedTest` | Prints how fast embedding and ranking are |
| `app/.../AutoPickTest` | Auto-pick against a pretend Android 13 gallery: which folders, new/deleted/ignored, permissions, jobs |
| `app/.../ScreenshotsTest` | Draws every screen to `docs/screenshots/` |

---

## 7. Things to try (to learn by changing)

1. **Change a colour.** In `ui/theme/Color.kt`, change `Pink`. Run the app or `./gradlew :app:recordRoborazziDebug`.
2. **Add a tag**, like "Recipes": add it to `Tags.kt` with keywords and 3 example sentences, give it a
   colour in `tagColor()`, and add a test sample in `core/src/test/.../Samples.kt`.
3. **Make the word match stronger.** In `SearchRanker.kt`, change `0.35f` to `0.5f` and run `./gradlew :core:test`.
4. **Show more of the key line.** In `HomeScreen.kt` → `ShotCard`, change `maxLines = 4`.
