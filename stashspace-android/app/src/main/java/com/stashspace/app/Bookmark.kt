package com.stashspace.app

import java.text.SimpleDateFormat
import java.util.*

data class Bookmark(
    val id: String,
    val url: String,
    val platform: String,
    val caption: String,
    val username: String,
    val displayName: String,
    val profileUrl: String,
    val mediaUrls: List<String>,
    val mediaType: String,
    val savedAt: String,
    val collections: List<String>,
    val isFavorite: Boolean = false,
    val tags: List<String> = emptyList(),
    val viewCount: Int = 0,
    val notes: String = ""
) {
    val isVideo: Boolean
        get() = mediaType.contains("video", true) || mediaType == "2" ||
                mediaType == "8" || // Carousel posts
                mediaUrls.any { 
                    it.substringBefore('?').substringAfterLast('.', "").lowercase() in 
                    setOf("mp4", "m4v", "webm", "mov", "gif") 
                }
    
    val isPhoto: Boolean
        get() = !isVideo
    
    val savedDate: Date get() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(savedAt) ?: Date()
    
    val displayNameOrUsername: String get() = displayName.ifBlank { username.ifBlank { "Unknown" } }
    
    fun hasMedia(): Boolean = mediaUrls.isNotEmpty()
    
    fun getPrimaryMediaUrl(): String? = mediaUrls.firstOrNull()
}