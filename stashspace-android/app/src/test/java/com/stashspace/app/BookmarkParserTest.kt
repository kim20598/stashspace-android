package com.stashspace.app

import org.junit.Assert.*
import org.junit.Test

class BookmarkParserTest {
    @Test fun parsesStashtInstagramExportAndNumericVideoType() {
        val json = """{"platform":"instagram","count":1,"bookmarks":[{"shortcode":"ABC123","url":"https://www.instagram.com/p/ABC123/","platform":"instagram","mediaType":2,"caption":"A saved clip","mediaUrls":["https://cdn.example/video.mp4"],"creator":{"username":"creator","fullName":"Creator Name"},"savedAt":"2025-01-02T03:04:05.000Z","collectionNames":["Travel"]}]}"""
        val item = BookmarkParser.parse(json).single()
        assertEquals("ABC123", item.id)
        assertEquals("Creator Name", item.displayName)
        assertTrue(item.isVideo)
        assertEquals(listOf("Travel"), item.collections)
    }

    @Test fun acceptsBareArrayAndMissingOptionalFields() {
        val item = BookmarkParser.parse("""[{"id":"1","caption":"hello"}]""").single()
        assertEquals("hello", item.caption)
        assertFalse(item.isVideo)
        assertTrue(item.mediaUrls.isEmpty())
    }
}
