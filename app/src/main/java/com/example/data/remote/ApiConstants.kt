package com.example.data.remote

object ApiConstants {
  // Koyeb FQDN configurable here:
  const val DEFAULT_KOYEB_FQDN = "https://url-stream-api.koyeb.app"

  // Base API routes
  const val ROUTE_HOME = "/api/home"
  const val ROUTE_MOVIES = "/api/movies"
  const val ROUTE_SERIES = "/api/series"
  const val ROUTE_EPISODES = "/api/series/{id}/episodes"
  const val ROUTE_SEARCH = "/api/search"
  const val ROUTE_RANDOM = "/api/random"
}
