# AUD — Music for every mood

A premium, Spotify-grade music listening app. Cinematic dark UI, real audio streaming,
curated playlists, artist and album pages, a full player with queue / shuffle / repeat-one,
user playlists, likes, keyboard shortcuts — and a clean seam to plug in **your own music service**.

Built with **React 19 · TypeScript · Vite · Tailwind CSS · Zustand · React Router · lucide-react**.

---

## Live mode — BeatBox (JioSaavn)

This checkout is wired to a live music service out of the box: a worker at
`https://aud29.thangamprabhu1.workers.dev` proxying JioSaavn search, metadata, artwork and
320 kbps AAC streams. On boot AUD fires **10 seed queries in parallel** (Tamil love, Anirudh,
A.R. Rahman, Vijay, breakup, Bollywood, melody, kuthu, Ilaiyaraaja), dedupes the results into
the catalog, caches them in `sessionStorage` for 15 minutes, and streams the audio straight
from Saavn's CDN — no bundled audio, no fictional data.

```ini
# .env  (already present in this checkout — gitignored)
VITE_MUSIC_API_URL=https://aud29.thangamprabhu1.workers.dev
VITE_CATALOG_MODE=auto     # auto | beatbox | rest | demo
```

- **auto** (default): BeatBox first, legacy REST contract as fallback, else the on-screen
  error with a one-click demo-catalog escape hatch.
- **beatbox / rest**: hard requirement on that single backend — failures surface the error
  screen, never silently fake data.
- **demo**: bundled catalog only.

Live touches in the UI: debounced search (≥3 chars) hits the worker and merges hits into
the catalog; a **LIVE · BeatBox API** badge in the sidebar; Settings → **Music service**
shows the connection state, a **stream-quality picker** (320 / 160 / 96 kbps) and
**Refresh catalog** to re-hydrate without reloading.

> **CORS caveat:** the worker must return `Access-Control-Allow-Origin` for browser GETs.
> If it doesn't, AUD shows a clear full-screen error with **Retry** and
> **Continue with demo catalog** — it never pretends the live data loaded.
> Saavn CDN links (images + audio) need no CORS headers.

---

## Quick start

```bash
npm install
npm run dev        # http://localhost:5173
```

Production build & preview:

```bash
npm run build      # type-checks (tsc -b) then bundles to dist/
npm run preview    # serves dist/ on http://localhost:4173
```

> Requires Node.js 18+ (developed on Node 20/22).

Without any configuration, AUD runs on a bundled **demo catalog** — 16 fully streamable
tracks across 6 artists, 6 albums and 8 curated playlists, with AI-generated artwork.

---

## Connecting your music service

> In this checkout the BeatBox worker handles live data (see above). The REST contract
> below remains fully supported — as the automatic fallback in `auto` mode, or explicitly via
> `VITE_CATALOG_MODE=rest`.

AUD is endpoint-agnostic. Point it at your API and **every screen runs on live data** —
no code changes needed.

### 1. Configure

```bash
cp .env.example .env
```

```ini
VITE_MUSIC_API_URL=https://your-api.example.com
```

Restart the dev server (`npm run dev`) — env vars are read at boot.

### 2. Implement four endpoints

All JSON, all `GET`, no auth required by the app (add your own headers in `src/services/apiClient.ts` if you need them):

| Endpoint        | Returns          |
| --------------- | ---------------- |
| `GET /songs`    | `Song[]`         |
| `GET /artists`  | `Artist[]`       |
| `GET /albums`   | `Album[]`        |
| `GET /playlists`| `Playlist[]`     |

### 3. Match the contract

```ts
interface Song {
  id: string
  title: string
  artistId: string          // → Artist.id
  albumId: string           // → Album.id
  duration: number          // seconds
  audioUrl: string          // streamable URL (mp3/ogg/m4a…)
  plays: number             // used for "Trending" + "Popular"
  explicit: boolean
  genre: string             // feeds search + genre browsing
  releaseDate: string       // ISO date
}

interface Artist {
  id: string
  name: string
  image: string | null      // absolute or /public path; null → initials avatar
  genres: string[]
  monthlyListeners: number
  followers: number
  bio: string
  verified: boolean
}

interface Album {
  id: string
  title: string
  artistId: string
  year: number
  cover: string | null      // absolute or /public path
  type: 'album' | 'single' | 'ep'
}

interface Playlist {
  id: string
  title: string
  description: string
  curator: string
  followers: number
  songIds: string[]         // references Song.id
  art:
    | { type: 'image'; src: string }
    | { type: 'gradient'; from: string; to: string; icon: string } // icon: lucide name, e.g. "Flame"
}
```

### 4. Failure behaviour (no silent fakes)

- `VITE_MUSIC_API_URL` unset (or `VITE_CATALOG_MODE=demo`) → bundled demo catalog, Settings badge shows **Demo**.
- `auto`: BeatBox error → automatic fall-through to the REST contract → if both fail, the error screen.
- Otherwise (URL set but the service errors / returns malformed payloads) → full-screen retry screen
  (**Try again**) with the error detail, plus one click to continue on the demo catalog.
  No data is ever invented silently.

See `src/services/apiClient.ts` (HTTP layer) and `src/services/musicService.ts` (catalog orchestration).

---

## Feature tour

| Area | What you get |
| --- | --- |
| Home | Time-aware greeting, quick picks, jump-back-in (recents), Made for You, trending, artists, fresh albums |
| Search | Instant filtering across songs/artists/albums/playlists, "Top result" card, genre browse tiles, curated keywords (e.g. *Monsoon*) — in live mode, ≥3 characters also queries BeatBox and merges fresh hits |
| Player | Play/pause, next/prev (restarts track if >3 s in), shuffle, repeat off→all→one, drag-to-seek scrubber, live position/duration |
| Queue | Now playing, up next, jump to any queued track, remove items, clear queue |
| Library | Filterable/sortable tabs — Playlists / Artists / Albums, includes your own playlists |
| User playlists | Create from sidebar (+), add songs via ⋯ menu or right-click → Add to playlist, remove/delete songs & playlists |
| Liked Songs | Persisted locally; dedicated page with gradient header |
| Artist pages | Verified badge, monthly listeners, follow/unfollow, popular tracks, discography, about |
| Album pages | Track list, blurred-cover banner, release footer |
| Full-screen player | Ambient artwork backdrop, big transport, persistent up-next rail |
| Extras | Notifications, Premium waitlist modal, device-connect popover, toasts, right-click context menu everywhere, compact mode, reduce motion, keyboard shortcuts |
| Settings | Music-service status (Demo / Connected live pill), stream-quality picker (320/160/96 kbps), refresh-from-BeatBox, wire-up docs for both backends |

### Keyboard shortcuts

| Key | Action | Key | Action |
| --- | --- | --- | --- |
| `Space` | Play / pause | `M` | Mute |
| `Shift` + `→` | Next track | `E` | Full-screen player |
| `Shift` + `←` | Previous track | `Esc` | Close overlays |
| `L` | Like current track | | |

Likes, recents, volume, shuffle/repeat, profile and playlists persist in `localStorage`
(`aud-player`, `aud-user` keys).

---

## Project structure

```
aud/
├── index.html                  # shell (fonts, meta, inline SVG favicon)
├── public/
│   ├── covers/                 # generated album artwork
│   └── artists/                # generated artist portraits
└── src/
    ├── App.tsx                 # HashRouter → Layout
    ├── components/             # Sidebar, TopBar, PlayerBar, ExpandedPlayer,
    │                           # SongRow, MediaCard, SeekBar, SongContextMenu, …
    ├── data/demoData.ts        # bundled demo catalog + genre tiles
    ├── hooks/                  # useCatalog (memoized lookups), useKeyboardShortcuts
    ├── pages/                  # Home, Search, Library, Playlist/Album/Artist detail,
    │                           # LikedSongs, Queue, Settings, NotFound
    ├── services/               # apiClient, musicService (BeatBox→REST→demo cascade),
    │                           # liveCatalog (Saavn mapping + cache + seed buckets), audioEngine
    ├── store/                  # Zustand: playerStore, catalogStore, userStore, toastStore, contextMenuStore
    └── types.ts                # Song / Artist / Album / Playlist contracts
```

## Demo catalog

Streams 16 sample tracks via [SoundHelix](https://www.soundhelix.com). Artwork is generated
in-app; metadata is fictional. Replace everything by pointing `VITE_MUSIC_API_URL` at your API.
