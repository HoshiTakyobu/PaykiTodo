package com.example.todoalarm.ui

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random

class QuoteRepository(context: Context) {
    private val appContext = context.applicationContext
    private val cacheFile = File(appContext.filesDir, CACHE_FILE_NAME)

    suspend fun loadQuotes(): List<String> = withContext(Dispatchers.IO) {
        val cachedQuotes = readCache().quotes
        val mergedQuotes = normalizeQuotes(cachedQuotes + seedQuotes)
        if (!cacheFile.exists()) {
            writeCache(QuoteCache(updatedAt = 0L, quotes = mergedQuotes))
        }
        mergedQuotes
    }

    suspend fun refreshQuotesIfNeeded(force: Boolean = false): List<String>? = withContext(Dispatchers.IO) {
        val currentCache = readCache()
        val now = System.currentTimeMillis()
        val shouldRefresh = force ||
            currentCache.updatedAt <= 0L ||
            currentCache.quotes.size < MIN_CACHE_SIZE ||
            now - currentCache.updatedAt >= REFRESH_INTERVAL_MS

        if (!shouldRefresh) return@withContext null

        val remoteQuotes = fetchRemoteQuotes()
        if (remoteQuotes.isEmpty()) return@withContext null

        val mergedQuotes = normalizeQuotes(remoteQuotes + currentCache.quotes + seedQuotes)
            .take(MAX_CACHE_SIZE)
        writeCache(QuoteCache(updatedAt = now, quotes = mergedQuotes))
        mergedQuotes
    }

    private suspend fun fetchRemoteQuotes(): List<String> = supervisorScope {
        val collected = linkedSetOf<String>()
        repeat(MAX_REMOTE_ROUNDS) {
            val batch = List(REMOTE_BATCH_SIZE) {
                async(Dispatchers.IO) { fetchSingleQuote() }
            }.awaitAll()
            batch.filterNotNull().forEach(collected::add)
            if (collected.size >= TARGET_REMOTE_QUOTE_COUNT) {
                return@supervisorScope collected.toList()
            }
        }
        collected.toList()
    }

    private fun fetchSingleQuote(): String? {
        val categories = REMOTE_CATEGORIES.shuffled(Random(System.nanoTime())).take(3)
        val query = categories.joinToString("&") { "c=$it" }
        val url = URL("https://v1.hitokoto.cn/?$query&encode=json")
        return runCatching {
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 1500
                readTimeout = 1800
                requestMethod = "GET"
                useCaches = false
            }
            connection.inputStream.bufferedReader().use { reader ->
                val json = JSONObject(reader.readText())
                sanitizeQuote(json.optString("hitokoto"))
            }
        }.getOrNull()
    }

    private fun readCache(): QuoteCache {
        if (!cacheFile.exists()) return QuoteCache()
        return runCatching {
            val json = JSONObject(cacheFile.readText())
            val quotes = json.optJSONArray("quotes")
                ?.let(::jsonArrayToList)
                .orEmpty()
            QuoteCache(
                updatedAt = json.optLong("updatedAt", 0L),
                quotes = normalizeQuotes(quotes)
            )
        }.getOrElse { QuoteCache() }
    }

    private fun writeCache(cache: QuoteCache) {
        val json = JSONObject().apply {
            put("updatedAt", cache.updatedAt)
            put("quotes", JSONArray(cache.quotes))
        }
        cacheFile.writeText(json.toString())
    }

    private fun jsonArrayToList(array: JSONArray): List<String> {
        return buildList(array.length()) {
            for (index in 0 until array.length()) {
                add(array.optString(index))
            }
        }
    }

    private fun normalizeQuotes(input: List<String>): List<String> {
        val seen = linkedSetOf<String>()
        input.map(::sanitizeQuote)
            .filter { it.length in 6..56 }
            .forEach(seen::add)
        return seen.toList()
    }

    private fun sanitizeQuote(raw: String?): String {
        return raw.orEmpty()
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    companion object {
        private const val CACHE_FILE_NAME = "payki_quotes_cache.json"
        private const val REFRESH_INTERVAL_MS = 12 * 60 * 60 * 1000L
        private const val MIN_CACHE_SIZE = 72
        private const val MAX_CACHE_SIZE = 240
        private const val TARGET_REMOTE_QUOTE_COUNT = 18
        private const val REMOTE_BATCH_SIZE = 6
        private const val MAX_REMOTE_ROUNDS = 3
        private val REMOTE_CATEGORIES = listOf("d", "h", "i", "k", "l")

        val seedQuotes: List<String> = listOf(
            "先把今天最重要的一步做出来，后面的路自然会亮。",
            "不用一次走很远，先把眼前这一格走稳。",
            "你不是没能力，只是还没把注意力收回来。",
            "把今天这件事完成，明天会轻很多。",
            "先开始，状态往往是在行动里出现的。",
            "别等特别完美的时候，先推进一厘米也算数。",
            "你现在愿意做的小事，正在替未来减压。",
            "专注十分钟，常常比焦虑一小时更有用。",
            "把难题拆开，第一小步通常没有那么可怕。",
            "真正拉开差距的，是愿不愿意继续把这一点做完。",
            "给现在的自己一点秩序，生活就会回你一点从容。",
            "越是想逃的事，越值得先轻轻碰一下。",
            "今天不求燃烧，只求稳定地向前挪动。",
            "把手头最重要的那件事推一下，心会安很多。",
            "很多时候不是你不行，而是你还没真正开始。",
            "慢一点没关系，停太久才最消耗自己。",
            "此刻做完一项，胜过脑海里计划十项。",
            "你能掌控的不是全部结果，而是下一次点击开始。",
            "今天做成一点点，已经在改变整天的走势。",
            "先把这一件做好，别让注意力四散。",
            "重要的事先落地，情绪往往会跟着变稳。",
            "你现在完成的，不只是任务，也是对自己的交代。",
            "眼前这一小步，就是把混乱拉回秩序的入口。",
            "没关系，先把最小的一块拿下来。",
            "别急着怀疑自己，先给行动一个机会。",
            "当你重新开始，今天就重新开始发光。",
            "压力很大时，更要把动作做小、做准、做实。",
            "把这一步走清楚，比幻想一百步更重要。",
            "你不是在追赶别人，只是在兑现自己。",
            "完成一件具体的事，会比反复内耗更能救场。",
            "情绪乱的时候，任务越要简单明确。",
            "把今天最该做的那件事，往前推到下一格。",
            "持续并不华丽，但它真的很强。",
            "只要还愿意把眼前这件事做下去，就不算输。",
            "先交出一个版本，再慢慢把它变好。",
            "专注不是一下子全拿回来，而是一次次拉回来。",
            "很多顿悟，都是在把一件小事做完之后发生的。",
            "你可以累，但别把今天最关键的一步放掉。",
            "今天先赢一小局，整体状态就会不同。",
            "能让你安心的，往往不是想通，而是做完。",
            "保持节奏，不必保持表演感。",
            "别被任务名吓到，先处理第一行就行。",
            "你不是非得很猛，你只要别散。",
            "先把要紧的做掉，其他噪音会自动变小。",
            "认真完成一件小事，就是在修复生活感。",
            "今天的推进，哪怕很少，也是真实的积累。",
            "与其盯着终点发愁，不如先把现在这一步踏实。",
            "手头最值得做的，通常不会等你完全准备好。",
            "完成会带来自信，自信再带来下一次完成。",
            "把一件事做完，是对抗失控感最直接的方式。",
            "你只需要先进入状态，不需要先战胜所有状态。",
            "哪怕只推进一点，也比原地打转强太多。",
            "开始之后，很多难受会自动退潮。",
            "此刻最该做的，不一定最轻松，但最能救你。",
            "把今天的秩序找回来，心就不会一直漂着。",
            "做一点，生活就会回一点确定感。",
            "从一个明确动作开始，比继续发愁更有效。",
            "让自己安下来的办法，常常是先完成眼前。",
            "不用和别人比进度，先把自己的节拍接上。",
            "只做当前这一件，世界会清净很多。",
            "先把拖着的事情碰一下，它就没那么可怕了。",
            "当下这次认真，不会白费。",
            "你以为你缺动力，很多时候你只是缺一个开始。",
            "把今天的重点抓回来，整个人都会稳一些。",
            "越想躲，越说明值得先处理。",
            "你不需要神奇的一天，只需要真实地推进今天。",
            "完成不是冷酷，它是一种温柔的自救。",
            "如果很乱，就先做最具体、最清楚的那一步。",
            "今天先不求漂亮，求一个落地。",
            "只要任务开始动了，很多卡点都会松开。",
            "有些安全感，只有做完之后才会出现。",
            "先替未来的自己省一点麻烦，就是很好的开始。",
            "把这一刻利用好，今天就不算被浪费。",
            "先完成，再评价；先推进，再纠结。",
            "你想要的松弛感，往往来自任务有序。",
            "哪怕只做十五分钟，也是在给自己托底。",
            "此刻的认真，会在稍后变成轻松。",
            "先把焦点收窄，效率自然会上来。",
            "不需要全都赢，把最关键的一项拿下就够了。"
        )
    }
}

private data class QuoteCache(
    val updatedAt: Long = 0L,
    val quotes: List<String> = emptyList()
)
