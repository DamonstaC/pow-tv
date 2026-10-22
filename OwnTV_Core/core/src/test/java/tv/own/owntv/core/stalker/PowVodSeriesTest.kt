package tv.own.owntv.core.stalker

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class PowVodSeriesTest {
    @Test fun falseNativePayloadDoesNotBlockAuthorizedVodSeriesAndCannotPruneNativeCache() = runBlocking {
        var vodReads = 0
        val result = PowVodSeries.loadCategories(
            fetchVod = { vodReads++; listOf(StalkerClient.Genre("20", "ENGLISH | TV SHOWS"), StalkerClient.Genre("134", "ENGLISH MOVIES | 4K")) },
            fetchNative = { throw StalkerClient.StalkerAuthException("js:false") },
        )
        assertEquals(listOf("powtv-vod-series:20"), result.items.map { it.id })
        assertEquals(2, vodReads)
        assertFalse(result.complete)
    }
    @Test fun supportedNativeAndVodCategoriesBothRemainAvailable() = runBlocking {
        val result = PowVodSeries.loadCategories(
            fetchVod = { listOf(StalkerClient.Genre("20", "TV SHOWS")) },
            fetchNative = { listOf(StalkerClient.Genre("20", "Native series")) },
        )
        assertEquals(listOf("20", "powtv-vod-series:20"), result.items.map { it.id })
        assertTrue(result.complete)
    }
    @Test(expected = StalkerClient.StalkerAuthException::class) fun expiredSessionStillTriggersAuthenticationRetry() = runBlocking {
        var reads = 0
        PowVodSeries.loadCategories(
            fetchVod = { if (++reads == 2) throw StalkerClient.StalkerAuthException("expired"); listOf(StalkerClient.Genre("20", "TV SHOWS")) },
            fetchNative = { throw StalkerClient.StalkerAuthException("expired") },
        )
        Unit
    }
    @Test(expected = StalkerClient.StalkerHttpException::class) fun nativeRateLimitIsNotTreatedAsMissingSeries() = runBlocking {
        PowVodSeries.loadCategories(
            fetchVod = { listOf(StalkerClient.Genre("20", "TV SHOWS")) },
            fetchNative = { throw StalkerClient.StalkerHttpException(429, "rate limited") },
        )
        Unit
    }
    @Test(expected = StalkerClient.StalkerAuthException::class) fun nativeFailureWithoutVodShowsIsNotAnEmptySuccess() = runBlocking {
        PowVodSeries.loadCategories(
            fetchVod = { listOf(StalkerClient.Genre("134", "MOVIES")) },
            fetchNative = { throw StalkerClient.StalkerAuthException("js:false") },
        )
        Unit
    }
    @Test fun separatesProviderTvCategoriesWithoutMatchingMovieCollections() {
        listOf("ENGLISH | TV SHOWS", "ENGLISH | K-DRAMAS", "Web Series", "SERIES", "Bangla Natok").forEach {
            assertTrue(it, PowVodSeries.isSeriesCategory(it))
        }
        listOf("ENGLISH MOVIES | 4K", "MOVIES | LATEST", "MOVIES | COLLECTION", "Drama", "Documentaries | Movies").forEach {
            assertFalse(it, PowVodSeries.isSeriesCategory(it))
        }
    }
    @Test fun bulkMoviesExcludeRoutedShowsButPartialPageKeepsPagingTotals() {
        fun item(id: String, category: String) = StalkerClient.VodItem(id, id, null, null, null, null, null, category, null)
        val rows = listOf(item("1", "movies"), item("2", "shows"))
        val full = PowVodSeries.moviePage(StalkerClient.Page(2, 2, rows), setOf("shows"))
        assertEquals(listOf("1"), full.items.map { it.id })
        assertEquals(1, full.totalItems)
        val partial = PowVodSeries.moviePage(StalkerClient.Page(100, 2, rows), setOf("shows"))
        assertEquals(100, partial.totalItems)
        assertEquals(2, partial.maxPageItems)
        assertEquals(rows, PowVodSeries.moviePage(StalkerClient.Page(2, 2, rows), emptySet()).items)
    }
    @Test fun seasonAndEpisodeFieldsSupportPortalScalarTypes() {
        for (flag in listOf("true", "1", "TRUE")) {
            val season = PowVodSeries.season(mapOf("id" to "500", "is_season" to flag, "season_number" to "2"))
            assertEquals(2, season.seasonNumber)
            val ep = PowVodSeries.episode(mapOf("id" to "900", "is_episode" to flag, "series_number" to "3", "name" to "Pilot"))
            assertEquals(3, ep.number)
            assertEquals("Pilot", ep.name)
        }
        assertEquals(4, PowVodSeries.episode(mapOf("id" to "901", "episode_num" to "4")).number)
    }
    @Test(expected = IllegalArgumentException::class) fun malformedSeasonDoesNotBecomeAnEmptyShow() {
        PowVodSeries.season(mapOf("id" to "5", "is_file" to "true"))
    }
    @Test(expected = IllegalArgumentException::class) fun episodeWithoutNumberFailsInsteadOfOverwritingCachedData() {
        PowVodSeries.episode(mapOf("id" to "5", "is_episode" to "true"))
    }
    @Test fun persistedLocatorRoundTripsReservedCharacters() {
        val locator = PowVodSeries.EpisodeLocator("a/b", "s:2", "e +3")
        assertEquals(locator, PowVodSeries.episodeLocator(locator.command()))
        assertNull(PowVodSeries.episodeLocator("ffmpeg http://localhost/1"))
    }
    @Test(expected = IllegalArgumentException::class) fun malformedLocatorCannotResolveAnotherEpisode() {
        PowVodSeries.episodeLocator("powtv-vod-episode:1/2")
    }
    @Test fun allEpisodePagesAreCollected() = runBlocking {
        val visited = mutableListOf<Int>()
        val result = PowVodSeries.collectPages { p ->
            visited += p
            StalkerClient.Page(5, 2, when(p) { 1 -> listOf(1,2); 2 -> listOf(3,4); else -> listOf(5) })
        }
        assertEquals(listOf(1,2,3,4,5), result)
        assertEquals(listOf(1,2,3), visited)
    }
    @Test(expected = IllegalArgumentException::class) fun truncatedPageCannotReplaceCachedSeason() = runBlocking {
        PowVodSeries.collectPages { p -> if (p == 1) StalkerClient.Page(5, 2, listOf(1,2)) else StalkerClient.Page(0, 0, emptyList()) }
        Unit
    }
    @Test(expected = IllegalArgumentException::class) fun repeatedPageCannotLoopOrDuplicateEpisodes() = runBlocking {
        PowVodSeries.collectPages { StalkerClient.Page(5, 2, listOf(1,2)) }
        Unit
    }
}
