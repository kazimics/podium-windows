package app.podiumpodcasts.podium.manager

import app.podiumpodcasts.podium.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

class ImportManager(private val db: AppDatabase) {

    private val podcastManager = PodcastManager(db)

    suspend fun importOpml(opmlContent: String): ImportResult = withContext(Dispatchers.Default) {
        var added = 0
        var skipped = 0
        var failed = 0
        val errors = mutableListOf<String>()

        try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = false
            val builder = factory.newDocumentBuilder()
            val document = builder.parse(InputSource(StringReader(opmlContent)))

            val outlines = document.getElementsByTagName("outline")
            for (i in 0 until outlines.length) {
                val outline = outlines.item(i)
                val attributes = outline.attributes
                val type = attributes?.getNamedItem("type")?.nodeValue
                val xmlUrl = attributes?.getNamedItem("xmlUrl")?.nodeValue
                val title = attributes?.getNamedItem("title")?.nodeValue
                    ?: attributes?.getNamedItem("text")?.nodeValue

                if (type == "rss" && xmlUrl != null) {
                    try {
                        when (val result = podcastManager.addPodcast(xmlUrl, null)) {
                            is AddPodcastResult.Created -> added++
                            is AddPodcastResult.Duplicate -> skipped++
                        }
                    } catch (e: Exception) {
                        failed++
                        errors.add("${title ?: xmlUrl}: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            return@withContext ImportResult.Error("Failed to parse OPML: ${e.message}")
        }

        ImportResult.Success(added = added, skipped = skipped, failed = failed, errors = errors)
    }
}

sealed class ImportResult {
    data class Success(
        val added: Int,
        val skipped: Int,
        val failed: Int,
        val errors: List<String>
    ) : ImportResult()

    data class Error(val message: String) : ImportResult()
}
