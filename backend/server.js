require('dotenv').config();
const express = require('express');
const cors = require('cors');
const path = require('path');
const jwt = require('jsonwebtoken');
const mongoose = require('mongoose');

const app = express();
const PORT = process.env.PORT || 8000;
const MONGODB_URL = process.env.MONGODB_URL;
const ADMIN_PASSWORD = process.env.ADMIN_PASSWORD || 'admin123';
const JWT_SECRET = process.env.JWT_SECRET || 'url_stream_super_jwt_secret_2026';

app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

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
const MovieSchema = new mongoose.Schema({
  title: { type: String, required: true },
  description: { type: String, default: "" },
  poster: { type: String, required: true },
  banner: { type: String, default: "" },
  category: { type: String, default: "Action" },
  year: { type: Number, default: 2025 },
  rating: { type: Number, default: 8.5 },
  duration: { type: String, default: "120m" },
  streamUrl: { type: String, required: true },
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
  createdAt: { type: Date, default: Date.now }
});

const EpisodeSchema = new mongoose.Schema({
  seriesId: { type: String, required: true },
  season: { type: Number, default: 1 },
  episodeNumber: { type: Number, default: 1 },
  title: { type: String, required: true },
  thumbnail: { type: String, default: "" },
  streamUrl: { type: String, required: true },
  duration: { type: String, default: "45m" }
});

let MovieModel, SeriesModel, EpisodeModel;

if (MONGODB_URL) {
  mongoose.connect(MONGODB_URL)
    .then(async () => {
      console.log('Connected to MongoDB successfully');
      isMongoConnected = true;
      MovieModel = mongoose.model('Movie', MovieSchema);
      SeriesModel = mongoose.model('Series', SeriesSchema);
      EpisodeModel = mongoose.model('Episode', EpisodeSchema);

      // Seed if empty
      const count = await MovieModel.countDocuments();
      if (count === 0) {
        console.log('Seeding initial MongoDB sample catalogue...');
        await MovieModel.insertMany(inMemoryMovies);
        await SeriesModel.insertMany(inMemorySeries);
        await EpisodeModel.insertMany(inMemoryEpisodes);
      }
    })
    .catch(err => {
      console.warn('MongoDB connection failed. Using in-memory store:', err.message);
    });
} else {
  console.log('No MONGODB_URL specified. Running with in-memory persistence.');
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
      ser.isSeries = true;
      const sId = ser._id ? ser._id.toString() : ser.id;
      if (isMongoConnected) {
        ser.episodes = await EpisodeModel.find({ seriesId: sId }).lean();
      } else {
        ser.episodes = inMemoryEpisodes.filter(e => e.seriesId === sId);
      }
    }

    const featured = [...series, ...movies].slice(0, 4);
    const recentlyAdded = [...movies, ...series].slice(0, 6);

    res.json({
      featured,
      recentlyAdded,
      movies,
      series,
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
      ser.isSeries = true;
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
    series.forEach(s => s.isSeries = true);

    let all = [];
    if (filter === 'movies') all = movies;
    else if (filter === 'web series' || filter === 'series') all = series;
    else all = [...movies, ...series];

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
    series.forEach(s => s.isSeries = true);
    const combined = [...movies, ...series].sort(() => 0.5 - Math.random());
    res.json(combined.slice(0, 6));
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// --- Admin Management APIs (Protected with authMiddleware) ---

// Create Movie
app.post('/api/movies', authMiddleware, async (req, res) => {
  try {
    const data = req.body;
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
app.put('/api/movies/:id', authMiddleware, async (req, res) => {
  try {
    const id = req.params.id;
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
app.delete('/api/movies/:id', authMiddleware, async (req, res) => {
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
app.post('/api/series', authMiddleware, async (req, res) => {
  try {
    const data = req.body;
    data.id = "ser_" + Date.now();
    data.isSeries = true;
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

// Add Episode
app.post('/api/episodes', authMiddleware, async (req, res) => {
  try {
    const data = req.body;
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

// Delete Series
app.delete('/api/series/:id', authMiddleware, async (req, res) => {
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

// Serve Admin Panel at /admin
app.get('/admin', (req, res) => {
  res.sendFile(path.join(__dirname, 'public', 'admin.html'));
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`URL Stream Server running on port ${PORT}`);
  console.log(`Admin Panel available at http://localhost:${PORT}/admin`);
});
