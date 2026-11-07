# AUD — home page design preview

A **static HTML mock** of the Android home screen (`aud-android`, v1.2.0), built to
review layout and colour before installing the app.

```bash
cd preview-home
python3 -m http.server 4173
# open http://localhost:4173
```

- Layout follows the *SonicWave* reference dashboard: masthead → search pill →
  Your Sound Universe (6 mood tiles) → Continue Listening → Made For Your Taste →
  Trending Now → floating mini player → 5-tab nav.
- Colours are AUD's own, **not** the reference's neon: deep slate page
  `#131719`, sea-glass teal primary `#6FC2AE`, body text `#E3E7E4` (~12:1),
  and every tile uses a low-chroma muted gradient.
- Cover art reuses the web app's demo images (`aud/public/covers`) purely as
  placeholders — the real app loads live JioSaavn artwork.

This folder is documentation only; nothing here ships in the APK.
