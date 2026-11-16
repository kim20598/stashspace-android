package com.stashspace.app

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

object ExportUtils {
    
    fun exportToJson(bookmarks: List<Bookmark>): String {
        val json = JSONObject()
            .put("exportedAt", Instant.now().toString())
            .put("platform", "stashspace")
            .put("version", "2.0")
            .put("count", bookmarks.size)
            .put("stats", JSONObject()
                .put("total", bookmarks.size)
                .put("videos", bookmarks.count { it.isVideo })
                .put("photos", bookmarks.count { it.isPhoto })
                .put("favorites", bookmarks.count { it.isFavorite })
            )
        
        val bookmarksArray = JSONArray()
        bookmarks.forEach { bookmark ->
            val bookmarkJson = JSONObject()
                .put("id", bookmark.id)
                .put("url", bookmark.url)
                .put("platform", bookmark.platform)
                .put("caption", bookmark.caption)
                .put("mediaType", bookmark.mediaType)
                .put("mediaUrls", JSONArray(bookmark.mediaUrls))
                .put("savedAt", bookmark.savedAt)
                .put("collections", JSONArray(bookmark.collections))
                .put("tags", JSONArray(bookmark.tags))
                .put("isFavorite", bookmark.isFavorite)
                .put("viewCount", bookmark.viewCount)
                .put("notes", bookmark.notes)
                .put("creator", JSONObject()
                    .put("username", bookmark.username)
                    .put("displayName", bookmark.displayName)
                    .put("profileUrl", bookmark.profileUrl)
                )
            bookmarksArray.put(bookmarkJson)
        }
        
        json.put("bookmarks", bookmarksArray)
        return json.toString(2)
    }
    
    fun exportSelected(bookmarks: List<Bookmark>, selectedIds: Set<String>): String {
        val selected = bookmarks.filter { it.id in selectedIds }
        return exportToJson(selected)
    }
    
    enum class ExportFormat {
        FULL,       // Complete data with all fields
        BASIC,      // URL, platform, caption only
        COLLECTION, // Group by collection
        CSV         // CSV format for spreadsheet
    }
    
    fun exportAsCsv(bookmarks: List<Bookmark>): String {
        val lines = mutableListOf<String>()
        lines.add("id,url,platform,caption,username,displayName,collections,tags,mediaUrls,savedAt,isVideo,isFavorite,viewCount")
        
        bookmarks.forEach { b ->
            lines.add("""${b.id},"${b.url}","${b.platform}","${b.caption.replace("\"", "\"\"")}","${b.username}","${b.displayName}","${b.collections.joinToString("||")}","${b.tags.joinToString("||")}","${b.mediaUrls.joinToString("||")}","${b.savedAt}",${b.isVideo},${b.isFavorite},${b.viewCount}""")
        }
        
        return lines.joinToString("\n")
    }
}