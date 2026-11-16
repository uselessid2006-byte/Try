package com.desitales2

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class DesiTales2Provider : MainAPI() {
    // Update this domain if the site rotates its mirror (e.g., .com, .net, .club)
    override var mainUrl = "https://desitales2.com"
    override var name = "DesiTales 2"
    override val hasMainPage = true
    override var lang = "en"
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "$mainUrl/page/" to "Latest Videos",
        "$mainUrl/most-viewed/page/" to "Most Viewed",
        "$mainUrl/top-rated/page/" to "Top Rated"
    )

    private fun Element.toSearchResult(): SearchResponse? {
        val anchor = this.selectFirst("a[href]") ?: return null
        val href = fixUrl(anchor.attr("href"))
        val title = anchor.attr("title").ifBlank {
            this.selectFirst("h2, h3, .title, .entry-title")?.text()
        }?.trim() ?: return null

        val img = this.selectFirst("img")
        val poster = fixUrlNull(
            img?.attr("data-src")?.ifBlank { null }
                ?: img?.attr("data-lazy-src")?.ifBlank { null }
                ?: img?.attr("src")
        )

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = poster
        }
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page == 1) {
            request.data.removeSuffix("page/")
        } else {
            "${request.data}$page/"
        }

        val document = app.get(url).document
        val items = document.select("article, div.video-block, div.thumb, div.item")
            .mapNotNull { it.toSearchResult() }
            .distinctBy { it.url }

        return newHomePageResponse(
            list = HomePageList(request.name, items),
            hasNext = items.isNotEmpty()
        )
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/?s=${query.replace(" ", "+")}").document
        return document.select("article, div.video-block, div.thumb, div.item")
            .mapNotNull { it.toSearchResult() }
            .distinctBy { it.url }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("h1.entry-title, h1")?.text()?.trim() ?: "Untitled"
        val poster = fixUrlNull(
            document.selectFirst("meta[property=og:image]")?.attr("content")
                ?: document.selectFirst("video")?.attr("poster")
        )
        val plot = document.selectFirst("meta[property=og:description], div.entry-content p")?.text()?.trim()

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = plot
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        var found = false

        // 1. Check direct HTML5 <video> and <source> tags
        document.select("video source[src], video[src]").forEach { el ->
            val src = fixUrlNull(el.attr("src")) ?: return@forEach
            callback.invoke(
                newExtractorLink(
                    source = name,
                    name = name,
                    url = src,
                    type = INFER_TYPE
                ) {
                    this.referer = data
                    this.quality = Qualities.Unknown.value
                }
            )
            found = true
        }

        // 2. Check inline player scripts for direct .mp4 or .m3u8 URLs
        val streamRegex = Regex("""["'](https?://[^"']+\.(?:mp4|m3u8)(?:\?[^"']*)?)["']""")
        streamRegex.findAll(document.html()).forEach { match ->
            val streamUrl = match.groupValues[1].replace("\\/", "/")
            callback.invoke(
                newExtractorLink(
                    source = name,
                    name = "$name Direct",
                    url = streamUrl,
                    type = INFER_TYPE
                ) {
                    this.referer = data
                    this.quality = Qualities.Unknown.value
                }
            )
            found = true
        }

        // 3. Check embedded <iframe> players (both built-in extractors and custom tube embeds)
        document.select("iframe[src]").forEach { iframe ->
            val iframeUrl = fixUrlNull(iframe.attr("src")) ?: return@forEach
            if (iframeUrl.contains("ads") || iframeUrl.contains("doubleclick")) return@forEach

            val extracted = loadExtractor(iframeUrl, data, subtitleCallback, callback)
            if (extracted) {
                found = true
            } else {
                // Fallback: fetch the iframe HTML directly and scrape its <video> tag
                val iframeDoc = app.get(iframeUrl, referer = data).document
                iframeDoc.select("video source[src], video[src]").forEach { vid ->
                    val rawSrc = fixUrlNull(vid.attr("src")) ?: return@forEach
                    callback.invoke(
                        newExtractorLink(
                            source = name,
                            name = "$name Embed",
                            url = rawSrc,
                            type = INFER_TYPE
                        ) {
                            this.referer = iframeUrl
                            this.quality = Qualities.Unknown.value
                        }
                    )
                    found = true
                }
            }
        }

        return found
    }
}
