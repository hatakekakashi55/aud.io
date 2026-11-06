import type { Album, Artist, Playlist, Song } from '../types'

const sh = (n: number) => `https://www.soundhelix.com/examples/mp3/SoundHelix-Song-${n}.mp3`

export const ARTISTS: Artist[] = [
  {
    id: 'karthik-rao',
    name: 'Karthik Rao',
    image: '/artists/karthik-rao.jpg',
    genres: ['Carnatic Melodic', 'Filmi', 'Classical Fusion'],
    monthlyListeners: 12_400_000,
    followers: 3_900_000,
    bio: 'Karthik Rao is a Chennai-born melodist whose voice bridges Carnatic tradition and contemporary filmi music. His 2024 album Vaanam became the defining monsoon soundtrack of the year.',
    verified: true,
  },
  {
    id: 'aanya-sharma',
    name: 'Aanya Sharma',
    image: '/artists/aanya-sharma.jpg',
    genres: ['Bollywood Pop', 'Dance Pop', 'Urban'],
    monthlyListeners: 9_600_000,
    followers: 2_700_000,
    bio: 'Aanya Sharma is the new voice of Hindi pop — a powerhouse vocalist blending film music with global dance-pop production. Raat Ke Sitare is her biggest era yet.',
    verified: true,
  },
  {
    id: 'lofi-cartel',
    name: 'Lo-Fi Cartel',
    image: null,
    genres: ['Lo-Fi', 'Chillhop', 'Ambient'],
    monthlyListeners: 3_100_000,
    followers: 940_000,
    bio: 'Lo-Fi Cartel crafts rainy-day beats and tape-saturated instrumentals for focus, study and late-night unwinding. New drops every Friday.',
    verified: true,
  },
  {
    id: 'nova-skye',
    name: 'Nova Skye',
    image: '/artists/nova-skye.jpg',
    genres: ['Synthwave', 'Retrowave', 'Electronic'],
    monthlyListeners: 2_400_000,
    followers: 780_000,
    bio: 'Nova Skye scores the neon night — analog synths, chrome arps and VHS nights. Midnight Neon is a love letter to the grid.',
    verified: true,
  },
  {
    id: 'velvet-owls',
    name: 'The Velvet Owls',
    image: '/artists/velvet-owls.jpg',
    genres: ['Indie Rock', 'Alt Country', 'Americana'],
    monthlyListeners: 1_800_000,
    followers: 520_000,
    bio: 'A four-piece desert outfit with dusty guitars, wide-open choruses and stories from the road between Joshua Tree and Jaipur.',
    verified: true,
  },
  {
    id: 'amber-collective',
    name: 'The Amber Collective',
    image: null,
    genres: ['Acoustic Folk', 'Indie Folk'],
    monthlyListeners: 860_000,
    followers: 310_000,
    bio: 'The Amber Collective is an acoustic folk ensemble built around close harmonies, fingerpicked guitars and golden-hour acoustics.',
    verified: false,
  },
]

export const ALBUMS: Album[] = [
  { id: 'midnight-neon', title: 'Midnight Neon', artistId: 'nova-skye', year: 2024, cover: '/covers/midnight-neon.jpg', type: 'album' },
  { id: 'desert-bloom', title: 'Desert Bloom', artistId: 'velvet-owls', year: 2023, cover: '/covers/desert-bloom.jpg', type: 'album' },
  { id: 'rainy-window', title: 'Rainy Window', artistId: 'lofi-cartel', year: 2024, cover: '/covers/rainy-window.jpg', type: 'album' },
  { id: 'raat-ke-sitare', title: 'Raat Ke Sitare', artistId: 'aanya-sharma', year: 2025, cover: '/covers/raat-ke-sitare.jpg', type: 'album' },
  { id: 'vaanam', title: 'Vaanam', artistId: 'karthik-rao', year: 2024, cover: '/covers/vaanam.jpg', type: 'album' },
  { id: 'golden-hour', title: 'Golden Hour', artistId: 'amber-collective', year: 2022, cover: '/covers/golden-hour.jpg', type: 'album' },
]

export const SONGS: Song[] = [
  // Nova Skye — Midnight Neon
  { id: 's1', title: 'Chrome Sunset', artistId: 'nova-skye', albumId: 'midnight-neon', duration: 254, audioUrl: sh(1), plays: 48_200_000, explicit: false, genre: 'Synthwave', releaseDate: '2024-09-13' },
  { id: 's2', title: 'Neon Overdrive', artistId: 'nova-skye', albumId: 'midnight-neon', duration: 231, audioUrl: sh(2), plays: 36_700_000, explicit: false, genre: 'Synthwave', releaseDate: '2024-09-13' },
  { id: 's3', title: 'Midnight Grid', artistId: 'nova-skye', albumId: 'midnight-neon', duration: 268, audioUrl: sh(3), plays: 22_900_000, explicit: false, genre: 'Synthwave', releaseDate: '2024-09-13' },
  { id: 's4', title: 'Ultraviolet (feat. Lo-Fi Cartel)', artistId: 'nova-skye', albumId: 'midnight-neon', duration: 289, audioUrl: sh(4), plays: 18_300_000, explicit: true, genre: 'Electronic', releaseDate: '2024-10-04' },
  // The Velvet Owls — Desert Bloom
  { id: 's5', title: 'Wildflowers', artistId: 'velvet-owls', albumId: 'desert-bloom', duration: 214, audioUrl: sh(5), plays: 41_500_000, explicit: false, genre: 'Indie Rock', releaseDate: '2023-05-19' },
  { id: 's6', title: 'Motel No Vacancy', artistId: 'velvet-owls', albumId: 'desert-bloom', duration: 187, audioUrl: sh(6), plays: 29_100_000, explicit: false, genre: 'Indie Rock', releaseDate: '2023-05-19' },
  { id: 's7', title: 'Desert Bloom', artistId: 'velvet-owls', albumId: 'desert-bloom', duration: 243, audioUrl: sh(7), plays: 63_800_000, explicit: false, genre: 'Indie Rock', releaseDate: '2023-08-02' },
  // Lo-Fi Cartel — Rainy Window
  { id: 's8', title: 'Rainy Window', artistId: 'lofi-cartel', albumId: 'rainy-window', duration: 172, audioUrl: sh(8), plays: 88_400_000, explicit: false, genre: 'Lo-Fi', releaseDate: '2024-02-16' },
  { id: 's9', title: '3AM Study Session', artistId: 'lofi-cartel', albumId: 'rainy-window', duration: 196, audioUrl: sh(9), plays: 54_600_000, explicit: false, genre: 'Lo-Fi', releaseDate: '2024-02-16' },
  { id: 's10', title: 'Coffee & Static', artistId: 'lofi-cartel', albumId: 'rainy-window', duration: 158, audioUrl: sh(10), plays: 31_200_000, explicit: false, genre: 'Lo-Fi', releaseDate: '2024-03-01' },
  // Aanya Sharma — Raat Ke Sitare
  { id: 's11', title: 'Raat Ke Sitare', artistId: 'aanya-sharma', albumId: 'raat-ke-sitare', duration: 264, audioUrl: sh(11), plays: 96_400_000, explicit: false, genre: 'Bollywood Pop', releaseDate: '2025-01-24' },
  { id: 's12', title: 'Barish Ki Boondein', artistId: 'aanya-sharma', albumId: 'raat-ke-sitare', duration: 238, audioUrl: sh(12), plays: 71_800_000, explicit: false, genre: 'Bollywood Pop', releaseDate: '2025-03-07' },
  { id: 's13', title: 'Neon Bazaar', artistId: 'aanya-sharma', albumId: 'raat-ke-sitare', duration: 221, audioUrl: sh(13), plays: 59_300_000, explicit: true, genre: 'Dance Pop', releaseDate: '2025-05-16' },
  // Karthik Rao — Vaanam
  { id: 's14', title: 'Vaanam', artistId: 'karthik-rao', albumId: 'vaanam', duration: 402, audioUrl: sh(14), plays: 132_600_000, explicit: false, genre: 'Carnatic Melodic', releaseDate: '2024-06-21' },
  { id: 's15', title: 'Mazhai (Monsoon)', artistId: 'karthik-rao', albumId: 'vaanam', duration: 356, audioUrl: sh(15), plays: 104_900_000, explicit: false, genre: 'Carnatic Melodic', releaseDate: '2024-06-21' },
  // The Amber Collective — Golden Hour
  { id: 's16', title: 'Golden Hour', artistId: 'amber-collective', albumId: 'golden-hour', duration: 279, audioUrl: sh(16), plays: 27_600_000, explicit: false, genre: 'Acoustic Folk', releaseDate: '2022-11-11' },
]

export const PLAYLISTS: Playlist[] = [
  {
    id: 'daily-discovery',
    title: 'Daily Discovery',
    description: 'Fresh picks tuned to your taste — Karthik Rao, Nova Skye, Aanya Sharma and more. Updated every day.',
    curator: 'AUD',
    followers: 4_200_000,
    songIds: ['s14', 's11', 's1', 's8', 's7', 's16', 's3', 's12', 's15', 's9'],
    art: { type: 'gradient', from: 'from-fuchsia-600', to: 'to-purple-900', icon: 'Sparkles' },
  },
  {
    id: 'midnight-drive',
    title: 'Midnight Drive',
    description: 'Neon-soaked synths and open-highway anthems for the 1AM crowd.',
    curator: 'AUD Drive',
    followers: 2_850_000,
    songIds: ['s1', 's3', 's2', 's4', 's13', 's11'],
    art: { type: 'image', src: '/covers/midnight-neon.jpg' },
  },
  {
    id: 'lofi-study',
    title: 'Lo-Fi Study Session',
    description: 'Tape-saturated beats to focus, read and flow. Zero distractions.',
    curator: 'AUD Focus',
    followers: 5_100_000,
    songIds: ['s8', 's9', 's10', 's16', 's4'],
    art: { type: 'image', src: '/covers/rainy-window.jpg' },
  },
  {
    id: 'tamil-melody-core',
    title: 'Tamil Melody Core',
    description: 'Soulful Tamil melodies, from monsoon ragas to homecoming anthems.',
    curator: 'AUD Tamil',
    followers: 3_600_000,
    songIds: ['s14', 's15', 's11', 's12', 's16'],
    art: { type: 'image', src: '/covers/vaanam.jpg' },
  },
  {
    id: 'indie-roadtrip',
    title: 'Indie Roadtrip',
    description: 'Dusty guitars and wide-open choruses — kilometres of sing-along ahead.',
    curator: 'AUD Indie',
    followers: 1_400_000,
    songIds: ['s7', 's5', 's6', 's16', 's10'],
    art: { type: 'gradient', from: 'from-amber-500', to: 'to-orange-800', icon: 'Guitar' },
  },
  {
    id: 'dance-floor-heat',
    title: 'Dance Floor Heat',
    description: 'Maximum-trouble anthems to move to. Volume mandatory.',
    curator: 'AUD Dance',
    followers: 6_300_000,
    songIds: ['s13', 's11', 's12', 's2', 's4', 's1'],
    art: { type: 'gradient', from: 'from-orange-500', to: 'to-rose-700', icon: 'Flame' },
  },
  {
    id: 'monsoon-feelings',
    title: 'Monsoon Feelings',
    description: 'Rain on the windowpane, chai in hand — 20 songs for the first showers.',
    curator: 'AUD Moods',
    followers: 2_100_000,
    songIds: ['s15', 's14', 's12', 's8', 's16', 's9'],
    art: { type: 'gradient', from: 'from-sky-600', to: 'to-slate-800', icon: 'CloudRain' },
  },
  {
    id: 'top-hits-2026',
    title: 'Top Hits 2026',
    description: 'The biggest songs right now, all in one place. Updated weekly.',
    curator: 'AUD Charts',
    followers: 8_900_000,
    songIds: ['s14', 's11', 's7', 's12', 's15', 's5', 's13', 's8'],
    art: { type: 'gradient', from: 'from-emerald-500', to: 'to-teal-800', icon: 'TrendingUp' },
  },
]

export interface GenreTile {
  id: string
  title: string
  from: string
  to: string
  icon: string
}

export const GENRE_TILES: GenreTile[] = [
  { id: 'g-pop', title: 'Bollywood Pop', from: 'from-pink-600', to: 'to-rose-900', icon: 'Star' },
  { id: 'g-synth', title: 'Synthwave', from: 'from-violet-600', to: 'to-indigo-950', icon: 'Zap' },
  { id: 'g-lofi', title: 'Lo-Fi', from: 'from-teal-600', to: 'to-emerald-950', icon: 'Coffee' },
  { id: 'g-indie', title: 'Indie Rock', from: 'from-amber-500', to: 'to-stone-800', icon: 'Guitar' },
  { id: 'g-carnatic', title: 'Carnatic', from: 'from-yellow-600', to: 'to-amber-950', icon: 'Drum' },
  { id: 'g-dance', title: 'Dance Pop', from: 'from-orange-500', to: 'to-red-900', icon: 'Flame' },
  { id: 'g-folk', title: 'Acoustic Folk', from: 'from-lime-600', to: 'to-green-950', icon: 'Sun' },
  { id: 'g-monsoon', title: 'Monsoon', from: 'from-sky-600', to: 'to-slate-900', icon: 'CloudRain' },
  { id: 'g-focus', title: 'Focus', from: 'from-slate-600', to: 'to-zinc-900', icon: 'Headphones' },
  { id: 'g-drive', title: 'Night Drive', from: 'from-fuchsia-600', to: 'to-purple-950', icon: 'Car' },
  { id: 'g-electronic', title: 'Electronic', from: 'from-cyan-600', to: 'to-blue-950', icon: 'Radio' },
  { id: 'g-charts', title: 'Top Hits', from: 'from-emerald-500', to: 'to-green-950', icon: 'TrendingUp' },
]
