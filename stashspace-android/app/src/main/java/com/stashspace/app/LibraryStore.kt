package com.stashspace.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray

class LibraryStore(context: Context) : SQLiteOpenHelper(context, "stashspace.db", null, 2) {
    
    companion object {
        private const val TABLE_NAME = "bookmarks"
        private const val VERSION = 2
    }
    
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_NAME (
                id TEXT PRIMARY KEY,
                payload TEXT NOT NULL,
                position INTEGER NOT NULL,
                is_favorite INTEGER DEFAULT 0,
                tags TEXT,
                view_count INTEGER DEFAULT 0,
                notes TEXT
            )
        """.trimIndent())
    }
    
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN is_favorite INTEGER DEFAULT 0")
            db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN tags TEXT")
            db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN view_count INTEGER DEFAULT 0")
            db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN notes TEXT")
        }
    }
    
    fun replace(items: List<Bookmark>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_NAME, null, null)
            items.forEachIndexed { index, item ->
                val values = ContentValues().apply {
                    put("id", item.id)  // FIXED: Use original ID, not id-index
                    put("payload", encode(item))
                    put("position", index)
                    put("is_favorite", if (item.isFavorite) 1 else 0)
                    put("tags", item.tags.joinToString(","))
                    put("view_count", item.viewCount)
                    put("notes", item.notes)
                }
                db.insertOrThrow(TABLE_NAME, null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
    
    fun add(items: List<Bookmark>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            items.forEach { item ->
                val values = ContentValues().apply {
                    put("id", item.id)
                    put("payload", encode(item))
                    put("position", getNextPosition(db))
                    put("is_favorite", if (item.isFavorite) 1 else 0)
                    put("tags", item.tags.joinToString(","))
                    put("view_count", item.viewCount)
                    put("notes", item.notes)
                }
                db.insertOrThrow(TABLE_NAME, null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
    
    fun updatePosition(id: String, newPosition: Int) {
        val db = writableDatabase
        db.update(TABLE_NAME, ContentValues().apply { put("position", newPosition) }, "id = ?", arrayOf(id))
    }
    
    fun toggleFavorite(id: String) {
        val db = writableDatabase
        val cursor = db.query(TABLE_NAME, arrayOf("is_favorite"), "id = ?", arrayOf(id), null, null, null)
        if (cursor.moveToFirst()) {
            val current = cursor.getInt(0)
            val values = ContentValues().apply { put("is_favorite", if (current == 0) 1 else 0) }
            db.update(TABLE_NAME, values, "id = ?", arrayOf(id))
        }
        cursor.close()
    }
    
    private fun getNextPosition(db: SQLiteDatabase): Int {
        val cursor = db.query(TABLE_NAME, arrayOf("MAX(position)"), null, null, null, null, null)
        val max = if (cursor.moveToFirst()) cursor.getInt(0) else -1
        cursor.close()
        return max + 1
    }
    
    fun load(): List<Bookmark> {
        val result = mutableListOf<Bookmark>()
        readableDatabase.rawQuery(
            "SELECT payload, is_favorite, tags, view_count, notes FROM $TABLE_NAME ORDER BY position ASC", 
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                runCatching {
                    val baseJson = cursor.getString(0)
                    val isFavorite = cursor.getInt(1) == 1
                    val tagsJson = cursor.getString(2)
                    val viewCount = cursor.getInt(3)
                    val notes = cursor.getString(4) ?: ""
                    
                    BookmarkParser.parseWithExtras(baseJson, isFavorite, tagsJson, viewCount, notes)
                }.getOrNull()?.let(result::add)
            }
        }
        return result
    }
    
    fun search(query: String, selectedType: String, selectedCollection: String, sort: String, searchMode: String): List<Bookmark> {
        val all = load()
        return all.filter { b ->
            val matchesQuery = query.isBlank() || 
                listOf(b.caption, b.username, b.displayName, b.url, b.platform, 
                       b.collections.joinToString(" "), b.tags.joinToString(" "))
                    .any { it.contains(query, true) }
            val matchesType = selectedType == "All" || 
                (selectedType == "Videos" && b.isVideo) || 
                (selectedType == "Photos" && b.isPhoto)
            val matchesCollection = selectedCollection == "All saves" || 
                selectedCollection in b.collections || 
                selectedCollection in b.tags
            matchesQuery && matchesType && matchesCollection
        }.let { rows ->
            when (sort) {
                "Oldest" -> rows.sortedBy { it.savedAt }
                "Creator A–Z" -> rows.sortedBy { it.displayName.ifBlank { it.username }.lowercase() }
                "Most viewed" -> rows.sortedByDescending { it.viewCount }
                "Favorite" -> rows.sortedByDescending { it.isFavorite }.thenByDescending { it.savedAt }
                else -> rows.sortedByDescending { it.savedAt }
            }
        }
    }
    
    fun incrementViewCount(id: String) {
        val db = writableDatabase
        val current = getCount(id)
        db.update(TABLE_NAME, ContentValues().apply { put("view_count", current + 1) }, "id = ?", arrayOf(id))
    }
    
    private fun getCount(id: String): Int {
        val db = readableDatabase
        val cursor = db.query(TABLE_NAME, arrayOf("view_count"), "id = ?", arrayOf(id), null, null, null)
        val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return count
    }
    
    private fun encode(b: Bookmark): String = org.json.JSONObject().apply {
        put("id", b.id)
        put("url", b.url)
        put("platform", b.platform)
        put("caption", b.caption)
        put("username", b.username)
        put("displayName", b.displayName)
        put("mediaUrls", JSONArray(b.mediaUrls))
        put("mediaType", b.mediaType)
        put("savedAt", b.savedAt)
        put("collections", JSONArray(b.collections))
        put("creator", org.json.JSONObject()
            .put("username", b.username)
            .put("displayName", b.displayName)
            .put("profileUrl", b.profileUrl))
    }.toString()
}