# Third-party notices

Yaad's own source code is MIT licensed (see `LICENSE`). That license covers only Yaad's own code.
The fonts, the search model and the libraries listed below are made by others and keep their own
licenses, listed in the table below (the font license texts are in the `licenses/` folder).

Yaad ships with these pieces made by others:

| What | Used for | License |
|---|---|---|
| **Inter** font, v4.0 (`app/src/main/res/font/inter_*.ttf`) by Rasmus Andersson / The Inter Project Authors | All text | SIL Open Font License 1.1, see `licenses/Inter-OFL.txt` |
| **Noto Sans Arabic** SemiBold (`app/src/main/res/font/noto_sans_arabic_semibold.ttf`) by The Noto Project Authors | Only the small یاد wordmark (Inter has no Arabic letters) | SIL Open Font License 1.1, see `licenses/NotoSansArabic-OFL.txt` |
| **all-MiniLM-L6-v2** search model, int8 ONNX build (`app/src/main/assets/model/`) | Search by meaning | Apache License 2.0 (sentence-transformers / Microsoft) |
| **ONNX Runtime** | Runs the search model | MIT |
| **Google ML Kit Text Recognition** (bundled model) | Reading text in screenshots | Free to use under Google's ML Kit terms. **Not open source.** It runs fully on the phone. |
| Jetpack Compose, Room, WorkManager, Navigation, AndroidX | The app itself | Apache License 2.0 |
| Coil | Loading thumbnails | Apache License 2.0 |

The fonts were cut down to keep the app small: Inter's variable font was turned into five fixed weights
(Regular, Medium, SemiBold, Bold, ExtraBold) and trimmed to Latin letters, punctuation and common symbols;
Noto Sans Arabic SemiBold was trimmed to the three letters of یاد. Under the OFL they keep their names and license.

Honest note: ML Kit is a closed-source Google library. It is bundled inside the app, works offline,
and Yaad removes the internet permission that ML Kit's analytics helper asks for, so it cannot send
anything. But "100% open source" would not be accurate while ML Kit is used. See `docs/V2_IDEAS.md`
for a fully open alternative.
