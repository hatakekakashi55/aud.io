# aud

AUD — a Spotify-style, high-end music listening app, plus its native Android counterpart.

| Path | What it is |
| ---- | ---------- |
| [`./aud`](./aud) | The web app — React 19 · TypeScript · Vite · Tailwind · Zustand. Production build verified (`tsc -b`, oxlint 0/0, `vite build`). Ships a bundled demo catalog and is wired to the live BeatBox/JioSaavn worker at `aud29.thangamprabhu1.workers.dev`. |
| [`./aud-android`](./aud-android) | **AUD for Android — The Listening Post.** Pure Kotlin + Jetpack Compose, editorial identity (sage-mint on deep moss, serif/mono type) — **Tamil-first live catalog** (28 seed queries), a personalized For You feed with skips + a **feed reset** that returns to the Tamil shelf, live search, on-device likes/recents, 15-min catalog cache. Written but **not compiled in this environment** — build it in Android Studio; see [`aud-android/README.md`](./aud-android/README.md). |

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
