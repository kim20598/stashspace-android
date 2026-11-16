package com.stashspace.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

private val Bg = Color(0xFF10120F)
private val Panel = Color(0xFF191D17)
private val Panel2 = Color(0xFF22271F)
private val Line = Color(0xFF30372B)
private val Ink = Color(0xFFF1F3EB)
private val Muted = Color(0xFF9DA596)
private val Lime = Color(0xFFC7F36B)
private val Red = Color(0xFFFF8D83)

class MainActivity : ComponentActivity() {
    private lateinit var store: LibraryStore
    private lateinit var exportLauncher: androidx.activity.result.ActivityResultLauncher<String>
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = LibraryStore(this)
        setContent { StashspaceApp(store) }
    }
    
    fun getExportLauncher(): androidx.activity.result.ActivityResultLauncher<String> {
        return exportLauncher
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StashspaceApp(store: LibraryStore) {
    val context = LocalContext.current
    var bookmarks by remember { mutableStateOf(store.load()) }
    var search by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("All") }
    var selectedCollection by remember { mutableStateOf("All saves") }
    var sort by remember { mutableStateOf("Newest") }
    var listMode by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Bookmark?>(null) }
    var playing by remember { mutableStateOf<Bookmark?>(null) }
    var showSort by remember { mutableStateOf(false) }
    var showClear by remember { mutableStateOf(false) }
    var showExportModal by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var multiSelectMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var searchMode by remember { mutableStateOf("All") }
    
    val filtered = remember(bookmarks, search, selectedType, selectedCollection, sort, searchMode) {
        val all = bookmarks.filter { b ->
            val matchesQuery = search.isBlank() || 
                listOf(b.caption, b.username, b.displayName, b.url, b.platform, 
                       b.collections.joinToString(" "), b.tags.joinToString(" "))
                    .any { it.contains(search, true) }
            val matchesType = selectedType == "All" || 
                (selectedType == "Videos" && b.isVideo) || 
                (selectedType == "Photos" && b.isPhoto)
            val matchesCollection = selectedCollection == "All saves" || 
                selectedCollection in b.collections || 
                selectedCollection in b.tags ||
                selectedCollection == "Favorites" && b.isFavorite
            val matchesSearchMode = when (searchMode) {
                "Videos" -> b.isVideo
                "Photos" -> b.isPhoto
                "Favorites" -> b.isFavorite
                "Recent" -> {
                    val savedDate = try {
                        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(b.savedAt)
                    } catch (e: Exception) { null }
                    savedDate != null && abs(System.currentTimeMillis() - savedDate.time) < 7 * 24 * 60 * 60 * 1000L
                }
                else -> true
            }
            matchesQuery && matchesType && matchesCollection && matchesSearchMode
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
    
    // Only show collections that actually have items
    val collections = remember(bookmarks) {
        val allCollections = bookmarks
            .flatMap { it.collections + it.tags }
            .distinct()
            .sorted()
        
        listOf("All saves", "Favorites") + allCollections.filter { collection ->
            bookmarks.any { it.collections.contains(collection) || it.tags.contains(collection) }
        }
    }
    
    val smartCollections = remember(bookmarks) {
        listOf(
            "Recent (7d)" to { bookmarks.count { 
                val savedDate = try {
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(it.savedAt)
                } catch (e: Exception) { null }
                savedDate != null && abs(System.currentTimeMillis() - savedDate.time) < 7 * 24 * 60 * 60 * 1000L
            }},
            "Videos" to { bookmarks.count { it.isVideo } },
            "Photos" to { bookmarks.count { it.isPhoto } },
            "Untagged" to { bookmarks.count { it.collections.isEmpty() && it.tags.isEmpty() } }
        )
    }
    
    MaterialTheme(colorScheme = darkColorScheme(primary = Lime, onPrimary = Color(0xFF17200D), background = Bg, surface = Panel, onSurface = Ink, secondary = Muted)) {
        Scaffold(containerColor = Bg, topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(Lime), contentAlignment = Alignment.Center) {
                            Text("S", color = Color(0xFF17200D), fontWeight = FontWeight.Black, fontSize = 20.sp)
                        }
                        Column { 
                            Text("stashspace", fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, color = Ink) 
                            Text("YOUR SAVED FEED", fontSize = 9.sp, letterSpacing = 1.8.sp, color = Muted) 
                        }
                    } 
                },
                actions = {
                    if (multiSelectMode) {
                        IconButton(onClick = {
                            if (selectedIds.size > 0) {
                                store.replace(bookmarks.filter { it.id !in selectedIds })
                                selectedIds = emptySet()
                                multiSelectMode = false
                            }
                        }) { Icon(Icons.Default.Delete, "Delete") }
                        
                        IconButton(onClick = {
                            // Export selected
                            val selectedBookmarks = bookmarks.filter { it.id in selectedIds }
                            // Export logic here
                            selectedIds = emptySet()
                            multiSelectMode = false
                        }) { Icon(Icons.Default.FileDownload, "Export selected") }
                        
                        IconButton(onClick = {
                            bookmarks = bookmarks.map { 
                                if (it.id in selectedIds) it.copy(isFavorite = !it.isFavorite) else it 
                            }
                            store.replace(bookmarks)
                        }) { Icon(Icons.Default.Favorite, "Toggle Favorite") }
                    } else {
                        IconButton(onClick = {
                            store.replace(bookmarks.map { it.copy(isFavorite = !it.isFavorite) })
                        }) { Icon(Icons.Default.Favorite, "Mark all as favorite", tint = if (bookmarks.any { it.isFavorite }) Lime else Muted) }
                        
                        IconButton(onClick = { exportLauncher.launch("stashspace-library.json") }, enabled = bookmarks.isNotEmpty()) { 
                            Icon(Icons.Default.FileDownload, "Export", tint = if (bookmarks.isNotEmpty()) Ink else Muted) 
                        }
                        IconButton(onClick = { showClear = true }, enabled = bookmarks.isNotEmpty()) { 
                            Icon(Icons.Default.DeleteOutline, "Clear library", tint = Muted) 
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Bg)
            )
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                if (bookmarks.isEmpty()) {
                    EmptyLibrary(onImport = { importLauncher.launch(arrayOf("application/json", "text/*")) }, importing = importing)
                } else {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                        Text("YOUR PERSONAL ARCHIVE", color = Lime, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Text("Every save,\nin one place.", color = Ink, fontSize = 33.sp, lineHeight = 36.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp)
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
                            StatCard("SAVES", bookmarks.size.toString(), Modifier.weight(1f))
                            StatCard("VIDEOS", bookmarks.count { it.isVideo }.toString(), Modifier.weight(1f))
                            StatCard("PHOTOS", bookmarks.count { it.isPhoto }.toString(), Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(15.dp))
                        
                        // Search Bar
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(Panel).padding(horizontal = 13.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Search, null, tint = Muted)
                            Spacer(Modifier.width(9.dp))
                            BasicTextField(value = search, onValueChange = { search = it }, modifier = Modifier.weight(1f).padding(vertical = 13.dp), singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(color = Ink, fontSize = 14.sp), decorationBox = { inner -> 
                                    if (search.isEmpty()) Text("Search captions, creators, links…", color = Muted, fontSize = 14.sp)
                                    inner() 
                                })
                            if (search.isNotEmpty()) IconButton(onClick = { search = "" }) { Icon(Icons.Default.Close, null, tint = Muted) }
                        }
                        Spacer(Modifier.height(10.dp))
                        
                        // Filter Row
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                            listOf("All", "Videos", "Photos").forEach { label -> 
                                FilterPill(label, selectedType == label) { selectedType = label } 
                            }
                            Spacer(Modifier.weight(1f))
                            
                            // Smart Collection Dropdown
                            Box {
                                IconButton(onClick = { /* show smart collections menu */ }) { 
                                    Icon(Icons.Default.ViewList, "Collections", tint = Ink) 
                                }
                            }
                            
                            IconButton(onClick = { showSort = true }, modifier = Modifier.size(38.dp)) { 
                                Icon(Icons.Default.Sort, "Sort", tint = Ink) 
                            }
                            DropdownMenu(expanded = showSort, onDismissRequest = { showSort = false }, modifier = Modifier.background(Panel)) {
                                listOf("Newest", "Oldest", "Creator A–Z", "Most viewed", "Favorite").forEach { option -> 
                                    DropdownMenuItem(text = { Text(option, color = Ink) }, onClick = { sort = option; showSort = false }) 
                                }
                            }
                            IconButton(onClick = { listMode = !listMode }, modifier = Modifier.size(38.dp)) { 
                                Icon(if (listMode) Icons.Default.GridView else Icons.Default.ViewAgenda, "Toggle layout", tint = Ink) 
                            }
                        }
                        
                        // Smart Collections
                        Text("Smart: Recent, Favorites, Popular", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                        
                        Row(Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 10.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            smartCollections.forEach { (label, count) ->
                if (count > 0) {
                    FilterPill("$label ($count)", selectedCollection == label) { selectedCollection = label }
                }
            }
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                            Text(if (selectedCollection == "All saves") "Your saves" else selectedCollection, color = Ink, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Spacer(Modifier.weight(1f)); Text("${filtered.size} / ${bookmarks.size}", color = Muted, fontSize = 11.sp)
                        }
                        
                        // Multi-select toggle
                        if (!multiSelectMode) {
                            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                                filtered.take(3).forEach { bookmark ->
                                    Box(Modifier.size(24.dp).clip(CircleShape).background(Lime.copy(alpha = 0.3f))) {
                                        // Visual indicator for multi-select
                                    }
                                }
                            }
                        }
                    }
                    
                    if (filtered.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { 
                            Column(horizontalAlignment = Alignment.CenterHorizontally) { 
                                Icon(Icons.Default.SearchOff, null, tint = Muted, modifier = Modifier.size(38.dp))
                                Text("Nothing found", color = Ink, fontWeight = FontWeight.Bold)
                                Text("Try another search, filter, or check your collections.", color = Muted, fontSize = 12.sp) 
                                Spacer(Modifier.height(8.dp))
                                Button(onClick = { searchMode = if (searchMode == "All") "Videos" else "All" }) {
                                    Text("Show ${if (searchMode == "All") "Videos" else "All"}")
                                }
                            } 
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = if (listMode) GridCells.Fixed(1) else GridCells.Adaptive(155.dp),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
                            horizontalArrangement = Arrangement.spacedBy(11.dp),
                            verticalArrangement = Arrangement.spacedBy(11.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filtered = filtered, key = { it.id }) { item -> 
                                BookmarkCard(item, listMode, multiSelectMode, selectedIds.contains(item.id)) { 
                                    if (multiSelectMode) {
                                        // Toggle selection
                                        val current = selectedIds.toMutableSet()
                                        if (current.contains(item.id)) current.remove(item.id) else current.add(item.id)
                                        selectedIds = current
                                    } else {
                                        if (item.isVideo) playing = item else selected = item
                                    }
                                } 
                            }
                        }
                    }
                }
            }
        }
    }
    
    selected?.let { item -> 
        BookmarkDetails(item, onClose = { selected = null }, onOpen = { openUrl(context, item.url) }) 
    }
    playing?.let { item -> 
        VideoDialog(item, onClose = { playing = null }, onOpen = { openUrl(context, item.url) }) 
    }
    
    if (showClear) AlertDialog(
        onDismissRequest = { showClear = false },
        containerColor = Panel,
        title = { Text("Clear your library?", color = Ink) },
        text = { Text("This removes the saved copy from this device. Your original bookmarks are not changed.", color = Muted) },
        confirmButton = { 
            TextButton(onClick = { 
                store.replace(emptyList())
                bookmarks = emptyList()
                showClear = false 
            }) { 
                Text("Clear library", color = Red) 
            } 
        },
        dismissButton = { 
            TextButton(onClick = { showClear = false }) { Text("Cancel", color = Ink) } 
        }
    )
}

@Composable private fun EmptyLibrary(onImport: () -> Unit, importing: Boolean) {
    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(78.dp).clip(RoundedCornerShape(24.dp)).background(Lime.copy(alpha = .12f)), contentAlignment = Alignment.Center) { 
            Icon(Icons.Default.Bookmarks, null, tint = Lime, modifier = Modifier.size(38.dp)) 
        }
        Spacer(Modifier.height(22.dp))
        Text("Bring your saves\ninto view.", color = Ink, fontSize = 32.sp, lineHeight = 36.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(10.dp))
        Text("Import the JSON file downloaded by the Stasht bookmark exporter. Your library stays on this device.", color = Muted, fontSize = 14.sp, lineHeight = 21.sp)
        Spacer(Modifier.height(23.dp))
        Button(onClick = onImport, enabled = !importing, colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Color(0xFF17200D)), shape = RoundedCornerShape(13.dp), modifier = Modifier.fillMaxWidth()) { 
            Icon(Icons.Default.FileOpen, null)
            Spacer(Modifier.width(8.dp))
            Text(if (importing) "Importing…" else "Choose bookmark JSON", fontWeight = FontWeight.Bold) 
        }
        Spacer(Modifier.height(12.dp))
        Text("Supports Stasht exports with a bookmarks array\n\nNew features:\n• Favorites tracking\n• Tags from hashtags\n• View counts\n• Multi-select mode\n• Smart collections", color = Muted, fontSize = 11.sp)
    }
}

@Composable private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(13.dp)).background(Panel).padding(12.dp)) { 
        Text(label, color = Muted, fontSize = 9.sp, letterSpacing = 1.1.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Ink, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        Text("in your library", color = Muted, fontSize = 9.sp) 
    }
}

@Composable private fun FilterPill(label: String, active: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, color = if (active) Lime.copy(alpha = .15f) else Panel, shape = RoundedCornerShape(50), border = androidx.compose.foundation.BorderStroke(1.dp, if (active) Lime.copy(alpha = .55f) else Line)) {
        Text(label, color = if (active) Lime else Muted, fontSize = 11.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), maxLines = 1)
    }
}

@Composable private fun BookmarkCard(item: Bookmark, listMode: Boolean, multiSelectMode: Boolean, isSelected: Boolean, onClick: () -> Unit) {
    Column(Modifier.clip(RoundedCornerShape(15.dp)).background(Panel).clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().then(if (listMode) Modifier.height(170.dp) else Modifier.aspectRatio(.79f)).background(Color(0xFF090B08))) {
            val image = item.mediaUrls.firstOrNull()
            if (image != null && item.isPhoto) AsyncImage(model = image, contentDescription = item.caption, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { 
                Icon(if (item.isVideo) Icons.Default.PlayCircle else Icons.Default.Image, null, tint = Muted, modifier = Modifier.size(42.dp)) 
            }
            
            // Video indicator
            if (item.isVideo) {
                Box(Modifier.align(Alignment.TopStart).padding(9.dp).size(34.dp).clip(CircleShape).background(Lime), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color(0xFF17200D))
                }
            }
            
            // Favorite star
            if (item.isFavorite) {
                Box(Modifier.align(Alignment.TopEnd).padding(9.dp)) {
                    Icon(Icons.Default.Favorite, null, tint = Red, modifier = Modifier.size(24.dp))
                }
            }
            
            // Selection checkbox for multi-select
            if (multiSelectMode) {
                Box(Modifier.align(Alignment.TopEnd).padding(9.dp)) {
                    val backgroundColor = if (isSelected) Lime else Panel.copy(alpha = 0.7f)
                    Box(Modifier.size(24.dp).clip(CircleShape).background(backgroundColor)) {
                        if (isSelected) Icon(Icons.Default.Check, null, tint = Color(0xFF17200D), modifier = Modifier.size(16.dp))
                    }
                }
            }
            
            Surface(color = Color.Black.copy(alpha = .68f), shape = RoundedCornerShape(6.dp), modifier = Modifier.align(Alignment.TopStart).padding(9.dp)) {
                Text(if (item.isVideo) "▶ VIDEO" else "▧ PHOTO", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp))
            }
            
            // View count
            if (item.viewCount > 0) {
                Text("${item.viewCount}", Modifier.align(Alignment.BottomEnd).padding(9.dp), color = Ink.copy(alpha = 0.8f), fontSize = 10.sp)
            }
        }
        Column(Modifier.padding(11.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.displayNameOrUsername, color = Ink, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (item.isFavorite) Icon(Icons.Default.Favorite, null, tint = Red, modifier = Modifier.size(16.dp), fontSize = 12.sp)
            }
            Text(item.caption.ifBlank { "No caption saved." }, color = Color(0xFFD4D9CE), fontSize = 11.sp, lineHeight = 15.sp, maxLines = if (listMode) 3 else 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
            Row(Modifier.fillMaxWidth().padding(top = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(item.platform, color = Muted, fontSize = 9.sp, modifier = Modifier.weight(1f), maxLines = 1)
                IconButton(onClick = { /* Mark as watched */ }) { 
                    Icon(Icons.Default.Visibility, "Mark as viewed", tint = Muted, modifier = Modifier.size(16.dp)) 
                }
                IconButton(onClick = { /* Add to favorite */ item.copy(isFavorite = true) }) { 
                    Icon(Icons.Default.Add, "Add to favorites", tint = Muted, modifier = Modifier.size(16.dp)) 
                }
                IconButton(onClick = { openUrl(context, item.url) }) { Icon(Icons.Default.OpenInNew, "Open original", tint = Muted, modifier = Modifier.size(16.dp)) }
            }
        }
    }
}

@Composable private fun BookmarkDetails(item: Bookmark, onClose: () -> Unit, onOpen: () -> Unit) {
    AlertDialog(onDismissRequest = onClose, containerColor = Panel, title = { 
        Text(item.displayNameOrUsername, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis) 
    }, text = {
        Column(Modifier.heightIn(max = 430.dp).verticalScroll(rememberScrollState())) {
            item.mediaUrls.firstOrNull()?.let { AsyncImage(it, null, modifier = Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Fit) }
            Spacer(Modifier.height(12.dp))
            Text(item.caption.ifBlank { "No caption saved." }, color = Ink)
            Spacer(Modifier.height(10.dp))
            Text("${item.platform}  ·  ${item.savedAt.ifBlank { "Date unknown" }}", color = Muted, fontSize = 11.sp)
            if (item.isFavorite) Icon(Icons.Default.Favorite, null, tint = Red, modifier = Modifier.padding(top = 4.dp))
            if (item.collections.isNotEmpty()) Text("Collections: ${item.collections.joinToString()}", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
            if (item.tags.isNotEmpty()) Text("Tags: ${item.tags.joinToString()}", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
            if (item.viewCount > 0) Text("Viewed ${item.viewCount} time(s)", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
            if (item.notes.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text("Notes: ${item.notes}", color = Ink, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }, confirmButton = { 
        TextButton(onClick = onOpen) { Text("Open original", color = Lime) } 
    }, dismissButton = { 
        TextButton(onClick = onClose) { Text("Close", color = Ink) } 
    })
}

@Composable private fun VideoDialog(item: Bookmark, onClose: () -> Unit, onOpen: () -> Unit) {
    val context = LocalContext.current
    val url = item.mediaUrls.firstOrNull()
    val player = remember(url) {
        if (url == null) null else ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) { onDispose { player?.release() } }
    AlertDialog(onDismissRequest = onClose, containerColor = Panel, title = { 
        Text(item.displayNameOrUsername, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis) 
    }, text = {
        Column {
            if (player != null) AndroidView(factory = { ctx -> PlayerView(ctx).apply { this.player = player; useController = true } }, modifier = Modifier.fillMaxWidth().height(350.dp))
            else Text("No direct video URL was included in this save.", color = Muted)
            Text(item.caption.ifBlank { "No caption saved." }, color = Ink, maxLines = 4, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 12.dp))
            Text("If playback fails, the source may have expired or require an account session.", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 7.dp))
            Text("• Favorited: ${item.isFavorite}", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
            Text("• Tags: ${item.tags.joinToString()}", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }, confirmButton = { 
        TextButton(onClick = onOpen) { Text("Open original", color = Lime) } 
    }, dismissButton = { 
        TextButton(onClick = onClose) { Text("Close", color = Ink) } 
    })
}

private fun openUrl(context: android.content.Context, url: String) {
    if (url.isBlank()) { Toast.makeText(context, "No original link was saved", Toast.LENGTH_SHORT).show(); return }
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        .onFailure { Toast.makeText(context, "Couldn't open this link", Toast.LENGTH_SHORT).show() }
}