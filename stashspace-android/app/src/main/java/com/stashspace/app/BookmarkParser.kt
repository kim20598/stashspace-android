package com.stashspace.app

import org.json.JSONArray
import org.json.JSONObject

object BookmarkParser {
    fun parse(text: String): List<Bookmark> {
        val root = org.json.JSONTokener(text).nextValue()
        val array = when (root) {
            is JSONArray -> root
            is JSONObject -> findArray(root)
            else -> null
        } ?: throw IllegalArgumentException("This file doesn't contain a bookmarks array.")
        val result = ArrayList<Bookmark>(array.length())
        for (i in 0 until array.length()) {
            val raw = array.optJSONObject(i) ?: continue
            result += normalize(raw, i)
        }
        return result
    }
    
    fun parseWithExtras(text: String, isFavorite: Boolean = false, tagsText: String? = null, 
                        viewCount: Int = 0, notes: String = ""): Bookmark {
        val root = org.json.JSONTokener(text).nextValue()
        val array = when (root) {
            is JSONArray -> root
            is JSONObject -> findArray(root)
            else -> throw IllegalArgumentException("This file doesn't contain a bookmark object.")
        }
        
        if (array.length() == 0) throw IllegalArgumentException("Empty bookmark")
        
        val raw = array.optJSONObject(0) ?: throw IllegalArgumentException("Invalid bookmark format")
        val index = if (text.contains("\")) 0 else 1 // Fallback index
        return normalize(raw, index).copy(
            isFavorite = isFavorite,
            tags = tagsText?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
            viewCount = viewCount,
            notes = notes
        )
    }
    
    private fun findArray(obj: JSONObject): JSONArray? {
        val keys = listOf("bookmarks", "savedPosts", "saved_posts", "items", "posts", "saves")
        for (key in keys) obj.optJSONArray(key)?.let { return it }
        obj.optJSONObject("data")?.let { nested ->
            for (key in keys) nested.optJSONArray(key)?.let { return it }
        }
        // Check if this is a single bookmark object wrapped in an array
        if (obj.has("url") || obj.has("id")) {
            return JSONArray().apply { put(obj) }
        }
        return null
    }
    
    private fun normalize(raw: JSONObject, index: Int): Bookmark {
        val creator = raw.optJSONObject("creator") ?: raw.optJSONObject("user") ?: raw.optJSONObject("author") ?: JSONObject()
        val url = first(raw, "url", "permalink", "postUrl", "post_url", "link")
        val username = first(creator, "username", "user_name").ifBlank {
            first(raw, "username", "author_username", "owner_username")
        }
        val displayName = first(creator, "displayName", "fullName", "full_name").ifBlank {
            first(raw, "displayName", "author_name").ifBlank { username }
        }
        val mediaRaw: Any? = when {
            raw.has("mediaUrls") -> raw.opt("mediaUrls")
            raw.has("media_urls") -> raw.opt("media_urls")
            raw.has("media_urls_json") -> raw.opt("media_urls_json")
            raw.has("mediaUrl") -> raw.opt("mediaUrl")
            else -> raw.opt("media_url")
        }
        val media = parseStringList(mediaRaw).filter { it.startsWith("https://") || it.startsWith("http://") }
        val collectionRaw: Any? = when {
            raw.has("collectionNames") -> raw.opt("collectionNames")
            raw.has("collection_names") -> raw.opt("collection_names")
            raw.has("collections") -> raw.opt("collections")
            raw.has("collectionName") -> raw.opt("collectionName")
            else -> raw.opt("collection_name")
        }
        val collections = parseStringList(collectionRaw).map { it.trim() }.filter { it.isNotBlank() }.distinct()
        
        // Enhanced: Parse additional tags/collections from caption/description
        val additionalTags = parseTagsFromCaption(raw.optString("caption", "") + " " + raw.optString("description", ""))
        
        val id = first(raw, "shortcode", "id").ifBlank { url.ifBlank { "save-$index" } }
        
        return Bookmark(
            id = id, 
            url = url, 
            platform = first(raw, "platform").ifBlank { "unknown" }.replace('_', ' '),
            caption = first(raw, "caption", "description", "text"), 
            username = username,
            displayName = displayName, 
            profileUrl = first(creator, "profileUrl", "profile_url"),
            mediaUrls = media, 
            mediaType = first(raw, "mediaType", "media_type", "type"),
            savedAt = first(raw, "savedAt", "saved_at", "bookmarkedAt", "created_at", "timestamp", "saved_at"),
            collections = collections + additionalTags,
            isFavorite = raw.optBoolean("isFavorite", false),
            tags = parseStringList(raw.opt("tags")).filter { it.isNotBlank() } + additionalTags,
            viewCount = raw.optInt("viewCount", 0),
            notes = raw.optString("notes", "")
        )
    }
    
    private fun parseTagsFromCaption(caption: String): List<String> {
        val tags = mutableListOf<String>()
        // Extract hashtags
        val hashtagPattern = """#(\w+)""".toRegex()
        hashtagPattern.findAll(caption).forEach { match ->
            tags.add(match.groupValues[1])
        }
        return tags
    }
    
    private fun first(obj: JSONObject, vararg keys: String): String {
        for (key in keys) {
            val value = obj.opt(key)
            if (value != null && value != JSONObject.NULL && value !is JSONObject && value !is JSONArray) {
                val text = value.toString()
                if (text.isNotBlank() && text != "null") return text
            }
        }
        return ""
    }
    
    private fun parseStringList(value: Any?): List<String> = when (value) {
        is JSONArray -> (0 until value.length()).mapNotNull { i ->
            when (val item = value.opt(i)) {
                is JSONObject -> item.optString("name").ifBlank { item.optString("title") }.takeIf { it.isNotBlank() }
                null, JSONObject.NULL -> null
                else -> item.toString()
            }
        }
        is String -> {
            val trimmed = value.trim()
            if (trimmed.startsWith("[")) try { parseStringList(JSONArray(trimmed)) } catch (_: Exception) { listOf(value) }
            else if (trimmed.isBlank()) emptyList() else listOf(trimmed)
        }
        null -> emptyList()
        else -> listOf(value.toString())
    }
}