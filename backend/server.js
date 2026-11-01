require('dotenv').config();
const express = require('express');
const cors = require('cors');
const path = require('path');
const jwt = require('jsonwebtoken');
const mongoose = require('mongoose');

const app = express();
app.set('trust proxy', true);
const PORT = process.env.PORT || 8000;
const MONGODB_URL = process.env.MONGODB_URL;
const ADMIN_PASSWORD = process.env.ADMIN_PASSWORD || 'admin123';
const JWT_SECRET = process.env.JWT_SECRET || 'url_stream_super_jwt_secret_2026';

app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

// --- Normalize Mongo docs for the Android app (_id -> id, createdAt -> Long) ---
function normalizeForApp(value) {
  if (Array.isArray(value)) return value.map(normalizeForApp);
  if (value && typeof value === 'object') {
    const out = {};
    for (const [k, v] of Object.entries(value)) {
      if (k === '__v') continue;
      if (k === '_id') { if (value.id === undefined) out.id = String(v); continue; }
      out[k] = normalizeForApp(v);
    }
    if (typeof out.createdAt === 'string') {
      const t = Date.parse(out.createdAt);
      out.createdAt = isNaN(t) ? Date.now() : t;
    }
    return out;
  }
  return value;
}

app.use((req, res, next) => {
  const origJson = res.json.bind(res);
  res.json = (body) => origJson(normalizeForApp(JSON.parse(JSON.stringify(body === undefined ? null : body))));
  next();
});


// --- In-Memory Fallback Store (Used if MONGODB_URL is not provided) ---
let isMongoConnected = false;

let inMemoryMovies = [
  {
    id: "mov_tears_steel",
    title: "Tears of Steel",
    description: "In a dystopian futuristic Amsterdam, scientists and warriors attempt a crucial memory recreation.",
    poster: "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=800&q=80",
    banner: "https://images.unsplash.com/photo-1578328819058-b69f3a3b0f6b?w=1200&q=80",
    category: "Sci-Fi",
    year: 2024,
    rating: 8.7,
    duration: "1h 52m",
    streamUrl: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
    createdAt: Date.now()
  },
  {
    id: "mov_hls_stream",
    title: "Universal Stream (HLS m3u8)",
    description: "Ultra high-definition direct HLS live adaptive bitrate stream demonstration.",
    poster: "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=800&q=80",
    banner: "https://images.unsplash.com/photo-1478760329108-5c3ed9d495a0?w=1200&q=80",
    category: "Action",
    year: 2025,
    rating: 9.2,
    duration: "2h 10m",
    streamUrl: "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
    createdAt: Date.now()
  },
  {
    id: "mov_big_bunny",
    title: "Big Buck Bunny",
    description: "A large, lovable rabbit awakens to a sunny day in the forest.",
    poster: "https://images.unsplash.com/photo-1518791841217-8f162f1e1131?w=800&q=80",
    banner: "https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=1200&q=80",
    category: "Comedy",
    year: 2023,
    rating: 8.1,
    duration: "1h 35m",
    streamUrl: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
    qualities: [],
    createdAt: Date.now()
  }
];

let inMemorySeries = [
  {
    id: "ser_cyberpunk",
    title: "Cyberpunk: 2099",
    description: "In the heart of megacity New Kowloon, mercenary hackers fight quantum surveillance.",
    poster: "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=800&q=80",
    banner: "https://images.unsplash.com/photo-1542751371-adc38448a05e?w=1200&q=80",
    category: "Sci-Fi",
    year: 2025,
    rating: 9.4,
    createdAt: Date.now()
  }
];

let inMemoryEpisodes = [
  {
    id: "ep_cyber_1",
    seriesId: "ser_cyberpunk",
    season: 1,
    episodeNumber: 1,
    title: "S1:E1 - The Neon Awakening",
    thumbnail: "https://images.unsplash.com/photo-1542751371-adc38448a05e?w=800&q=80",
    streamUrl: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
    duration: "42m"
  },
  {
    id: "ep_cyber_2",
    seriesId: "ser_cyberpunk",
    season: 1,
    episodeNumber: 2,
    title: "S1:E2 - Glitch in the Matrix",
    thumbnail: "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800&q=80",
    streamUrl: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
    duration: "48m"
  }
];

// --- Mongoose Schemas (if MONGODB_URL is provided) ---
// Quality variants (480p / 720p / 1080p), each with its own direct link
const QualitySchema = new mongoose.Schema({
  label: { type: String, required: true },
  url: { type: String, required: true }
}, { _id: false });

const MovieSchema = new mongoose.Schema({
  title: { type: String, required: true },
  description: { type: String, default: "" },
  poster: { type: String, required: true },
  banner: { type: String, default: "" },
  category: { type: String, default: "Action" },
  year: { type: Number, default: 2025 },
  rating: { type: Number, default: 8.5 },
  duration: { type: String, default: "120m" },
  streamUrl: { type: String, default: "" },
  fileName: { type: String, default: "" },
  qualities: { type: [QualitySchema], default: [] },
  tags: { type: [String], default: [] },
  createdAt: { type: Date, default: Date.now }
});

const SeriesSchema = new mongoose.Schema({
  title: { type: String, required: true },
  description: { type: String, default: "" },
  poster: { type: String, required: true },
  banner: { type: String, default: "" },
  category: { type: String, default: "Sci-Fi" },
  year: { type: Number, default: 2025 },
  rating: { type: Number, default: 8.8 },
  type: { type: String, enum: ['series', 'anime'], default: 'series' },
  tags: { type: [String], default: [] },
  createdAt: { type: Date, default: Date.now }
});

// Notifications sent from the admin panel to the app
const NotificationSchema = new mongoose.Schema({
  title: { type: String, required: true },
  message: { type: String, required: true },
  imageUrl: { type: String, default: "" },
  link: { type: String, default: "" },
  createdAt: { type: Date, default: Date.now }
});

const AppUpdateSchema = new mongoose.Schema({
  versionCode: { type: Number, required: true },
  versionName: { type: String, default: "" },
  apkUrl: { type: String, required: true },
  changelog: { type: String, default: "" },
  force: { type: Boolean, default: false },
  createdAt: { type: Date, default: Date.now }
});

// Short links: /dl/<code> -> real video URL (optionally served under a custom file name)
const ShortLinkSchema = new mongoose.Schema({
  code: { type: String, required: true, unique: true },
  url: { type: String, required: true },
  name: { type: String, default: "" },
  createdAt: { type: Date, default: Date.now }
});

// App usage tracking: one doc per phone (anonymous id) + one doc per day
const DeviceSchema = new mongoose.Schema({
  deviceId: { type: String, required: true, unique: true },
  firstSeen: { type: Date, default: Date.now },
  lastSeen: { type: Date, default: Date.now },
  lastDay: { type: String, default: "" },
  openCount: { type: Number, default: 0 },
  appVersion: { type: String, default: "" }
});

const DailyStatSchema = new mongoose.Schema({
  day: { type: String, required: true, unique: true },
  opens: { type: Number, default: 0 },
  activeUsers: { type: Number, default: 0 },
  newUsers: { type: Number, default: 0 }
});

const EpisodeSchema = new mongoose.Schema({
  seriesId: { type: String, required: true },
  season: { type: Number, default: 1 },
  episodeNumber: { type: Number, default: 1 },
  title: { type: String, required: true },
  thumbnail: { type: String, default: "" },
  streamUrl: { type: String, default: "" },
  fileName: { type: String, default: "" },
  qualities: { type: [QualitySchema], default: [] },
  duration: { type: String, default: "45m" }
});


// --- Quality helpers ---
const QUALITY_ORDER = ['480p', '720p', '1080p'];

// Keep only valid {label,url} pairs, sorted 480p -> 1080p
function cleanQualities(list) {
  if (!Array.isArray(list)) return [];
  const seen = new Set();
  const out = [];
  for (const q of list) {
    const label = q && typeof q.label === 'string' ? q.label.trim() : '';
    const url = q && typeof q.url === 'string' ? q.url.trim() : '';
    if (!label || !url || seen.has(label)) continue;
    seen.add(label);
    out.push({ label, url });
  }
  const rank = (l) => { const i = QUALITY_ORDER.indexOf(l); return i === -1 ? 99 : i; };
  return out.sort((a, b) => rank(a.label) - rank(b.label));
}

// Default link = 720p, else 1080p, else the first one available
function defaultStreamUrl(qualities) {
  const by = (l) => (qualities.find(q => q.label === l) || {}).url;
  return by('720p') || by('1080p') || by('480p') || (qualities[0] && qualities[0].url) || '';
}

// Normalise a movie/episode body. Returns an error string if no link at all.
// Home-screen tags the admin can tick on a movie / series / anime
const ALLOWED_TAGS = ['Trending', 'Hollywood', 'Bollywood', 'KDrama', 'Chinese', 'South Indian', '18+'];
function cleanTags(tags) {
  if (!Array.isArray(tags)) return [];
  const out = [];
  for (const t of tags) {
    const hit = ALLOWED_TAGS.find(a => a.toLowerCase() === String(t).trim().toLowerCase());
    if (hit && !out.includes(hit)) out.push(hit);
  }
  return out;
}

function prepareVideoBody(data, partial) {
  if (data.tags !== undefined || !partial) data.tags = cleanTags(data.tags);
  if (data.qualities !== undefined || !partial) {
    data.qualities = cleanQualities(data.qualities);
  }
  if (typeof data.streamUrl === 'string') data.streamUrl = data.streamUrl.trim();
  if (data.qualities && data.qualities.length) {
    // When qualities are provided the default link always follows them
    data.streamUrl = defaultStreamUrl(data.qualities);
  }
  if (!partial && !data.streamUrl) {
    return 'Add at least one video link (480p / 720p / 1080p)';
  }
  return null;
}

let MovieModel, SeriesModel, EpisodeModel, NotificationModel, AppUpdateModel, ShortLinkModel, DeviceModel, DailyStatModel;
let inMemoryDevices = new Map();
let inMemoryDaily = new Map();
let inMemoryShortLinks = [];
let inMemoryNotifications = [];
let inMemoryUpdate = null;
let mongoError = null;

// Models are registered up-front so they exist as soon as the connection is ready
MovieModel = mongoose.model('Movie', MovieSchema);
SeriesModel = mongoose.model('Series', SeriesSchema);
EpisodeModel = mongoose.model('Episode', EpisodeSchema);
NotificationModel = mongoose.model('Notification', NotificationSchema);
AppUpdateModel = mongoose.model('AppUpdate', AppUpdateSchema);
ShortLinkModel = mongoose.model('ShortLink', ShortLinkSchema);
DeviceModel = mongoose.model('Device', DeviceSchema);
DailyStatModel = mongoose.model('DailyStat', DailyStatSchema);

function connectMongo() {
  mongoose.connect(MONGODB_URL, { serverSelectionTimeoutMS: 10000 })
    .then(async () => {
      console.log('Connected to MongoDB successfully');
      isMongoConnected = true;
      mongoError = null;

      // Seed if empty
      const count = await MovieModel.countDocuments();
      if (count === 0) {
        console.log('Seeding initial MongoDB sample catalogue...');
        const strip = ({ id, ...rest }) => rest;
        await MovieModel.insertMany(inMemoryMovies.map(strip));
        const seededSeries = await SeriesModel.insertMany(inMemorySeries.map(strip));
        const idMap = {};
        inMemorySeries.forEach((s, i) => { idMap[s.id] = String(seededSeries[i]._id); });
        await EpisodeModel.insertMany(
          inMemoryEpisodes.map(e => ({ ...strip(e), seriesId: idMap[e.seriesId] || e.seriesId }))
        );
      }
    })
    .catch(err => {
      mongoError = err.message;
      console.error('MongoDB connection FAILED:', err.message);
      console.error('Check MONGODB_URL and that Atlas Network Access allows 0.0.0.0/0. Retrying in 15s...');
      setTimeout(connectMongo, 15000);
    });
}

if (MONGODB_URL) {
  connectMongo();
} else {
  console.log('No MONGODB_URL specified. Running with in-memory persistence.');
}

// Marks a series document for the app: always isSeries, isAnime when type is 'anime'
function decorateSeries(ser) {
  ser.isSeries = true;
  ser.isAnime = ser.type === 'anime';
  return ser;
}

// --- JWT Auth Middleware for Admin ---
function authMiddleware(req, res, next) {
  const authHeader = req.headers.authorization;
  if (!authHeader) {
    return res.status(401).json({ error: "Missing authorization token" });
  }
  const token = authHeader.split(" ")[1];
  try {
    const decoded = jwt.verify(token, JWT_SECRET);
    req.user = decoded;
    next();
  } catch (err) {
    return res.status(403).json({ error: "Invalid or expired token" });
  }
}

// If MONGODB_URL is set but the DB is down, refuse writes instead of silently saving
// to memory (memory data is lost on every restart / redeploy on Koyeb).
function requireDb(req, res, next) {
  if (MONGODB_URL && !isMongoConnected) {
    return res.status(503).json({
      error: "Database not connected, so nothing can be saved. Check MONGODB_URL and Atlas Network Access (allow 0.0.0.0/0). " + (mongoError || "")
    });
  }
  next();
}

// --- Admin Authentication ---
app.post('/api/auth/login', (req, res) => {
  const { password } = req.body;
  if (!password || password !== ADMIN_PASSWORD) {
    return res.status(401).json({ error: "Incorrect admin password" });
  }
  const token = jwt.sign({ role: 'admin' }, JWT_SECRET, { expiresIn: '7d' });
  res.json({ token, message: "Authentication successful" });
});

// --- Public REST APIs ---

// 1. GET /api/home
app.get('/api/home', async (req, res) => {
  try {
    let movies = isMongoConnected ? await MovieModel.find().lean() : inMemoryMovies;
    let series = isMongoConnected ? await SeriesModel.find().lean() : inMemorySeries;

    // Attach episodes to series
    for (const ser of series) {
      decorateSeries(ser);
      const sId = ser._id ? ser._id.toString() : ser.id;
      if (isMongoConnected) {
        ser.episodes = await EpisodeModel.find({ seriesId: sId }).lean();
      } else {
        ser.episodes = inMemoryEpisodes.filter(e => e.seriesId === sId);
      }
    }

    const anime = series.filter(s => s.isAnime);
    const webSeries = series.filter(s => !s.isAnime);
    const featured = [...series, ...movies].slice(0, 4);
    const ts = (x) => {
      const t = x && x.createdAt ? new Date(x.createdAt).getTime() : 0;
      return isNaN(t) ? 0 : t;
    };
    // Newest first across movies, web series and anime
    const recentlyAdded = [...movies, ...series]
      .sort((a, b) => ts(b) - ts(a))
      .slice(0, 12);

    res.json({
      featured,
      recentlyAdded,
      movies,
      series: webSeries,
      anime,
      categories: ["All", "Action", "Sci-Fi", "Comedy", "Drama", "Thriller", "Romance"]
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 2. GET /api/movies
app.get('/api/movies', async (req, res) => {
  try {
    const movies = isMongoConnected ? await MovieModel.find().lean() : inMemoryMovies;
    res.json(movies);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 3. GET /api/series
app.get('/api/series', async (req, res) => {
  try {
    const series = isMongoConnected ? await SeriesModel.find().lean() : inMemorySeries;
    for (const ser of series) {
      decorateSeries(ser);
      const sId = ser._id ? ser._id.toString() : ser.id;
      if (isMongoConnected) {
        ser.episodes = await EpisodeModel.find({ seriesId: sId }).lean();
      } else {
        ser.episodes = inMemoryEpisodes.filter(e => e.seriesId === sId);
      }
    }
    res.json(series);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 4. GET /api/series/:id/episodes
app.get('/api/series/:id/episodes', async (req, res) => {
  try {
    const seriesId = req.params.id;
    const episodes = isMongoConnected
      ? await EpisodeModel.find({ seriesId }).lean()
      : inMemoryEpisodes.filter(e => e.seriesId === seriesId);
    res.json(episodes);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 5. GET /api/search?q=&filter=
app.get('/api/search', async (req, res) => {
  try {
    const query = (req.query.q || '').toLowerCase();
    const filter = (req.query.filter || 'All').toLowerCase();

    let movies = isMongoConnected ? await MovieModel.find().lean() : inMemoryMovies;
    let series = isMongoConnected ? await SeriesModel.find().lean() : inMemorySeries;
    series.forEach(decorateSeries);

    let all = [];
    if (filter === 'movies') all = movies;
    else if (filter === 'anime') all = series.filter(s => s.isAnime);
    else if (filter === 'web series' || filter === 'series') all = series.filter(s => !s.isAnime);
    else {
      all = [...movies, ...series];
      const tag = ALLOWED_TAGS.find(a => a.toLowerCase() === filter);
      if (tag) all = all.filter(i => (i.tags || []).includes(tag));
    }

    if (!query) {
      return res.json(all.slice(0, 10));
    }

    const filtered = all.filter(item =>
      item.title.toLowerCase().includes(query) ||
      (item.category && item.category.toLowerCase().includes(query)) ||
      (item.description && item.description.toLowerCase().includes(query))
    );

    res.json(filtered);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 6. GET /api/random
app.get('/api/random', async (req, res) => {
  try {
    let movies = isMongoConnected ? await MovieModel.find().lean() : inMemoryMovies;
    let series = isMongoConnected ? await SeriesModel.find().lean() : inMemorySeries;
    series.forEach(decorateSeries);
    const combined = [...movies, ...series].sort(() => 0.5 - Math.random());
    res.json(combined.slice(0, 6));
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// --- Admin Management APIs (Protected with authMiddleware) ---

// Create Movie
app.post('/api/movies', authMiddleware, requireDb, async (req, res) => {
  try {
    const data = req.body;
    const bad = prepareVideoBody(data, false);
    if (bad) return res.status(400).json({ error: bad });
    data.id = "mov_" + Date.now();
    if (isMongoConnected) {
      const created = await MovieModel.create(data);
      return res.status(201).json(created);
    }
    inMemoryMovies.unshift(data);
    res.status(201).json(data);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Update Movie
app.put('/api/movies/:id', authMiddleware, requireDb, async (req, res) => {
  try {
    const id = req.params.id;
    const bad = prepareVideoBody(req.body, true);
    if (bad) return res.status(400).json({ error: bad });
    if (isMongoConnected) {
      const updated = await MovieModel.findByIdAndUpdate(id, req.body, { new: true });
      return res.json(updated);
    }
    const idx = inMemoryMovies.findIndex(m => m.id === id);
    if (idx !== -1) {
      inMemoryMovies[idx] = { ...inMemoryMovies[idx], ...req.body };
      return res.json(inMemoryMovies[idx]);
    }
    res.status(404).json({ error: "Movie not found" });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Delete Movie
app.delete('/api/movies/:id', authMiddleware, requireDb, async (req, res) => {
  try {
    const id = req.params.id;
    if (isMongoConnected) {
      await MovieModel.findByIdAndDelete(id);
      return res.json({ success: true });
    }
    inMemoryMovies = inMemoryMovies.filter(m => m.id !== id);
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Create Series
app.post('/api/series', authMiddleware, requireDb, async (req, res) => {
  try {
    const data = req.body;
    data.id = "ser_" + Date.now();
    data.isSeries = true;
    data.type = data.type === 'anime' ? 'anime' : 'series';
    data.tags = cleanTags(data.tags);
    if (isMongoConnected) {
      const created = await SeriesModel.create(data);
      return res.status(201).json(created);
    }
    inMemorySeries.unshift(data);
    res.status(201).json(data);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Update Series / Anime
app.put('/api/series/:id', authMiddleware, requireDb, async (req, res) => {
  try {
    const id = req.params.id;
    const data = { ...req.body };
    delete data.id; delete data._id; delete data.episodes;
    if (data.type !== undefined) data.type = data.type === 'anime' ? 'anime' : 'series';
    if (data.tags !== undefined) data.tags = cleanTags(data.tags);
    if (isMongoConnected) {
      const updated = await SeriesModel.findByIdAndUpdate(id, data, { new: true });
      return updated ? res.json(updated) : res.status(404).json({ error: "Series not found" });
    }
    const idx = inMemorySeries.findIndex(s => s.id === id);
    if (idx === -1) return res.status(404).json({ error: "Series not found" });
    inMemorySeries[idx] = { ...inMemorySeries[idx], ...data };
    res.json(inMemorySeries[idx]);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Add Episode
app.post('/api/episodes', authMiddleware, requireDb, async (req, res) => {
  try {
    const data = req.body;
    const bad = prepareVideoBody(data, false);
    if (bad) return res.status(400).json({ error: bad });
    data.id = "ep_" + Date.now();
    if (isMongoConnected) {
      const created = await EpisodeModel.create(data);
      return res.status(201).json(created);
    }
    inMemoryEpisodes.push(data);
    res.status(201).json(data);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Update Episode (e.g. add / change quality links)
app.put('/api/episodes/:id', authMiddleware, requireDb, async (req, res) => {
  try {
    const id = req.params.id;
    const bad = prepareVideoBody(req.body, true);
    if (bad) return res.status(400).json({ error: bad });
    if (isMongoConnected) {
      const updated = await EpisodeModel.findByIdAndUpdate(id, req.body, { new: true });
      return updated ? res.json(updated) : res.status(404).json({ error: "Episode not found" });
    }
    const idx = inMemoryEpisodes.findIndex(e => e.id === id);
    if (idx === -1) return res.status(404).json({ error: "Episode not found" });
    inMemoryEpisodes[idx] = { ...inMemoryEpisodes[idx], ...req.body };
    res.json(inMemoryEpisodes[idx]);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Delete Episode
app.delete('/api/episodes/:id', authMiddleware, requireDb, async (req, res) => {
  try {
    const id = req.params.id;
    if (isMongoConnected) {
      await EpisodeModel.findByIdAndDelete(id);
      return res.json({ success: true });
    }
    inMemoryEpisodes = inMemoryEpisodes.filter(e => e.id !== id);
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Delete Series
app.delete('/api/series/:id', authMiddleware, requireDb, async (req, res) => {
  try {
    const id = req.params.id;
    if (isMongoConnected) {
      await SeriesModel.findByIdAndDelete(id);
      await EpisodeModel.deleteMany({ seriesId: id });
      return res.json({ success: true });
    }
    inMemorySeries = inMemorySeries.filter(s => s.id !== id);
    inMemoryEpisodes = inMemoryEpisodes.filter(e => e.seriesId !== id);
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// --- Notifications ---
// Public: the app reads the latest notifications (newest first)
app.get('/api/notifications', async (req, res) => {
  try {
    const limit = Math.min(parseInt(req.query.limit) || 30, 100);
    const list = isMongoConnected
      ? await NotificationModel.find().sort({ createdAt: -1 }).limit(limit).lean()
      : inMemoryNotifications.slice(0, limit);
    res.json(list);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Admin: send a notification to every app user
app.post('/api/notifications', authMiddleware, requireDb, async (req, res) => {
  try {
    const title = String(req.body.title || '').trim();
    const message = String(req.body.message || '').trim();
    if (!title || !message) {
      return res.status(400).json({ error: "Title and message are required" });
    }
    const data = {
      title: title.slice(0, 100),
      message: message.slice(0, 500),
      imageUrl: String(req.body.imageUrl || '').trim(),
      link: String(req.body.link || '').trim()
    };
    if (isMongoConnected) {
      const created = await NotificationModel.create(data);
      return res.status(201).json(created);
    }
    const item = { ...data, id: "ntf_" + Date.now(), createdAt: Date.now() };
    inMemoryNotifications.unshift(item);
    res.status(201).json(item);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Admin: remove a notification
app.delete('/api/notifications/:id', authMiddleware, requireDb, async (req, res) => {
  try {
    const id = req.params.id;
    if (isMongoConnected) {
      await NotificationModel.findByIdAndDelete(id);
      return res.json({ success: true });
    }
    inMemoryNotifications = inMemoryNotifications.filter(n => n.id !== id);
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// --- App updates ---
// Public: the app asks for the newest published version (null body fields when none)
app.get('/api/update', async (req, res) => {
  try {
    const latest = isMongoConnected
      ? await AppUpdateModel.findOne().sort({ createdAt: -1 }).lean()
      : inMemoryUpdate;
    res.json(latest || { versionCode: 0, versionName: '', apkUrl: '', changelog: '', force: false });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Admin: publish a new version. Every app with a lower versionCode shows an update prompt.
app.post('/api/update', authMiddleware, requireDb, async (req, res) => {
  try {
    const versionCode = parseInt(req.body.versionCode);
    const apkUrl = String(req.body.apkUrl || '').trim();
    if (!versionCode || versionCode < 1) {
      return res.status(400).json({ error: "Version code must be a number greater than 0" });
    }
    if (!/^https?:\/\//i.test(apkUrl)) {
      return res.status(400).json({ error: "APK link must start with http:// or https://" });
    }
    const data = {
      versionCode,
      versionName: String(req.body.versionName || '').trim().slice(0, 30),
      apkUrl,
      changelog: String(req.body.changelog || '').trim().slice(0, 1000),
      force: req.body.force === true || req.body.force === 'true'
    };
    if (isMongoConnected) {
      const created = await AppUpdateModel.create(data);
      return res.status(201).json(created);
    }
    inMemoryUpdate = { ...data, id: "upd_" + Date.now(), createdAt: Date.now() };
    res.status(201).json(inMemoryUpdate);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Admin: stop offering the update
app.delete('/api/update', authMiddleware, requireDb, async (req, res) => {
  try {
    if (isMongoConnected) await AppUpdateModel.deleteMany({});
    else inMemoryUpdate = null;
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Share link: opens the newest APK (used by the app's Share button)
app.get('/download', async (req, res) => {
  try {
    const latest = isMongoConnected
      ? await AppUpdateModel.findOne().sort({ createdAt: -1 }).lean()
      : inMemoryUpdate;
    if (latest && latest.apkUrl) return res.redirect(latest.apkUrl);
    res.status(404).send('No download is published yet.');
  } catch (err) {
    res.status(500).send(err.message);
  }
});

// --- App usage stats ---
// Day key in Indian time so "today" matches the admin's day
function dayKey(ts) {
  return new Date((ts === undefined ? Date.now() : ts) + 5.5 * 3600 * 1000).toISOString().slice(0, 10);
}

// Public: the app calls this once every time it is opened
app.post('/api/ping', async (req, res) => {
  try {
    const deviceId = String(req.body.deviceId || '').trim();
    if (!/^[A-Za-z0-9_-]{8,64}$/.test(deviceId)) {
      return res.status(400).json({ error: "Invalid deviceId" });
    }
    if (MONGODB_URL && !isMongoConnected) return res.json({ ok: false });
    const appVersion = String(req.body.appVersion || '').slice(0, 20);
    const now = Date.now();
    const today = dayKey(now);

    let prev;
    if (isMongoConnected) {
      prev = await DeviceModel.findOneAndUpdate(
        { deviceId },
        {
          $setOnInsert: { firstSeen: new Date(now) },
          $set: { lastSeen: new Date(now), lastDay: today, appVersion },
          $inc: { openCount: 1 }
        },
        { upsert: true, new: false }
      ).lean();
    } else {
      prev = inMemoryDevices.get(deviceId);
      inMemoryDevices.set(deviceId, {
        deviceId,
        firstSeen: prev ? prev.firstSeen : now,
        lastSeen: now,
        lastDay: today,
        openCount: (prev ? prev.openCount : 0) + 1,
        appVersion
      });
    }

    const inc = { opens: 1, activeUsers: (!prev || prev.lastDay !== today) ? 1 : 0, newUsers: prev ? 0 : 1 };
    if (isMongoConnected) {
      await DailyStatModel.updateOne({ day: today }, { $inc: inc }, { upsert: true });
    } else {
      const d = inMemoryDaily.get(today) || { day: today, opens: 0, activeUsers: 0, newUsers: 0 };
      d.opens += inc.opens; d.activeUsers += inc.activeUsers; d.newUsers += inc.newUsers;
      inMemoryDaily.set(today, d);
    }
    res.json({ ok: true });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Admin: usage numbers for the dashboard
app.get('/api/stats', authMiddleware, async (req, res) => {
  try {
    const now = Date.now();
    const today = dayKey(now);
    const days = [];
    for (let i = 6; i >= 0; i--) days.push(dayKey(now - i * 86400000));

    let totalUsers, active7d, active30d, totalOpens, dailyRows;
    if (isMongoConnected) {
      totalUsers = await DeviceModel.countDocuments();
      active7d = await DeviceModel.countDocuments({ lastSeen: { $gte: new Date(now - 7 * 86400000) } });
      active30d = await DeviceModel.countDocuments({ lastSeen: { $gte: new Date(now - 30 * 86400000) } });
      const agg = await DeviceModel.aggregate([{ $group: { _id: null, opens: { $sum: '$openCount' } } }]);
      totalOpens = agg.length ? agg[0].opens : 0;
      dailyRows = await DailyStatModel.find({ day: { $in: days } }).lean();
    } else {
      const all = [...inMemoryDevices.values()];
      totalUsers = all.length;
      active7d = all.filter(d => d.lastSeen >= now - 7 * 86400000).length;
      active30d = all.filter(d => d.lastSeen >= now - 30 * 86400000).length;
      totalOpens = all.reduce((a, d) => a + d.openCount, 0);
      dailyRows = days.map(d => inMemoryDaily.get(d)).filter(Boolean);
    }
    const byDay = Object.fromEntries(dailyRows.map(r => [r.day, r]));
    const last7 = days.map(d => ({
      day: d,
      opens: (byDay[d] || {}).opens || 0,
      activeUsers: (byDay[d] || {}).activeUsers || 0,
      newUsers: (byDay[d] || {}).newUsers || 0
    }));
    const t = last7[last7.length - 1];
    res.json({
      totalUsers,
      activeToday: t.activeUsers,
      newToday: t.newUsers,
      opensToday: t.opens,
      active7d,
      active30d,
      totalOpens,
      last7
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// --- Short links (/dl/<code>) ---
function safeFileName(name) {
  return String(name || '').replace(/[\\/:*?"<>|\r\n]+/g, ' ').trim().slice(0, 120);
}

async function findShortLink(code) {
  return isMongoConnected
    ? await ShortLinkModel.findOne({ code }).lean()
    : inMemoryShortLinks.find(l => l.code === code);
}

// Admin: create a short link for a real URL, with an optional display name
app.post('/api/links', authMiddleware, requireDb, async (req, res) => {
  try {
    const url = String(req.body.url || '').trim();
    const name = safeFileName(req.body.name);
    if (!/^https?:\/\//i.test(url)) {
      return res.status(400).json({ error: "URL must start with http:// or https://" });
    }
    // Code comes from the name (tears-of-steel-720p); falls back to a 4-digit number
    const base = name.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-+|-+$/g, '').slice(0, 60);
    let code = null;
    for (let i = 0; i < 30 && !code; i++) {
      const cand = base
        ? (i === 0 ? base : `${base}-${i + 1}`)
        : String(Math.floor(1000 + Math.random() * 9000));
      if (!(await findShortLink(cand))) code = cand;
    }
    if (!code) return res.status(500).json({ error: "Could not generate a code, try again" });
    if (isMongoConnected) await ShortLinkModel.create({ code, url, name });
    else inMemoryShortLinks.unshift({ code, url, name, createdAt: Date.now() });
    const shortUrl = `${req.protocol}://${req.get('host')}/dl/${code}`;
    res.status(201).json({ code, name, url, shortUrl });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Admin: list short links
app.get('/api/links', authMiddleware, async (req, res) => {
  try {
    const list = isMongoConnected
      ? await ShortLinkModel.find().sort({ createdAt: -1 }).limit(200).lean()
      : inMemoryShortLinks.slice(0, 200);
    res.json(list);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Admin: delete a short link
app.delete('/api/links/:code', authMiddleware, requireDb, async (req, res) => {
  try {
    if (isMongoConnected) await ShortLinkModel.deleteOne({ code: req.params.code });
    else inMemoryShortLinks = inMemoryShortLinks.filter(l => l.code !== req.params.code);
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Public: /dl/tears-of-steel (optionally /dl/5184/anything.mp4 so the URL ends with a file name)
// - no name set  -> plain redirect (no bandwidth used on this server)
// - name set     -> streamed through this server with that file name (Range supported)
app.get(['/dl/:code', '/dl/:code/:filename'], async (req, res) => {
  try {
    const link = await findShortLink(req.params.code);
    if (!link) return res.status(404).send('Link not found');
    if (!link.name) return res.redirect(link.url);

    const headers = {};
    if (req.headers.range) headers.Range = req.headers.range;
    const upstream = await fetch(link.url, { headers, redirect: 'follow' });
    res.status(upstream.status);
    for (const h of ['content-type', 'content-length', 'content-range', 'accept-ranges']) {
      const v = upstream.headers.get(h);
      if (v) res.setHeader(h, v);
    }
    const ext = (link.url.split('?')[0].match(/\.(mp4|mkv|m3u8|webm|avi|mov)$/i) || [''])[0];
    const fname = /\.\w{2,4}$/.test(link.name) ? link.name : link.name + ext;
    res.setHeader('Content-Disposition', `inline; filename*=UTF-8''${encodeURIComponent(fname)}`);
    if (!upstream.body) return res.end();
    const { Readable } = require('stream');
    const stream = Readable.fromWeb(upstream.body);
    req.on('close', () => stream.destroy());
    stream.on('error', () => res.end());
    stream.pipe(res);
  } catch (err) {
    if (!res.headersSent) res.status(502).send('Could not reach the source: ' + err.message);
  }
});

// Health check (Koyeb)
app.get('/health', (req, res) => {
  res.json({
    status: 'ok',
    db: isMongoConnected ? 'mongodb' : 'memory',
    persistent: isMongoConnected,
    mongoConfigured: !!MONGODB_URL,
    mongoError
  });
});

app.get('/', (req, res) => {
  res.redirect('/admin');
});

// Serve Admin Panel at /admin
app.get('/admin', (req, res) => {
  res.sendFile(path.join(__dirname, 'public', 'admin.html'));
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`URL Stream Server running on port ${PORT}`);
  console.log(`Admin Panel available at http://localhost:${PORT}/admin`);
});
