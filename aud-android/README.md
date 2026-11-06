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

A **Tamil-first shelf** — Tamil cinema across eras and composers (Anirudh, A. R. Rahman,
Ilaiyaraaja, Yuvan, Harris Jayaraj, Santhosh Narayanan, G. V. Prakash, Vijay/Ajith/Dhanush
film hits, Tamil love, kuthu, melody, 90s gold, rap, indie, party, fresh releases), with a
short **world tail** (Bollywood, Telugu, Malayalam, English pop, K-Pop, global charts, lo-fi)
so the home page still feels like a world of music rather than one region.

**28 live seed queries** now hydrate the catalog instead of the old 12, each one doubling as
an editorial "tape". They run in parallel on a small pool, so the bigger shelf costs about
the same boot time; each seed contributes up to 20 tracks and the union is fully deduped.
The home page is built from that union — For You rail, "Trending in Tamil", "Your last spins",
"Artists you'll love" (grouped from the live catalog), the tape rail, and a blended chart
(Tamil leads, the world follows at roughly 1 in 4).

## Personalized feed — "For You"

Not another copy of the charts. Every spin is counted on-device:

- play counts per track, artist affinity, language affinity, likes — and **skips**: a manual
  `next` inside the first 40 % (and under 90 s) of a track demotes that track and softens
  that artist;
- the **For You** rail re-ranks the catalog on every track change, heavy on the artists and
  languages you actually play, with a discovery boost for unplayed tracks, demotion of
  overplayed ones, per-artist diversity guardrails, and a **Tamil quota** (≥ 2 of every 3
  picks) so the rail stays home-language-first no matter what you play;
- the hero card (**Play for You**) starts that personalized queue in one tap;
- a brand-new listener — or anyone who just reset the feed — gets the **Tamil shelf**:
  Tamil mixes interleaved round-robin, then a short world tail. Resets rotate the shelf so
  it is never a carbon copy.

### Resetting the feed

Two taps, no dialog: the **RESET FEED** control sits under the For You rail on **NOW** and in
the **Feed tuning** card on **SHELF**. The first tap arms it ("tap again to reset"), the second
wipes play counts, artist/language affinity and skips — then the home feed immediately comes
back as the **Tamil shelf**. Likes and recents are deliberately kept: they are your library,
not a ranking signal.

Everything is computed locally in `data/Personalize.kt` — zero extra network calls.

## Identity — a calm room, easier on the eyes

Re-tuned for long sessions and tired eyes — and for legibility first:

- deep **moss charcoal** background (never pure black), warmer card surfaces, barely-there
  hairlines;
- **soft sage-mint primary** (replacing the old brass/amber) for state, actions and the play
  button, with a single warm-sand secondary used only for section kickers;
- parchment-sage text instead of chalk white — every text tone clears **WCAG AA (4.5:1)** on
  the darkest surface, and the muted tones clear it on raised cards too;
- larger type in rails and rows, language tags on every credit line (`ANIRUDH · TAMIL`),
  roomier touch targets, softer corner radii.

The visual language is still a print music paper's listening page — serif headlines,
monospace metadata, numbered liner-notes index, "tapes" instead of playlists, a Canvas-drawn
tape-waveform seek bar, and a mini player with a primary-colour progress hairline.

Bottom nav: **NOW · FIND · SHELF**.

- **NOW** — masthead greeting (time-aware), Play-for-You hero, For You rail + reset,
  last spins, Trending in Tamil, Artists you'll love, worldwide tape rail, blended chart.
- **FIND** — instant local matches + a live "From the Wire" section (debounced, always
  fresh), with Tamil-first shortcuts.
- **SHELF** — feed tuning (reset) + your likes + recently spun, stored on this device.

## Live connection (how it works)

```
GET {BASE}/api/search/songs?query=<q>
→ { "data": { "results": [ { "id", "name", "duration" (sec), "year", "language",
                             "image": [{"quality":"500x500","link"}],
                             "primaryArtists": "A, B", "album": "…",
                             "downloadUrl": [{"quality":"320kbps","url":"…"}] } ] } }
```

- `BASE` lives in `data/CatalogService.kt` (`const val BASE`) — change it in one place.
- Catalog = 28 Tamil-first seed queries fired in parallel off the main thread, deduped by
  track id; <40 merged tracks from <3 mixes and it's an honest error, never filler.
- **Cache**: merged catalog is written to SharedPreferences as JSON (15-minute TTL).
  Fresh cache short-circuits the network; a *stale* cache is the offline fallback.
- Queries are re-encoded with `URLEncoder`; HTML entities in names are unescaped;
  320 kbps preferred with 160 → 96 → 48 fallback.
- Likes, recents, play/skip telemetry and the feed-reset counter persist via
  `data/Store.kt` (SharedPreferences).
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
    data/Models.kt             # Track / Mix / Catalog / artist shelf (@Immutable)
    data/Languages.kt          # language normalisation + Tamil inference
    data/CatalogService.kt     # all network + cache + parsing — the only door
    data/Personalize.kt        # the For You ranking engine (no network)
    data/Store.kt              # likes, recents, play/skip telemetry, feed reset
    player/PlaybackManager.kt  # MediaPlayer + StateFlow, bounded auto-skip
    ui/Theme.kt                # calm moss + sage-mint palette (WCAG AA) + shapes
    ui/WaveformBar.kt          # the tape-waveform seek bar (tap/drag, Canvas)
    ui/Components.kt           # Artwork, LinerRow, TapeCard, …
    ui/NowScreen.kt            # front page: hero, For You (Tamil), rails, chart
    ui/SearchScreen.kt         # local matches + live "From the Wire"
    ui/CollectionScreen.kt     # your shelf — likes + recently spun
    ui/FullPlayer.kt           # the "listening card" (slides up)
    ui/AudApp.kt               # scaffold: NOW/FIND/SHELF, mini player, states
```

## Honest status

- Code is written and carefully reviewed, **never compiled or run** (no toolchain here).
- Audio, artwork, search and ranking are 100% live data — nothing is mocked.
