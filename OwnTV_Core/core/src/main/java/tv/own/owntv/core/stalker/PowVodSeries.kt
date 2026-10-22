package tv.own.owntv.core.stalker

import java.net.URLDecoder
import java.net.URLEncoder

object PowVodSeries {
    const val PREFIX = "powtv-vod-series:"
    private const val EPISODE_PREFIX = "powtv-vod-episode:"
    private val categoryPattern = Regex("\\b(?:tv\\s*shows?|web\\s*series|series|k[\\s-]*dramas?|nat[ao]k)\\b", RegexOption.IGNORE_CASE)
    fun isSeriesCategory(title: String): Boolean = categoryPattern.containsMatchIn(title.trim())
    fun flag(value: String?): Boolean = value == "1" || value.equals("true", ignoreCase = true)
    internal fun moviePage(page: StalkerClient.Page<StalkerClient.VodItem>, excluded: Set<String>): StalkerClient.Page<StalkerClient.VodItem> {
        if (excluded.isEmpty()) return page
        val movies = page.items.filterNot { it.categoryId in excluded }
        // Keep the portal total on a partial page; only a complete dump has a known movie count.
        val total = if (page.items.size >= page.totalItems) movies.size else page.totalItems
        return page.copy(totalItems = total, items = movies)
    }
    internal fun season(fields: Map<String, String>): StalkerClient.SeasonItem {
        require(flag(fields["is_season"]))
        val id = requireNotNull(fields["id"]).also { require(it.isNotBlank()) }
        return StalkerClient.SeasonItem(
            id, fields["name"], null,
            fields["season_number"]?.toIntOrNull() ?: StalkerClient.seasonNumberOf(id, fields["name"]), emptyList(),
        )
    }
    internal fun episode(fields: Map<String, String>): StalkerClient.VodSeriesEpisode {
        val number = fields["series_number"]?.toIntOrNull()?.takeIf { it > 0 }
            ?: fields["episode_num"]?.toIntOrNull()?.takeIf { it > 0 }
        require(flag(fields["is_episode"]) || number != null)
        val id = requireNotNull(fields["id"]).also { require(it.isNotBlank()) }
        return StalkerClient.VodSeriesEpisode(id, requireNotNull(number), fields["name"].orEmpty())
    }

    internal suspend fun <T> collectPages(fetch: suspend (Int) -> StalkerClient.Page<T>): List<T> {
        val result = LinkedHashSet<T>()
        var expected = 0
        for (page in 1..100) {
            val response = fetch(page)
            expected = maxOf(expected, response.totalItems)
            val before = result.size
            result += response.items
            if (result.size >= expected) return result.toList()
            require(result.size > before)
        }
        error("Portal hierarchy page limit exceeded")
    }

    data class EpisodeLocator(val movieId: String, val seasonId: String, val episodeId: String) {
        fun command(): String = EPISODE_PREFIX + listOf(movieId, seasonId, episodeId)
            .joinToString("/") { URLEncoder.encode(it, "UTF-8") }
    }
    fun episodeLocator(command: String): EpisodeLocator? {
        if (!command.startsWith(EPISODE_PREFIX)) return null
        val parts = command.removePrefix(EPISODE_PREFIX).split("/").map { URLDecoder.decode(it, "UTF-8") }
        require(parts.size == 3 && parts.all { it.isNotBlank() })
        return EpisodeLocator(parts[0], parts[1], parts[2])
    }
}
