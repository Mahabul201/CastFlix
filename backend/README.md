# URL Stream - Backend & Admin Panel

This is the official Node.js + Express + MongoDB backend and CMS Admin Panel for the **URL Stream** Android application.

## Key Features
- **Public REST APIs**:
  - `GET /api/home` - Returns featured carousel, recently added, movies, web series, and categories.
  - `GET /api/movies` - Lists all movies with metadata and direct stream URLs.
  - `GET /api/series` - Lists all web series.
  - `GET /api/series/:id/episodes` - Fetches episode lists for a series.
  - `GET /api/search?q=&filter=` - Live search with filters for Movies and Web Series.
  - `GET /api/random` - Random media mix.
- **Admin Dashboard (`/admin`)**:
  - Password protected with JWT sessions (`ADMIN_PASSWORD`).
  - Add, Edit, Delete Movies (Title, Synopsis, Poster, Banner, Category, Year, Rating, Duration, Stream Link).
  - Add Web Series and Episodes (Season, Episode #, Title, Thumbnail, Stream Link).
  - Live preview test of video streams.
- **Zero Video Hosting**:
  - Only direct media links (`.m3u8` HLS, `.mp4`, `.mkv`) are stored in the database.
- **Resilient Storage**:
  - Automatically seeds default demo catalogue.
  - Connects to MongoDB when `MONGODB_URL` is configured, with an automatic in-memory fallback for instant zero-config testing.

---

## Deploying to Koyeb

1. **Push your code to GitHub** or upload the `backend/` folder to a repository.
2. Log into [Koyeb](https://www.koyeb.com/).
3. Click **Create Service** and select **GitHub**.
4. Choose the repository and set the **Work Directory** to `backend` (or deploy from root if in a standalone repo).
5. Koyeb will automatically detect the `Dockerfile`.
6. Add the following **Environment Variables**:
   - `PORT`: `8000`
   - `MONGODB_URL`: Your MongoDB Atlas connection URI (`mongodb+srv://...`)
   - `ADMIN_PASSWORD`: Your secret admin password
   - `JWT_SECRET`: A secure random secret string
7. Deploy! Koyeb provides you with an FQDN like `https://your-service-name.koyeb.app`.
8. In the Android App: Go to **Settings -> Backend Server** and paste your Koyeb FQDN.

---

## Local Development

```bash
cd backend
npm install
npm start
```
Visit `http://localhost:8000/admin` to open the Admin Panel.
Default Admin Password: `admin123`
