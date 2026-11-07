# aud

AUD — a Spotify-style, high-end music listening app, plus its native Android counterpart.

| Path | What it is |
| ---- | ---------- |
| [`./aud`](./aud) | The web app — React 19 · TypeScript · Vite · Tailwind · Zustand. Production build verified (`tsc -b`, oxlint 0/0, `vite build`). Ships a bundled demo catalog and is wired to the live BeatBox/JioSaavn worker at `aud29.thangamprabhu1.workers.dev`. |
| [`./preview-home`](./preview-home) | Static HTML **design preview** of the Android home page (dashboard layout, muted palette). `python3 -m http.server` in that folder — a mock for review, not the app. |
| [`./aud-android`](./aud-android) | **AUD for Android — The Listening Post.** Pure Kotlin + Jetpack Compose, editorial identity (sea-glass teal on cool slate, serif/mono type) — **Tamil-first live catalog (54 seed buckets, 38 Tamil)** with ~12 home shelves and artist circles, a personalized For You feed (on-device ranking, ~80 % Tamil for new/reset listeners), **feed reset in Settings**, 5 accent choices / 4 text sizes / high-contrast text, artist pages, live search, on-device likes/recents, 20-min catalog cache. Written but **not compiled in this environment** — build it in Android Studio; see [`aud-android/README.md`](./aud-android/README.md). |

```bash
cd aud
npm install
npm run dev      # http://localhost:5173
npm run build && npm run preview   # production on :4173
```

Live mode: the repo's `aud/.env` (gitignored) points `VITE_MUSIC_API_URL` at the BeatBox worker
with `VITE_CATALOG_MODE=auto` (BeatBox → legacy REST fallback → demo escape hatch).
If the worker doesn't send `Access-Control-Allow-Origin`, the browser will show AUD's
full-screen error instead of live data — click **Continue with demo catalog** or fix CORS on
the worker. Saavn CDN artwork/audio need no CORS headers.

Details: [`aud/README.md`](./aud/README.md) · [`aud/.env.example`](./aud/.env.example).
