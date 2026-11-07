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

## What it plays — Tamil first, then everywhere

The home page is no longer a handful of rails. It is built from **54 live seed
buckets** (was 12) — **38 of them Tamil** — fetched 8 at a time, every result
language-filtered so a "Tamil melody" query can never quietly return Hindi rows.
51 buckets feed the shelves; 3 are chart-only (Tamil / India / World Top 50).

| Tier | Buckets |
| --- | --- |
| **Tamil** (majority) | fresh releases, Kollywood 2024, Anirudh, Rahman, Ilaiyaraaja, MSV, SPB, 80s–90s, love & duets, sad, kuthu, mass BGM, Vijay, Ajith, Sid Sriram, Chinmayi/Shreya, Hariharan, GV Prakash, Harris, Yuvan, Imman, Vidyasagar, Santhosh Narayanan, indie, rap, bhakti, folk, Tamil Top 50 |
| **Across India** | Bollywood, Hindi fresh, Telugu, Malayalam, Punjabi, India Top 50 |
| **The world** | Top 40, Pop, Hip-Hop, R&B, EDM, Rock, Lo-fi, K-Pop, Afrobeats, World Top 50 |

Tamil rows are detected by the service's `language` field **plus** Tamil-script
sniffing on title/artist/album, so blank or mis-tagged rows still land on the
right shelf. Language-filtering means the Tamil shelves stay Tamil.

### Home page (NOW)

Masthead (with live Tamil share %) → *Play for You* hero → **For You** rail →
*Because you played …* (once there is history) →
**Fresh Tamil** → **Tamil artists** circles (tap → artist page) → *Love, in Tamil*
→ *Kuthu & mass* → *Evergreen Tamil gold* → *Beyond the big two* → *Voices* →
*Tamil indie & rap* → *Bhakti & folk* → *Across India* → *Around the world* →
the chart → colophon. Roughly 12 rails, so there is always another shelf below
the fold instead of the same twelve songs.

## Personalized feed — "For You", and resetting it

Not another copy of the charts. Every spin is counted on-device once the stream is
actually live (a dead URL that auto-skips does not count):

- play counts per track, artist affinity, language affinity, likes;
- the **For You** rail re-ranks the whole catalog on every track change — heavy on
  the artists and languages you actually play, with a discovery boost for unplayed
  tracks, demotion of overplayed ones, and per-artist *and* per-language diversity
  guardrails (no single language can own the rail);
- **a listener with no history — or a freshly reset one — gets a Tamil-first feed:
  ~80 % Tamil, woven 4-to-1 with India and the world.** Deterministic, so it does
  not jitter while you scroll;
- the hero card (*Play for You*) starts that queue in one tap.

### Reset the feed — `Settings → Your feed → Reset my feed`

One tap opens a confirm step, then `Store.resetFeed()` wipes **only** the learning
signal (play counts, artist affinity, language affinity) and forces a fresh live
catalog fetch. Likes and recently played are deliberately kept. Afterwards the feed
is back to the Tamil-first default and the last-reset timestamp is shown.

Everything is computed locally in `data/Personalize.kt` — zero extra network calls.

## Identity — re-tuned for tired eyes

The old palette (warm brass on warm charcoal) has been replaced with a **cool slate
page and a calm sea-glass teal primary**, because the primary is now chosen for
comfort rather than punch:

- nothing is pure black or pure white; body text sits near 12:1 contrast instead of
  the ~17:1 "text glare" range that causes headaches;
- every accent is desaturated and mid-luminance, so it never blooms on a dark page,
  and the primary is distinguishable for protan/deutan vision (the old brass-vs-
  terracotta pair was the hardest combination);
- secondary text still clears WCAG AA on every surface.

The editorial language stays — serif display headlines, monospace metadata, numbered
liner-notes index, a Canvas-drawn tape-waveform seek bar, a mini player with a 2dp
accent progress hairline and full transport detail.

**Settings → Look & feel** (4th tab, *YOU*):
- **accent** — Sea glass (default), Sage, Periwinkle, Warm sand, Dusty rose;
- **text size** — S / M / L / XL, multiplied onto the system font scale;
- **high-contrast text** — brighter body copy if the default reads too soft.

All of it is observable state on `Ink`, so a change repaints the app instantly.
Bottom nav: **NOW · FIND · SHELF · YOU**.

- **NOW** — masthead greeting (time-aware), Play-for-You hero, For You rail,
  Fresh Tamil, artist circles and ~10 editorial shelves, then the chart.
- **FIND** — instant local matches + a live "From the Wire" section (debounced,
  Tamil-first quick searches).
- **SHELF** — your likes + recently spun, stored on this device.
- **YOU** — feed reset, taste summary, accent / text size / contrast.

Tapping an artist anywhere (home circles, the player's artist line) opens an
**artist page**: local tracks immediately, then a live "more from …" fetch that
adds a Tamil qualifier for Kollywood artists.

## Live connection (how it works)

```
GET {BASE}/api/search/songs?query=<q>
→ { "data": { "results": [ { "id", "name", "duration" (sec), "year", "language",
                             "image": [{"quality":"500x500","link"}],
                             "primaryArtists": "A, B", "album": "…",
                             "downloadUrl": [{"quality":"320kbps","url":"…"}] } ] } }
```

- `BASE` lives in `data/CatalogService.kt` (`const val BASE`) — change it in one place.
- Catalog = 54 seed queries fired in parallel on `Dispatchers.IO` (8 at a time),
  language-filtered per bucket, deduped by track id; **<24 Tamil tracks** or it is
  an honest error, never filler.
- **Cache**: tracks + mixes + shelves are written to SharedPreferences as JSON
  (20-minute TTL, versioned schema v3).
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
    data/Models.kt             # Track / Mix / Shelf / Catalog (@Immutable) + Tamil detection
    data/CatalogService.kt     # 54 seeds, language filtering, network + cache — the only door
    data/Personalize.kt        # Tamil-first ranking engine + default feed (no network)
    data/Store.kt              # likes, recents, play telemetry, feed reset
    data/Appearance.kt         # accent / text scale / contrast settings
    player/PlaybackManager.kt  # MediaPlayer + StateFlow, bounded auto-skip
    ui/Theme.kt                # slate palette, 5 accents, text scale, contrast
    ui/WaveformBar.kt          # the tape-waveform seek bar (tap/drag, Canvas)
    ui/Components.kt           # Artwork, LinerRow, SquareCard, ArtistCircle, …
    ui/NowScreen.kt            # front page: hero, For You, ~12 Tamil-first rails, chart
    ui/SearchScreen.kt         # local matches + live "From the Wire"
    ui/CollectionScreen.kt     # your shelf — likes + recently spun
    ui/SettingsScreen.kt       # feed reset + look & feel
    ui/ArtistScreen.kt         # artist page (local + live "more from")
    ui/FullPlayer.kt           # the "listening card" (slides up)
    ui/AudApp.kt               # scaffold: NOW/FIND/SHELF/YOU, mini player, states
```

## v1.1.0 — what changed

- Tamil-first catalog: **54 seed buckets** (was 12; 38 Tamil), language-filtered per bucket.
- Home page: ~12 Spotify-style rails + artist circles; square-card component.
- For You: Tamil-first default/after reset (~80 % Tamil), per-language guardrails,
  spins counted only when a stream actually starts.
- **Feed reset** in *Settings → Your feed*, with taste summary and last-reset time.
- New look: slate + sea-glass teal, 5 accents, 4 text sizes, high-contrast switch.
- New screens: Settings (*YOU*), Artist page. Tamil detected by language *and* script.
- *Because you played …* rail appears once real history exists; it disappears again
  (by design) after a reset, because a reset clears the affinity behind it.

## Honest status

- Code is written and carefully reviewed, and **syntax-checked with a tree-sitter
  Kotlin parser**, but still **not compiled or run** (no JDK/Gradle/Android SDK in
  this environment, and no route to maven/Google for dependencies). Build it in
  Android Studio.
- Audio, artwork, search and ranking are 100% live data — nothing is mocked.
