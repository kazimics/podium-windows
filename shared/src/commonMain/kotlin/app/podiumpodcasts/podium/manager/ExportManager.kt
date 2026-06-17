package app.podiumpodcasts.podium.manager

import app.podiumpodcasts.podium.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ExportManager(private val db: AppDatabase) {

    suspend fun exportOpml(): String = withContext(Dispatchers.Default) {
        val podcasts = db.podcasts.getAllSync()
        val sb = StringBuilder()
        sb.appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        sb.appendLine("<opml version=\"2.0\">")
        sb.appendLine("  <head><title>Podium Podcasts</title></head>")
        sb.appendLine("  <body>")
        podcasts.forEach { podcast ->
            sb.appendLine("    <outline type=\"rss\" text=\"${escapeXml(podcast.title)}\" title=\"${escapeXml(podcast.title)}\" xmlUrl=\"${escapeXml(podcast.origin)}\" htmlUrl=\"${escapeXml(podcast.link)}\"/>")
        }
        sb.appendLine("  </body>")
        sb.appendLine("</opml>")
        sb.toString()
    }

    private fun escapeXml(text: String): String {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
    }
}
