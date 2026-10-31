# Ideas for Yaad v2

Roughly in order of "most useful for the effort".

## Bigger features
1. **Real AI summaries.** A small on-device language model (for example a 0.5–1B model through
   MediaPipe LLM Inference or llama.cpp) could write a one-sentence summary. Cost: a 300 MB–1 GB download
   and a few seconds per screenshot, so make it optional ("Better summaries" switch, run while charging).
2. **Urdu, Sindhi and other languages.** ML Kit's Latin reader doesn't read Arabic script. Tesseract
   (with `urd` / `snd` data) does, and a multilingual embedding model (e.g. multilingual-e5-small,
   ~120 MB) would let you search across languages.
3. **Collections and favourites.** Pin screenshots, make your own folders ("Final year project").
4. **Notes on a screenshot.** Add a sentence of your own; it becomes searchable too.
5. **Smart actions.** A screenshot with an address → "Open in Maps"; with a date → "Add to calendar";
   with a phone number → "Call". All on-device, from the text Yaad already has.
6. **Backup and move to a new phone.** Export the database to a file you choose (no server needed).

## Polish
- A home-screen widget with the search box.
- "On this day": screenshots from a year ago.
- Find duplicates and near-duplicates (same embedding) and offer to clean up.
- Pick which folders count as screenshots (some apps save to their own folders).
- Better key lines using the text's position (e.g. ignore the top app bar).
- Show *why* a result matched (highlight the matching words or the best chunk).
- Material You colour option for people who want a calmer look.

## Fully open source
- Replace ML Kit (free but closed-source) with **Tesseract** or **PaddleOCR (ONNX)**. Then Yaad
  could be listed on **F-Droid**, the open-source app store, which doesn't accept closed libraries.

## Speed and size
- Ship a "mobile-only" ONNX Runtime build with just the operators MiniLM needs: the APK could shrink
  by ~10 MB.
- Store embeddings as 8-bit numbers instead of 32-bit: 4× less space, almost the same search quality.
- An approximate-nearest-neighbour index only matters past ~50,000 screenshots. Not needed yet.

## Distribution
- **GitHub Releases** first (free), then **F-Droid** (after the OCR swap) or **Google Play**
  ($25 once; needs a privacy policy page, which is easy since Yaad collects nothing).
