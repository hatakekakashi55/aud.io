# AUD for Android — The Listening Post

Native Android client for **AUD**, talking directly over HTTPS to the live AUD music
service (`https://aud29.thangamprabhu1.workers.dev`). Native HTTP means **no browser
CORS layer** — the worldwide catalog, artwork and live search all work out of the box
from any device with connectivity. Audio streams best-effort at 320 kbps with automatic
fallback to lower rates.

> This module was written in an environment without the Android toolchain
> (no JDK / Gradle / Android SDK). **It has not been compiled here.** Open it in
> Android Studio and build — details below. If anything trips, it will be a small
> Compose/Gradle detail, not the architecture.

## What it plays

A **worldwide shelf**, not a regional one — seeded from global genres
(Global Top 40, Pop, Hip-Hop, Electronic, R&B, Latin, K-Pop) plus a local shelf
(Rahman, Ilaiyaraaja, Tamil love, latest), so the feed opens up the whole map.

## Personalized feed — "For You"

Not another copy of the charts. Every spin is counted on-device:

- play counts per track, artist affinity, language affinity, likes;
- the **For You** rail (home) re-ranks the catalog on every track change — heavy on
  the artists and languages you actually play, with a discovery boost for unplayed
  tracks, demotion of overplayed ones, and per-artist diversity guardrails;
- the hero card (**Play for You**) starts that personalized queue in one tap;
- a brand-new listener gets a deterministic round-robin spread across all mixes —
  the whole world, no region bias.

Everything is computed locally in `data/Personalize.kt` — zero extra network calls.

## Identity — a relaxed listening room, typeset

Built for long sessions and tired eyes: warm soft-charcoal background (never pure
black), desaturated brass accent, parchment text (never chalk white), low-contrast
hairlines, roomier row heights and touch targets, larger nav icons. The visual
language is a print music paper's listening page — serif display headlines, monospace
metadata, numbered liner-notes index, "tapes" instead of playlists, a Canvas-drawn
tape-waveform seek bar, and a mini player with a 2dp brass progress hairline and
full transport detail (NOW SPINNING kicker, title, artist · year, like, next, play).

Bottom nav: **NOW · FIND · SHELF**.

- **NOW** — masthead greeting (time-aware), Play-for-You hero, For You rail,
  worldwide tape rail, Trending.
- **FIND** — instant local matches + a live "From the Wire" section (debounced,
  always fresh).
- **SHELF** — your likes + recently spun, stored on this device.

## Live connection (how it works)

```
GET {BASE}/api/search/songs?query=<q>
→ { "data": { "results": [ { "id", "name", "duration" (sec), "year", "language",
                             "image": [{"quality":"500x500","link"}],
                             "primaryArtists": "A, B", "album": "…",
                             "downloadUrl": [{"quality":"320kbps","url":"…"}] } ] } }
```

- `BASE` lives in `data/CatalogService.kt` (`const val BASE`) — change it in one place.
- Catalog = seed queries fired on `Dispatchers.IO`, deduped by track id;
  <24 merged tracks or it's an honest error, never filler.
- **Cache**: merged catalog is written to SharedPreferences as JSON (15-minute TTL).
  Fresh cache short-circuits the network; a *stale* cache is the offline fallback.
- Queries are re-encoded with `URLEncoder`; HTML entities in names are unescaped;
  320 kbps preferred with 160 → 96 → 48 fallback.
- Likes, recents and play telemetry persist via `data/Store.kt` (SharedPreferences).
- Playback: `MediaPlayer` engine (`player/PlaybackManager.kt`) exposing `StateFlow`
  (queue / position / shuffle / repeat-one), with bounded auto-skip of dead streams.

## Performance notes

- Artwork decodes capped (`192–288 px`, `RGB_565`) via Coil `ImageRequest.size(...)`.
- `Catalog` / `Mix` / `Track` are `@Immutable` — Compose can skip unchanged rows.
- Every lazy list is keyed; brushes/palettes are hoisted; position ticker runs at 700 ms.
- Catalog parse, fetch and ranking all happen off the main thread.

## Build & run

1. Android Studio **Ladybug (2024.2.1)+**, JDK 17 (bundled), device/emulator **API 24+**.
2. `File ▸ Open…` → select **this `aud-android/` folder** → let Gradle sync
   (IDE generates the wrapper and pulls dependencies).
3. Run ▶. First launch fetches the live catalog (a few seconds on a decent link).

CLI, with a global Gradle 8.7+:

```bash
cd aud-android
gradle assembleDebug    # app/build/outputs/apk/debug/app-debug.apk
gradle installDebug
```

Release: `gradle assembleRelease` — sign with your own keystore (none is bundled).

## Layout

```
aud-android/
  app/src/main/java/com/aud/android/
    MainActivity.kt            # store + player singletons, theme entry
    data/Models.kt             # Track / Mix / Catalog (@Immutable)
    data/CatalogService.kt     # all network + cache + parsing — the only door
    data/Personalize.kt        # the For You ranking engine (no network)
    data/Store.kt              # likes, recents, play telemetry (SharedPreferences)
    player/PlaybackManager.kt  # MediaPlayer + StateFlow, bounded auto-skip
    ui/Theme.kt                # relaxed Ink palette + dark scheme
    ui/WaveformBar.kt          # the tape-waveform seek bar (tap/drag, Canvas)
    ui/Components.kt           # Artwork, LinerRow, TapeCard, …
    ui/NowScreen.kt            # front page: greeting, hero, For You rail, chart
    ui/SearchScreen.kt         # local matches + live "From the Wire"
    ui/CollectionScreen.kt     # your shelf — likes + recently spun
    ui/FullPlayer.kt           # the "listening card" (slides up)
    ui/AudApp.kt               # scaffold: NOW/FIND/SHELF, mini player, states
```

## Honest status

- Code is written and carefully reviewed, **never compiled or run** (no toolchain here).
- Audio, artwork, search and ranking are 100% live data — nothing is mocked.
