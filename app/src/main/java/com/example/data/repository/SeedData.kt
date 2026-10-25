package com.example.data.repository

import com.example.data.model.Episode
import com.example.data.model.MediaItem

object SeedData {
  val sampleEpisodesSeries1 = listOf(
    Episode(
      id = "ep_cyber_1",
      seriesId = "ser_cyberpunk",
      season = 1,
      episodeNumber = 1,
      title = "S1:E1 - The Neon Awakening",
      thumbnail = "https://images.unsplash.com/photo-1542751371-adc38448a05e?w=800&q=80",
      streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
      duration = "42m"
    ),
    Episode(
      id = "ep_cyber_2",
      seriesId = "ser_cyberpunk",
      season = 1,
      episodeNumber = 2,
      title = "S1:E2 - Glitch in the Matrix",
      thumbnail = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800&q=80",
      streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
      duration = "48m"
    ),
    Episode(
      id = "ep_cyber_3",
      seriesId = "ser_cyberpunk",
      season = 1,
      episodeNumber = 3,
      title = "S1:E3 - Synthetic Soul",
      thumbnail = "https://images.unsplash.com/photo-1508739773434-c26b3d09e071?w=800&q=80",
      streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
      duration = "50m"
    ),
    Episode(
      id = "ep_cyber_4",
      seriesId = "ser_cyberpunk",
      season = 1,
      episodeNumber = 4,
      title = "S1:E4 - The Final Firewall",
      thumbnail = "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=800&q=80",
      streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyBlazes.mp4",
      duration = "55m"
    )
  )

  val sampleEpisodesSeries2 = listOf(
    Episode(
      id = "ep_deep_1",
      seriesId = "ser_deepspace",
      season = 1,
      episodeNumber = 1,
      title = "S1:E1 - Beyond the Nebula",
      thumbnail = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=800&q=80",
      streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackSeeTheWorld.mp4",
      duration = "46m"
    ),
    Episode(
      id = "ep_deep_2",
      seriesId = "ser_deepspace",
      season = 1,
      episodeNumber = 2,
      title = "S1:E2 - Signal in the Void",
      thumbnail = "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?w=800&q=80",
      streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
      duration = "52m"
    )
  )

  val sampleMovies = listOf(
    MediaItem(
      id = "mov_tears_steel",
      title = "Tears of Steel",
      description = "In a dystopian futuristic Amsterdam, a group of scientists and warriors desperately attempt to stage a crucial memory recreation to save mankind from destructive robotic overlords.",
      poster = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=800&q=80",
      banner = "https://images.unsplash.com/photo-1578328819058-b69f3a3b0f6b?w=1200&q=80",
      category = "Sci-Fi",
      year = 2024,
      rating = 8.7,
      duration = "1h 52m",
      streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
      isSeries = false
    ),
    MediaItem(
      id = "mov_hls_stream",
      title = "Universal Stream (HLS m3u8)",
      description = "Ultra high-definition direct HLS live adaptive bitrate stream demonstration featuring dynamic resolution scaling and multi-track audio switching.",
      poster = "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=800&q=80",
      banner = "https://images.unsplash.com/photo-1478760329108-5c3ed9d495a0?w=1200&q=80",
      category = "Action",
      year = 2025,
      rating = 9.2,
      duration = "2h 10m",
      streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
      isSeries = false
    ),
    MediaItem(
      id = "mov_big_bunny",
      title = "Big Buck Bunny",
      description = "A large, lovable rabbit awakens to a beautiful sunny day in the forest, only to find mischievous rodents threatening the tranquility of the woodland creatures.",
      poster = "https://images.unsplash.com/photo-1518791841217-8f162f1e1131?w=800&q=80",
      banner = "https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=1200&q=80",
      category = "Comedy",
      year = 2023,
      rating = 8.1,
      duration = "1h 35m",
      streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
      isSeries = false
    ),
    MediaItem(
      id = "mov_sintel",
      title = "Sintel: Dragon Quest",
      description = "A determined lone warrior journeys across perilous icy peaks and scorching wastelands in search of Scales, the baby dragon she rescued and befriended.",
      poster = "https://images.unsplash.com/photo-1519074069444-1ba4ea16e604?w=800&q=80",
      banner = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1200&q=80",
      category = "Drama",
      year = 2024,
      rating = 8.9,
      duration = "2h 05m",
      streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
      isSeries = false
    ),
    MediaItem(
      id = "mov_elephants",
      title = "Elephants Dream",
      description = "Two explorers navigate a gigantic, mechanical, clockwork dream machine as strange mechanisms trigger reality shifts around their perception.",
      poster = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=800&q=80",
      banner = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=1200&q=80",
      category = "Thriller",
      year = 2023,
      rating = 7.9,
      duration = "1h 40m",
      streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
      isSeries = false
    ),
    MediaItem(
      id = "mov_romance_aurora",
      title = "Northern Lights Symphony",
      description = "An intimate romantic drama filmed under the Arctic polar night following two astronomers chasing the elusive solar storms.",
      poster = "https://images.unsplash.com/photo-1531366936337-7c912a4589a7?w=800&q=80",
      banner = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=1200&q=80",
      category = "Romance",
      year = 2024,
      rating = 8.3,
      duration = "1h 48m",
      streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
      isSeries = false
    )
  )

  val sampleSeries = listOf(
    MediaItem(
      id = "ser_cyberpunk",
      title = "Cyberpunk: 2099",
      description = "In the heart of megacity New Kowloon, mercenary hackers and rebel cyborgs fight to unveil a conspiracy buried beneath quantum intelligence networks.",
      poster = "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=800&q=80",
      banner = "https://images.unsplash.com/photo-1542751371-adc38448a05e?w=1200&q=80",
      category = "Sci-Fi",
      year = 2025,
      rating = 9.4,
      duration = "4 Episodes",
      streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
      isSeries = true,
      episodes = sampleEpisodesSeries1
    ),
    MediaItem(
      id = "ser_deepspace",
      title = "Deep Space Reconnaissance",
      description = "A deep-space long haul research vessel encounters unknown cosmic anomalies at the outer fringes of the solar system.",
      poster = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=800&q=80",
      banner = "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?w=1200&q=80",
      category = "Thriller",
      year = 2024,
      rating = 8.8,
      duration = "2 Episodes",
      streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackSeeTheWorld.mp4",
      isSeries = true,
      episodes = sampleEpisodesSeries2
    )
  )

  val categories = listOf(
    "All", "Action", "Sci-Fi", "Comedy", "Drama", "Thriller", "Romance", "Horror", "Documentary"
  )
}
