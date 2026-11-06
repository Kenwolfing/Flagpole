package com.example.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import coil.compose.AsyncImage
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import com.example.viewmodel.ResetType
import com.example.viewmodel.CopiedItem
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.example.R
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.gestures.scrollBy
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.data.*
import com.example.viewmodel.GoalViewModel

// Milestone models for handling local UI parsing
data class Milestone(
    val value: Int,
    val type: String // "flag", "star", "heart", "diamond", "line", "dot", "danger", "cross"
)

fun parseMilestones(str: String): List<Milestone> {
    if (str.isBlank()) return emptyList()
    return str.split(";").mapNotNull { part ->
        val subparts = part.split(":")
        if (subparts.size == 2) {
            val value = subparts[0].toIntOrNull()
            if (value != null) {
                Milestone(value, subparts[1].lowercase())
            } else null
        } else null
    }.sortedBy { it.value }
}

fun getMilestoneTypePriority(type: String): Int {
    return when (type.lowercase()) {
        "flag" -> 1
        "star" -> 2
        "heart" -> 3
        "diamond" -> 4
        "line" -> 5
        "dot" -> 6
        "danger" -> 7
        "cross" -> 8
        else -> 9
    }
}

fun getMilestoneTypeLabel(type: String): String {
    return when (type.lowercase()) {
        "flag" -> "FLAGS"
        "star" -> "STARS"
        "heart" -> "HEARTS"
        "diamond" -> "DIAMONDS"
        "line" -> "LINES"
        "dot" -> "DOTS"
        "danger" -> "DANGER"
        "cross" -> "CROSS"
        else -> type.uppercase() + "S"
    }
}

@Composable
fun MilestoneIconDisplay(
    type: String,
    tint: Color,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 16.dp
) {
    when (type.lowercase()) {
        "flag" -> Icon(Icons.Filled.Flag, contentDescription = "Flag", tint = tint, modifier = modifier.size(size))
        "star" -> Icon(Icons.Filled.Star, contentDescription = "Star", tint = tint, modifier = modifier.size(size))
        "heart" -> Icon(Icons.Filled.Favorite, contentDescription = "Heart", tint = tint, modifier = modifier.size(size))
        "diamond" -> Box(
            modifier = modifier
                .size((size.value * 0.707f).dp.coerceAtLeast(3.dp))
                .rotate(45f)
                .clip(RoundedCornerShape(0.5.dp))
                .background(tint)
        )
        "danger" -> Icon(Icons.Filled.Warning, contentDescription = "Danger", tint = tint, modifier = modifier.size(size))
        "cross" -> Icon(Icons.Filled.Close, contentDescription = "Cross", tint = tint, modifier = modifier.size(size))
        "line" -> Box(
            modifier = modifier
                .width((size.value * 0.25f).dp.coerceAtLeast(1.5.dp))
                .height(size)
                .clip(RoundedCornerShape(1.dp))
                .background(tint)
        )
        else -> Box( // "dot"
            modifier = modifier
                .size((size.value * 0.75f).dp.coerceAtLeast(3.5.dp))
                .clip(CircleShape)
                .background(tint)
        )
    }
}

fun getMilestonePriority(type: String): Int = getMilestoneTypePriority(type)

fun generateIncrementalMilestones(
    count: Int,
    startFrom: Int,
    increaseBy: Int,
    weightPercent: Float
): List<Int> {
    val clampedCount = count.coerceIn(1, 100)
    val result = mutableListOf<Int>()
    val acceleration = (weightPercent / 100f) * (increaseBy * 0.2f)
    var current = startFrom.toDouble()
    var currentStep = increaseBy.toDouble()

    for (i in 0 until clampedCount) {
        if (i == 0) {
            current = startFrom.toDouble()
        } else {
            current += currentStep
            currentStep = (currentStep + acceleration).coerceAtLeast(1.0)
        }
        result.add(current.toInt())
    }
    return result
}

fun generateIncrementalMilestones(
    count: Int,
    startFrom: Int,
    increaseBy: Int,
    eachIncreaseBy: Int
): List<Int> {
    val clampedCount = count.coerceIn(1, 100)
    val result = mutableListOf<Int>()
    var current = startFrom
    var currentStep = increaseBy
    for (i in 0 until clampedCount) {
        if (i == 0) {
            current = startFrom
        } else {
            current += currentStep
            currentStep += eachIncreaseBy
        }
        result.add(current)
    }
    return result
}

fun generateRangeMilestones(
    count: Int,
    from: Int,
    to: Int,
    weightPercent: Float
): List<Int> = generateInRangeMilestones(from, to, count, weightPercent)

fun generateInRangeMilestones(
    from: Int,
    to: Int,
    count: Int,
    weightPercent: Float
): List<Int> {
    val clampedCount = count.coerceIn(1, 100)
    if (clampedCount <= 0) return emptyList()
    val minVal = minOf(from, to)
    val maxVal = maxOf(from, to)
    val distance = (maxVal - minVal).toDouble()
    if (distance <= 0) return List(clampedCount) { minVal }

    val w = (weightPercent / 100f).coerceIn(-0.999f, 0.999f)

    if (w == 0f) {
        val step = distance / (clampedCount + 1)
        return (1..clampedCount).map { i ->
            kotlin.math.floor(minVal + i * step).toInt()
        }
    } else if (w > 0f) {
        val result = mutableListOf<Int>()
        var current = minVal.toDouble()
        for (i in 0 until clampedCount) {
            val next = current + w * (maxVal - current)
            result.add(kotlin.math.floor(next).toInt())
            current = next
        }
        return result
    } else {
        val wAbs = kotlin.math.abs(w).toDouble()
        val reversedResult = mutableListOf<Int>()
        var current = maxVal.toDouble()
        for (i in 0 until clampedCount) {
            val prev = current - wAbs * (current - minVal)
            reversedResult.add(kotlin.math.floor(prev).toInt())
            current = prev
        }
        return reversedResult.reversed()
    }
}

@Composable
fun getCardBackgroundColor(isDark: Boolean, secondaryColorHex: String): Color {
    if (secondaryColorHex == "DEFAULT") {
        return if (isDark) Color(0xFF1E1C24) else Color(0xFFFFFFFF)
    }
    val baseColor = try {
        Color(android.graphics.Color.parseColor(secondaryColorHex))
    } catch (e: Exception) {
        if (isDark) Color(0xFF1E1C24) else Color(0xFFFFFFFF)
    }
    return if (isDark) {
        // Blend baseColor subtly on top of dark surface (reduced from 0.12f to 0.05f)
        val tintAlpha = 0.05f
        Color(
            red = (1f - tintAlpha) * 0.12f + tintAlpha * baseColor.red,
            green = (1f - tintAlpha) * 0.11f + tintAlpha * baseColor.green,
            blue = (1f - tintAlpha) * 0.15f + tintAlpha * baseColor.blue,
            alpha = 1f
        )
    } else {
        // Soft bright pastel version of baseColor (increased from 0.08f to 0.18f for visibility)
        val tintAlpha = 0.18f
        Color(
            red = (1f - tintAlpha) * 1.0f + tintAlpha * baseColor.red,
            green = (1f - tintAlpha) * 1.0f + tintAlpha * baseColor.green,
            blue = (1f - tintAlpha) * 1.0f + tintAlpha * baseColor.blue,
            alpha = 1f
        )
    }
}

sealed class DashboardItem {
    abstract val id: Int
    abstract val createdAt: Long
    abstract val parentGroupId: Int

    data class GoalItem(val goal: Goal) : DashboardItem() {
        override val id = goal.id
        override val createdAt = goal.createdAt
        override val parentGroupId = goal.parentGroupId
    }

    data class NoteItem(val note: Note) : DashboardItem() {
        override val id = note.id
        override val createdAt = note.createdAt
        override val parentGroupId = note.parentGroupId
    }

    data class GroupItem(val group: GoalGroup) : DashboardItem() {
        override val id = group.id
        override val createdAt = group.createdAt
        override val parentGroupId = group.parentGroupId
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    viewModel: GoalViewModel,
    modifier: Modifier = Modifier
) {
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val groupNavigationStack by viewModel.groupNavigationStack.collectAsStateWithLifecycle()
    val activeGroup by viewModel.activeGroup.collectAsStateWithLifecycle()
    val groupGoalStats by viewModel.groupGoalStats.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val allGoals by viewModel.allGoals.collectAsStateWithLifecycle(initialValue = emptyList())
    val allGroups by viewModel.allGroups.collectAsStateWithLifecycle(initialValue = emptyList())
    val allLogs by viewModel.allLogs.collectAsStateWithLifecycle(initialValue = emptyList())

    val currentActiveGroup = activeGroup

    val isDark by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val isAnimationEnabled by viewModel.isAnimationEnabled.collectAsStateWithLifecycle()
    val animationDurationSeconds by viewModel.animationDurationSeconds.collectAsStateWithLifecycle()
    val dashboardBackgroundUri by viewModel.dashboardBackgroundUri.collectAsStateWithLifecycle()
    val landscapeColumns by viewModel.landscapeColumns.collectAsStateWithLifecycle()
    val animationEasingType by viewModel.animationEasingType.collectAsStateWithLifecycle()
    val isAestheticDashboardEnabled by viewModel.isAestheticDashboardEnabled.collectAsStateWithLifecycle()
    val expandedItemIds by viewModel.expandedItemIds.collectAsStateWithLifecycle()
    val isBrighterText by viewModel.isBrighterTextEnabled.collectAsStateWithLifecycle()
    val copiedItem by viewModel.copiedItem.collectAsStateWithLifecycle()

    val dashboardBgPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            viewModel.setDashboardBackgroundUri(uri.toString())
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var currentTab by remember { mutableStateOf(0) } // 0 = Goals, 1 = Settings/Options

    var showAddDialog by remember { mutableStateOf(false) }
    var showCreditsDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editTargetGoal by remember { mutableStateOf<Goal?>(null) }
    var showLandscapeMenu by remember { mutableStateOf(false) }
    var showAnimationDurationMenu by remember { mutableStateOf(false) }
    var showAnimationEasingMenu by remember { mutableStateOf(false) }
    var showAdjustDialog by remember { mutableStateOf(false) }
    var adjustTargetGoal by remember { mutableStateOf<Goal?>(null) }
    var isAdjustmentAddition by remember { mutableStateOf(true) }
    var focusedGoalForStats by remember { mutableStateOf<Goal?>(null) }
    var showSnapshotDialog by remember { mutableStateOf(false) }
    var isGeneratingPng by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Notes dialog triggers
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var showEditNoteDialog by remember { mutableStateOf(false) }
    var editTargetNote by remember { mutableStateOf<Note?>(null) }

    // Groups dialog triggers
    var showAddGroupDialog by remember { mutableStateOf(false) }
    var showEditGroupDialog by remember { mutableStateOf(false) }
    var editTargetGroup by remember { mutableStateOf<GoalGroup?>(null) }
    var resetTargetGoal by remember { mutableStateOf<Goal?>(null) }
    var completeTargetGoal by remember { mutableStateOf<Goal?>(null) }
    var resetTargetGroup by remember { mutableStateOf<GoalGroup?>(null) }
    var groupStatsTarget by remember { mutableStateOf<GoalGroup?>(null) }

    var showFabOptions by remember { mutableStateOf(false) }
    var isSortingModeEnabled by remember { mutableStateOf(false) }
    var selectedItemForMove by remember { mutableStateOf<DashboardItem?>(null) }
    LaunchedEffect(isSortingModeEnabled) {
        if (!isSortingModeEnabled) {
            selectedItemForMove = null
        }
    }
    var showCreateChoiceDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val backupContext = context
    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
        onResult = { uri ->
            if (uri != null) {
                viewModel.exportBackup(
                    uri = uri,
                    onSuccess = {
                        Toast.makeText(backupContext, "Backup exported successfully!", Toast.LENGTH_LONG).show()
                    },
                    onError = { err ->
                        Toast.makeText(backupContext, "Export failed: $err", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    )

    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var showImportBackupDialog by remember { mutableStateOf(false) }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                pendingImportUri = uri
                showImportBackupDialog = true
            }
        }
    )

    val currentGroupId = groupNavigationStack.lastOrNull() ?: 0

    val dashboardItems = remember(goals, notes, groups) {
        val currentGoals = goals.map { DashboardItem.GoalItem(it) }
        val currentNotes = notes.map { DashboardItem.NoteItem(it) }
        val currentGroups = groups.map { DashboardItem.GroupItem(it) }
        
        val sortedGroups = currentGroups.sortedByDescending { it.createdAt }
        val sortedOthers = (currentGoals + currentNotes).sortedByDescending { it.createdAt }
        
        sortedGroups + sortedOthers
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = if (isDark) Color(0xFF0F0E13) else Color(0xFFF6F5F8),
        bottomBar = {
            NavigationBar(
                containerColor = if (isDark) Color(0xFF1C1B22) else Color(0xFFF3EDF7),
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.List, contentDescription = "Goals") },
                    label = { Text("Goals") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF21005D),
                        unselectedIconColor = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F),
                        selectedTextColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF21005D),
                        unselectedTextColor = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F),
                        indicatorColor = if (isDark) Color(0xFF381E72) else Color(0xFFE8DEF8)
                    )
                )
                val currentCopied = copiedItem
                if (currentCopied == null) {
                    NavigationBarItem(
                        selected = false,
                        onClick = { showCreateChoiceDialog = true },
                        icon = { Icon(Icons.Default.AddCircle, contentDescription = "New Goal", modifier = Modifier.size(26.dp)) },
                        label = { Text("New Goal") },
                        colors = NavigationBarItemDefaults.colors(
                            unselectedIconColor = if (isDark) Color(0xFFBB86FC) else Color(0xFF6750A4),
                            unselectedTextColor = if (isDark) Color(0xFFBB86FC) else Color(0xFF6750A4)
                        ),
                        modifier = Modifier.testTag("nav_new_goal_button")
                    )
                } else {
                    val pasteLabel = when (currentCopied) {
                        is CopiedItem.CopiedGoal -> "Paste Goal"
                        is CopiedItem.CopiedNote -> "Paste Note"
                        is CopiedItem.CopiedGroup -> "Paste Group"
                    }
                    NavigationBarItem(
                        selected = true,
                        onClick = {
                            viewModel.pasteCopiedItem(currentGroupId)
                            Toast.makeText(context, "$pasteLabel placed at top of page", Toast.LENGTH_SHORT).show()
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = pasteLabel,
                                modifier = Modifier.size(26.dp)
                            )
                        },
                        label = { Text(pasteLabel) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32),
                            selectedTextColor = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32),
                            indicatorColor = if (isDark) Color(0xFF1E3524) else Color(0xFFE8F5E9)
                        ),
                        modifier = Modifier.testTag("nav_paste_button")
                    )
                }
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF21005D),
                        unselectedIconColor = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F),
                        selectedTextColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF21005D),
                        unselectedTextColor = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F),
                        indicatorColor = if (isDark) Color(0xFF381E72) else Color(0xFFE8DEF8)
                    )
                )
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (currentTab == 0) {
                if (currentActiveGroup != null && currentActiveGroup.imageUri != null && currentActiveGroup.applyBg) {
                    AsyncImage(
                        model = currentActiveGroup.imageUri,
                        contentDescription = "Group Background Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = 0.1f
                    )
                } else if (dashboardBackgroundUri != null) {
                    AsyncImage(
                        model = dashboardBackgroundUri,
                        contentDescription = "Dashboard Background Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = 0.1f
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxSize()
            ) {
            if (currentTab == 0) {
                if (currentGroupId == 0) {
                    // High Density Header with Integrated Hero Banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_flagpole_logo_1783178411745),
                                contentDescription = "Flagpole Logo",
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showCreditsDialog = true },
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Flagpole",
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontWeight = FontWeight.Normal,
                                        color = if (isDark) Color.White else Color(0xFF1D1B20),
                                        fontSize = 32.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Track your daily evolution",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F),
                                        fontSize = 14.sp
                                    )
                                )
                            }
                        }

                        // Decorative miniature preview and toggle sorting button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            IconButton(
                                onClick = { showSnapshotDialog = true },
                                modifier = Modifier
                                    .background(Color.Transparent, CircleShape)
                                    .size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Take Snapshot",
                                    tint = if (isDark) Color.White else Color(0xFF1D1B20)
                                )
                            }

                            IconButton(
                                onClick = { isSortingModeEnabled = !isSortingModeEnabled },
                                modifier = Modifier
                                    .background(
                                        color = if (isSortingModeEnabled) {
                                            if (isDark) Color(0xFF381E72) else Color(0xFFE8DEF8)
                                        } else {
                                            Color.Transparent
                                        },
                                        shape = CircleShape
                                    )
                                    .testTag("toggle_sorting_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Toggle Sorting Mode",
                                    tint = if (isSortingModeEnabled) {
                                        if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
                                    } else {
                                        if (isDark) Color.White else Color(0xFF1D1B20)
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Scrollable List of Goals, Notes, and Groups
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4),
                            modifier = Modifier.testTag("loading_indicator")
                        )
                    }
                } else if (isLandscape && landscapeColumns == 2) {
                    val staggeredGridState = rememberLazyStaggeredGridState()
                    LazyVerticalStaggeredGrid(
                        state = staggeredGridState,
                        columns = StaggeredGridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalItemSpacing = 12.dp
                    ) {
                        if (currentGroupId != 0 && currentActiveGroup != null) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { viewModel.goBack() },
                                        modifier = Modifier
                                            .background(if (isDark) Color(0xFF1C1B22) else Color(0xFFECE6F0), CircleShape)
                                            .size(40.dp)
                                            .testTag("group_back_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Back",
                                            tint = if (isDark) Color.White else Color(0xFF1D1B20)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    CompactGroupCard(
                                        group = currentActiveGroup,
                                        isDark = isDark,
                                        groupGoalStats = groupGoalStats,
                                        allGoals = allGoals,
                                        isSortingModeEnabled = isSortingModeEnabled,
                                        onToggleSorting = { isSortingModeEnabled = !isSortingModeEnabled },
                                        onTakeSnapshot = { showSnapshotDialog = true },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        if (currentGroupId == 0 && goals.isNotEmpty() && isAestheticDashboardEnabled) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                val totalGoals = goals.size
                                val completedGoals = goals.count { it.currentPoints >= it.maxPoints }
                                val totalProgress = goals.sumOf { it.currentPoints }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isDark) Color(0xFF1C1B22) else Color(0xFFFFFFFF)
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Aesthetic Dashboard",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) Color.White else Color(0xFF1D1B20)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Keep updating your progress daily!",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F)
                                            )
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                            // Badge 1: Completed
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    imageVector = Icons.Outlined.EmojiEvents,
                                                    contentDescription = "Completed",
                                                    tint = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Text(
                                                    text = "$completedGoals/$totalGoals",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isDark) Color.White else Color(0xFF1D1B20)
                                                )
                                                Text(
                                                    text = "Done",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F)
                                                )
                                            }

                                            // Badge 2: Total points
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Flag,
                                                    contentDescription = "Total points",
                                                    tint = if (isDark) Color(0xFF80DEEA) else Color(0xFF0288D1),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Text(
                                                    text = "$totalProgress",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isDark) Color.White else Color(0xFF1D1B20)
                                                )
                                                Text(
                                                    text = "Points",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (dashboardItems.isEmpty()) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(24.dp)
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.img_empty_state_1783080011920),
                                            contentDescription = "Empty Goals Illustration",
                                            modifier = Modifier
                                                .size(160.dp)
                                                .clip(RoundedCornerShape(16.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "No items here yet",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Click the 'New Goal' button below to define and start tracking objectives, notes, or groups!",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            items(
                                items = dashboardItems,
                                key = { item ->
                                    when (item) {
                                        is DashboardItem.GoalItem -> "goal_${item.goal.id}"
                                        is DashboardItem.NoteItem -> "note_${item.note.id}"
                                        is DashboardItem.GroupItem -> "group_${item.group.id}"
                                    }
                                },
                                span = { item ->
                                    val isExpanded = when (item) {
                                        is DashboardItem.GroupItem -> expandedItemIds.contains("group_${item.group.id}")
                                        is DashboardItem.GoalItem -> expandedItemIds.contains("goal_${item.goal.id}")
                                        is DashboardItem.NoteItem -> expandedItemIds.contains("note_${item.note.id}")
                                    }
                                    if (isExpanded) StaggeredGridItemSpan.FullLine else StaggeredGridItemSpan.SingleLane
                                }
                            ) { item ->
                                val outlineColor = if (currentActiveGroup?.outlineMainColor == true) currentActiveGroup.colorHex else null
                                SortableCardContainer(
                                    item = item,
                                    isDark = isDark,
                                    isSortingModeEnabled = isSortingModeEnabled,
                                    currentGroupId = currentGroupId,
                                    viewModel = viewModel,
                                    selectedItemForMove = selectedItemForMove,
                                    onSelectItemForMove = { selectedItemForMove = it },
                                    onScrollBy = { amount -> staggeredGridState.scrollBy(amount) }
                                ) {
                                    when (item) {
                                        is DashboardItem.GoalItem -> {
                                            GoalCard(
                                                goal = item.goal,
                                                isDark = isDark,
                                                isAnimationEnabled = isAnimationEnabled,
                                                animationEasingType = animationEasingType,
                                                animationDurationSeconds = animationDurationSeconds,
                                                onIncrement = { viewModel.incrementPoints(item.goal) },
                                                onDecrement = { viewModel.decrementPoints(item.goal) },
                                                onHoldIncrement = {
                                                    adjustTargetGoal = item.goal
                                                    isAdjustmentAddition = true
                                                    showAdjustDialog = true
                                                },
                                                onHoldDecrement = {
                                                    adjustTargetGoal = item.goal
                                                    isAdjustmentAddition = false
                                                    showAdjustDialog = true
                                                },
                                                onEdit = {
                                                    editTargetGoal = item.goal
                                                    showEditDialog = true
                                                },
                                                onDuplicate = { viewModel.duplicateGoal(item.goal) },
                                                onCopy = {
                                                    viewModel.copyGoal(item.goal)
                                                    Toast.makeText(context, "Copied Goal! Tap 'Paste Goal' to place at top", Toast.LENGTH_SHORT).show()
                                                },
                                                onReset = { resetTargetGoal = item.goal },
                                                onComplete = { completeTargetGoal = item.goal },
                                                onDelete = { viewModel.deleteGoal(item.goal) },
                                                onInfoClick = { focusedGoalForStats = item.goal },
                                                outlineColorHex = if (item.goal.cardBorderHex != "NONE") item.goal.cardBorderHex else outlineColor,
                                                isExpanded = expandedItemIds.contains("goal_${item.goal.id}"),
                                                onToggleExpand = { viewModel.toggleItemExpanded("goal_${item.goal.id}") },
                                                isBrighterText = isBrighterText
                                            )
                                        }
                                        is DashboardItem.NoteItem -> {
                                            NoteCard(
                                                note = item.note,
                                                isDark = isDark,
                                                onEdit = {
                                                    editTargetNote = item.note
                                                    showEditNoteDialog = true
                                                },
                                                onDuplicate = { viewModel.duplicateNote(item.note) },
                                                onCopy = {
                                                    viewModel.copyNote(item.note)
                                                    Toast.makeText(context, "Copied Note! Tap 'Paste Note' to place at top", Toast.LENGTH_SHORT).show()
                                                },
                                                onDelete = { viewModel.deleteNote(item.note) },
                                                outlineColorHex = outlineColor,
                                                isExpanded = expandedItemIds.contains("note_${item.note.id}"),
                                                onToggleExpand = { viewModel.toggleItemExpanded("note_${item.note.id}") }
                                            )
                                        }
                                        is DashboardItem.GroupItem -> {
                                            GroupCard(
                                                group = item.group,
                                                isDark = isDark,
                                                groupGoalStats = groupGoalStats,
                                                allGoals = allGoals,
                                                onEnter = { viewModel.enterGroup(item.group.id) },
                                                onEdit = {
                                                    editTargetGroup = item.group
                                                    showEditGroupDialog = true
                                                },
                                                onDuplicate = { viewModel.duplicateGroup(item.group) },
                                                onCopy = {
                                                    viewModel.copyGroup(item.group)
                                                    Toast.makeText(context, "Copied Group! Tap 'Paste Group' to place at top", Toast.LENGTH_SHORT).show()
                                                },
                                                onResetGoals = { resetTargetGroup = item.group },
                                                onCompleteGoals = { viewModel.completeGroupGoals(item.group.id) },
                                                onDelete = { viewModel.deleteGroup(item.group) },
                                                outlineColorHex = outlineColor,
                                                isExpanded = expandedItemIds.contains("group_${item.group.id}"),
                                                onToggleExpand = { viewModel.toggleItemExpanded("group_${item.group.id}") },
                                                isBrighterText = isBrighterText,
                                                onShowStats = { groupStatsTarget = item.group }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    val listState = rememberLazyListState()
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (currentGroupId != 0 && currentActiveGroup != null) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { viewModel.goBack() },
                                        modifier = Modifier
                                            .background(if (isDark) Color(0xFF1C1B22) else Color(0xFFECE6F0), CircleShape)
                                            .size(40.dp)
                                            .testTag("group_back_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Back",
                                            tint = if (isDark) Color.White else Color(0xFF1D1B20)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    CompactGroupCard(
                                        group = currentActiveGroup,
                                        isDark = isDark,
                                        groupGoalStats = groupGoalStats,
                                        allGoals = allGoals,
                                        isSortingModeEnabled = isSortingModeEnabled,
                                        onToggleSorting = { isSortingModeEnabled = !isSortingModeEnabled },
                                        onTakeSnapshot = { showSnapshotDialog = true },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        if (currentGroupId == 0 && goals.isNotEmpty() && isAestheticDashboardEnabled) {
                            item {
                                val totalGoals = goals.size
                                val completedGoals = goals.count { it.currentPoints >= it.maxPoints }
                                val totalProgress = goals.sumOf { it.currentPoints }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isDark) Color(0xFF1C1B22) else Color(0xFFFFFFFF)
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Aesthetic Dashboard",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) Color.White else Color(0xFF1D1B20)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Keep updating your progress daily!",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F)
                                            )
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                            // Badge 1: Completed
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    imageVector = Icons.Outlined.EmojiEvents,
                                                    contentDescription = "Completed",
                                                    tint = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Text(
                                                    text = "$completedGoals/$totalGoals",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isDark) Color.White else Color(0xFF1D1B20)
                                                )
                                                Text(
                                                    text = "Done",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F)
                                                )
                                            }

                                            // Badge 2: Total points
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Flag,
                                                    contentDescription = "Total points",
                                                    tint = if (isDark) Color(0xFF80DEEA) else Color(0xFF0288D1),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Text(
                                                    text = "$totalProgress",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isDark) Color.White else Color(0xFF1D1B20)
                                                )
                                                Text(
                                                    text = "Points",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (dashboardItems.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(24.dp)
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.img_empty_state_1783080011920),
                                            contentDescription = "Empty Goals Illustration",
                                            modifier = Modifier
                                                .size(160.dp)
                                                .clip(RoundedCornerShape(16.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "No items here yet",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Click the 'New Goal' button below to define and start tracking objectives, notes, or groups!",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            items(
                                items = dashboardItems,
                                key = { item ->
                                    when (item) {
                                        is DashboardItem.GoalItem -> "goal_${item.goal.id}"
                                        is DashboardItem.NoteItem -> "note_${item.note.id}"
                                        is DashboardItem.GroupItem -> "group_${item.group.id}"
                                    }
                                }
                            ) { item ->
                                val outlineColor = if (currentActiveGroup?.outlineMainColor == true) currentActiveGroup.colorHex else null
                                Column(modifier = Modifier.fillMaxWidth()) {
                                SortableCardContainer(
                                    item = item,
                                    isDark = isDark,
                                    isSortingModeEnabled = isSortingModeEnabled,
                                    currentGroupId = currentGroupId,
                                    viewModel = viewModel,
                                    selectedItemForMove = selectedItemForMove,
                                    onSelectItemForMove = { selectedItemForMove = it },
                                    onScrollBy = { amount -> listState.scrollBy(amount) }
                                ) {
                                    when (item) {
                                        is DashboardItem.GoalItem -> {
                                            GoalCard(
                                                goal = item.goal,
                                                isDark = isDark,
                                                isAnimationEnabled = isAnimationEnabled,
                                                animationEasingType = animationEasingType,
                                                animationDurationSeconds = animationDurationSeconds,
                                                onIncrement = { viewModel.incrementPoints(item.goal) },
                                                onDecrement = { viewModel.decrementPoints(item.goal) },
                                                onHoldIncrement = {
                                                    adjustTargetGoal = item.goal
                                                    isAdjustmentAddition = true
                                                    showAdjustDialog = true
                                                },
                                                onHoldDecrement = {
                                                    adjustTargetGoal = item.goal
                                                    isAdjustmentAddition = false
                                                    showAdjustDialog = true
                                                },
                                                onEdit = {
                                                    editTargetGoal = item.goal
                                                    showEditDialog = true
                                                },
                                                onDuplicate = { viewModel.duplicateGoal(item.goal) },
                                                onCopy = {
                                                    viewModel.copyGoal(item.goal)
                                                    Toast.makeText(context, "Copied Goal! Tap 'Paste Goal' to place at top", Toast.LENGTH_SHORT).show()
                                                },
                                                onReset = { resetTargetGoal = item.goal },
                                                onComplete = { completeTargetGoal = item.goal },
                                                onDelete = { viewModel.deleteGoal(item.goal) },
                                                onInfoClick = { focusedGoalForStats = item.goal },
                                                outlineColorHex = if (item.goal.cardBorderHex != "NONE") item.goal.cardBorderHex else outlineColor,
                                                isExpanded = expandedItemIds.contains("goal_${item.goal.id}"),
                                                onToggleExpand = { viewModel.toggleItemExpanded("goal_${item.goal.id}") },
                                                isBrighterText = isBrighterText
                                            )
                                        }
                                        is DashboardItem.NoteItem -> {
                                            NoteCard(
                                                note = item.note,
                                                isDark = isDark,
                                                onEdit = {
                                                    editTargetNote = item.note
                                                    showEditNoteDialog = true
                                                },
                                                onDuplicate = { viewModel.duplicateNote(item.note) },
                                                onCopy = {
                                                    viewModel.copyNote(item.note)
                                                    Toast.makeText(context, "Copied Note! Tap 'Paste Note' to place at top", Toast.LENGTH_SHORT).show()
                                                },
                                                onDelete = { viewModel.deleteNote(item.note) },
                                                outlineColorHex = outlineColor,
                                                isExpanded = expandedItemIds.contains("note_${item.note.id}"),
                                                onToggleExpand = { viewModel.toggleItemExpanded("note_${item.note.id}") }
                                            )
                                        }
                                        is DashboardItem.GroupItem -> {
                                            GroupCard(
                                                group = item.group,
                                                isDark = isDark,
                                                groupGoalStats = groupGoalStats,
                                                allGoals = allGoals,
                                                onEnter = { viewModel.enterGroup(item.group.id) },
                                                onEdit = {
                                                    editTargetGroup = item.group
                                                    showEditGroupDialog = true
                                                },
                                                onDuplicate = { viewModel.duplicateGroup(item.group) },
                                                onCopy = {
                                                    viewModel.copyGroup(item.group)
                                                    Toast.makeText(context, "Copied Group! Tap 'Paste Group' to place at top", Toast.LENGTH_SHORT).show()
                                                },
                                                onResetGoals = { resetTargetGroup = item.group },
                                                onCompleteGoals = { viewModel.completeGroupGoals(item.group.id) },
                                                onDelete = { viewModel.deleteGroup(item.group) },
                                                outlineColorHex = outlineColor,
                                                isExpanded = expandedItemIds.contains("group_${item.group.id}"),
                                                onToggleExpand = { viewModel.toggleItemExpanded("group_${item.group.id}") },
                                                isBrighterText = isBrighterText,
                                                onShowStats = { groupStatsTarget = item.group }
                                            )
                                        }
                                    }
                                }
                                }
                            }
                        }
                    }
                }
            } else {
                // Settings Tab View
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Normal,
                            color = if (isDark) Color.White else Color(0xFF1D1B20),
                            fontSize = 32.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Customize your app experience",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F),
                            fontSize = 14.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                     Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF1C1B22) else Color(0xFFFFFFFF)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Theme Selection",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isDark) Color.White else Color(0xFF1D1B20)
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isDark) "Dark mode is active" else "Light mode is active",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F)
                                        )
                                    )
                                }

                                Switch(
                                    checked = isDark,
                                    onCheckedChange = { viewModel.setDarkTheme(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = if (isDark) Color(0xFFBB86FC) else Color(0xFF6750A4),
                                        uncheckedThumbColor = Color(0xFF79747E),
                                        uncheckedTrackColor = if (isDark) Color(0xFF2C2A30) else Color(0xFFE7E0EC)
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF1C1B22) else Color(0xFFFFFFFF)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Aesthetic Dashboard",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isDark) Color.White else Color(0xFF1D1B20)
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isAestheticDashboardEnabled) "Dashboard progress header is visible" else "Dashboard progress header is hidden",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F)
                                        )
                                    )
                                }

                                Switch(
                                    checked = isAestheticDashboardEnabled,
                                    onCheckedChange = { viewModel.setAestheticDashboardEnabled(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = if (isDark) Color(0xFFBB86FC) else Color(0xFF6750A4),
                                        uncheckedThumbColor = Color(0xFF79747E),
                                        uncheckedTrackColor = if (isDark) Color(0xFF2C2A30) else Color(0xFFE7E0EC)
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF1C1B22) else Color(0xFFFFFFFF)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Landscape Layout",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isDark) Color.White else Color(0xFF1D1B20)
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (landscapeColumns == 1) "1 column (Stretched)" else "2 columns (Compact)",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F)
                                        )
                                    )
                                }

                                Box {
                                    Button(
                                        onClick = { showLandscapeMenu = true },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isDark) Color(0xFF381E72) else Color(0xFFE8DEF8),
                                            contentColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF21005D)
                                        )
                                    ) {
                                        Text("Options")
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }

                                    DropdownMenu(
                                        expanded = showLandscapeMenu,
                                        onDismissRequest = { showLandscapeMenu = false },
                                        modifier = Modifier.background(if (isDark) Color(0xFF1E1C24) else Color(0xFFF3EDF7))
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("1 Column") },
                                            onClick = {
                                                viewModel.setLandscapeColumns(1)
                                                showLandscapeMenu = false
                                            },
                                            colors = MenuDefaults.itemColors(
                                                textColor = if (isDark) Color.White else Color(0xFF1D1B20)
                                            )
                                        )
                                        DropdownMenuItem(
                                            text = { Text("2 Columns") },
                                            onClick = {
                                                viewModel.setLandscapeColumns(2)
                                                showLandscapeMenu = false
                                            },
                                            colors = MenuDefaults.itemColors(
                                                textColor = if (isDark) Color.White else Color(0xFF1D1B20)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF1C1B22) else Color(0xFFFFFFFF)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Gauge Animation",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isDark) Color.White else Color(0xFF1D1B20)
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isAnimationEnabled) "Animated changes enabled (5s)" else "Instant changes",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F)
                                        )
                                    )
                                }

                                Switch(
                                    checked = isAnimationEnabled,
                                    onCheckedChange = { viewModel.setAnimationEnabled(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = if (isDark) Color(0xFFBB86FC) else Color(0xFF6750A4),
                                        uncheckedThumbColor = Color(0xFF79747E),
                                        uncheckedTrackColor = if (isDark) Color(0xFF2C2A30) else Color(0xFFE7E0EC)
                                    )
                                )
                            }
                        }
                    }

                    if (isAnimationEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) Color(0xFF1C1B22) else Color(0xFFFFFFFF)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Animation Type",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isDark) Color.White else Color(0xFF1D1B20)
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        val typeLabel = when (animationEasingType) {
                                            0 -> "Fast at first, then slower"
                                            1 -> "Slow at first, then fast last 2s"
                                            else -> "Constant growing (5s)"
                                        }
                                        Text(
                                            text = typeLabel,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F)
                                            )
                                        )
                                    }

                                    Box {
                                        Button(
                                            onClick = { showAnimationEasingMenu = true },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isDark) Color(0xFF381E72) else Color(0xFFE8DEF8),
                                                contentColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF21005D)
                                            )
                                        ) {
                                            Text("Choices")
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }

                                        DropdownMenu(
                                            expanded = showAnimationEasingMenu,
                                            onDismissRequest = { showAnimationEasingMenu = false },
                                            modifier = Modifier.background(if (isDark) Color(0xFF1E1C24) else Color(0xFFF3EDF7))
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Fast at first, then slower") },
                                                onClick = {
                                                    viewModel.setAnimationEasingType(0)
                                                    showAnimationEasingMenu = false
                                                },
                                                colors = MenuDefaults.itemColors(
                                                    textColor = if (isDark) Color.White else Color(0xFF1D1B20)
                                                )
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Slow at first, then growing fast") },
                                                onClick = {
                                                    viewModel.setAnimationEasingType(1)
                                                    showAnimationEasingMenu = false
                                                },
                                                colors = MenuDefaults.itemColors(
                                                    textColor = if (isDark) Color.White else Color(0xFF1D1B20)
                                                )
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Constant growing") },
                                                onClick = {
                                                    viewModel.setAnimationEasingType(2)
                                                    showAnimationEasingMenu = false
                                                },
                                                colors = MenuDefaults.itemColors(
                                                    textColor = if (isDark) Color.White else Color(0xFF1D1B20)
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF1C1B22) else Color(0xFFFFFFFF)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Text(
                                text = "Dashboard Background Image",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color.White else Color(0xFF1D1B20)
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (dashboardBackgroundUri != null) "Custom background is active" else "Default slate background is active",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F)
                                )
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            if (dashboardBackgroundUri != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = dashboardBackgroundUri,
                                        contentDescription = "Background Thumbnail",
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { dashboardBgPickerLauncher.launch("image/*") },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isDark) Color(0xFF381E72) else Color(0xFFE8DEF8),
                                                contentColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF21005D)
                                            )
                                        ) {
                                            Text("Change Image")
                                        }
                                        TextButton(
                                            onClick = { viewModel.setDashboardBackgroundUri(null) },
                                            colors = ButtonDefaults.textButtonColors(
                                                contentColor = MaterialTheme.colorScheme.error
                                            )
                                        ) {
                                            Text("Remove Background")
                                        }
                                    }
                                }
                            } else {
                                Button(
                                    onClick = { dashboardBgPickerLauncher.launch("image/*") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isDark) Color(0xFF381E72) else Color(0xFFE8DEF8),
                                        contentColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF21005D)
                                    )
                                ) {
                                    Icon(Icons.Default.Image, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Choose Background Image")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF1C1B22) else Color(0xFFFFFFFF)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Text(
                                text = "Data Management",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color.White else Color(0xFF1D1B20)
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Export or import your settings, goals, notes, groups, and associated images.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F)
                                )
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { exportBackupLauncher.launch("flagpole_backup_${System.currentTimeMillis()}.zip") },
                                    modifier = Modifier.weight(1f).height(48.dp).testTag("export_backup_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isDark) Color(0xFFBB86FC) else Color(0xFF6750A4),
                                        contentColor = if (isDark) Color.Black else Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Export Data", style = MaterialTheme.typography.labelLarge)
                                }
                                
                                Button(
                                    onClick = { importBackupLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
                                    modifier = Modifier.weight(1f).height(48.dp).testTag("import_backup_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isDark) Color(0xFF381E72) else Color(0xFFE8DEF8),
                                        contentColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF21005D)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Publish, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Import Data", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCreditsDialog = true }
                            .testTag("credits_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF1C1B22) else Color(0xFFFFFFFF)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Credits & Developers",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDark) Color.White else Color(0xFF1D1B20)
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Meet the brilliant minds behind Flagpole",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F)
                                    )
                                )
                            }
                            Image(
                                painter = painterResource(id = R.drawable.ic_flagpole_logo_1783178411745),
                                contentDescription = "Credits",
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }
        }

        // Choice Dialog for New Item Creation
        if (showCreateChoiceDialog) {
            AlertDialog(
                onDismissRequest = { showCreateChoiceDialog = false },
                title = {
                    Text(
                        text = "Create New Item",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Choose what you'd like to add to this dashboard:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                        
                        // Create Goal Option
                        Button(
                            onClick = {
                                showCreateChoiceDialog = false
                                showAddDialog = true
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("dialog_choice_goal"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) Color(0xFFBB86FC) else Color(0xFF6750A4),
                                contentColor = if (isDark) Color.Black else Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Add New Goal", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                        }

                        // Create Note Option
                        Button(
                            onClick = {
                                showCreateChoiceDialog = false
                                showAddNoteDialog = true
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("dialog_choice_note"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) Color(0xFFE8DEF8) else Color(0xFFE8DEF8),
                                contentColor = if (isDark) Color(0xFF21005D) else Color(0xFF21005D)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Add New Note", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                        }

                        // Create Group Option
                        Button(
                            onClick = {
                                showCreateChoiceDialog = false
                                showAddGroupDialog = true
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("dialog_choice_group"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) Color(0xFFECE6F0) else Color(0xFFECE6F0),
                                contentColor = if (isDark) Color(0xFF1D1B20) else Color(0xFF1D1B20)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Add New Group", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showCreateChoiceDialog = false }) {
                        Text("Cancel")
                    }
                },
                containerColor = if (isDark) Color(0xFF1C1B22) else Color(0xFFFFFFFF),
                shape = RoundedCornerShape(28.dp)
            )
        }

        if (isGeneratingPng) {
            AlertDialog(
                onDismissRequest = {},
                confirmButton = {},
                title = {
                    Text(
                        "Generating PNG Document",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF1D1B20)
                    )
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Compiling all goals, groups, and notes into a high-quality landscape layout...",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F)
                        )
                    }
                },
                containerColor = if (isDark) Color(0xFF1C1B22) else Color(0xFFFFFFFF),
                shape = RoundedCornerShape(28.dp)
            )
        }

        // Snapshot Dialog
        if (showSnapshotDialog) {
            val context = androidx.compose.ui.platform.LocalContext.current
            SnapshotDialog(
                onDismiss = { showSnapshotDialog = false },
                onCapture = { quality, splitParts ->
                    showSnapshotDialog = false
                    isGeneratingPng = true
                    
                    scope.launch(kotlinx.coroutines.Dispatchers.Default) {
                        try {
                            val baseWidth = quality
                            val scale = baseWidth / 1080f
                            
                            val paddingLeft = 40f * scale
                            val paddingRight = 40f * scale
                            val columnGap = 32f * scale
                            val colWidth = (baseWidth - paddingLeft - paddingRight - columnGap) / 2f
                            
                            // Text paints
                            val titlePaint = android.text.TextPaint().apply {
                                color = if (isDark) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#1D1B20")
                                textSize = 32f * scale
                                isAntiAlias = true
                                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                            }
                            
                            val bodyPaint = android.text.TextPaint().apply {
                                color = if (isDark) android.graphics.Color.parseColor("#CAC4D0") else android.graphics.Color.parseColor("#49454F")
                                textSize = 22f * scale
                                isAntiAlias = true
                                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                            }
                            
                            val detailPaint = android.text.TextPaint().apply {
                                color = if (isDark) android.graphics.Color.parseColor("#D0BCFF") else android.graphics.Color.parseColor("#6750A4")
                                textSize = 20f * scale
                                isAntiAlias = true
                                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                            }
                            
                            val helperMeasureTextHeight = { text: String, width: Int, paint: android.text.TextPaint ->
                                if (text.isEmpty()) 0 else {
                                    val layout = android.text.StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
                                        .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                                        .setLineSpacing(0f, 1.15f)
                                        .setIncludePad(true)
                                        .build()
                                    layout.height
                                }
                            }
                            
                            val helperDrawText = { canvas: android.graphics.Canvas, text: String, x: Float, y: Float, width: Int, paint: android.text.TextPaint ->
                                val layout = android.text.StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
                                    .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                                    .setLineSpacing(0f, 1.15f)
                                    .setIncludePad(true)
                                    .build()
                                canvas.save()
                                canvas.translate(x, y)
                                layout.draw(canvas)
                                canvas.restore()
                                layout.height
                            }
                            
                            val helperGetCardColor = { scHex: String ->
                                if (scHex == "DEFAULT" || scHex == "NONE") {
                                    if (isDark) 0xFF1E1C24.toInt() else 0xFFFFFFFF.toInt()
                                } else {
                                    val baseColor = try {
                                        android.graphics.Color.parseColor(if (scHex.startsWith("#")) scHex else "#$scHex")
                                    } catch (e: Exception) {
                                        if (isDark) 0xFF1E1C24.toInt() else 0xFFFFFFFF.toInt()
                                    }
                                    val baseR = android.graphics.Color.red(baseColor) / 255f
                                    val baseG = android.graphics.Color.green(baseColor) / 255f
                                    val baseB = android.graphics.Color.blue(baseColor) / 255f
                                    if (isDark) {
                                        val tintAlpha = 0.05f
                                        val r = (1f - tintAlpha) * 0.12f + tintAlpha * baseR
                                        val g = (1f - tintAlpha) * 0.11f + tintAlpha * baseG
                                        val b = (1f - tintAlpha) * 0.15f + tintAlpha * baseB
                                        android.graphics.Color.rgb((r * 255).toInt(), (g * 255).toInt(), (b * 255).toInt())
                                    } else {
                                        val tintAlpha = 0.18f
                                        val r = (1f - tintAlpha) * 1.0f + tintAlpha * baseR
                                        val g = (1f - tintAlpha) * 1.0f + tintAlpha * baseG
                                        val b = (1f - tintAlpha) * 1.0f + tintAlpha * baseB
                                        android.graphics.Color.rgb((r * 255).toInt(), (g * 255).toInt(), (b * 255).toInt())
                                    }
                                }
                            }
                            
                            val parseColorHex = { hex: String, defaultColor: Int ->
                                if (hex == "DEFAULT" || hex == "NONE" || hex.isEmpty()) {
                                    defaultColor
                                } else {
                                    try {
                                        if (hex.startsWith("#")) {
                                            android.graphics.Color.parseColor(hex)
                                        } else {
                                            android.graphics.Color.parseColor("#$hex")
                                        }
                                    } catch (e: Exception) {
                                        defaultColor
                                    }
                                }
                            }
                            
                            val isItemExpanded = { item: DashboardItem ->
                                when (item) {
                                    is DashboardItem.GoalItem -> expandedItemIds.contains("goal_${item.goal.id}")
                                    is DashboardItem.NoteItem -> expandedItemIds.contains("note_${item.note.id}")
                                    is DashboardItem.GroupItem -> expandedItemIds.contains("group_${item.group.id}")
                                }
                            }
                            
                            // 1. Measure layout heights
                            val measuredItems = dashboardItems.map { item ->
                                val cardPadding = 32f * scale
                                val isExp = isItemExpanded(item)
                                val currentCardWidth = if (isExp) {
                                    baseWidth - paddingLeft - paddingRight
                                } else {
                                    colWidth
                                }
                                val cardInnerWidth = (currentCardWidth - 2 * cardPadding).toInt()
                                
                                val itemH = when (item) {
                                    is DashboardItem.GoalItem -> {
                                        val g = item.goal
                                        val titleH = helperMeasureTextHeight(g.title, cardInnerWidth, titlePaint)
                                        val descH = if (g.description.isNotEmpty()) helperMeasureTextHeight(g.description, cardInnerWidth, bodyPaint) + 12f * scale else 0f
                                        val progressH = when (g.progressType) {
                                            1 -> 160f * scale // Circle
                                            0 -> 66f * scale // Bar
                                            else -> { // Dots
                                                val dPerRow = if (g.dotsPerRow > 0) g.dotsPerRow else 8
                                                val dotSpacing = 8f * scale
                                                val availableW = currentCardWidth - 2 * cardPadding
                                                val dotRadius = ((availableW - (dPerRow - 1) * dotSpacing) / dPerRow) / 2f
                                                val clampedDotRadius = dotRadius.coerceIn(4f * scale, 16f * scale)
                                                val rowH = 2 * clampedDotRadius + dotSpacing
                                                val rows = kotlin.math.ceil(g.maxPoints.toFloat() / dPerRow).toInt()
                                                36f * scale + rows * rowH + 10f * scale
                                            }
                                        }
                                        val statsH = if (g.streaksEnabled || g.showRemainingCount) 40f * scale else 0f
                                        cardPadding * 2 + titleH + descH + progressH + statsH + 20f * scale
                                    }
                                    is DashboardItem.NoteItem -> {
                                        val n = item.note
                                        val textH = helperMeasureTextHeight(n.text, cardInnerWidth, bodyPaint)
                                        val imageH = if (n.imageUri != null) 240f * scale + 16f * scale else 0f
                                        cardPadding * 2 + textH + imageH + 10f * scale
                                    }
                                    is DashboardItem.GroupItem -> {
                                        val grp = item.group
                                        val titleH = helperMeasureTextHeight(grp.title, cardInnerWidth, titlePaint)
                                        val descH = if (grp.description.isNotEmpty()) helperMeasureTextHeight(grp.description, cardInnerWidth, bodyPaint) + 12f * scale else 0f
                                        val statsH = 40f * scale
                                        cardPadding * 2 + titleH + descH + statsH + 16f * scale
                                    }
                                }
                                item to itemH
                            }
                            
                            var currentY = 40f * scale
                            var headerHeight = 0f
                            
                            if (currentGroupId != 0 && currentActiveGroup != null) {
                                val cardPadding = 40f * scale
                                val titleH = helperMeasureTextHeight(currentActiveGroup.title, (baseWidth - paddingLeft - paddingRight - 2 * cardPadding).toInt(), titlePaint)
                                val descH = if (currentActiveGroup.description.isNotEmpty()) {
                                    helperMeasureTextHeight(currentActiveGroup.description, (baseWidth - paddingLeft - paddingRight - 2 * cardPadding).toInt(), bodyPaint) + 12f * scale
                                } else 0f
                                val progressH = 50f * scale
                                headerHeight = cardPadding * 2 + titleH + descH + progressH
                            } else if (currentGroupId == 0 && goals.isNotEmpty() && isAestheticDashboardEnabled) {
                                val cardPadding = 40f * scale
                                val titleH = helperMeasureTextHeight("Aesthetic Dashboard", (baseWidth - paddingLeft - paddingRight - 2 * cardPadding).toInt(), titlePaint)
                                val descH = helperMeasureTextHeight("Keep updating your progress daily!", (baseWidth - paddingLeft - paddingRight - 2 * cardPadding).toInt(), bodyPaint) + 12f * scale
                                val statsH = 60f * scale
                                headerHeight = cardPadding * 2 + titleH + descH + statsH
                            }
                            
                            if (headerHeight > 0f) {
                                currentY += headerHeight + 32f * scale
                            }
                            
                            var leftColY = currentY
                            var rightColY = currentY
                            
                            val placements = mutableListOf<Triple<DashboardItem, Float, Float>>()
                            measuredItems.forEach { (item, itemH) ->
                                val isExp = isItemExpanded(item)
                                if (isExp) {
                                    val targetY = kotlin.math.max(leftColY, rightColY)
                                    placements.add(Triple(item, paddingLeft, targetY))
                                    val nextY = targetY + itemH + 32f * scale
                                    leftColY = nextY
                                    rightColY = nextY
                                } else {
                                    if (leftColY <= rightColY) {
                                        placements.add(Triple(item, paddingLeft, leftColY))
                                        leftColY += itemH + 32f * scale
                                    } else {
                                        placements.add(Triple(item, paddingLeft + colWidth + columnGap, rightColY))
                                        rightColY += itemH + 32f * scale
                                    }
                                }
                            }
                            
                            val columnsMaxY = if (measuredItems.isEmpty()) currentY + 120f * scale else kotlin.math.max(leftColY, rightColY)
                            val footerH = 160f * scale
                            val finalBitmapHeight = (columnsMaxY + footerH).toInt()
                            
                            // 2. Allocate and draw standard Bitmap
                            val finalBitmap = android.graphics.Bitmap.createBitmap(baseWidth, finalBitmapHeight, android.graphics.Bitmap.Config.ARGB_8888)
                            val canvas = android.graphics.Canvas(finalBitmap)
                            
                            // Draw background
                            val baseBgColor = if (isDark) 0xFF0F0E13.toInt() else 0xFFF6F5F8.toInt()
                            canvas.drawColor(baseBgColor)
                            
                            val bgBitmap = try {
                                if (currentActiveGroup != null && currentActiveGroup.imageUri != null && currentActiveGroup.applyBg) {
                                    val uri = android.net.Uri.parse(currentActiveGroup.imageUri)
                                    context.contentResolver.openInputStream(uri).use { android.graphics.BitmapFactory.decodeStream(it) }
                                } else if (dashboardBackgroundUri != null) {
                                    val uri = android.net.Uri.parse(dashboardBackgroundUri)
                                    context.contentResolver.openInputStream(uri).use { android.graphics.BitmapFactory.decodeStream(it) }
                                } else null
                            } catch (e: Exception) {
                                null
                            }
                            
                            if (bgBitmap != null) {
                                val sX = baseWidth.toFloat() / bgBitmap.width
                                val sY = finalBitmapHeight.toFloat() / bgBitmap.height
                                val maxS = kotlin.math.max(sX, sY)
                                val newW = bgBitmap.width * maxS
                                val newH = bgBitmap.height * maxS
                                val leftOffset = (baseWidth - newW) / 2f
                                val topOffset = (finalBitmapHeight - newH) / 2f
                                
                                val bgPaint = android.graphics.Paint().apply {
                                    alpha = 25 // ~0.1 alpha
                                    isAntiAlias = true
                                    isFilterBitmap = true
                                }
                                canvas.drawBitmap(bgBitmap, null, android.graphics.RectF(leftOffset, topOffset, leftOffset + newW, topOffset + newH), bgPaint)
                                bgBitmap.recycle()
                            }
                            
                            // Paint configurations
                            val cardPaint = android.graphics.Paint().apply {
                                style = android.graphics.Paint.Style.FILL
                                isAntiAlias = true
                            }
                            
                            val shadowPaint = android.graphics.Paint().apply {
                                style = android.graphics.Paint.Style.FILL
                                isAntiAlias = true
                                color = if (isDark) 0x22000000 else 0x0C000000
                            }
                            
                            // Draw Header
                            var drawY = 40f * scale
                            if (headerHeight > 0f) {
                                val cardPadding = 40f * scale
                                val cardRect = android.graphics.RectF(paddingLeft, drawY, baseWidth - paddingRight, drawY + headerHeight)
                                
                                // Draw Card Shadow & Background
                                canvas.drawRoundRect(android.graphics.RectF(cardRect.left, cardRect.top + 4f * scale, cardRect.right, cardRect.bottom + 4f * scale), 24f * scale, 24f * scale, shadowPaint)
                                val headerBgColor = if (currentGroupId != 0 && currentActiveGroup != null) {
                                    helperGetCardColor(currentActiveGroup.secondaryColorHex)
                                } else {
                                    if (isDark) 0xFF1C1B22.toInt() else 0xFFFFFFFF.toInt()
                                }
                                cardPaint.color = headerBgColor
                                canvas.drawRoundRect(cardRect, 24f * scale, 24f * scale, cardPaint)
                                
                                // Draw Card Outline if group outline is enabled
                                val outlineHex = if (currentGroupId != 0 && currentActiveGroup != null && currentActiveGroup.outlineMainColor) currentActiveGroup.colorHex else null
                                if (outlineHex != null) {
                                    val strokeP = android.graphics.Paint().apply {
                                        color = parseColorHex(outlineHex, 0)
                                        style = android.graphics.Paint.Style.STROKE
                                        strokeWidth = 3f * scale
                                        isAntiAlias = true
                                    }
                                    canvas.drawRoundRect(cardRect, 24f * scale, 24f * scale, strokeP)
                                }
                                
                                if (currentGroupId != 0 && currentActiveGroup != null) {
                                    // Group Header Text
                                    var textY = drawY + cardPadding
                                    val titleH = helperDrawText(canvas, currentActiveGroup.title, paddingLeft + cardPadding, textY, (baseWidth - paddingLeft - paddingRight - 2 * cardPadding).toInt(), titlePaint)
                                    textY += titleH
                                    
                                    if (currentActiveGroup.description.isNotEmpty()) {
                                        val descH = helperDrawText(canvas, currentActiveGroup.description, paddingLeft + cardPadding, textY + 12f * scale, (baseWidth - paddingLeft - paddingRight - 2 * cardPadding).toInt(), bodyPaint)
                                        textY += descH + 12f * scale
                                    }
                                    
                                    // Stats progress bar
                                    val activeStats = groupGoalStats.find { it.parentGroupId == currentActiveGroup.id }
                                    if (activeStats != null) {
                                        val totalG = activeStats.totalGoals
                                        val compG = activeStats.completedGoals
                                        val isPointsTracking = currentActiveGroup.displayType == 1
                                        val childGoals = if (isPointsTracking) allGoals.filter { it.parentGroupId == currentActiveGroup.id } else emptyList()
                                        val maxTotalPoints = childGoals.sumOf { it.groupValue }
                                        val currentTotalPoints = childGoals.sumOf {
                                            if (it.maxPoints > 0) {
                                                val fraction = it.currentPoints.toDouble() / it.maxPoints.toDouble()
                                                if (it.allowGroupPointsOverflow) fraction * it.groupValue else minOf(it.groupValue, fraction * it.groupValue)
                                            } else 0.0
                                        }

                                        val labelText = if (isPointsTracking) {
                                            "Folder Progress: ${formatGroupPoint(currentTotalPoints)} / ${formatGroupPoint(maxTotalPoints)} group points"
                                        } else {
                                            "Folder Progress: $compG of $totalG objectives done"
                                        }
                                        helperDrawText(canvas, labelText, paddingLeft + cardPadding, textY + 16f * scale, (baseWidth - paddingLeft - paddingRight - 2 * cardPadding).toInt(), detailPaint)
                                        
                                        val barY = textY + 44f * scale
                                        val barW = (baseWidth - paddingLeft - paddingRight - 2 * cardPadding).toFloat()
                                        val trackR = android.graphics.RectF(paddingLeft + cardPadding, barY, paddingLeft + cardPadding + barW, barY + 12f * scale)
                                        cardPaint.color = if (isDark) 0xFF333038.toInt() else 0xFFE6E0E9.toInt()
                                        canvas.drawRoundRect(trackR, 6f * scale, 6f * scale, cardPaint)
                                        
                                        val ratio = if (isPointsTracking) {
                                            if (maxTotalPoints > 0.0) (currentTotalPoints / maxTotalPoints).toFloat().coerceIn(0f, 1f) else 0f
                                        } else {
                                            if (totalG > 0) (compG.toFloat() / totalG.toFloat()).coerceIn(0f, 1f) else 0f
                                        }
                                        if (ratio > 0f) {
                                            val fillW = (barW * ratio).coerceIn(0f, barW)
                                            val fillR = android.graphics.RectF(paddingLeft + cardPadding, barY, paddingLeft + cardPadding + fillW, barY + 12f * scale)
                                            cardPaint.color = parseColorHex(currentActiveGroup.colorHex, 0xFF6750A4.toInt())
                                            canvas.drawRoundRect(fillR, 6f * scale, 6f * scale, cardPaint)
                                        }
                                    }
                                } else {
                                    // Aesthetic Dashboard Header text
                                    var textY = drawY + cardPadding
                                    val titleH = helperDrawText(canvas, "Aesthetic Dashboard", paddingLeft + cardPadding, textY, (baseWidth - paddingLeft - paddingRight - 2 * cardPadding).toInt(), titlePaint)
                                    textY += titleH
                                    
                                    val descH = helperDrawText(canvas, "Keep updating your progress daily!", paddingLeft + cardPadding, textY + 12f * scale, (baseWidth - paddingLeft - paddingRight - 2 * cardPadding).toInt(), bodyPaint)
                                    textY += descH + 12f * scale
                                    
                                    val totalGoals = goals.size
                                    val completedGoals = goals.count { it.currentPoints >= it.maxPoints }
                                    val totalProgress = goals.sumOf { it.currentPoints }
                                    val statsText = "🏆 Completed: $completedGoals/$totalGoals  •  🚩 Total Points: $totalProgress"
                                    helperDrawText(canvas, statsText, paddingLeft + cardPadding, textY + 16f * scale, (baseWidth - paddingLeft - paddingRight - 2 * cardPadding).toInt(), detailPaint)
                                }
                            }
                            
                            // Draw Items
                            placements.forEach { (item, itemX, itemY) ->
                                val index = placements.indexOfFirst { it.first == item }
                                val itemH = measuredItems[index].second
                                val cardPadding = 32f * scale
                                val isExp = isItemExpanded(item)
                                val currentCardWidth = if (isExp) {
                                    baseWidth - paddingLeft - paddingRight
                                } else {
                                    colWidth
                                }
                                val cardInnerWidth = (currentCardWidth - 2 * cardPadding).toInt()
                                
                                val cardRect = android.graphics.RectF(itemX, itemY, itemX + currentCardWidth, itemY + itemH)
                                
                                // Shadow & Background
                                canvas.drawRoundRect(android.graphics.RectF(cardRect.left, cardRect.top + 3f * scale, cardRect.right, cardRect.bottom + 3f * scale), 20f * scale, 20f * scale, shadowPaint)
                                
                                val scHex = when (item) {
                                    is DashboardItem.GoalItem -> item.goal.secondaryColorHex
                                    is DashboardItem.NoteItem -> item.note.secondaryColorHex
                                    is DashboardItem.GroupItem -> item.group.secondaryColorHex
                                }
                                cardPaint.color = helperGetCardColor(scHex)
                                canvas.drawRoundRect(cardRect, 20f * scale, 20f * scale, cardPaint)
                                
                                // Border (accurate override calculation)
                                val borderDrawHex = when (item) {
                                    is DashboardItem.GoalItem -> {
                                        val g = item.goal
                                        if (g.cardBorderHex != "NONE") {
                                            g.cardBorderHex
                                        } else {
                                            val pg = allGroups.find { it.id == g.parentGroupId }
                                            if (pg?.outlineMainColor == true) pg.colorHex else null
                                        }
                                    }
                                    is DashboardItem.NoteItem -> {
                                        val n = item.note
                                        if (n.cardBorderHex != "NONE") {
                                            n.cardBorderHex
                                        } else {
                                            val pg = allGroups.find { it.id == n.parentGroupId }
                                            if (pg?.outlineMainColor == true) pg.colorHex else null
                                        }
                                    }
                                    is DashboardItem.GroupItem -> {
                                        val grp = item.group
                                        if (grp.cardBorderHex != "NONE") {
                                            grp.cardBorderHex
                                        } else {
                                            null
                                        }
                                    }
                                }
                                if (borderDrawHex != null) {
                                    val strokeP = android.graphics.Paint().apply {
                                        color = parseColorHex(borderDrawHex, 0)
                                        style = android.graphics.Paint.Style.STROKE
                                        strokeWidth = 3f * scale
                                        isAntiAlias = true
                                    }
                                    canvas.drawRoundRect(cardRect, 20f * scale, 20f * scale, strokeP)
                                }
                                
                                when (item) {
                                    is DashboardItem.GoalItem -> {
                                        val g = item.goal
                                        var textY = itemY + cardPadding
                                        
                                        // Title
                                        val titleH = helperDrawText(canvas, g.title, itemX + cardPadding, textY, cardInnerWidth, titlePaint)
                                        textY += titleH
                                        
                                        // Description
                                        if (g.description.isNotEmpty()) {
                                            val descH = helperDrawText(canvas, g.description, itemX + cardPadding, textY + 12f * scale, cardInnerWidth, bodyPaint)
                                            textY += descH + 12f * scale
                                        }
                                        
                                        val pointsColor = parseColorHex(g.colorHex, if (isDark) android.graphics.Color.WHITE else 0xFF6750A4.toInt())
                                        val pointsPaint = android.text.TextPaint().apply {
                                            color = pointsColor
                                            textSize = 22f * scale
                                            isAntiAlias = true
                                            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                                        }
                                        val maxPointsPaint = android.text.TextPaint().apply {
                                            color = if (isDark) 0xFFCAC4D0.toInt() else 0xFF49454F.toInt()
                                            textSize = 14f * scale
                                            isAntiAlias = true
                                            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                                        }
                                        val percentPaint = android.text.TextPaint().apply {
                                            color = pointsColor
                                            textSize = 13f * scale
                                            isAntiAlias = true
                                            textAlign = android.graphics.Paint.Align.RIGHT
                                            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                                        }
                                        
                                        // Progress Display
                                        val progressY = textY + 16f * scale
                                        when (g.progressType) {
                                            1 -> { // Circle Meter
                                                val arcCX = itemX + currentCardWidth / 2f
                                                val arcCY = progressY + 80f * scale
                                                val rad = 56f * scale
                                                val arcR = android.graphics.RectF(arcCX - rad, arcCY - rad, arcCX + rad, arcCY + rad)
                                                
                                                val arcPaint = android.graphics.Paint().apply {
                                                    style = android.graphics.Paint.Style.STROKE
                                                    strokeWidth = g.meterThickness * scale
                                                    isAntiAlias = true
                                                    strokeCap = android.graphics.Paint.Cap.ROUND
                                                }
                                                // Background arc
                                                arcPaint.color = if (isDark) 0xFF333038.toInt() else 0xFFE6E0E9.toInt()
                                                canvas.drawArc(arcR, -225f, 270f, false, arcPaint)
                                                
                                                // Fill arc
                                                if (g.maxPoints > 0) {
                                                     val ratio = (g.currentPoints.toFloat() / g.maxPoints.toFloat()).coerceIn(0f, 1f)
                                                     val sweep = (270f * ratio).coerceIn(0f, 270f)
                                                     if (sweep > 0f) {
                                                         arcPaint.color = parseColorHex(g.colorHex, 0xFF6750A4.toInt())
                                                         canvas.drawArc(arcR, -225f, sweep, false, arcPaint)
                                                     }
                                                }
                                                
                                                // Center text
                                                val centerP = android.text.TextPaint().apply {
                                                    color = pointsColor
                                                    textSize = 24f * scale
                                                    textAlign = android.graphics.Paint.Align.CENTER
                                                    isAntiAlias = true
                                                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                                                }
                                                canvas.drawText(g.currentPoints.toString() + "/" + g.maxPoints.toString(), arcCX, arcCY - 2f * scale, centerP)
                                                centerP.color = if (isDark) 0xFFCAC4D0.toInt() else 0xFF49454F.toInt()
                                                centerP.textSize = 14f * scale
                                                centerP.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                                                canvas.drawText(g.unit, arcCX, arcCY + 18f * scale, centerP)
                                                
                                                textY += 160f * scale
                                            }
                                            0 -> { // Horizontal Bar
                                                // Draw metrics row above the bar
                                                val textX = itemX + cardPadding
                                                val currentPointsStr = g.currentPoints.toString() + " "
                                                canvas.drawText(currentPointsStr, textX, progressY + 20f * scale, pointsPaint)
                                                
                                                val curW = pointsPaint.measureText(currentPointsStr)
                                                val maxPointsStr = "/ " + g.maxPoints.toString() + " " + g.unit
                                                canvas.drawText(maxPointsStr, textX + curW, progressY + 18f * scale, maxPointsPaint)
                                                
                                                val pct = if (g.maxPoints > 0) {
                                                    ((g.currentPoints.toFloat() / g.maxPoints.toFloat()) * 100).toInt()
                                                } else 0
                                                canvas.drawText(pct.toString() + "%", itemX + currentCardWidth - cardPadding, progressY + 18f * scale, percentPaint)
                                                
                                                // Draw the progress bar below the metrics row
                                                val barY = progressY + 34f * scale
                                                val barW = (currentCardWidth - 2 * cardPadding).toFloat()
                                                val trackR = android.graphics.RectF(itemX + cardPadding, barY, itemX + cardPadding + barW, barY + 14f * scale)
                                                cardPaint.color = if (isDark) 0xFF333038.toInt() else 0xFFE6E0E9.toInt()
                                                canvas.drawRoundRect(trackR, 7f * scale, 7f * scale, cardPaint)
                                                
                                                if (g.maxPoints > 0) {
                                                    val ratio = (g.currentPoints.toFloat() / g.maxPoints.toFloat()).coerceIn(0f, 1f)
                                                    val fillW = (barW * ratio).coerceIn(0f, barW)
                                                    if (fillW > 0) {
                                                        val fillR = android.graphics.RectF(itemX + cardPadding, barY, itemX + cardPadding + fillW, barY + 14f * scale)
                                                        cardPaint.color = parseColorHex(g.colorHex, 0xFF6750A4.toInt())
                                                        canvas.drawRoundRect(fillR, 7f * scale, 7f * scale, cardPaint)
                                                    }
                                                }
                                                
                                                textY += 66f * scale
                                            }
                                            else -> { // Big Dots
                                                // Draw metrics row above dots
                                                val textX = itemX + cardPadding
                                                val currentPointsStr = g.currentPoints.toString() + " "
                                                canvas.drawText(currentPointsStr, textX, progressY + 20f * scale, pointsPaint)
                                                
                                                val curW = pointsPaint.measureText(currentPointsStr)
                                                val maxPointsStr = "/ " + g.maxPoints.toString() + " " + g.unit
                                                canvas.drawText(maxPointsStr, textX + curW, progressY + 18f * scale, maxPointsPaint)
                                                
                                                val pct = if (g.maxPoints > 0) ((g.currentPoints.toFloat() / g.maxPoints.toFloat()) * 100).toInt() else 0
                                                canvas.drawText(pct.toString() + "%", itemX + currentCardWidth - cardPadding, progressY + 18f * scale, percentPaint)
                                                
                                                // Draw dots grid below the metrics row
                                                val dotsStartY = progressY + 36f * scale
                                                val totalDots = g.maxPoints
                                                val dPerRow = if (g.dotsPerRow > 0) g.dotsPerRow else 8
                                                
                                                // Compute dot size based on card width
                                                val dotSpacing = 8f * scale
                                                val availableW = currentCardWidth - 2 * cardPadding
                                                val dotRadius = ((availableW - (dPerRow - 1) * dotSpacing) / dPerRow) / 2f
                                                val clampedDotRadius = dotRadius.coerceIn(4f * scale, 16f * scale)
                                                
                                                val rowH = 2 * clampedDotRadius + dotSpacing
                                                
                                                for (i in 0 until totalDots) {
                                                    val c = i % dPerRow
                                                    val r = i / dPerRow
                                                    val dx = itemX + cardPadding + c * (2 * clampedDotRadius + dotSpacing) + clampedDotRadius
                                                    val dy = dotsStartY + r * rowH + clampedDotRadius
                                                     
                                                    val dotPaint = android.graphics.Paint().apply { isAntiAlias = true }
                                                    if (i < g.currentPoints) {
                                                        dotPaint.color = parseColorHex(g.colorHex, 0xFF6750A4.toInt())
                                                        dotPaint.style = android.graphics.Paint.Style.FILL
                                                        canvas.drawCircle(dx.toFloat(), dy.toFloat(), clampedDotRadius, dotPaint)
                                                    } else {
                                                        dotPaint.color = if (isDark) 0xFF49454F.toInt() else 0xFFCAC4D0.toInt()
                                                        dotPaint.style = android.graphics.Paint.Style.STROKE
                                                        dotPaint.strokeWidth = 2f * scale
                                                        canvas.drawCircle(dx.toFloat(), dy.toFloat(), clampedDotRadius, dotPaint)
                                                    }
                                                }
                                                val rows = kotlin.math.ceil(totalDots.toFloat() / dPerRow).toInt()
                                                textY += 36f * scale + rows * rowH + 10f * scale
                                            }
                                        }
                                        
                                        // Streak / stats footer
                                        if (g.streaksEnabled || g.showRemainingCount) {
                                            var footerStr = ""
                                            if (g.streaksEnabled) {
                                                footerStr += "🔥 Active Streak  "
                                            }
                                            if (g.showRemainingCount) {
                                                val remaining = kotlin.math.max(0, g.maxPoints - g.currentPoints)
                                                footerStr += "•  " + remaining.toString() + " remaining"
                                            }
                                            helperDrawText(canvas, footerStr, itemX + cardPadding, textY + 14f * scale, cardInnerWidth, bodyPaint)
                                        }
                                    }
                                    is DashboardItem.NoteItem -> {
                                        val n = item.note
                                        var textY = itemY + cardPadding
                                        val textH = helperDrawText(canvas, n.text, itemX + cardPadding, textY, cardInnerWidth, bodyPaint)
                                        textY += textH
                                        
                                        if (n.imageUri != null) {
                                            val noteImg = try {
                                                context.contentResolver.openInputStream(android.net.Uri.parse(n.imageUri))?.use {
                                                    android.graphics.BitmapFactory.decodeStream(it)
                                                }
                                            } catch (e: Exception) {
                                                null
                                            }
                                            
                                            if (noteImg != null) {
                                                val targetRatio = cardInnerWidth.toFloat() / (240f * scale)
                                                val noteRatio = noteImg.width.toFloat() / noteImg.height.toFloat()
                                                val srcR = if (noteRatio > targetRatio) {
                                                    val subW = noteImg.height * targetRatio
                                                    val offset = (noteImg.width - subW) / 2f
                                                    android.graphics.Rect(offset.toInt(), 0, (offset + subW).toInt(), noteImg.height)
                                                } else {
                                                    val subH = noteImg.width / targetRatio
                                                    val offset = (noteImg.height - subH) / 2f
                                                    android.graphics.Rect(0, offset.toInt(), noteImg.width, (offset + subH).toInt())
                                                }
                                                
                                                val destR = android.graphics.RectF(
                                                    itemX + cardPadding,
                                                    textY + 16f * scale,
                                                    itemX + cardPadding + cardInnerWidth,
                                                    textY + 16f * scale + 240f * scale
                                                )
                                                
                                                val path = android.graphics.Path().apply {
                                                    addRoundRect(destR, 16f * scale, 16f * scale, android.graphics.Path.Direction.CW)
                                                }
                                                canvas.save()
                                                canvas.clipPath(path)
                                                canvas.drawBitmap(noteImg, srcR, destR, android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG))
                                                canvas.restore()
                                                noteImg.recycle()
                                            }
                                        }
                                    }
                                    is DashboardItem.GroupItem -> {
                                        val grp = item.group
                                        var textY = itemY + cardPadding
                                        val titleH = helperDrawText(canvas, grp.title, itemX + cardPadding, textY, cardInnerWidth, titlePaint)
                                        textY += titleH
                                        
                                        if (grp.description.isNotEmpty()) {
                                            val descH = helperDrawText(canvas, grp.description, itemX + cardPadding, textY + 12f * scale, cardInnerWidth, bodyPaint)
                                            textY += descH + 12f * scale
                                        }
                                        
                                        val isPointsTracking = grp.displayType == 1
                                        val childGoals = if (isPointsTracking) allGoals.filter { it.parentGroupId == grp.id } else emptyList()
                                        val maxTotalPoints = childGoals.sumOf { it.groupValue }
                                        val currentTotalPoints = childGoals.sumOf {
                                            if (it.maxPoints > 0) {
                                                val fraction = it.currentPoints.toDouble() / it.maxPoints.toDouble()
                                                if (it.allowGroupPointsOverflow) fraction * it.groupValue else minOf(it.groupValue, fraction * it.groupValue)
                                            } else 0.0
                                        }

                                        val grpStats = groupGoalStats.find { it.parentGroupId == grp.id }
                                        val progressText = if (isPointsTracking) {
                                            "📁 Folder • ${formatGroupPoint(currentTotalPoints)} / ${formatGroupPoint(maxTotalPoints)} group pts"
                                        } else if (grpStats != null) {
                                            "📁 Folder • ${grpStats.completedGoals} of ${grpStats.totalGoals} done"
                                        } else {
                                            "📁 Folder • Empty"
                                        }
                                        helperDrawText(canvas, progressText, itemX + cardPadding, textY + 14f * scale, cardInnerWidth, detailPaint)
                                    }
                                }
                            }
                            
                            // 3. Draw Footer
                            val footerY = columnsMaxY + 20f * scale
                            val footerTextPaint = android.graphics.Paint().apply {
                                color = if (isDark) android.graphics.Color.parseColor("#CAC4D0") else android.graphics.Color.parseColor("#49454F")
                                textSize = 22f * scale
                                isAntiAlias = true
                                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                            }
                            
                            val dateFormat = java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.getDefault())
                            val dateStr = dateFormat.format(java.util.Date())
                            val footerStr = "Made by Flagpole app  •  $dateStr"
                            canvas.drawText(footerStr, paddingLeft, footerY + 80f * scale, footerTextPaint)
                            
                            // App Icon on the right
                            val iconSize = (80f * scale).toInt()
                            val appIconDrawable = try {
                                context.packageManager.getApplicationIcon(context.packageName)
                            } catch (e: Exception) {
                                context.resources.getDrawable(com.example.R.mipmap.ic_launcher, context.theme)
                            }
                            
                            if (appIconDrawable != null) {
                                val iconBitmap = android.graphics.Bitmap.createBitmap(iconSize, iconSize, android.graphics.Bitmap.Config.ARGB_8888)
                                val iconCanvas = android.graphics.Canvas(iconBitmap)
                                appIconDrawable.setBounds(0, 0, iconSize, iconSize)
                                appIconDrawable.draw(iconCanvas)
                                
                                val iconX = baseWidth - paddingRight - iconSize
                                val iconY = footerY + 80f * scale - iconSize / 1.5f
                                canvas.drawBitmap(iconBitmap, iconX, iconY, android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG))
                                iconBitmap.recycle()
                            }
                            
                            val timestamp = System.currentTimeMillis()
                            val savedCount: Int
                            if (splitParts > 1) {
                                var successfulSlices = 0
                                val partHeight = finalBitmapHeight / splitParts
                                for (part in 0 until splitParts) {
                                    val top = part * partHeight
                                    val bottom = if (part == splitParts - 1) finalBitmapHeight else (part + 1) * partHeight
                                    val h = (bottom - top).coerceAtLeast(1)
                                    val partBitmap = android.graphics.Bitmap.createBitmap(finalBitmap, 0, top, baseWidth, h)
                                    val partFilename = "Flagpole_Document_${timestamp}_part_${part + 1}_of_${splitParts}.png"
                                    val partUri = saveBitmapToStorage(context, partBitmap, partFilename)
                                    if (partUri != null) successfulSlices++
                                    partBitmap.recycle()
                                }
                                savedCount = successfulSlices
                            } else {
                                val filename = "Flagpole_Document_${timestamp}.png"
                                val uri = saveBitmapToStorage(context, finalBitmap, filename)
                                savedCount = if (uri != null) 1 else 0
                            }
                            finalBitmap.recycle()
                            
                            withContext(kotlinx.coroutines.Dispatchers.Main) {
                                isGeneratingPng = false
                                if (savedCount > 0) {
                                    val msg = if (splitParts > 1) "$savedCount of $splitParts PNGs saved to Pictures/Flagpole!" else "PNG saved to Pictures/Flagpole!"
                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                                } else {
                                    android.widget.Toast.makeText(context, "Failed to save PNG.", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            withContext(kotlinx.coroutines.Dispatchers.Main) {
                                isGeneratingPng = false
                                android.widget.Toast.makeText(context, "Generation error: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            )
        }

        // Credits Dialog
        if (showCreditsDialog) {
            CreditsDialog(
                isDark = isDark,
                onDismiss = { showCreditsDialog = false }
            )
        }

        // Import Backup Dialog
        if (showImportBackupDialog && pendingImportUri != null) {
            val importUri = pendingImportUri!!
            AlertDialog(
                onDismissRequest = {
                    showImportBackupDialog = false
                    pendingImportUri = null
                },
                containerColor = if (isDark) Color(0xFF1E1E24) else Color.White,
                titleContentColor = if (isDark) Color.White else Color(0xFF1D1B20),
                textContentColor = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F),
                title = {
                    Text(
                        text = "Import Backup",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Choose how you want to import this backup file:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDark) Color(0xFF2B2A32) else Color(0xFFF3F0F7),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showImportBackupDialog = false
                                    pendingImportUri = null
                                    viewModel.importBackup(
                                        uri = importUri,
                                        replaceAll = false,
                                        onSuccess = {
                                            Toast.makeText(backupContext, "Backup added on top successfully!", Toast.LENGTH_LONG).show()
                                        },
                                        onError = { err ->
                                            Toast.makeText(backupContext, "Import failed: $err", Toast.LENGTH_LONG).show()
                                        }
                                    )
                                }
                                .padding(14.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Add on top",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Adds the backup items alongside your current goals, notes, and groups without deleting existing data.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F)
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDark) Color(0xFF2B2A32) else Color(0xFFF3F0F7),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showImportBackupDialog = false
                                    pendingImportUri = null
                                    viewModel.importBackup(
                                        uri = importUri,
                                        replaceAll = true,
                                        onSuccess = {
                                            Toast.makeText(backupContext, "Backup restored successfully!", Toast.LENGTH_LONG).show()
                                        },
                                        onError = { err ->
                                            Toast.makeText(backupContext, "Import failed: $err", Toast.LENGTH_LONG).show()
                                        }
                                    )
                                }
                                .padding(14.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Replace all",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isDark) Color(0xFFFFB4AB) else Color(0xFFBA1A1A)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Replaces all existing data with the data from this backup file.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F)
                                )
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(
                        onClick = {
                            showImportBackupDialog = false
                            pendingImportUri = null
                        }
                    ) {
                        Text(
                            text = "Cancel",
                            color = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
                        )
                    }
                }
            )
        }

        // Add New Goal Dialog
        if (showAddDialog) {
            AddGoalDialog(
                isDark = isDark,
                currentGroupId = currentGroupId,
                onDismiss = { showAddDialog = false },
                onConfirm = { title, description, current, max, allowExceed, colorHex, secondaryColorHex, milestonesStr, barBehavior, hidePassed, showRemaining, showMilestoneProgressBar, progressType, meterThickness, dotsPerRow, cardBorderHex, unit, statsEnabled, streaksEnabled, pointTrackingEnabled, statsVisualType, showHighlights, groupValue, allowGroupPointsOverflow ->
                    viewModel.addGoal(
                        title, description, current, max, allowExceed, colorHex, secondaryColorHex, milestonesStr, barBehavior, hidePassed, showRemaining, showMilestoneProgressBar, progressType, meterThickness, dotsPerRow, currentGroupId, cardBorderHex, unit, statsEnabled, streaksEnabled, pointTrackingEnabled, statsVisualType, showHighlights, groupValue, allowGroupPointsOverflow
                    )
                    showAddDialog = false
                }
            )
        }

        // Add/Edit Note Dialogs
        if (showAddNoteDialog) {
            AddNoteDialog(
                isDark = isDark,
                onDismiss = { showAddNoteDialog = false },
                onConfirm = { text, textSize, colorHex, secondaryColorHex, imageUri, cardBorderHex ->
                    viewModel.addNote(text, textSize, colorHex, secondaryColorHex, imageUri, currentGroupId, cardBorderHex)
                    showAddNoteDialog = false
                }
            )
        }

        if (showEditNoteDialog && editTargetNote != null) {
            AddNoteDialog(
                isDark = isDark,
                noteToEdit = editTargetNote,
                onDismiss = {
                    showEditNoteDialog = false
                    editTargetNote = null
                },
                onConfirm = { text, textSize, colorHex, secondaryColorHex, imageUri, cardBorderHex ->
                    viewModel.updateNote(
                        editTargetNote!!.copy(
                            text = text,
                            textSize = textSize,
                            colorHex = colorHex,
                            secondaryColorHex = secondaryColorHex,
                            imageUri = imageUri,
                            cardBorderHex = cardBorderHex
                        )
                    )
                    showEditNoteDialog = false
                    editTargetNote = null
                }
            )
        }

        // Add/Edit Group Dialogs
        if (showAddGroupDialog) {
            AddGroupDialog(
                isDark = isDark,
                onDismiss = { showAddGroupDialog = false },
                onConfirm = { title, description, colorHex, secondaryColorHex, imageUri, applyBg, outlineMainColor, cardBorderHex, displayType ->
                    viewModel.addGroup(
                        title, description, colorHex, secondaryColorHex, imageUri, applyBg, outlineMainColor, currentGroupId, cardBorderHex, displayType
                    )
                    showAddGroupDialog = false
                }
            )
        }

        if (showEditGroupDialog && editTargetGroup != null) {
            AddGroupDialog(
                isDark = isDark,
                groupToEdit = editTargetGroup,
                onDismiss = {
                    showEditGroupDialog = false
                    editTargetGroup = null
                },
                onConfirm = { title, description, colorHex, secondaryColorHex, imageUri, applyBg, outlineMainColor, cardBorderHex, displayType ->
                    viewModel.updateGroup(
                        editTargetGroup!!.copy(
                            title = title,
                            description = description,
                            colorHex = colorHex,
                            secondaryColorHex = secondaryColorHex,
                            imageUri = imageUri,
                            applyBg = applyBg,
                            outlineMainColor = outlineMainColor,
                            cardBorderHex = cardBorderHex,
                            displayType = displayType
                        )
                    )
                    showEditGroupDialog = false
                    editTargetGroup = null
                }
            )
        }

        // Edit Goal Dialog
        if (showEditDialog && editTargetGoal != null) {
            AddGoalDialog(
                isDark = isDark,
                goalToEdit = editTargetGoal,
                currentGroupId = currentGroupId,
                onDismiss = {
                    showEditDialog = false
                    editTargetGoal = null
                },
                onConfirm = { title, description, current, max, allowExceed, colorHex, secondaryColorHex, milestonesStr, barBehavior, hidePassed, showRemaining, showMilestoneProgressBar, progressType, meterThickness, dotsPerRow, cardBorderHex, unit, statsEnabled, streaksEnabled, pointTrackingEnabled, statsVisualType, showHighlights, groupValue, allowGroupPointsOverflow ->
                    val finalMax = maxOf(1, max)
                    val finalCurrent = if (!allowExceed) minOf(maxOf(0, current), finalMax) else maxOf(0, current)
                    viewModel.updateGoal(
                        editTargetGoal!!.copy(
                            title = title,
                            description = description,
                            currentPoints = finalCurrent,
                            maxPoints = finalMax,
                            allowExceed = allowExceed,
                            colorHex = colorHex,
                            secondaryColorHex = secondaryColorHex,
                            milestonesStr = milestonesStr,
                            barBehavior = barBehavior,
                            hidePassedMilestones = hidePassed,
                            showRemainingCount = showRemaining,
                            showMilestoneProgressBar = showMilestoneProgressBar,
                            progressType = progressType,
                            meterThickness = meterThickness,
                            dotsPerRow = dotsPerRow,
                            cardBorderHex = cardBorderHex,
                            unit = unit,
                            statsEnabled = statsEnabled,
                            streaksEnabled = streaksEnabled,
                            pointTrackingEnabled = pointTrackingEnabled,
                            statsVisualType = statsVisualType,
                            showHighlights = showHighlights,
                            groupValue = groupValue,
                            allowGroupPointsOverflow = allowGroupPointsOverflow
                        )
                    )
                    showEditDialog = false
                    editTargetGoal = null
                }
            )
        }

        // Adjust Goal (Long press) Dialog
        if (showAdjustDialog && adjustTargetGoal != null) {
            AdjustGoalDialog(
                goal = adjustTargetGoal!!,
                isAddition = isAdjustmentAddition,
                isDark = isDark,
                onDismiss = {
                    showAdjustDialog = false
                    adjustTargetGoal = null
                },
                onConfirm = { amount, adjustMax ->
                    viewModel.adjustGoal(adjustTargetGoal!!, isAdjustmentAddition, amount, adjustMax)
                    showAdjustDialog = false
                    adjustTargetGoal = null
                }
            )
        }

        if (focusedGoalForStats != null) {
            val isExpanded = expandedItemIds.contains("goal_${focusedGoalForStats!!.id}")
            StatsOverlay(
                goal = focusedGoalForStats!!,
                isDark = isDark,
                isLandscape = isLandscape,
                landscapeColumns = landscapeColumns,
                isExpanded = isExpanded,
                onDismiss = { focusedGoalForStats = null },
                viewModel = viewModel
            )
        }

        if (resetTargetGoal != null) {
            val goal = resetTargetGoal!!
            val customColor = try {
                Color(android.graphics.Color.parseColor(goal.colorHex))
            } catch (e: Exception) {
                MaterialTheme.colorScheme.primary
            }
            ResetConfirmationDialog(
                title = "Reset Goal",
                subtitle = "Resetting '${goal.title}'. Choose what to reset:",
                customColor = customColor,
                isDark = isDark,
                onDismiss = { resetTargetGoal = null },
                onConfirm = { type ->
                    viewModel.resetGoal(goal, type)
                    resetTargetGoal = null
                }
            )
        }

        if (completeTargetGoal != null) {
            val goal = completeTargetGoal!!
            val customColor = try {
                Color(android.graphics.Color.parseColor(goal.colorHex))
            } catch (e: Exception) {
                MaterialTheme.colorScheme.primary
            }
            AlertDialog(
                onDismissRequest = { completeTargetGoal = null },
                title = {
                    Text(
                        text = "Complete Goal?",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF1D1B20)
                        )
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to mark '${goal.title}' as completed? Current points will be set to ${goal.maxPoints}.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (isDark) Color.White.copy(alpha = 0.8f) else Color(0xFF49454F)
                        )
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.completeGoal(goal)
                            completeTargetGoal = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = customColor,
                            contentColor = if (isDark) Color.Black else Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Complete", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { completeTargetGoal = null }
                    ) {
                        Text(
                            "Cancel",
                            color = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF49454F)
                        )
                    }
                },
                containerColor = if (isDark) Color(0xFF25232A) else Color(0xFFF3EDF7),
                shape = RoundedCornerShape(20.dp)
            )
        }

        if (resetTargetGroup != null) {
            val group = resetTargetGroup!!
            val customColor = try {
                Color(android.graphics.Color.parseColor(group.colorHex))
            } catch (e: Exception) {
                MaterialTheme.colorScheme.primary
            }
            ResetConfirmationDialog(
                title = "Reset Group",
                subtitle = "This will affect all goals in '${group.title}'. Choose what to reset:",
                customColor = customColor,
                isDark = isDark,
                onDismiss = { resetTargetGroup = null },
                onConfirm = { type ->
                    viewModel.resetGroupGoals(group.id, type)
                    resetTargetGroup = null
                }
            )
        }

        if (groupStatsTarget != null) {
            GroupStatsDialog(
                group = groupStatsTarget!!,
                isDark = isDark,
                allGoals = allGoals,
                groupGoalStats = groupGoalStats,
                allLogs = allLogs,
                isLandscape = isLandscape,
                onDismiss = { groupStatsTarget = null },
                outlineColorHex = if (currentActiveGroup?.outlineMainColor == true) currentActiveGroup.colorHex else null,
                isBrighterText = isBrighterText
            )
        }
    }
}

sealed class DotGridItem {
    data class StartBadge(val text: String) : DotGridItem()
    data class StandardDot(val index: Int) : DotGridItem()
    data class EndBadge(val text: String) : DotGridItem()
}

data class MilestoneCelebrationParticle(
    val id: Int,
    val type: String,
    val startXRatio: Float,
    val startYRatio: Float,
    val vxDp: Float,
    val vyDp: Float,
    val initialSizeDp: Float,
    val initialRotation: Float,
    val rotationSpeed: Float
)

fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMilestoneParticle(
    type: String,
    color: Color,
    sizePx: Float,
    center: androidx.compose.ui.geometry.Offset,
    rotation: Float,
    alpha: Float
) {
    if (sizePx <= 0f || alpha <= 0f) return
    val pColor = color.copy(alpha = alpha.coerceIn(0f, 1f))
    when (type.lowercase()) {
        "star" -> {
            val path = androidx.compose.ui.graphics.Path()
            val outerR = sizePx / 2f
            val innerR = outerR * 0.42f
            for (i in 0 until 10) {
                val r = if (i % 2 == 0) outerR else innerR
                val angle = Math.toRadians((i * 36 - 90).toDouble())
                val x = center.x + (r * kotlin.math.cos(angle)).toFloat()
                val y = center.y + (r * kotlin.math.sin(angle)).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            withTransform({
                rotate(degrees = rotation, pivot = center)
            }) {
                drawPath(path = path, color = pColor)
            }
        }
        "diamond" -> {
            val s = sizePx * 0.707f
            withTransform({
                rotate(degrees = rotation + 45f, pivot = center)
            }) {
                drawRoundRect(
                    color = pColor,
                    topLeft = androidx.compose.ui.geometry.Offset(center.x - s / 2f, center.y - s / 2f),
                    size = androidx.compose.ui.geometry.Size(s, s),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.1f, s * 0.1f)
                )
            }
        }
        "heart" -> {
            val path = androidx.compose.ui.graphics.Path()
            val s = sizePx * 0.45f
            path.moveTo(center.x, center.y + s * 0.75f)
            path.cubicTo(center.x - s * 1.2f, center.y, center.x - s * 1.2f, center.y - s * 0.9f, center.x - s * 0.5f, center.y - s * 0.9f)
            path.cubicTo(center.x - s * 0.1f, center.y - s * 0.9f, center.x, center.y - s * 0.5f, center.x, center.y - s * 0.4f)
            path.cubicTo(center.x, center.y - s * 0.4f, center.x + s * 0.1f, center.y - s * 0.9f, center.x + s * 0.5f, center.y - s * 0.9f)
            path.cubicTo(center.x + s * 1.2f, center.y - s * 0.9f, center.x + s * 1.2f, center.y, center.x, center.y + s * 0.75f)
            path.close()
            withTransform({
                rotate(degrees = rotation, pivot = center)
            }) {
                drawPath(path = path, color = pColor)
            }
        }
        "flag" -> {
            val path = androidx.compose.ui.graphics.Path()
            val w = sizePx * 0.7f
            val h = sizePx * 0.5f
            val left = center.x - w / 2f
            val top = center.y - sizePx / 2f
            withTransform({
                rotate(degrees = rotation, pivot = center)
            }) {
                drawLine(
                    color = pColor,
                    start = androidx.compose.ui.geometry.Offset(left, top),
                    end = androidx.compose.ui.geometry.Offset(left, top + sizePx),
                    strokeWidth = (sizePx * 0.15f).coerceAtLeast(2f),
                    cap = StrokeCap.Round
                )
                path.moveTo(left, top)
                path.lineTo(left + w, top + h * 0.5f)
                path.lineTo(left, top + h)
                path.close()
                drawPath(path = path, color = pColor)
            }
        }
        "danger" -> {
            val path = androidx.compose.ui.graphics.Path()
            val r = sizePx / 2f
            path.moveTo(center.x, center.y - r)
            path.lineTo(center.x + r * 0.866f, center.y + r * 0.5f)
            path.lineTo(center.x - r * 0.866f, center.y + r * 0.5f)
            path.close()
            withTransform({
                rotate(degrees = rotation, pivot = center)
            }) {
                drawPath(path = path, color = pColor)
            }
        }
        "cross" -> {
            val half = sizePx * 0.38f
            val strokeW = (sizePx * 0.2f).coerceAtLeast(2f)
            withTransform({
                rotate(degrees = rotation + 45f, pivot = center)
            }) {
                drawLine(
                    color = pColor,
                    start = androidx.compose.ui.geometry.Offset(center.x - half, center.y),
                    end = androidx.compose.ui.geometry.Offset(center.x + half, center.y),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = pColor,
                    start = androidx.compose.ui.geometry.Offset(center.x, center.y - half),
                    end = androidx.compose.ui.geometry.Offset(center.x, center.y + half),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
            }
        }
        "line" -> {
            val w = (sizePx * 0.25f).coerceAtLeast(2f)
            val h = sizePx
            withTransform({
                rotate(degrees = rotation, pivot = center)
            }) {
                drawRoundRect(
                    color = pColor,
                    topLeft = androidx.compose.ui.geometry.Offset(center.x - w / 2f, center.y - h / 2f),
                    size = androidx.compose.ui.geometry.Size(w, h),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w / 2f, w / 2f)
                )
            }
        }
        else -> { // "dot"
            drawCircle(
                color = pColor,
                radius = (sizePx * 0.45f).coerceAtLeast(2f),
                center = center
            )
        }
    }
}

@Composable
fun GoalCard(
    goal: Goal,
    isDark: Boolean,
    isAnimationEnabled: Boolean = true,
    animationEasingType: Int = 2,
    animationDurationSeconds: Int = 3,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onHoldIncrement: () -> Unit,
    onHoldDecrement: () -> Unit,
    onEdit: () -> Unit = {},
    onDuplicate: () -> Unit = {},
    onReset: () -> Unit = {},
    onComplete: () -> Unit = {},
    onDelete: () -> Unit,
    onInfoClick: () -> Unit = {},
    onCopy: () -> Unit = {},
    modifier: Modifier = Modifier,
    outlineColorHex: String? = null,
    isExpanded: Boolean = false,
    onToggleExpand: (() -> Unit)? = null,
    isBrighterText: Boolean = false
) {
    var showCardMenu by remember { mutableStateOf(false) }

    var lastPoints by remember(goal.id) { mutableStateOf(goal.currentPoints) }
    var animateThisChange by remember(goal.id) { mutableStateOf(false) }

    if (goal.currentPoints != lastPoints) {
        val delta = kotlin.math.abs(goal.currentPoints - lastPoints)
        animateThisChange = isAnimationEnabled && (delta != 1)
        lastPoints = goal.currentPoints
    }

    var lastMaxPoints by remember(goal.id) { mutableStateOf(goal.maxPoints) }
    var animateMaxChange by remember(goal.id) { mutableStateOf(false) }

    if (goal.maxPoints != lastMaxPoints) {
        animateMaxChange = isAnimationEnabled
        lastMaxPoints = goal.maxPoints
    }

    val easing = when (animationEasingType) {
        0 -> androidx.compose.animation.core.CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
        1 -> androidx.compose.animation.core.CubicBezierEasing(0.8f, 0.0f, 1.0f, 1.0f)
        else -> androidx.compose.animation.core.LinearEasing
    }

    val animatedPoints by animateFloatAsState(
        targetValue = goal.currentPoints.toFloat(),
        animationSpec = if (animateThisChange) {
            tween(durationMillis = animationDurationSeconds * 1000, easing = easing)
        } else {
            snap()
        },
        label = "GoalPointsAnimation"
    )

    val displayPoints = if (isAnimationEnabled) {
        kotlin.math.round(animatedPoints).toInt()
    } else {
        goal.currentPoints
    }

    val animatedMaxPoints by animateFloatAsState(
        targetValue = goal.maxPoints.toFloat(),
        animationSpec = if (animateMaxChange) {
            tween(durationMillis = animationDurationSeconds * 1000, easing = easing)
        } else {
            snap()
        },
        label = "GoalMaxPointsAnimation"
    )

    val displayMaxPoints = if (isAnimationEnabled) {
        kotlin.math.round(animatedMaxPoints).toInt()
    } else {
        goal.maxPoints
    }

    val currentPointsValue = if (isAnimationEnabled) animatedPoints else goal.currentPoints.toFloat()
    val currentMaxPointsValue = if (isAnimationEnabled) animatedMaxPoints else goal.maxPoints.toFloat()

    val rawProgressRatio = if (currentMaxPointsValue > 0) {
        currentPointsValue / currentMaxPointsValue
    } else 0f
    val progressRatio = minOf(1f, rawProgressRatio)
    val percentage = (rawProgressRatio * 100).toInt()
    val isCompleted = displayPoints >= displayMaxPoints

    val customColor = remember(goal.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(goal.colorHex))
        } catch (e: Exception) {
            Color(0xFF6750A4)
        }
    }

    val effectiveBorderColor = remember(goal.cardBorderHex, outlineColorHex, customColor) {
        if (goal.cardBorderHex != "NONE") {
            try { Color(android.graphics.Color.parseColor(goal.cardBorderHex)) } catch (e: Exception) { customColor }
        } else if (outlineColorHex != null) {
            try { Color(android.graphics.Color.parseColor(outlineColorHex)) } catch (e: Exception) { customColor }
        } else {
            customColor
        }
    }

    val milestones = remember(goal.milestonesStr) { parseMilestones(goal.milestonesStr) }

    // Celebration Animation State
    var lastCelebratedVisualPoints by remember(goal.id) { mutableStateOf(displayPoints) }
    var celebrationTrigger by remember(goal.id) { mutableStateOf(0) }
    var celebrationMilestoneType by remember(goal.id) { mutableStateOf("star") }
    var celebrationIsGoalComplete by remember(goal.id) { mutableStateOf(false) }

    val celebrationAnim = remember { Animatable(0f) }
    val bigDotScaleAnim = remember { Animatable(0f) }

    val newlyPassedMilestones = remember(displayPoints, lastCelebratedVisualPoints) {
        if (displayPoints > lastCelebratedVisualPoints) {
            milestones.filter { it.value in (lastCelebratedVisualPoints + 1)..displayPoints }
        } else emptyList()
    }
    val isGoalNewlyDone = displayPoints >= displayMaxPoints && lastCelebratedVisualPoints < displayMaxPoints && displayMaxPoints > 0

    LaunchedEffect(displayPoints) {
        if (isAnimationEnabled && displayPoints > lastCelebratedVisualPoints) {
            if (newlyPassedMilestones.isNotEmpty() || isGoalNewlyDone) {
                celebrationIsGoalComplete = isGoalNewlyDone
                celebrationMilestoneType = newlyPassedMilestones.lastOrNull()?.type ?: "star"
                celebrationTrigger++
            }
        }
        lastCelebratedVisualPoints = displayPoints
    }

    var particles by remember(celebrationTrigger) { mutableStateOf<List<MilestoneCelebrationParticle>>(emptyList()) }

    LaunchedEffect(celebrationTrigger) {
        if (celebrationTrigger > 0 && isAnimationEnabled) {
            val count = if (celebrationIsGoalComplete) (20..30).random() else (5..10).random()
            val allTypes = listOf("flag", "star", "heart", "diamond", "danger", "cross", "line", "dot")
            val newParticles = (0 until count).map { id ->
                val type = if (celebrationIsGoalComplete) allTypes.random() else celebrationMilestoneType
                val side = (0..3).random()
                val startXRatio = when (side) {
                    0, 2 -> (5..95).random() / 100f
                    1 -> 1.0f
                    else -> 0.0f
                }
                val startYRatio = when (side) {
                    1, 3 -> (5..95).random() / 100f
                    0 -> 0.0f
                    else -> 1.0f
                }
                val speed = (100..280).random().toFloat()
                val angleDeg = when (side) {
                    0 -> (-150..-30).random().toFloat()
                    1 -> (-60..60).random().toFloat()
                    2 -> (30..150).random().toFloat()
                    else -> (120..240).random().toFloat()
                }
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val vx = (speed * kotlin.math.cos(angleRad)).toFloat()
                val vy = (speed * kotlin.math.sin(angleRad)).toFloat()
                val size = (12..22).random().toFloat()
                MilestoneCelebrationParticle(
                    id = id,
                    type = type,
                    startXRatio = startXRatio,
                    startYRatio = startYRatio,
                    vxDp = vx,
                    vyDp = vy,
                    initialSizeDp = size,
                    initialRotation = (0..360).random().toFloat(),
                    rotationSpeed = (-160..160).random().toFloat()
                )
            }
            particles = newParticles

            launch {
                bigDotScaleAnim.snapTo(0f)
                bigDotScaleAnim.animateTo(1.35f, tween(140, easing = LinearEasing))
                bigDotScaleAnim.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
                delay(1650)
                bigDotScaleAnim.animateTo(0f, tween(350, easing = FastOutSlowInEasing))
            }

            celebrationAnim.snapTo(0f)
            celebrationAnim.animateTo(1f, tween(durationMillis = 2500, easing = LinearEasing))
            particles = emptyList()
        }
    }

    // Math calculation for different behaviors driven directly from animated points and animated max points
    val rangeStart: Float
    val rangeEnd: Float
    val progressFraction: Float

    when (goal.barBehavior) {
        1 -> { // ALL_MILESTONES (show everything)
            rangeStart = 0f
            val maxMilestone = milestones.maxOfOrNull { it.value } ?: 0
            rangeEnd = maxOf(currentMaxPointsValue, maxMilestone.toFloat())
            progressFraction = if (rangeEnd > 0) currentPointsValue / rangeEnd else 0f
        }
        2 -> { // UP_TO_NEXT
            rangeStart = 0f
            val nextMilestone = milestones.firstOrNull { it.value > currentPointsValue }?.value ?: currentMaxPointsValue.toInt()
            rangeEnd = nextMilestone.toFloat()
            progressFraction = if (rangeEnd > 0) currentPointsValue / rangeEnd else 0f
        }
        3 -> { // BETWEEN_LAST_AND_NEXT
            val lastMilestone = milestones.lastOrNull { it.value <= currentPointsValue }?.value ?: 0
            val nextMilestone = milestones.firstOrNull { it.value > currentPointsValue }?.value ?: currentMaxPointsValue.toInt()
            rangeStart = lastMilestone.toFloat()
            rangeEnd = nextMilestone.toFloat()
            val denom = rangeEnd - rangeStart
            progressFraction = if (denom > 0) (currentPointsValue - lastMilestone) / denom else 0f
        }
        else -> { // BEFORE_GOAL (default)
            rangeStart = 0f
            rangeEnd = currentMaxPointsValue
            progressFraction = if (rangeEnd > 0) currentPointsValue / rangeEnd else 0f
        }
    }

    val cardBgColor = getCardBackgroundColor(isDark, goal.secondaryColorHex)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("goal_card_${goal.id}")
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = cardBgColor
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp),
            border = if (goal.cardBorderHex != "NONE") {
                val strokeColor = try { Color(android.graphics.Color.parseColor(goal.cardBorderHex)) } catch (e: Exception) { Color(0xFF6750A4) }
                BorderStroke(2.dp, strokeColor)
            } else if (outlineColorHex != null) {
                val strokeColor = try { Color(android.graphics.Color.parseColor(outlineColorHex)) } catch (e: Exception) { Color(0xFF6750A4) }
                BorderStroke(2.dp, strokeColor)
            } else {
                if (isDark) null else borderStrokeForCard(goal.secondaryColorHex)
            }
        ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Title and Status Badge / Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp,
                            color = if (isDark) Color.White else Color(0xFF1D1B20)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Info Icon and Completion Check Circles
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Info Circle with "i" Symbol
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, customColor, CircleShape)
                                .clickable { onInfoClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "i",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = customColor,
                                    fontSize = 11.sp
                                )
                            )
                        }

                        // Completion Circle
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, customColor, CircleShape)
                                .background(if (isCompleted) customColor else Color.Transparent)
                                .clickable {
                                    if (isCompleted) {
                                        onReset()
                                    } else {
                                        onComplete()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCompleted) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Completed",
                                    tint = if (isDark) Color.Black else Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }

                    // 3 Vertical Dots Menu button
                    Box {
                        IconButton(
                            onClick = { showCardMenu = true },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("menu_button_${goal.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Goal options",
                                tint = customColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showCardMenu,
                            onDismissRequest = { showCardMenu = false },
                            modifier = Modifier.background(if (isDark) Color(0xFF1E1C24) else Color(0xFFF3EDF7))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit Goal") },
                                onClick = {
                                    showCardMenu = false
                                    onEdit()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = if (isDark) Color.White else Color(0xFF1D1B20),
                                    leadingIconColor = customColor
                                )
                            )
                            DropdownMenuItem(
                                text = { Text("Duplicate") },
                                onClick = {
                                    showCardMenu = false
                                    onDuplicate()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = if (isDark) Color.White else Color(0xFF1D1B20),
                                    leadingIconColor = customColor
                                )
                            )
                            DropdownMenuItem(
                                text = { Text("Reset") },
                                onClick = {
                                    showCardMenu = false
                                    onReset()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = if (isDark) Color.White else Color(0xFF1D1B20),
                                    leadingIconColor = customColor
                                )
                            )
                            DropdownMenuItem(
                                text = { Text("Complete") },
                                onClick = {
                                    showCardMenu = false
                                    onComplete()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = if (isDark) Color.White else Color(0xFF1D1B20),
                                    leadingIconColor = customColor
                                )
                            )
                            if (onToggleExpand != null) {
                                HorizontalDivider(color = if (isDark) Color(0xFF333038) else Color(0xFFE6E0E9))
                                DropdownMenuItem(
                                    text = { Text(if (isExpanded) "Shrink Card" else "Expand Card") },
                                    onClick = {
                                        showCardMenu = false
                                        onToggleExpand()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    colors = MenuDefaults.itemColors(
                                        textColor = if (isDark) Color.White else Color(0xFF1D1B20),
                                        leadingIconColor = customColor
                                    )
                                )
                            }
                            HorizontalDivider(color = if (isDark) Color(0xFF333038) else Color(0xFFE6E0E9))
                            DropdownMenuItem(
                                text = { Text("Copy") },
                                onClick = {
                                    showCardMenu = false
                                    onCopy()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy",
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = if (isDark) Color.White else Color(0xFF1D1B20),
                                    leadingIconColor = customColor
                                )
                            )
                            HorizontalDivider(color = if (isDark) Color(0xFF333038) else Color(0xFFE6E0E9))
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    showCardMenu = false
                                    onDelete()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = MaterialTheme.colorScheme.error,
                                    leadingIconColor = MaterialTheme.colorScheme.error
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (goal.progressType) {
                1 -> { // SPEEDOMETER METER TYPE
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        BoxWithConstraints(
                            modifier = Modifier
                                .size(270.dp)
                                .align(Alignment.CenterHorizontally),
                            contentAlignment = Alignment.Center
                        ) {
                            val strokeWidthPx = with(androidx.compose.ui.platform.LocalDensity.current) { goal.meterThickness.dp.toPx() }
                            val trackColor = if (isDark) Color(0xFF333038) else Color(0xFFE6E0E9)
                            val trackRadius = 135.dp - (goal.meterThickness.dp / 2) - 6.dp

                            // 1. Draw the Arc Gauge Tracks
                            Canvas(modifier = Modifier.fillMaxSize().padding(goal.meterThickness.dp / 2 + 6.dp)) {
                                // Draw main background arc
                                drawArc(
                                    color = trackColor,
                                    startAngle = 150f,
                                    sweepAngle = 240f,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                                )
                                // Draw main active progress arc
                                drawArc(
                                    color = customColor,
                                    startAngle = 150f,
                                    sweepAngle = progressFraction.coerceIn(0f, 1f) * 240f,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                                )

                                // Inner Sliced Milestone Meter Gauge
                                if (goal.showMilestoneProgressBar && milestones.isNotEmpty() && (goal.barBehavior == 2 || goal.barBehavior == 3)) {
                                    val count = milestones.size
                                    val innerStrokePx = strokeWidthPx * 0.45f
                                    val innerOffsetPx = strokeWidthPx * 0.85f + 12f
                                    val gapAngle = if (count > 1) 7f else 0f
                                    val totalGap = (count - 1) * gapAngle
                                    val availableSweep = 240f - totalGap
                                    val sliceSweep = (availableSweep / count).coerceAtLeast(1f)

                                    val innerSize = androidx.compose.ui.geometry.Size(
                                        size.width - innerOffsetPx * 2,
                                        size.height - innerOffsetPx * 2
                                    )
                                    val innerTopLeft = androidx.compose.ui.geometry.Offset(innerOffsetPx, innerOffsetPx)

                                    for (i in 0 until count) {
                                        val sliceStart = 150f + i * (sliceSweep + gapAngle)
                                        val isPassed = milestones[i].value <= displayPoints
                                        val sliceColor = if (isPassed) customColor else trackColor

                                        drawArc(
                                            color = sliceColor,
                                            startAngle = sliceStart,
                                            sweepAngle = sliceSweep,
                                            useCenter = false,
                                            topLeft = innerTopLeft,
                                            size = innerSize,
                                            style = Stroke(width = innerStrokePx, cap = StrokeCap.Round)
                                        )
                                    }
                                }
                            }

                            // 2. Overlay Milestones checkpoints along the circular arc
                            milestones.forEach { milestone ->
                                val isPassed = milestone.value <= displayPoints
                                val isVisible = when (goal.barBehavior) {
                                    0 -> milestone.value.toFloat() <= currentMaxPointsValue
                                    1 -> true
                                    2 -> milestone.value <= rangeEnd
                                    3 -> milestone.value >= rangeStart && milestone.value <= rangeEnd
                                    else -> false
                                } && (!goal.hidePassedMilestones || !isPassed)

                                if (isVisible) {
                                    val fraction = when (goal.barBehavior) {
                                        3 -> {
                                            val denom = rangeEnd - rangeStart
                                            if (denom > 0) (milestone.value - rangeStart) / denom else 0f
                                        }
                                        else -> {
                                            if (rangeEnd > 0) milestone.value.toFloat() / rangeEnd else 0f
                                        }
                                    }.coerceIn(0f, 1f)

                                    val angle = 150f + (fraction * 240f)
                                    val angleRad = Math.toRadians(angle.toDouble())
                                    val xDp = 135.dp + trackRadius * kotlin.math.cos(angleRad).toFloat()
                                    val yDp = 135.dp + trackRadius * kotlin.math.sin(angleRad).toFloat()

                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .offset(x = xDp - 7.dp, y = yDp - 7.dp)
                                            .rotate(angle - 270f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val passedColor = if (isDark) Color.Black else Color.White
                                        MilestoneIconDisplay(
                                            type = milestone.type,
                                            tint = if (isPassed) passedColor else customColor,
                                            size = 14.dp
                                        )
                                    }
                                }
                            }

                            // Render main Goal target (maxPoints) at end of arc (Angle 390f, on the track)
                            val goalVisible = when (goal.barBehavior) {
                                0 -> true
                                1 -> true
                                2 -> currentMaxPointsValue <= rangeEnd
                                3 -> currentMaxPointsValue >= rangeStart && currentMaxPointsValue <= rangeEnd
                                else -> false
                            }
                            if (goalVisible) {
                                val goalFraction = when (goal.barBehavior) {
                                    3 -> {
                                        val denom = rangeEnd - rangeStart
                                        if (denom > 0) (currentMaxPointsValue - rangeStart) / denom else 0f
                                    }
                                    else -> {
                                        if (rangeEnd > 0) currentMaxPointsValue / rangeEnd else 1f
                                    }
                                }.coerceIn(0f, 1f)

                                val angle = 150f + (goalFraction * 240f)
                                val angleRad = Math.toRadians(angle.toDouble())
                                val xDp = 135.dp + trackRadius * kotlin.math.cos(angleRad).toFloat()
                                val yDp = 135.dp + trackRadius * kotlin.math.sin(angleRad).toFloat()

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .offset(x = xDp - 9.dp, y = yDp - 9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(customColor)
                                            .border(2.5.dp, Color.White, CircleShape)
                                    )
                                }
                            }

                            // 3. Center Info Column
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = "$percentage%",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = customColor,
                                        fontSize = 13.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$displayPoints",
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = customColor,
                                        fontSize = 32.sp
                                    )
                                )
                                Text(
                                    text = "of",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = customColor.copy(alpha = 0.5f),
                                        fontSize = 11.sp
                                    )
                                )
                                Text(
                                    text = "$displayMaxPoints ${goal.unit}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = customColor.copy(alpha = 0.8f),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Controls Row at Bottom
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Minus Button
                            HoldableIconButton(
                                onClick = onDecrement,
                                onLongClick = onHoldDecrement,
                                contentDescription = "Decrease points",
                                testTag = "minus_button_${goal.id}",
                                containerColor = customColor.copy(alpha = if (isDark) 0.22f else 0.15f),
                                contentColor = customColor
                            ) {
                                Text(
                                    text = "−",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 22.sp,
                                        color = customColor
                                    )
                                )
                            }

                            // Next Milestone / left indicator
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (goal.barBehavior == 2 || goal.barBehavior == 3) {
                                    val nextMilestoneVal = milestones.firstOrNull { it.value > displayPoints }?.value ?: displayMaxPoints
                                    Text(
                                        text = "Next: $nextMilestoneVal ${goal.unit}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = customColor,
                                            fontSize = 12.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                }
                                val isCountActive = goal.showRemainingCount && (goal.barBehavior == 2 || goal.barBehavior == 3)
                                val remainingMilestones = milestones.count { it.value > goal.currentPoints }
                                if (isCountActive && remainingMilestones > 0) {
                                    Surface(
                                        color = customColor.copy(alpha = 0.15f),
                                        contentColor = customColor,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "+$remainingMilestones milestones",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            // Plus Button
                            HoldableIconButton(
                                onClick = onIncrement,
                                onLongClick = onHoldIncrement,
                                contentDescription = "Increase points",
                                testTag = "plus_button_${goal.id}",
                                containerColor = customColor.copy(alpha = if (isDark) 0.22f else 0.15f),
                                contentColor = customColor
                            ) {
                                Text(
                                    text = "+",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 22.sp,
                                        color = customColor
                                    )
                                )
                            }
                        }
                    }
                }
                2 -> { // BIG DOTS TYPE
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Header info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "$displayPoints ",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = customColor,
                                        fontSize = 22.sp
                                    )
                                )
                                Text(
                                    text = "/ $displayMaxPoints ${goal.unit}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = customColor.copy(alpha = 0.65f),
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 14.sp
                                    )
                                )
                            }
                            Text(
                                text = "$percentage%",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = customColor,
                                    fontSize = 13.sp
                                )
                            )
                        }

                        // Dots grid inside BoxWithConstraints to scale dynamically
                        BoxWithConstraints(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val currentDisplayPts = displayPoints

                            // Compute behavior parameters
                            val startPts: Int
                            val endPts: Int
                            val extraStartBadgeText: String?
                            val extraEndBadgeText: String?

                            when (goal.barBehavior) {
                                1 -> { // ALL MILESTONES
                                    val maxMilestone = milestones.maxOfOrNull { it.value } ?: displayMaxPoints
                                    startPts = 0
                                    extraStartBadgeText = null
                                    if (currentDisplayPts < displayMaxPoints) {
                                        endPts = maxOf(1, displayMaxPoints)
                                        val remDots = maxMilestone - displayMaxPoints
                                        extraEndBadgeText = if (remDots > 0) "$remDots" else null
                                    } else {
                                        endPts = maxOf(1, maxMilestone)
                                        extraEndBadgeText = null
                                    }
                                }
                                2 -> { // UP TO NEXT MILESTONE
                                    startPts = 0
                                    extraStartBadgeText = null
                                    val nextMilestoneVal = milestones.firstOrNull { it.value > currentDisplayPts }?.value ?: displayMaxPoints
                                    endPts = maxOf(1, nextMilestoneVal)
                                    val remainingMilestoneCount = milestones.count { it.value > nextMilestoneVal }
                                    extraEndBadgeText = if (remainingMilestoneCount > 0) "+$remainingMilestoneCount" else null
                                }
                                3 -> { // BETWEEN LAST AND NEXT MILESTONE
                                    val lastMilestoneVal = milestones.lastOrNull { it.value <= currentDisplayPts }?.value
                                    val nextMilestoneVal = milestones.firstOrNull { it.value > currentDisplayPts }?.value ?: displayMaxPoints
                                    if (lastMilestoneVal != null && lastMilestoneVal > 0) {
                                        extraStartBadgeText = "$lastMilestoneVal"
                                        startPts = lastMilestoneVal
                                    } else {
                                        extraStartBadgeText = null
                                        startPts = 0
                                    }
                                    endPts = maxOf(startPts + 1, nextMilestoneVal)
                                    val remainingToGoal = maxOf(0, displayMaxPoints - nextMilestoneVal)
                                    extraEndBadgeText = if (remainingToGoal > 0) "$remainingToGoal" else null
                                }
                                else -> { // DEFAULT BEFORE GOAL
                                    startPts = 0
                                    endPts = maxOf(1, displayMaxPoints)
                                    extraStartBadgeText = null
                                    extraEndBadgeText = null
                                }
                            }

                            val pointSpan = endPts - startPts
                            val totalDotSpan = if (pointSpan <= 100) pointSpan else 100
                            val currentPointsInSpan = (currentDisplayPts - startPts).coerceIn(0, pointSpan)
                            val activeDotsCount = if (pointSpan <= 100) currentPointsInSpan else (currentPointsInSpan.toFloat() / pointSpan * 100).toInt()

                            val prevActiveDotsArray = remember { intArrayOf(activeDotsCount) }
                            val currentPrev = prevActiveDotsArray[0]
                            SideEffect {
                                prevActiveDotsArray[0] = activeDotsCount
                            }

                            val cols = goal.dotsPerRow
                            val spacing = 6.dp

                            val gridItems = mutableListOf<DotGridItem>()
                            if (extraStartBadgeText != null) {
                                gridItems.add(DotGridItem.StartBadge(extraStartBadgeText))
                            }
                            for (i in 0 until totalDotSpan) {
                                gridItems.add(DotGridItem.StandardDot(i))
                            }
                            if (extraEndBadgeText != null) {
                                gridItems.add(DotGridItem.EndBadge(extraEndBadgeText))
                            }

                            val dotSize = (maxWidth - (spacing * (cols - 1))) / cols
                            val finalDotSize = dotSize.coerceIn(8.dp, 48.dp)

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(spacing)
                            ) {
                                val rows = gridItems.chunked(cols)
                                rows.forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(spacing, Alignment.Start)
                                    ) {
                                        rowItems.forEach { item ->
                                            when (item) {
                                                is DotGridItem.StartBadge -> {
                                                    // Extra colored dot at start
                                                    Box(
                                                        modifier = Modifier
                                                            .size(finalDotSize)
                                                            .clip(CircleShape)
                                                            .background(customColor),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = item.text,
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                color = Color.White,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = (finalDotSize.value * 0.35f).sp
                                                            ),
                                                            maxLines = 1
                                                        )
                                                    }
                                                }
                                                is DotGridItem.EndBadge -> {
                                                    // Extra uncolored/outlined dot at end
                                                    Box(
                                                        modifier = Modifier
                                                            .size(finalDotSize)
                                                            .clip(CircleShape)
                                                            .background(if (isDark) Color(0xFF333038) else Color(0xFFE6E0E9))
                                                            .border(1.dp, customColor, CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = item.text,
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                color = customColor,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = (finalDotSize.value * 0.35f).sp
                                                            ),
                                                            maxLines = 1
                                                        )
                                                    }
                                                }
                                                is DotGridItem.StandardDot -> {
                                                    val index = item.index
                                                    val isActive = index < activeDotsCount
                                                    val actualPointValue = if (pointSpan <= 100) startPts + index + 1 else startPts + ((index + 1) * pointSpan.toFloat() / 100).toInt()
                                                    val milestoneAtThisDot = milestones.firstOrNull { milestone ->
                                                        milestone.value == actualPointValue
                                                    }

                                                    val dotScale = remember { Animatable(1f) }

                                                    LaunchedEffect(activeDotsCount) {
                                                        val wasActive = index < currentPrev
                                                        val isActiveDot = index < activeDotsCount
                                                        if (wasActive != isActiveDot && currentPrev != activeDotsCount) {
                                                            try {
                                                                val diff = kotlin.math.abs(activeDotsCount - currentPrev)
                                                                val delayMs = if (diff == 1) {
                                                                    0
                                                                } else {
                                                                    ((if (activeDotsCount > currentPrev) (index - currentPrev) else (currentPrev - 1 - index)).coerceAtLeast(0) * 45)
                                                                }
                                                                kotlinx.coroutines.delay(delayMs.toLong())
                                                                dotScale.animateTo(
                                                                    targetValue = 1.35f,
                                                                    animationSpec = tween(durationMillis = 120)
                                                                )
                                                                dotScale.animateTo(
                                                                    targetValue = 1f,
                                                                    animationSpec = spring(
                                                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                                                        stiffness = Spring.StiffnessLow
                                                                    )
                                                                )
                                                            } finally {
                                                                kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                                                                    dotScale.snapTo(1f)
                                                                }
                                                            }
                                                        }
                                                    }

                                                    Box(
                                                        modifier = Modifier
                                                            .size(finalDotSize)
                                                            .graphicsLayer(
                                                                scaleX = dotScale.value,
                                                                scaleY = dotScale.value
                                                            )
                                                            .clip(CircleShape)
                                                            .background(
                                                                if (isActive) customColor else {
                                                                    if (isDark) Color(0xFF333038) else Color(0xFFE6E0E9)
                                                                }
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (milestoneAtThisDot != null) {
                                                            val isPassed = milestoneAtThisDot.value <= displayPoints
                                                            val passedColor = if (isDark) Color.Black else Color.White
                                                            MilestoneIconDisplay(
                                                                type = milestoneAtThisDot.type,
                                                                tint = if (isPassed) passedColor else customColor,
                                                                size = finalDotSize * 0.6f
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Controls Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Minus Button
                            HoldableIconButton(
                                onClick = onDecrement,
                                onLongClick = onHoldDecrement,
                                contentDescription = "Decrease points",
                                testTag = "minus_button_${goal.id}",
                                containerColor = customColor.copy(alpha = if (isDark) 0.22f else 0.15f),
                                contentColor = customColor
                            ) {
                                Text(
                                    text = "−",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 22.sp,
                                        color = customColor
                                    )
                                )
                            }

                            // Next Milestone info
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (goal.barBehavior == 2 || goal.barBehavior == 3) {
                                    val nextMilestoneVal = milestones.firstOrNull { it.value > displayPoints }?.value ?: displayMaxPoints
                                    Text(
                                        text = "Next: $nextMilestoneVal ${goal.unit}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = customColor,
                                            fontSize = 12.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                }
                                val isCountActive = goal.showRemainingCount && (goal.barBehavior == 2 || goal.barBehavior == 3)
                                val remainingMilestones = milestones.count { it.value > goal.currentPoints }
                                if (isCountActive && remainingMilestones > 0) {
                                    Surface(
                                        color = customColor.copy(alpha = 0.15f),
                                        contentColor = customColor,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "+$remainingMilestones milestones",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            // Plus Button
                            HoldableIconButton(
                                onClick = onIncrement,
                                onLongClick = onHoldIncrement,
                                contentDescription = "Increase points",
                                testTag = "plus_button_${goal.id}",
                                containerColor = customColor.copy(alpha = if (isDark) 0.22f else 0.15f),
                                contentColor = customColor
                            ) {
                                Text(
                                    text = "+",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 22.sp,
                                        color = customColor
                                    )
                                )
                            }
                        }
                    }
                }
                else -> { // DEFAULT BAR TYPE
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Minus Button
                        HoldableIconButton(
                            onClick = onDecrement,
                            onLongClick = onHoldDecrement,
                            contentDescription = "Decrease points (Hold to customize)",
                            testTag = "minus_button_${goal.id}",
                            containerColor = customColor.copy(alpha = if (isDark) 0.22f else 0.15f),
                            contentColor = customColor
                        ) {
                            Text(
                                text = "−",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp,
                                    color = customColor
                                )
                            )
                        }

                        // Progress Info and Bar in Center
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            // Metrics header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                // "45 / 60 pts"
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = "$displayPoints ",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = customColor,
                                            fontSize = 22.sp
                                        )
                                    )
                                    Text(
                                        text = "/ $displayMaxPoints ${goal.unit}",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = customColor.copy(alpha = 0.65f),
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 14.sp
                                        )
                                    )
                                }

                                // Top right text: display target of next milestone if behavior is 2 or 3
                                Column(horizontalAlignment = Alignment.End) {
                                    if (goal.barBehavior == 2 || goal.barBehavior == 3) {
                                        val nextMilestoneVal = milestones.firstOrNull { it.value > displayPoints }?.value ?: displayMaxPoints
                                        Text(
                                            text = "Next: $nextMilestoneVal ${goal.unit}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = customColor,
                                                fontSize = 12.sp
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                    }
                                    Text(
                                        text = "$percentage%",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (goal.barBehavior == 2 || goal.barBehavior == 3) customColor.copy(alpha = 0.7f) else customColor,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Progress Bar container (Secondary sliced milestone bar on top + Primary continuous bar on bottom)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(1.dp)
                                ) {
                                    // 1. Secondary Sliced Milestone Progress Bar (ABOVE main bar, 4dp height)
                                    if (goal.showMilestoneProgressBar && milestones.isNotEmpty() && (goal.barBehavior == 2 || goal.barBehavior == 3)) {
                                        val spacing = 2.dp
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(spacing),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            milestones.forEach { milestone ->
                                                val isPassed = milestone.value <= displayPoints
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight()
                                                        .clip(RoundedCornerShape(2.dp))
                                                        .background(
                                                            if (isPassed) customColor
                                                            else if (isDark) Color(0xFF333038)
                                                            else Color(0xFFE6E0E9)
                                                        )
                                                )
                                            }
                                        }
                                    }

                                    // 2. Primary Continuous Progress Bar with Milestones and Goal Dot (BELOW milestone bar)
                                    BoxWithConstraints(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(16.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        val barWidth = maxWidth
                                        val trackColor = if (isDark) Color(0xFF333038) else Color(0xFFE6E0E9)
                                        val trackHeight = 11.dp
                                        val markerSize = 8.5.dp
                                        val halfMarker = markerSize / 2
                                        val goalDotSize = 16.dp
                                        val halfGoalDot = goalDotSize / 2

                                        // Background Track
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(trackHeight)
                                                .clip(RoundedCornerShape(trackHeight / 2))
                                                .background(trackColor)
                                        )

                                        // Active Progress Fill
                                        val activeFraction = progressFraction.coerceIn(0f, 1f)
                                        if (activeFraction > 0f) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(activeFraction)
                                                    .height(trackHeight)
                                                    .clip(RoundedCornerShape(trackHeight / 2))
                                                    .background(customColor)
                                            )
                                        }

                                        // Milestone glyph icons placed directly along the progress bar track
                                        milestones.forEach { milestone ->
                                            val isPassed = milestone.value <= displayPoints
                                            val isVisible = when (goal.barBehavior) {
                                                0 -> milestone.value.toFloat() <= currentMaxPointsValue
                                                1 -> true
                                                2 -> milestone.value <= rangeEnd
                                                3 -> milestone.value >= rangeStart && milestone.value <= rangeEnd
                                                else -> false
                                            } && (!goal.hidePassedMilestones || !isPassed)

                                            if (isVisible) {
                                                val fraction = when (goal.barBehavior) {
                                                    3 -> {
                                                        val denom = rangeEnd - rangeStart
                                                        if (denom > 0) (milestone.value - rangeStart) / denom else 0f
                                                    }
                                                    else -> {
                                                        if (rangeEnd > 0) milestone.value.toFloat() / rangeEnd else 0f
                                                    }
                                                }.coerceIn(0f, 1f)

                                                val offset = barWidth * fraction
                                                val clampedX = (offset - halfMarker).coerceIn(0.dp, (barWidth - markerSize).coerceAtLeast(0.dp))

                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.CenterStart)
                                                        .offset(x = clampedX),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    val passedColor = if (isDark) Color.Black else Color.White
                                                    MilestoneIconDisplay(
                                                        type = milestone.type,
                                                        tint = if (isPassed) passedColor else customColor,
                                                        size = markerSize
                                                    )
                                                }
                                            }
                                        }

                                        // Main Goal Target Dot placed on the progress bar track
                                        val goalVisible = when (goal.barBehavior) {
                                            0 -> true
                                            1 -> true
                                            2 -> currentMaxPointsValue <= rangeEnd
                                            3 -> currentMaxPointsValue >= rangeStart && currentMaxPointsValue <= rangeEnd
                                            else -> false
                                        }
                                        if (goalVisible) {
                                            val goalFraction = when (goal.barBehavior) {
                                                3 -> {
                                                    val denom = rangeEnd - rangeStart
                                                    if (denom > 0) (currentMaxPointsValue - rangeStart) / denom else 0f
                                                }
                                                else -> {
                                                    if (rangeEnd > 0) currentMaxPointsValue / rangeEnd else 1f
                                                }
                                            }.coerceIn(0f, 1f)

                                            val goalOffset = barWidth * goalFraction
                                            val clampedGoalX = (goalOffset - halfGoalDot).coerceIn(0.dp, (barWidth - goalDotSize).coerceAtLeast(0.dp))

                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.CenterStart)
                                                    .offset(x = clampedGoalX),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(goalDotSize)
                                                        .clip(CircleShape)
                                                        .background(customColor)
                                                        .border(2.dp, if (isDark) Color(0xFF1D1B20) else Color.White, CircleShape)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Remaining count indicator badge (+N)
                                val isCountActive = goal.showRemainingCount && (goal.barBehavior == 2 || goal.barBehavior == 3)
                                val remainingMilestones = milestones.count { it.value > goal.currentPoints }
                                if (isCountActive && remainingMilestones > 0) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = customColor.copy(alpha = 0.15f),
                                        contentColor = customColor,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "+$remainingMilestones",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Plus Button
                        HoldableIconButton(
                            onClick = onIncrement,
                            onLongClick = onHoldIncrement,
                            contentDescription = "Increase points (Hold to customize)",
                            testTag = "plus_button_${goal.id}",
                            containerColor = customColor.copy(alpha = if (isDark) 0.22f else 0.15f),
                            contentColor = customColor
                        ) {
                            Text(
                                text = "+",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp,
                                    color = customColor
                                )
                            )
                        }
                    }
                }
            }

            // Description Below
            if (goal.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = goal.description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F),
                        lineHeight = 20.sp,
                        fontSize = 14.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    // 1. Expanding Border Wave Effect (Expands fast then slow, fades out in 2.5s)
    if (celebrationAnim.value > 0f && celebrationAnim.value < 1f) {
        val animT = celebrationAnim.value
        val deceleratedT = 1f - (1f - animT) * (1f - animT)
        val borderScale = 1.0f + 0.12f * deceleratedT
        val borderAlpha = (1f - animT).coerceIn(0f, 1f)

        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer(
                    scaleX = borderScale,
                    scaleY = borderScale,
                    alpha = borderAlpha
                )
                .border(
                    width = 2.5.dp,
                    color = effectiveBorderColor,
                    shape = RoundedCornerShape(28.dp)
                )
        )
    }

    // 2. Particle Effects (5-10 current milestone icons on milestone, 20-30 random icons on goal complete)
    if (particles.isNotEmpty() && celebrationAnim.value > 0f && celebrationAnim.value < 1f) {
        val density = LocalDensity.current
        val animT = celebrationAnim.value
        val timeSec = animT * 2.5f
        val particleProgress = animT.coerceIn(0f, 1f)
        val particleAlpha = (1f - particleProgress).coerceIn(0f, 1f)

        Canvas(
            modifier = Modifier
                .matchParentSize()
                .zIndex(20f)
        ) {
            val widthPx = size.width
            val heightPx = size.height

            particles.forEach { particle ->
                val startX = particle.startXRatio * widthPx
                val startY = particle.startYRatio * heightPx
                val vxPx = with(density) { particle.vxDp.dp.toPx() }
                val vyPx = with(density) { particle.vyDp.dp.toPx() }
                val gravityPx = with(density) { 45.dp.toPx() }

                val currentX = startX + vxPx * timeSec
                val currentY = startY + vyPx * timeSec + 0.5f * gravityPx * timeSec * timeSec
                val initialSizePx = with(density) { particle.initialSizeDp.dp.toPx() }
                val currentSizePx = initialSizePx * (1f - particleProgress)
                val currentRotation = particle.initialRotation + particle.rotationSpeed * timeSec

                drawMilestoneParticle(
                    type = particle.type,
                    color = customColor,
                    sizePx = currentSizePx,
                    center = androidx.compose.ui.geometry.Offset(currentX, currentY),
                    rotation = currentRotation,
                    alpha = particleAlpha
                )
            }
        }
    }

    // 3. Celebratory Big Dot in Top-Left Corner
    if (bigDotScaleAnim.value > 0.01f) {
        val dotScale = bigDotScaleAnim.value
        val dotAlpha = (dotScale * 1.5f).coerceIn(0f, 1f)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 12.dp, y = 12.dp)
                .graphicsLayer(
                    scaleX = dotScale,
                    scaleY = dotScale,
                    alpha = dotAlpha
                )
                .size(38.dp)
                .clip(CircleShape)
                .background(customColor)
                .border(1.5.dp, if (isDark) Color(0xFF1D1B20) else Color.White, CircleShape)
                .zIndex(30f),
            contentAlignment = Alignment.Center
        ) {
            val passedColor = if (isDark) Color.Black else Color.White
            MilestoneIconDisplay(
                type = celebrationMilestoneType,
                tint = passedColor,
                size = 20.dp
            )
        }
    }
}
}

private fun borderStrokeForCard(secondaryColorHex: String): androidx.compose.foundation.BorderStroke? {
    if (secondaryColorHex == "DEFAULT") return null
    return try {
        val color = Color(android.graphics.Color.parseColor(secondaryColorHex)).copy(alpha = 0.2f)
        androidx.compose.foundation.BorderStroke(1.dp, color)
    } catch (e: Exception) {
        null
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HoldableIconButton(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    contentDescription: String,
    testTag: String,
    modifier: Modifier = Modifier,
    containerColor: Color = Color(0xFFEADDFF),
    contentColor: Color = Color(0xFF21005D),
    content: @Composable () -> Unit
) {
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongClick by rememberUpdatedState(onLongClick)

    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .size(48.dp)
            .testTag(testTag)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = { currentOnClick() },
                onLongClick = { currentOnLongClick() }
            )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            content()
        }
    }
}

@Composable
fun NoteCard(
    note: Note,
    isDark: Boolean,
    onEdit: () -> Unit = {},
    onDuplicate: () -> Unit = {},
    onDelete: () -> Unit,
    onCopy: () -> Unit = {},
    outlineColorHex: String? = null,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    onToggleExpand: (() -> Unit)? = null
) {
    var showCardMenu by remember { mutableStateOf(false) }
    val cardBgColor = getCardBackgroundColor(isDark, note.secondaryColorHex)
    val mainColor = try {
        Color(android.graphics.Color.parseColor(note.colorHex))
    } catch (e: Exception) {
        if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
    }

    val cardBorder = if (note.cardBorderHex != "NONE") {
        val strokeColor = try { Color(android.graphics.Color.parseColor(note.cardBorderHex)) } catch (e: Exception) { mainColor }
        BorderStroke(2.5.dp, strokeColor)
    } else if (outlineColorHex != null) {
        val outColor = try {
            Color(android.graphics.Color.parseColor(outlineColorHex))
        } catch (e: Exception) {
            mainColor
        }
        BorderStroke(2.5.dp, outColor)
    } else {
        if (isDark) null else borderStrokeForCard(note.secondaryColorHex)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        shape = RoundedCornerShape(16.dp),
        border = cardBorder,
        modifier = modifier
            .fillMaxWidth()
            .testTag("note_card_${note.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (note.imageUri != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    AsyncImage(
                        model = note.imageUri,
                        contentDescription = "Note Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        cardBgColor.copy(alpha = 0f),
                                        cardBgColor.copy(alpha = 0.5f),
                                        cardBgColor
                                    )
                                )
                            )
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        Text(
                            text = note.text,
                            fontSize = note.textSize.sp,
                            color = if (isDark) Color.White else Color(0xFF1D1B20),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showCardMenu = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Note Options",
                                tint = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F)
                            )
                        }

                        DropdownMenu(
                            expanded = showCardMenu,
                            onDismissRequest = { showCardMenu = false },
                            containerColor = if (isDark) Color(0xFF2C2A30) else Color(0xFFFFFFFF)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                onClick = {
                                    showCardMenu = false
                                    onEdit()
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                            )
                            DropdownMenuItem(
                                text = { Text("Duplicate") },
                                onClick = {
                                    showCardMenu = false
                                    onDuplicate()
                                },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate") }
                            )
                            if (onToggleExpand != null) {
                                DropdownMenuItem(
                                    text = { Text(if (isExpanded) "Shrink Card" else "Expand Card") },
                                    onClick = {
                                        showCardMenu = false
                                        onToggleExpand()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = null
                                        )
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Copy") },
                                onClick = {
                                    showCardMenu = false
                                    onCopy()
                                },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = "Copy") }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    showCardMenu = false
                                    onDelete()
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red) }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatGroupPoint(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format("%.1f", value)
    }
}

@Composable
fun GroupCard(
    group: GoalGroup,
    isDark: Boolean,
    groupGoalStats: List<com.example.data.GroupGoalStats>,
    allGoals: List<com.example.data.Goal> = emptyList(),
    onEnter: () -> Unit,
    onEdit: () -> Unit = {},
    onDuplicate: () -> Unit = {},
    onResetGoals: () -> Unit = {},
    onCompleteGoals: () -> Unit = {},
    onDelete: () -> Unit,
    onCopy: () -> Unit = {},
    outlineColorHex: String? = null,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    onToggleExpand: (() -> Unit)? = null,
    isBrighterText: Boolean = false,
    onShowStats: (() -> Unit)? = null
) {
    var showCardMenu by remember { mutableStateOf(false) }
    val cardBgColor = getCardBackgroundColor(isDark, group.secondaryColorHex)
    val mainColor = try {
        Color(android.graphics.Color.parseColor(group.colorHex))
    } catch (e: Exception) {
        if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
    }

    val cardBorder = if (group.cardBorderHex != "NONE") {
        val strokeColor = try { Color(android.graphics.Color.parseColor(group.cardBorderHex)) } catch (e: Exception) { mainColor }
        BorderStroke(2.5.dp, strokeColor)
    } else if (outlineColorHex != null) {
        val outColor = try {
            Color(android.graphics.Color.parseColor(outlineColorHex))
        } catch (e: Exception) {
            mainColor
        }
        BorderStroke(2.5.dp, outColor)
    } else {
        if (isDark) null else borderStrokeForCard(group.secondaryColorHex)
    }

    val stats = groupGoalStats.find { it.parentGroupId == group.id }
    val totalGoals = stats?.totalGoals ?: 0
    val completedGoals = stats?.completedGoals ?: 0

    val isPointsTracking = group.displayType == 1
    val childGoals = if (isPointsTracking) allGoals.filter { it.parentGroupId == group.id } else emptyList()
    val maxTotalPoints = childGoals.sumOf { it.groupValue }
    val currentTotalPoints = childGoals.sumOf {
        if (it.maxPoints > 0) {
            val fraction = it.currentPoints.toDouble() / it.maxPoints.toDouble()
            if (it.allowGroupPointsOverflow) fraction * it.groupValue else minOf(it.groupValue, fraction * it.groupValue)
        } else 0.0
    }

    val progressText = if (isPointsTracking) {
        val currentStr = formatGroupPoint(currentTotalPoints)
        val maxStr = formatGroupPoint(maxTotalPoints)
        "$currentStr/$maxStr Group points achieved"
    } else {
        "$completedGoals/$totalGoals Goals completed"
    }

    val progressFraction = if (isPointsTracking) {
        if (maxTotalPoints > 0.0) (currentTotalPoints / maxTotalPoints).toFloat().coerceIn(0f, 1f) else 0f
    } else {
        if (totalGoals > 0) completedGoals.toFloat() / totalGoals else 0f
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        shape = RoundedCornerShape(16.dp),
        border = cardBorder,
        modifier = modifier
            .fillMaxWidth()
            .clickable { onEnter() }
            .testTag("group_card_${group.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                if (group.imageUri != null) {
                    AsyncImage(
                        model = group.imageUri,
                        contentDescription = "Group Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(mainColor.copy(alpha = 0.25f))
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    cardBgColor.copy(alpha = 0f),
                                    cardBgColor.copy(alpha = 0.7f),
                                    cardBgColor
                                ),
                                endY = 400f
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onShowStats != null) {
                        IconButton(
                            onClick = { onShowStats() },
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                                .size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Group Stats",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Box {
                        IconButton(
                            onClick = { showCardMenu = true },
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                                .size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Group Options",
                                tint = Color.White
                            )
                        }

                        DropdownMenu(
                            expanded = showCardMenu,
                            onDismissRequest = { showCardMenu = false },
                            containerColor = if (isDark) Color(0xFF2C2A30) else Color(0xFFFFFFFF)
                        ) {
                        DropdownMenuItem(
                            text = { Text("Edit Group") },
                            onClick = {
                                showCardMenu = false
                                onEdit()
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicate Group & Contents") },
                            onClick = {
                                showCardMenu = false
                                onDuplicate()
                            },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate") }
                        )
                        if (onToggleExpand != null) {
                            DropdownMenuItem(
                                text = { Text(if (isExpanded) "Shrink Card" else "Expand Card") },
                                onClick = {
                                    showCardMenu = false
                                    onToggleExpand()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null
                                    )
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Reset Group (All Goals)") },
                            onClick = {
                                showCardMenu = false
                                onResetGoals()
                            },
                            leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = "Reset") }
                        )
                        DropdownMenuItem(
                            text = { Text("Complete Group (All Goals)") },
                            onClick = {
                                showCardMenu = false
                                onCompleteGoals()
                            },
                            leadingIcon = { Icon(Icons.Default.Check, contentDescription = "Complete") }
                        )
                        DropdownMenuItem(
                            text = { Text("Copy") },
                            onClick = {
                                showCardMenu = false
                                onCopy()
                            },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = "Copy") }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Group") },
                            onClick = {
                                showCardMenu = false
                                onDelete()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red) }
                        )
                    }
                }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = group.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = if (isDark) Color.White else Color(0xFF1D1B20)
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (group.description.isNotEmpty()) {
                    Text(
                        text = group.description,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F)
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                Text(
                    text = progressText,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = mainColor,
                        fontSize = 18.sp
                    ),
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isDark) Color(0xFF381E72).copy(alpha = 0.3f) else Color(0xFFE8DEF8))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressFraction)
                            .background(mainColor)
                    )
                }
            }
        }
    }
}

@Composable
fun CompactGroupCard(
    group: GoalGroup,
    isDark: Boolean,
    groupGoalStats: List<com.example.data.GroupGoalStats>,
    allGoals: List<com.example.data.Goal> = emptyList(),
    isSortingModeEnabled: Boolean = false,
    onToggleSorting: (() -> Unit)? = null,
    onTakeSnapshot: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val cardBgColor = getCardBackgroundColor(isDark, group.secondaryColorHex)
    val mainColor = try {
        Color(android.graphics.Color.parseColor(group.colorHex))
    } catch (e: Exception) {
        if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
    }

    val stats = groupGoalStats.find { it.parentGroupId == group.id }
    val totalGoals = stats?.totalGoals ?: 0
    val completedGoals = stats?.completedGoals ?: 0

    val isPointsTracking = group.displayType == 1
    val childGoals = if (isPointsTracking) allGoals.filter { it.parentGroupId == group.id } else emptyList()
    val maxTotalPoints = childGoals.sumOf { it.groupValue }
    val currentTotalPoints = childGoals.sumOf {
        if (it.maxPoints > 0) {
            val fraction = it.currentPoints.toDouble() / it.maxPoints.toDouble()
            if (it.allowGroupPointsOverflow) fraction * it.groupValue else minOf(it.groupValue, fraction * it.groupValue)
        } else 0.0
    }

    val progressText = if (isPointsTracking) {
        val currentStr = formatGroupPoint(currentTotalPoints)
        val maxStr = formatGroupPoint(maxTotalPoints)
        "$currentStr/$maxStr Group points achieved"
    } else {
        "$completedGoals/$totalGoals Goals completed"
    }

    val progressFraction = if (isPointsTracking) {
        if (maxTotalPoints > 0.0) (currentTotalPoints / maxTotalPoints).toFloat().coerceIn(0f, 1f) else 0f
    } else {
        if (totalGoals > 0) completedGoals.toFloat() / totalGoals else 0f
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("compact_group_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (group.imageUri != null) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    AsyncImage(
                        model = group.imageUri,
                        contentDescription = "Group Thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, cardBgColor.copy(alpha = 0.4f), cardBgColor)
                                )
                            )
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF1D1B20)
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = progressText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = mainColor,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isDark) Color(0xFF381E72).copy(alpha = 0.3f) else Color(0xFFE8DEF8))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressFraction)
                            .background(mainColor)
                    )
                }
            }

            if (onToggleSorting != null || onTakeSnapshot != null) {
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (onTakeSnapshot != null) {
                        IconButton(
                            onClick = onTakeSnapshot,
                            modifier = Modifier
                                .background(Color.Transparent, CircleShape)
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Take Snapshot",
                                tint = if (isDark) Color.White else Color(0xFF1D1B20),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    if (onToggleSorting != null) {
                        IconButton(
                            onClick = onToggleSorting,
                            modifier = Modifier
                                .background(
                                    color = if (isSortingModeEnabled) {
                                        if (isDark) Color(0xFF381E72) else Color(0xFFE8DEF8)
                                    } else {
                                        Color.Transparent
                                    },
                                    shape = CircleShape
                                )
                                .size(36.dp)
                                .testTag("group_toggle_sorting_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Toggle Sorting Mode",
                                tint = if (isSortingModeEnabled) {
                                    if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
                                } else {
                                    if (isDark) Color.White else Color(0xFF1D1B20)
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SortableCardContainer(
    item: DashboardItem,
    isDark: Boolean,
    isSortingModeEnabled: Boolean,
    currentGroupId: Int,
    viewModel: GoalViewModel,
    selectedItemForMove: DashboardItem?,
    onSelectItemForMove: (DashboardItem?) -> Unit,
    onScrollBy: (suspend (Float) -> Unit)? = null,
    content: @Composable () -> Unit
) {
    var showMenu by remember(item) { mutableStateOf(false) }

    val itemType = when (item) {
        is DashboardItem.GoalItem -> "goal"
        is DashboardItem.NoteItem -> "note"
        is DashboardItem.GroupItem -> "group"
    }

    val isSelectedForMove = selectedItemForMove != null && 
        selectedItemForMove.id == item.id && 
        ((selectedItemForMove is DashboardItem.GoalItem && item is DashboardItem.GoalItem) ||
         (selectedItemForMove is DashboardItem.NoteItem && item is DashboardItem.NoteItem) ||
         (selectedItemForMove is DashboardItem.GroupItem && item is DashboardItem.GroupItem))

    val currentItemState by rememberUpdatedState(item)
    val currentSelectedState by rememberUpdatedState(selectedItemForMove)
    val currentIsSelectedForMove by rememberUpdatedState(isSelectedForMove)
    val currentOnSelectItemForMove by rememberUpdatedState(onSelectItemForMove)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSortingModeEnabled) {
                    Modifier.pointerInput(isSortingModeEnabled) {
                        detectTapGestures(
                            onLongPress = {
                                currentOnSelectItemForMove(currentItemState)
                            },
                            onTap = {
                                val selected = currentSelectedState
                                if (selected != null) {
                                    if (currentIsSelectedForMove) {
                                        currentOnSelectItemForMove(null)
                                    } else {
                                        val source = selected
                                        val sourceType = when (source) {
                                            is DashboardItem.GoalItem -> "goal"
                                            is DashboardItem.NoteItem -> "note"
                                            is DashboardItem.GroupItem -> "group"
                                        }
                                        viewModel.moveItemBeforeTarget(source.id, sourceType, currentItemState.id, itemType, currentGroupId)
                                        currentOnSelectItemForMove(null)
                                    }
                                }
                            }
                        )
                    }
                } else Modifier
            )
    ) {
        // Render content card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isSelectedForMove) {
                        Modifier.border(2.5.dp, Color.White, RoundedCornerShape(16.dp))
                    } else Modifier
                )
        ) {
            content()
        }

        // Overlay the Sort options button on the top-left
        if (isSortingModeEnabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .zIndex(1f)
            ) {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .size(32.dp)
                        .testTag("sort_button_${itemType}_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = "Sort Options",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    containerColor = if (isDark) Color(0xFF2C2A30) else Color(0xFFFFFFFF)
                ) {
                    DropdownMenuItem(
                        text = { Text("Move Up") },
                        onClick = {
                            showMenu = false
                            viewModel.moveItemUp(item.id, itemType, currentGroupId)
                        },
                        leadingIcon = { Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text("Move Down") },
                        onClick = {
                            showMenu = false
                            viewModel.moveItemDown(item.id, itemType, currentGroupId)
                        },
                        leadingIcon = { Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    HorizontalDivider(color = if (isDark) Color(0xFF333038) else Color(0xFFE6E0E9))
                    DropdownMenuItem(
                        text = { Text("Move to Top") },
                        onClick = {
                            showMenu = false
                            viewModel.moveItemToTop(item.id, itemType, currentGroupId)
                        },
                        leadingIcon = { Icon(Icons.Default.VerticalAlignTop, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text("Move to Bottom") },
                        onClick = {
                            showMenu = false
                            viewModel.moveItemToBottom(item.id, itemType, currentGroupId)
                        },
                        leadingIcon = { Icon(Icons.Default.VerticalAlignBottom, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddNoteDialog(
    isDark: Boolean,
    noteToEdit: Note? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        text: String,
        textSize: Float,
        colorHex: String,
        secondaryColorHex: String,
        imageUri: String?,
        cardBorderHex: String
    ) -> Unit
) {
    var text by remember { mutableStateOf(noteToEdit?.text ?: "") }
    var textSize by remember { mutableStateOf(noteToEdit?.textSize ?: 14f) }
    var selectedColorHex by remember { mutableStateOf(noteToEdit?.colorHex ?: "#6750A4") }
    var selectedSecondaryColorHex by remember { mutableStateOf(noteToEdit?.secondaryColorHex ?: "DEFAULT") }
    var selectedCardBorderHex by remember { mutableStateOf(noteToEdit?.cardBorderHex ?: "NONE") }
    var selectedImageUri by remember { mutableStateOf<String?>(noteToEdit?.imageUri) }
    var activeTab by remember { mutableStateOf(0) }
    var hsvTargetPicker by remember { mutableStateOf<String?>(null) }

    if (hsvTargetPicker != null) {
        val initialColor = when (hsvTargetPicker) {
            "ACCENT" -> selectedColorHex
            "SECONDARY" -> if (selectedSecondaryColorHex == "DEFAULT") "#6750A4" else selectedSecondaryColorHex
            "BORDER" -> if (selectedCardBorderHex == "NONE") "#6750A4" else selectedCardBorderHex
            else -> "#6750A4"
        }
        HsvPickerDialog(
            initialColorHex = initialColor,
            onConfirm = { chosenHex ->
                when (hsvTargetPicker) {
                    "ACCENT" -> selectedColorHex = chosenHex
                    "SECONDARY" -> selectedSecondaryColorHex = chosenHex
                    "BORDER" -> selectedCardBorderHex = chosenHex
                }
                hsvTargetPicker = null
            },
            onDismiss = { hsvTargetPicker = null },
            isDark = isDark
        )
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = {
            focusManager.clearFocus()
            keyboardController?.hide()
            onDismiss()
        },
        containerColor = if (isDark) Color(0xFF1E1C24) else Color(0xFFF3EDF7),
        shape = RoundedCornerShape(28.dp),
        title = {
            Column {
                Text(
                    text = if (noteToEdit != null) "Edit Note" else "Add New Note",
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.titleLarge.copy(color = if (isDark) Color.White else Color(0xFF1D1B20))
                )
                Spacer(modifier = Modifier.height(12.dp))
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = Color.Transparent,
                    contentColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("General", style = MaterialTheme.typography.labelMedium) }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("Styles", style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (activeTab == 0) {
                    Column {
                        OutlinedTextField(
                            value = text,
                            onValueChange = { if (it.length <= 1000) text = it },
                            label = { Text("Note Content") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .testTag("note_text_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = if (isDark) Color.White else Color(0xFF1D1B20),
                                unfocusedTextColor = if (isDark) Color.White else Color(0xFF1D1B20)
                            )
                        )
                        Text(
                            text = "${text.length}/1000",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F),
                            modifier = Modifier.align(Alignment.End).padding(top = 2.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Text Size: ${textSize.toInt()} sp",
                            style = MaterialTheme.typography.titleSmall.copy(color = if (isDark) Color.White else Color(0xFF1D1B20)),
                            fontWeight = FontWeight.Medium
                        )
                        Slider(
                            value = textSize,
                            onValueChange = { textSize = it },
                            valueRange = 12f..35f,
                            modifier = Modifier.testTag("note_size_slider")
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Note Image",
                            style = MaterialTheme.typography.titleSmall.copy(color = if (isDark) Color.White else Color(0xFF1D1B20)),
                            fontWeight = FontWeight.Medium
                        )
                        if (selectedImageUri != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { imagePickerLauncher.launch("image/*") }
                            ) {
                                AsyncImage(
                                    model = selectedImageUri,
                                    contentDescription = "Selected Image",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                IconButton(
                                    onClick = { selectedImageUri = null },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                        .size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear Image",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        } else {
                            Button(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDark) Color(0xFF381E72) else Color(0xFFE8DEF8),
                                    contentColor = if (isDark) Color(0xFFE8DEF8) else Color(0xFF381E72)
                                )
                            ) {
                                Icon(Icons.Default.Image, contentDescription = "Select Image")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Choose an image")
                            }
                        }
                    }
                } else {
                    val colorsList = listOf("#6750A4", "#0288D1", "#2E7D32", "#EF6C00", "#C2185B", "#00796B", "#FBC02D", "#8D6E63", "#78909C", "#26A69A", "#EC407A", "#FF7043")
                    val bordersList = listOf("#FFD700", "#6750A4", "#0288D1", "#2E7D32", "#EF6C00", "#C2185B", "#00796B", "#FBC02D", "#8D6E63", "#78909C", "#26A69A", "#EC407A", "#FF7043")
                    Column {
                        Text(
                            text = "Pick Note Accent Color",
                            style = MaterialTheme.typography.titleSmall.copy(color = if (isDark) Color.White else Color(0xFF1D1B20)),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        ) {
                            val colorsList = listOf("#6750A4", "#0288D1", "#2E7D32", "#EF6C00", "#C2185B", "#00796B", "#FBC02D", "#8D6E63", "#78909C", "#26A69A", "#EC407A", "#FF7043")
                            colorsList.forEach { hex ->
                                val color = Color(android.graphics.Color.parseColor(hex))
                                val isSelected = selectedColorHex == hex
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 2.5.dp else 0.dp,
                                            color = if (isSelected) (if (isDark) Color.White else Color(0xFF1D1B20)) else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedColorHex = hex },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            // Custom HSV Option
                            val isCustomActive = selectedColorHex !in colorsList
                            CustomColorWheelIcon(
                                isCustomActive = isCustomActive,
                                customColorHex = selectedColorHex,
                                onClick = { hsvTargetPicker = "ACCENT" },
                                isDark = isDark
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "Pick Card Background (Secondary Color)",
                            style = MaterialTheme.typography.titleSmall.copy(color = if (isDark) Color.White else Color(0xFF1D1B20)),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0xFF2C2A30) else Color(0xFFE0E0E0))
                                    .border(
                                        width = if (selectedSecondaryColorHex == "DEFAULT") 2.5.dp else 1.dp,
                                        color = if (selectedSecondaryColorHex == "DEFAULT") (if (isDark) Color.White else Color(0xFF1D1B20)) else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedSecondaryColorHex = "DEFAULT" },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Def",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color.White else Color(0xFF1D1B20)
                                    )
                                )
                            }

                            val colorsList = listOf("#6750A4", "#0288D1", "#2E7D32", "#EF6C00", "#C2185B", "#00796B", "#FBC02D", "#8D6E63", "#78909C", "#26A69A", "#EC407A", "#FF7043")
                            colorsList.forEach { hex ->
                                val color = Color(android.graphics.Color.parseColor(hex))
                                val isSelected = selectedSecondaryColorHex == hex
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 2.5.dp else 0.dp,
                                            color = if (isSelected) (if (isDark) Color.White else Color(0xFF1D1B20)) else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedSecondaryColorHex = hex },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            // Custom HSV Option for Background
                            val isCustomActive = selectedSecondaryColorHex != "DEFAULT" && selectedSecondaryColorHex !in colorsList
                            CustomColorWheelIcon(
                                isCustomActive = isCustomActive,
                                customColorHex = selectedSecondaryColorHex,
                                onClick = { hsvTargetPicker = "SECONDARY" },
                                isDark = isDark
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "Card Border Custom Styling",
                            style = MaterialTheme.typography.titleSmall.copy(color = if (isDark) Color.White else Color(0xFF1D1B20)),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0xFF1D1B20) else Color.White)
                                    .border(
                                        width = if (selectedCardBorderHex == "NONE") 2.5.dp else 1.dp,
                                        color = if (selectedCardBorderHex == "NONE") (if (isDark) Color.White else Color(0xFF1D1B20)) else (if (isDark) Color(0xFF49454F) else Color(0xFFCAC4D0)),
                                        shape = CircleShape
                                    )
                                    .clickable { selectedCardBorderHex = "NONE" },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "None",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color.White else Color(0xFF1D1B20)
                                    )
                                )
                            }

                            val bordersList = listOf("#FFD700", "#6750A4", "#0288D1", "#2E7D32", "#EF6C00", "#C2185B", "#00796B", "#FBC02D", "#8D6E63", "#78909C", "#26A69A", "#EC407A", "#FF7043")
                            bordersList.forEach { hex ->
                                val color = Color(android.graphics.Color.parseColor(hex))
                                val isSelected = selectedCardBorderHex == hex
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(color.copy(alpha = 0.2f))
                                        .border(
                                            width = if (isSelected) 3.dp else 1.5.dp,
                                            color = color,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedCardBorderHex = hex },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = color,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            // Custom HSV Option for Border
                            val isCustomActive = selectedCardBorderHex != "NONE" && selectedCardBorderHex !in bordersList
                            CustomColorWheelIcon(
                                isCustomActive = isCustomActive,
                                customColorHex = selectedCardBorderHex,
                                onClick = { hsvTargetPicker = "BORDER" },
                                isDark = isDark
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    onConfirm(
                        text,
                        textSize,
                        selectedColorHex,
                        selectedSecondaryColorHex,
                        selectedImageUri,
                        selectedCardBorderHex
                    )
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
                ),
                enabled = text.isNotBlank()
            ) {
                Text("Confirm", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    onDismiss()
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
                )
            ) {
                Text("Cancel", style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddGroupDialog(
    isDark: Boolean,
    groupToEdit: GoalGroup? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        description: String,
        colorHex: String,
        secondaryColorHex: String,
        imageUri: String?,
        applyBg: Boolean,
        outlineMainColor: Boolean,
        cardBorderHex: String,
        displayType: Int
    ) -> Unit
) {
    var title by remember { mutableStateOf(groupToEdit?.title ?: "") }
    var description by remember { mutableStateOf(groupToEdit?.description ?: "") }
    var selectedColorHex by remember { mutableStateOf(groupToEdit?.colorHex ?: "#6750A4") }
    var selectedSecondaryColorHex by remember { mutableStateOf(groupToEdit?.secondaryColorHex ?: "DEFAULT") }
    var selectedCardBorderHex by remember { mutableStateOf(groupToEdit?.cardBorderHex ?: "NONE") }
    var selectedImageUri by remember { mutableStateOf<String?>(groupToEdit?.imageUri) }
    var applyBg by remember { mutableStateOf(groupToEdit?.applyBg ?: true) }
    var outlineMainColor by remember { mutableStateOf(groupToEdit?.outlineMainColor ?: false) }
    var displayType by remember { mutableStateOf(groupToEdit?.displayType ?: 0) }
    var activeTab by remember { mutableStateOf(0) }
    var hsvTargetPicker by remember { mutableStateOf<String?>(null) }

    if (hsvTargetPicker != null) {
        val initialColor = when (hsvTargetPicker) {
            "ACCENT" -> selectedColorHex
            "SECONDARY" -> if (selectedSecondaryColorHex == "DEFAULT") "#6750A4" else selectedSecondaryColorHex
            "BORDER" -> if (selectedCardBorderHex == "NONE") "#6750A4" else selectedCardBorderHex
            else -> "#6750A4"
        }
        HsvPickerDialog(
            initialColorHex = initialColor,
            onConfirm = { chosenHex ->
                when (hsvTargetPicker) {
                    "ACCENT" -> selectedColorHex = chosenHex
                    "SECONDARY" -> selectedSecondaryColorHex = chosenHex
                    "BORDER" -> selectedCardBorderHex = chosenHex
                }
                hsvTargetPicker = null
            },
            onDismiss = { hsvTargetPicker = null },
            isDark = isDark
        )
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = {
            focusManager.clearFocus()
            keyboardController?.hide()
            onDismiss()
        },
        containerColor = if (isDark) Color(0xFF1E1C24) else Color(0xFFF3EDF7),
        shape = RoundedCornerShape(28.dp),
        title = {
            Column {
                Text(
                    text = if (groupToEdit != null) "Edit Group" else "Add New Group",
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.titleLarge.copy(color = if (isDark) Color.White else Color(0xFF1D1B20))
                )
                Spacer(modifier = Modifier.height(12.dp))
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = Color.Transparent,
                    contentColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("General", style = MaterialTheme.typography.labelMedium) }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("Styles", style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (activeTab == 0) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Group Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("group_title_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = if (isDark) Color.White else Color(0xFF1D1B20),
                            unfocusedTextColor = if (isDark) Color.White else Color(0xFF1D1B20)
                        )
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Group Description") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("group_desc_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = if (isDark) Color.White else Color(0xFF1D1B20),
                            unfocusedTextColor = if (isDark) Color.White else Color(0xFF1D1B20)
                        )
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { outlineMainColor = !outlineMainColor }
                    ) {
                        Checkbox(
                            checked = outlineMainColor,
                            onCheckedChange = { outlineMainColor = it },
                            modifier = Modifier.testTag("group_outline_checkbox")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Outline all contents with main color",
                            style = MaterialTheme.typography.bodyMedium.copy(color = if (isDark) Color.White else Color(0xFF1D1B20))
                        )
                    }

                    // Progress display type dropdown
                    var showDisplayDropdown by remember { mutableStateOf(false) }
                    val displayOptions = listOf("Classic goal completion", "Group points tracking")
                    
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Progress Display Mode",
                            style = MaterialTheme.typography.titleSmall.copy(color = if (isDark) Color.White else Color(0xFF1D1B20)),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isDark) Color(0xFF2C2A30) else Color(0xFFE8DEF8), RoundedCornerShape(8.dp))
                                .border(1.dp, if (isDark) Color(0xFF49454F) else Color(0xFFCAC4D0), RoundedCornerShape(8.dp))
                                .clickable { showDisplayDropdown = true }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = displayOptions[displayType],
                                    style = MaterialTheme.typography.bodyLarge.copy(color = if (isDark) Color.White else Color(0xFF1D1B20))
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = if (isDark) Color.White else Color(0xFF1D1B20)
                                )
                            }
                            DropdownMenu(
                                expanded = showDisplayDropdown,
                                onDismissRequest = { showDisplayDropdown = false },
                                modifier = Modifier.fillMaxWidth(0.8f).background(if (isDark) Color(0xFF2C2A30) else Color.White)
                            ) {
                                displayOptions.forEachIndexed { idx, opt ->
                                    DropdownMenuItem(
                                        text = { Text(opt, style = MaterialTheme.typography.bodyMedium.copy(color = if (isDark) Color.White else Color(0xFF1D1B20))) },
                                        onClick = {
                                            displayType = idx
                                            showDisplayDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Group Image",
                            style = MaterialTheme.typography.titleSmall.copy(color = if (isDark) Color.White else Color(0xFF1D1B20)),
                            fontWeight = FontWeight.Medium
                        )
                        if (selectedImageUri != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { imagePickerLauncher.launch("image/*") }
                            ) {
                                AsyncImage(
                                    model = selectedImageUri,
                                    contentDescription = "Selected Image",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                IconButton(
                                    onClick = { selectedImageUri = null },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                        .size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear Image",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        } else {
                            Button(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDark) Color(0xFF381E72) else Color(0xFFE8DEF8),
                                    contentColor = if (isDark) Color(0xFFE8DEF8) else Color(0xFF381E72)
                                )
                            ) {
                                Icon(Icons.Default.Image, contentDescription = "Select Image")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Choose an image")
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { applyBg = !applyBg }
                    ) {
                        Checkbox(
                            checked = applyBg,
                            onCheckedChange = { applyBg = it },
                            modifier = Modifier.testTag("group_bg_checkbox")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Apply image as dashboard background",
                            style = MaterialTheme.typography.bodyMedium.copy(color = if (isDark) Color.White else Color(0xFF1D1B20))
                        )
                    }
                } else {
                    val colorsList = listOf("#6750A4", "#0288D1", "#2E7D32", "#EF6C00", "#C2185B", "#00796B", "#FBC02D", "#8D6E63", "#78909C", "#26A69A", "#EC407A", "#FF7043")
                    val bordersList = listOf("#FFD700", "#6750A4", "#0288D1", "#2E7D32", "#EF6C00", "#C2185B", "#00796B", "#FBC02D", "#8D6E63", "#78909C", "#26A69A", "#EC407A", "#FF7043")
                    Column {
                        Text(
                            text = "Pick Group Accent Color",
                            style = MaterialTheme.typography.titleSmall.copy(color = if (isDark) Color.White else Color(0xFF1D1B20)),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        ) {
                            val colorsList = listOf("#6750A4", "#0288D1", "#2E7D32", "#EF6C00", "#C2185B", "#00796B", "#FBC02D", "#8D6E63", "#78909C", "#26A69A", "#EC407A", "#FF7043")
                            colorsList.forEach { hex ->
                                val color = Color(android.graphics.Color.parseColor(hex))
                                val isSelected = selectedColorHex == hex
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 2.5.dp else 0.dp,
                                            color = if (isSelected) (if (isDark) Color.White else Color(0xFF1D1B20)) else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedColorHex = hex },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            // Custom HSV Option
                            val isCustomActive = selectedColorHex !in colorsList
                            CustomColorWheelIcon(
                                isCustomActive = isCustomActive,
                                customColorHex = selectedColorHex,
                                onClick = { hsvTargetPicker = "ACCENT" },
                                isDark = isDark
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "Pick Card Background (Secondary Color)",
                            style = MaterialTheme.typography.titleSmall.copy(color = if (isDark) Color.White else Color(0xFF1D1B20)),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0xFF2C2A30) else Color(0xFFE0E0E0))
                                    .border(
                                        width = if (selectedSecondaryColorHex == "DEFAULT") 2.5.dp else 1.dp,
                                        color = if (selectedSecondaryColorHex == "DEFAULT") (if (isDark) Color.White else Color(0xFF1D1B20)) else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedSecondaryColorHex = "DEFAULT" },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Def",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color.White else Color(0xFF1D1B20)
                                    )
                                )
                            }

                            val colorsList = listOf("#6750A4", "#0288D1", "#2E7D32", "#EF6C00", "#C2185B", "#00796B", "#FBC02D", "#8D6E63", "#78909C", "#26A69A", "#EC407A", "#FF7043")
                            colorsList.forEach { hex ->
                                val color = Color(android.graphics.Color.parseColor(hex))
                                val isSelected = selectedSecondaryColorHex == hex
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 2.5.dp else 0.dp,
                                            color = if (isSelected) (if (isDark) Color.White else Color(0xFF1D1B20)) else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedSecondaryColorHex = hex },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            // Custom HSV Option for Background
                            val isCustomActive = selectedSecondaryColorHex != "DEFAULT" && selectedSecondaryColorHex !in colorsList
                            CustomColorWheelIcon(
                                isCustomActive = isCustomActive,
                                customColorHex = selectedSecondaryColorHex,
                                onClick = { hsvTargetPicker = "SECONDARY" },
                                isDark = isDark
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "Card Border Custom Styling",
                            style = MaterialTheme.typography.titleSmall.copy(color = if (isDark) Color.White else Color(0xFF1D1B20)),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0xFF1D1B20) else Color.White)
                                    .border(
                                        width = if (selectedCardBorderHex == "NONE") 2.5.dp else 1.dp,
                                        color = if (selectedCardBorderHex == "NONE") (if (isDark) Color.White else Color(0xFF1D1B20)) else (if (isDark) Color(0xFF49454F) else Color(0xFFCAC4D0)),
                                        shape = CircleShape
                                    )
                                    .clickable { selectedCardBorderHex = "NONE" },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "None",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color.White else Color(0xFF1D1B20)
                                    )
                                )
                            }

                            val bordersList = listOf("#FFD700", "#6750A4", "#0288D1", "#2E7D32", "#EF6C00", "#C2185B", "#00796B", "#FBC02D", "#8D6E63", "#78909C", "#26A69A", "#EC407A", "#FF7043")
                            bordersList.forEach { hex ->
                                val color = Color(android.graphics.Color.parseColor(hex))
                                val isSelected = selectedCardBorderHex == hex
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(color.copy(alpha = 0.2f))
                                        .border(
                                            width = if (isSelected) 3.dp else 1.5.dp,
                                            color = color,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedCardBorderHex = hex },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = color,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            // Custom HSV Option for Border
                            val isCustomActive = selectedCardBorderHex != "NONE" && selectedCardBorderHex !in bordersList
                            CustomColorWheelIcon(
                                isCustomActive = isCustomActive,
                                customColorHex = selectedCardBorderHex,
                                onClick = { hsvTargetPicker = "BORDER" },
                                isDark = isDark
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    onConfirm(
                        title,
                        description,
                        selectedColorHex,
                        selectedSecondaryColorHex,
                        selectedImageUri,
                        applyBg,
                        outlineMainColor,
                        selectedCardBorderHex,
                        displayType
                    )
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
                ),
                enabled = title.isNotBlank()
            ) {
                Text("Confirm", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    onDismiss()
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
                )
            ) {
                Text("Cancel", style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun AddGoalDialog(
    isDark: Boolean,
    goalToEdit: Goal? = null,
    currentGroupId: Int = 0,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        description: String,
        current: Int,
        max: Int,
        allowExceed: Boolean,
        colorHex: String,
        secondaryColorHex: String,
        milestonesStr: String,
        barBehavior: Int,
        hidePassedMilestones: Boolean,
        showRemainingCount: Boolean,
        showMilestoneProgressBar: Boolean,
        progressType: Int,
        meterThickness: Float,
        dotsPerRow: Int,
        cardBorderHex: String,
        unit: String,
        statsEnabled: Boolean,
        streaksEnabled: Boolean,
        pointTrackingEnabled: Boolean,
        statsVisualType: Int,
        showHighlights: Boolean,
        groupValue: Double,
        allowGroupPointsOverflow: Boolean
    ) -> Unit
) {
    val isInsideGroup = currentGroupId != 0 || (goalToEdit != null && goalToEdit.parentGroupId != 0)
    var title by remember { mutableStateOf(goalToEdit?.title ?: "") }
    var description by remember { mutableStateOf(goalToEdit?.description ?: "") }
    var currentPointsStr by remember { mutableStateOf(goalToEdit?.currentPoints?.toString() ?: "") }
    var maxPointsStr by remember { mutableStateOf(goalToEdit?.maxPoints?.toString() ?: "") }
    var allowExceed by remember { mutableStateOf(goalToEdit?.allowExceed ?: false) }
    var allowGroupPointsOverflow by remember { mutableStateOf(goalToEdit?.allowGroupPointsOverflow ?: true) }
    var selectedColorHex by remember { mutableStateOf(goalToEdit?.colorHex ?: "#6750A4") }
    var selectedSecondaryColorHex by remember { mutableStateOf(goalToEdit?.secondaryColorHex ?: "DEFAULT") }
    var selectedCardBorderHex by remember { mutableStateOf(goalToEdit?.cardBorderHex ?: "NONE") }
    var unit by remember { mutableStateOf(goalToEdit?.unit ?: "pts") }
    var groupValueStr by remember {
        mutableStateOf(
            if (goalToEdit != null) {
                if (goalToEdit.groupValue == 1.0) "" else goalToEdit.groupValue.toString()
            } else ""
        )
    }

    // Stats configuration states
    var statsEnabled by remember { mutableStateOf(goalToEdit?.statsEnabled ?: true) }
    var streaksEnabled by remember { mutableStateOf(goalToEdit?.streaksEnabled ?: false) }
    var pointTrackingEnabled by remember { mutableStateOf(goalToEdit?.pointTrackingEnabled ?: true) }
    var statsVisualType by remember { mutableStateOf(goalToEdit?.statsVisualType ?: 0) }
    var showHighlights by remember { mutableStateOf(goalToEdit?.showHighlights ?: true) }

    // Milestones Tab state
    var milestonesList by remember { mutableStateOf(goalToEdit?.let { parseMilestones(it.milestonesStr) } ?: listOf<Milestone>()) }
    var barBehavior by remember { mutableStateOf(goalToEdit?.barBehavior ?: 0) }
    var hidePassedMilestones by remember { mutableStateOf(goalToEdit?.hidePassedMilestones ?: false) }
    var showRemainingCount by remember { mutableStateOf(goalToEdit?.showRemainingCount ?: true) }
    var showMilestoneProgressBar by remember { mutableStateOf(goalToEdit?.showMilestoneProgressBar ?: true) }

    // Types Tab state
    var progressType by remember { mutableStateOf(goalToEdit?.progressType ?: 0) }
    var meterThickness by remember { mutableStateOf(goalToEdit?.meterThickness ?: 14f) }
    var dotsPerRow by remember { mutableStateOf(goalToEdit?.dotsPerRow ?: 8) }

    var activeTab by remember { mutableStateOf(0) } // 0 = General, 1 = Types, 2 = Milestones, 3 = Styles
    var showAddMilestoneDialog by remember { mutableStateOf(false) }
    var showMultipleMilestonesDialog by remember { mutableStateOf(false) }

    var titleError by remember { mutableStateOf(false) }
    var maxPointsError by remember { mutableStateOf(false) }
    var hsvTargetPicker by remember { mutableStateOf<String?>(null) }

    if (hsvTargetPicker != null) {
        val initialColor = when (hsvTargetPicker) {
            "ACCENT" -> selectedColorHex
            "SECONDARY" -> if (selectedSecondaryColorHex == "DEFAULT") "#6750A4" else selectedSecondaryColorHex
            "BORDER" -> if (selectedCardBorderHex == "NONE") "#6750A4" else selectedCardBorderHex
            else -> "#6750A4"
        }
        HsvPickerDialog(
            initialColorHex = initialColor,
            onConfirm = { chosenHex ->
                when (hsvTargetPicker) {
                    "ACCENT" -> selectedColorHex = chosenHex
                    "SECONDARY" -> selectedSecondaryColorHex = chosenHex
                    "BORDER" -> selectedCardBorderHex = chosenHex
                }
                hsvTargetPicker = null
            },
            onDismiss = { hsvTargetPicker = null },
            isDark = isDark
        )
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    AlertDialog(
        onDismissRequest = {
            focusManager.clearFocus()
            keyboardController?.hide()
            onDismiss()
        },
        containerColor = if (isDark) Color(0xFF1E1C24) else Color(0xFFF3EDF7),
        shape = RoundedCornerShape(28.dp),
        title = {
            Column {
                Text(
                    text = if (goalToEdit != null) "Edit Goal" else "Add New Goal",
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.titleLarge.copy(color = if (isDark) Color.White else Color(0xFF1D1B20))
                )
                Spacer(modifier = Modifier.height(12.dp))
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = Color.Transparent,
                    contentColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("General", style = MaterialTheme.typography.labelMedium) }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("Types", style = MaterialTheme.typography.labelMedium) }
                    )
                    Tab(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        text = { Text("Milestones", style = MaterialTheme.typography.labelMedium) }
                    )
                    Tab(
                        selected = activeTab == 3,
                        onClick = { activeTab = 3 },
                        text = { Text("Styles", style = MaterialTheme.typography.labelMedium) }
                    )
                    Tab(
                        selected = activeTab == 4,
                        onClick = { activeTab = 4 },
                        text = { Text("Stats", style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (activeTab == 0) {
                    // General tab
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            titleError = it.isBlank()
                        },
                        label = { Text("Goal Title") },
                        isError = titleError,
                        supportingText = {
                            if (titleError) {
                                Text("Title cannot be empty")
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = if (isDark) Color.White else Color(0xFF1D1B20),
                            unfocusedTextColor = if (isDark) Color.White else Color(0xFF1D1B20),
                            focusedBorderColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF21005D),
                            unfocusedBorderColor = if (isDark) Color(0xFF938F99) else Color(0xFF6750A4),
                            focusedLabelColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF21005D),
                            unfocusedLabelColor = if (isDark) Color(0xFFCAC4D0) else Color(0xFF6750A4),
                            cursorColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_goal_title"),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = currentPointsStr,
                            onValueChange = { input ->
                                if (input.isEmpty() || input.all { it.isDigit() }) {
                                    currentPointsStr = input
                                }
                            },
                            label = { Text("Starting") },
                            placeholder = { Text("0", color = Color(0xFF9E9E9E)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = if (isDark) Color.White else Color(0xFF1D1B20),
                                unfocusedTextColor = if (isDark) Color.White else Color(0xFF1D1B20),
                                focusedBorderColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF21005D),
                                unfocusedBorderColor = if (isDark) Color(0xFF938F99) else Color(0xFF6750A4),
                                focusedLabelColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF21005D),
                                unfocusedLabelColor = if (isDark) Color(0xFFCAC4D0) else Color(0xFF6750A4),
                                cursorColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("add_goal_current_points"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = maxPointsStr,
                            onValueChange = { input ->
                                if (input.isEmpty() || input.all { it.isDigit() }) {
                                    maxPointsStr = input
                                    maxPointsError = (input.toIntOrNull() ?: 0) <= 0
                                }
                            },
                            label = { Text("Max Target") },
                            placeholder = { Text("10", color = Color(0xFF9E9E9E)) },
                            isError = maxPointsError,
                            supportingText = {
                                if (maxPointsError) {
                                    Text("Must be > 0")
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = if (isDark) Color.White else Color(0xFF1D1B20),
                                unfocusedTextColor = if (isDark) Color.White else Color(0xFF1D1B20),
                                focusedBorderColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF21005D),
                                unfocusedBorderColor = if (isDark) Color(0xFF938F99) else Color(0xFF6750A4),
                                focusedLabelColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF21005D),
                                unfocusedLabelColor = if (isDark) Color(0xFFCAC4D0) else Color(0xFF6750A4),
                                cursorColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("add_goal_max_points"),
                            singleLine = true
                        )
                    }

                    if (isInsideGroup) {
                        OutlinedTextField(
                            value = groupValueStr,
                            onValueChange = { input ->
                                if (input.isEmpty() || input.all { it.isDigit() || it == '.' }) {
                                    groupValueStr = input
                                }
                            },
                            label = { Text("Group value") },
                            placeholder = { Text("1", color = Color(0xFF9E9E9E)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = if (isDark) Color.White else Color(0xFF1D1B20),
                                unfocusedTextColor = if (isDark) Color.White else Color(0xFF1D1B20),
                                focusedBorderColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF21005D),
                                unfocusedBorderColor = if (isDark) Color(0xFF938F99) else Color(0xFF6750A4),
                                focusedLabelColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF21005D),
                                unfocusedLabelColor = if (isDark) Color(0xFFCAC4D0) else Color(0xFF6750A4),
                                cursorColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_goal_group_value"),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Counts") },
                        placeholder = { Text("pts", color = Color(0xFF9E9E9E)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = if (isDark) Color.White else Color(0xFF1D1B20),
                            unfocusedTextColor = if (isDark) Color.White else Color(0xFF1D1B20),
                            focusedBorderColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF21005D),
                            unfocusedBorderColor = if (isDark) Color(0xFF938F99) else Color(0xFF6750A4),
                            focusedLabelColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF21005D),
                            unfocusedLabelColor = if (isDark) Color(0xFFCAC4D0) else Color(0xFF6750A4),
                            cursorColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_goal_unit"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = if (isDark) Color.White else Color(0xFF1D1B20),
                            unfocusedTextColor = if (isDark) Color.White else Color(0xFF1D1B20),
                            focusedBorderColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF21005D),
                            unfocusedBorderColor = if (isDark) Color(0xFF938F99) else Color(0xFF6750A4),
                            focusedLabelColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF21005D),
                            unfocusedLabelColor = if (isDark) Color(0xFFCAC4D0) else Color(0xFF6750A4),
                            cursorColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_goal_description"),
                        maxLines = 3
                    )

                    // Checkbox for exceeding max points
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { allowExceed = !allowExceed }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = allowExceed,
                            onCheckedChange = { allowExceed = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4),
                                uncheckedColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4),
                                checkmarkColor = if (isDark) Color(0xFF381E72) else Color.White
                            ),
                            modifier = Modifier.testTag("add_goal_allow_exceed")
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Allow points to exceed target max",
                                style = MaterialTheme.typography.bodyMedium.copy(color = if (isDark) Color.White else Color(0xFF1D1B20)),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "If unchecked, current progress clamps at target max.",
                                style = MaterialTheme.typography.bodySmall.copy(color = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F))
                            )
                        }
                    }

                    if (isInsideGroup) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { allowGroupPointsOverflow = !allowGroupPointsOverflow }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = allowGroupPointsOverflow,
                                onCheckedChange = { allowGroupPointsOverflow = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4),
                                    uncheckedColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4),
                                    checkmarkColor = if (isDark) Color(0xFF381E72) else Color.White
                                ),
                                modifier = Modifier.testTag("add_goal_allow_group_overflow")
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Allow overflow of group points",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = if (isDark) Color.White else Color(0xFF1D1B20)),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Allow contribution to group points to exceed 100% if points exceed target max.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F))
                                )
                            }
                        }
                    }
                } else if (activeTab == 1) {
                    // Types tab
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Choose Progress Visualization",
                            style = MaterialTheme.typography.titleMedium.copy(color = if (isDark) Color.White else Color(0xFF1D1B20)),
                            fontWeight = FontWeight.SemiBold
                        )

                        // 1. Bar type
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { progressType = 0 },
                            colors = CardDefaults.cardColors(
                                containerColor = if (progressType == 0) {
                                    if (isDark) Color(0xFF381E72) else Color(0xFFE8DEF8)
                                } else {
                                    if (isDark) Color(0xFF2C2A30) else Color(0xFFF1EEF4)
                                }
                            ),
                            border = if (progressType == 0) BorderStroke(2.dp, if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)) else null
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = progressType == 0,
                                        onClick = { progressType = 0 },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Bar Type",
                                        style = MaterialTheme.typography.titleSmall.copy(color = if (isDark) Color.White else Color(0xFF1D1B20)),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "A standard, clean progress bar displaying milestones and targets horizontally along a linear track.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F))
                                )
                            }
                        }

                        // 2. Meter type
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { progressType = 1 },
                            colors = CardDefaults.cardColors(
                                containerColor = if (progressType == 1) {
                                    if (isDark) Color(0xFF381E72) else Color(0xFFE8DEF8)
                                } else {
                                    if (isDark) Color(0xFF2C2A30) else Color(0xFFF1EEF4)
                                }
                            ),
                            border = if (progressType == 1) BorderStroke(2.dp, if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)) else null
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = progressType == 1,
                                        onClick = { progressType = 1 },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Meter Type",
                                        style = MaterialTheme.typography.titleSmall.copy(color = if (isDark) Color.White else Color(0xFF1D1B20)),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "A speedometer-style circular gauge with customizable thickness. Perfect for highly visual progress indicators.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F))
                                )

                                if (progressType == 1) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Gauge Thickness: ${meterThickness.toInt()} dp",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = if (isDark) Color.White else Color(0xFF1D1B20),
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Slider(
                                        value = meterThickness,
                                        onValueChange = { meterThickness = it },
                                        valueRange = 4f..32f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4),
                                            activeTrackColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
                                        )
                                    )
                                }
                            }
                        }

                        // 3. Big Dots type
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { progressType = 2 },
                            colors = CardDefaults.cardColors(
                                containerColor = if (progressType == 2) {
                                    if (isDark) Color(0xFF381E72) else Color(0xFFE8DEF8)
                                } else {
                                    if (isDark) Color(0xFF2C2A30) else Color(0xFFF1EEF4)
                                }
                            ),
                            border = if (progressType == 2) BorderStroke(2.dp, if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)) else null
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = progressType == 2,
                                        onClick = { progressType = 2 },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Big Dots Type",
                                        style = MaterialTheme.typography.titleSmall.copy(color = if (isDark) Color.White else Color(0xFF1D1B20)),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "A grid of large circles representation, where each dot represents a step/point (caps at 100). The dots scale according to dots per row.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F))
                                )

                                if (progressType == 2) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Dots Per Row: $dotsPerRow",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = if (isDark) Color.White else Color(0xFF1D1B20),
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Slider(
                                        value = dotsPerRow.toFloat(),
                                        onValueChange = { dotsPerRow = it.toInt() },
                                        valueRange = 4f..20f,
                                        steps = 15,
                                        colors = SliderDefaults.colors(
                                            thumbColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4),
                                            activeTrackColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
                                        )
                                    )
                                }
                            }
                        }
                    }
                } else if (activeTab == 2) {
                    // Milestones tab
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .combinedClickable(
                                        onClick = { showAddMilestoneDialog = true },
                                        onLongClick = { showMultipleMilestonesDialog = true }
                                    ),
                                color = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFF381E72) else Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "+ Add Milestone",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color(0xFF381E72) else Color.White
                                        )
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "(Hold to add multiple milestones)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4),
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }

                        // Milestones List
                        if (milestonesList.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No milestones added yet.",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F))
                                )
                            }
                        } else {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDark) Color(0xFF2C2A30) else Color(0xFFF1EEF4)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    milestonesList.forEach { milestone ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                MilestoneIconDisplay(
                                                    type = milestone.type,
                                                    tint = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Value: ${milestone.value} pts (${milestone.type.uppercase()})",
                                                    style = MaterialTheme.typography.bodyMedium.copy(color = if (isDark) Color.White else Color(0xFF1D1B20)),
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    milestonesList = milestonesList.filter { it != milestone }
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete",
                                                    tint = Color.Red.copy(alpha = 0.8f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Checkbox 0: Show milestone progress bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showMilestoneProgressBar = !showMilestoneProgressBar }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = showMilestoneProgressBar,
                                onCheckedChange = { showMilestoneProgressBar = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4),
                                    uncheckedColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4),
                                    checkmarkColor = if (isDark) Color(0xFF381E72) else Color.White
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Show milestone progress bar",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = if (isDark) Color.White else Color(0xFF1D1B20)),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Renders sliced segmented milestone progress bar when milestones exist and behavior is set to next milestone (Type 3 or 4).",
                                    style = MaterialTheme.typography.bodySmall.copy(color = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F))
                                )
                            }
                        }

                        // Progress Bar Behavior Dropdown
                        var showDropdown by remember { mutableStateOf(false) }
                        val behaviors = listOf(
                            "Entire bar with milestones before Goal (Default)",
                            "Entire bar showing all milestones past Goal",
                            "Up to next unreached milestone",
                            "Between last passed and next milestone"
                        )
                        Column {
                            Text(
                                text = "Progress Bar Behavior",
                                style = MaterialTheme.typography.titleSmall.copy(color = if (isDark) Color.White else Color(0xFF1D1B20)),
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, if (isDark) Color(0xFF938F99) else Color(0xFF79747E), RoundedCornerShape(8.dp))
                                    .clickable { showDropdown = true }
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = behaviors[barBehavior],
                                        style = MaterialTheme.typography.bodyMedium.copy(color = if (isDark) Color.White else Color(0xFF1D1B20)),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = if (isDark) Color.White else Color(0xFF49454F)
                                    )
                                }
                                DropdownMenu(
                                    expanded = showDropdown,
                                    onDismissRequest = { showDropdown = false },
                                    modifier = Modifier.fillMaxWidth(0.8f).background(if (isDark) Color(0xFF2C2A30) else Color.White)
                                ) {
                                    behaviors.forEachIndexed { idx, opt ->
                                        DropdownMenuItem(
                                            text = { Text(opt, style = MaterialTheme.typography.bodyMedium.copy(color = if (isDark) Color.White else Color(0xFF1D1B20))) },
                                            onClick = {
                                                barBehavior = idx
                                                showDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Checkbox 1: Hide passed milestones
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { hidePassedMilestones = !hidePassedMilestones }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = hidePassedMilestones,
                                onCheckedChange = { hidePassedMilestones = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4),
                                    uncheckedColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4),
                                    checkmarkColor = if (isDark) Color(0xFF381E72) else Color.White
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Hide milestones once passed",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = if (isDark) Color.White else Color(0xFF1D1B20)),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Milestones will become invisible on the progress bar once achieved.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F))
                                )
                            }
                        }

                        // Checkbox 2: Show remaining count
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showRemainingCount = !showRemainingCount }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = showRemainingCount,
                                onCheckedChange = { showRemainingCount = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4),
                                    uncheckedColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4),
                                    checkmarkColor = if (isDark) Color(0xFF381E72) else Color.White
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Show count of remaining milestones",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = if (isDark) Color.White else Color(0xFF1D1B20)),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Displays '+N' next to progress bar for unreached milestones (only applies to last 2 dropdown options).",
                                    style = MaterialTheme.typography.bodySmall.copy(color = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F))
                                )
                            }
                        }
                    }
                } else if (activeTab == 3) {
                    val colorsList = listOf("#6750A4", "#0288D1", "#2E7D32", "#EF6C00", "#C2185B", "#00796B", "#FBC02D", "#8D6E63", "#78909C", "#26A69A", "#EC407A", "#FF7043")
                    val bordersList = listOf("#FFD700", "#6750A4", "#0288D1", "#2E7D32", "#EF6C00", "#C2185B", "#00796B", "#FBC02D", "#8D6E63", "#78909C", "#26A69A", "#EC407A", "#FF7043")
                    // Styles tab
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Customize Colors & Border",
                            style = MaterialTheme.typography.titleMedium.copy(color = if (isDark) Color.White else Color(0xFF1D1B20)),
                            fontWeight = FontWeight.SemiBold
                        )

                        // Accent Color Picker
                        Column {
                            Text(
                                text = "Pick Goal Accent Color",
                                style = MaterialTheme.typography.titleSmall.copy(color = if (isDark) Color.White else Color(0xFF1D1B20)),
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                            ) {
                                val colorsList = listOf("#6750A4", "#0288D1", "#2E7D32", "#EF6C00", "#C2185B", "#00796B", "#FBC02D", "#8D6E63", "#78909C", "#26A69A", "#EC407A", "#FF7043")
                                colorsList.forEach { hex ->
                                    val color = Color(android.graphics.Color.parseColor(hex))
                                    val isSelected = selectedColorHex == hex
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(
                                                width = if (isSelected) 2.5.dp else 0.dp,
                                                color = if (isSelected) (if (isDark) Color.White else Color(0xFF1D1B20)) else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable { selectedColorHex = hex },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                // Custom HSV Option
                                val isCustomActive = selectedColorHex !in colorsList
                                CustomColorWheelIcon(
                                    isCustomActive = isCustomActive,
                                    customColorHex = selectedColorHex,
                                    onClick = { hsvTargetPicker = "ACCENT" },
                                    isDark = isDark
                                )
                            }
                        }

                        // Background Color Picker
                        Column {
                            Text(
                                text = "Pick Card Background (Secondary Color)",
                                style = MaterialTheme.typography.titleSmall.copy(color = if (isDark) Color.White else Color(0xFF1D1B20)),
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                            ) {
                                // Default choice
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(if (isDark) Color(0xFF2C2A30) else Color(0xFFE0E0E0))
                                        .border(
                                            width = if (selectedSecondaryColorHex == "DEFAULT") 2.5.dp else 1.dp,
                                            color = if (selectedSecondaryColorHex == "DEFAULT") (if (isDark) Color.White else Color(0xFF1D1B20)) else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedSecondaryColorHex = "DEFAULT" },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Def",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color.White else Color(0xFF1D1B20)
                                        )
                                    )
                                }

                                val colorsList = listOf("#6750A4", "#0288D1", "#2E7D32", "#EF6C00", "#C2185B", "#00796B", "#FBC02D", "#8D6E63", "#78909C", "#26A69A", "#EC407A", "#FF7043")
                                colorsList.forEach { hex ->
                                    val color = Color(android.graphics.Color.parseColor(hex))
                                    val isSelected = selectedSecondaryColorHex == hex
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(
                                                width = if (isSelected) 2.5.dp else 0.dp,
                                                color = if (isSelected) (if (isDark) Color.White else Color(0xFF1D1B20)) else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable { selectedSecondaryColorHex = hex },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                // Custom HSV Option for Background
                                val isCustomActive = selectedSecondaryColorHex != "DEFAULT" && selectedSecondaryColorHex !in colorsList
                                CustomColorWheelIcon(
                                    isCustomActive = isCustomActive,
                                    customColorHex = selectedSecondaryColorHex,
                                    onClick = { hsvTargetPicker = "SECONDARY" },
                                    isDark = isDark
                                )
                            }
                        }

                        // Border Color Picker
                        Column {
                            Text(
                                text = "Card Border Custom Styling",
                                style = MaterialTheme.typography.titleSmall.copy(color = if (isDark) Color.White else Color(0xFF1D1B20)),
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                            ) {
                                // None choice
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(if (isDark) Color(0xFF1D1B20) else Color.White)
                                        .border(
                                            width = if (selectedCardBorderHex == "NONE") 2.5.dp else 1.dp,
                                            color = if (selectedCardBorderHex == "NONE") (if (isDark) Color.White else Color(0xFF1D1B20)) else (if (isDark) Color(0xFF49454F) else Color(0xFFCAC4D0)),
                                            shape = CircleShape
                                        )
                                        .clickable { selectedCardBorderHex = "NONE" },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "None",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color.White else Color(0xFF1D1B20)
                                        )
                                    )
                                }

                                val bordersList = listOf("#FFD700", "#6750A4", "#0288D1", "#2E7D32", "#EF6C00", "#C2185B", "#00796B", "#FBC02D", "#8D6E63", "#78909C", "#26A69A", "#EC407A", "#FF7043")
                                bordersList.forEach { hex ->
                                    val color = Color(android.graphics.Color.parseColor(hex))
                                    val isSelected = selectedCardBorderHex == hex
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(color.copy(alpha = 0.2f))
                                            .border(
                                                width = if (isSelected) 3.dp else 1.5.dp,
                                                color = color,
                                                shape = CircleShape
                                            )
                                            .clickable { selectedCardBorderHex = hex },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = color,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                // Custom HSV Option for Border
                                val isCustomActive = selectedCardBorderHex != "NONE" && selectedCardBorderHex !in bordersList
                                CustomColorWheelIcon(
                                    isCustomActive = isCustomActive,
                                    customColorHex = selectedCardBorderHex,
                                    onClick = { hsvTargetPicker = "BORDER" },
                                    isDark = isDark
                                )
                            }
                        }
                    }
                } else if (activeTab == 4) {
                    // Stats tab content
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Tracking Configuration",
                            style = MaterialTheme.typography.titleMedium.copy(color = if (isDark) Color.White else Color(0xFF1D1B20)),
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { statsEnabled = !statsEnabled },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked = statsEnabled,
                                onCheckedChange = { statsEnabled = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
                                )
                            )
                            Column {
                                Text(
                                    text = "Enable tracking stats",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = if (isDark) Color.White else Color(0xFF1D1B20))
                                )
                                Text(
                                    text = "Enables optional/non-essential tracking.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = if (isDark) Color.LightGray else Color.Gray)
                                )
                            }
                        }

                        if (statsEnabled) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { streaksEnabled = !streaksEnabled },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = streaksEnabled,
                                    onCheckedChange = { streaksEnabled = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
                                    )
                                )
                                Column {
                                    Text(
                                        text = "Enable streaks",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = if (isDark) Color.White else Color(0xFF1D1B20))
                                    )
                                    Text(
                                        text = "Track consecutive daily additions.",
                                        style = MaterialTheme.typography.bodySmall.copy(color = if (isDark) Color.LightGray else Color.Gray)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { pointTrackingEnabled = !pointTrackingEnabled },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = pointTrackingEnabled,
                                    onCheckedChange = { pointTrackingEnabled = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
                                    )
                                )
                                Column {
                                    Text(
                                        text = "Enable point tracking stats",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = if (isDark) Color.White else Color(0xFF1D1B20))
                                    )
                                    Text(
                                        text = "Log history, averages, last update, etc.",
                                        style = MaterialTheme.typography.bodySmall.copy(color = if (isDark) Color.LightGray else Color.Gray)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showHighlights = !showHighlights },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = showHighlights,
                                    onCheckedChange = { showHighlights = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
                                    )
                                )
                                Column {
                                    Text(
                                        text = "Show Highlight Summaries",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = if (isDark) Color.White else Color(0xFF1D1B20))
                                    )
                                    Text(
                                        text = "Include performance record highlights.",
                                        style = MaterialTheme.typography.bodySmall.copy(color = if (isDark) Color.LightGray else Color.Gray)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Visual Display Options",
                                style = MaterialTheme.typography.titleSmall.copy(color = if (isDark) Color.White else Color(0xFF1D1B20)),
                                fontWeight = FontWeight.SemiBold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .weight(1.5f)
                                        .background(
                                            if (statsVisualType == 0) (if (isDark) Color(0xFF381E72) else Color(0xFFE8DDFF))
                                            else Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            width = if (statsVisualType == 0) 2.dp else 1.dp,
                                            color = if (statsVisualType == 0) (if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4))
                                                    else (if (isDark) Color(0xFF49454F) else Color(0xFFCAC4D0)),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { statsVisualType = 0 }
                                        .padding(vertical = 10.dp, horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.BarChart,
                                        contentDescription = "Bar Style",
                                        tint = if (statsVisualType == 0) (if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4))
                                               else (if (isDark) Color.LightGray else Color.Gray),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Bar",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (statsVisualType == 0) (if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4))
                                                   else (if (isDark) Color.White else Color(0xFF1D1B20))
                                        )
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .weight(1.5f)
                                        .background(
                                            if (statsVisualType == 1) (if (isDark) Color(0xFF381E72) else Color(0xFFE8DDFF))
                                            else Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            width = if (statsVisualType == 1) 2.dp else 1.dp,
                                            color = if (statsVisualType == 1) (if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4))
                                                    else (if (isDark) Color(0xFF49454F) else Color(0xFFCAC4D0)),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { statsVisualType = 1 }
                                        .padding(vertical = 10.dp, horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ShowChart,
                                        contentDescription = "Graph Style",
                                        tint = if (statsVisualType == 1) (if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4))
                                               else (if (isDark) Color.LightGray else Color.Gray),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Graph",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (statsVisualType == 1) (if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4))
                                                   else (if (isDark) Color.White else Color(0xFF1D1B20))
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val isTitleEmpty = title.isBlank()
                    val targetMax = maxPointsStr.toIntOrNull() ?: 10
                    val isMaxInvalid = targetMax <= 0

                    titleError = isTitleEmpty
                    maxPointsError = isMaxInvalid

                    if (!isTitleEmpty && !isMaxInvalid) {
                        val current = currentPointsStr.toIntOrNull() ?: 0
                        val milestonesJoined = milestonesList.joinToString(";") { "${it.value}:${it.type}" }
                        val gVal = groupValueStr.toDoubleOrNull() ?: 1.0
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        onConfirm(
                            title.trim(),
                            description.trim(),
                            current,
                            targetMax,
                            allowExceed,
                            selectedColorHex,
                            selectedSecondaryColorHex,
                            milestonesJoined,
                            barBehavior,
                            hidePassedMilestones,
                            showRemainingCount,
                            showMilestoneProgressBar,
                            progressType,
                            meterThickness,
                            dotsPerRow,
                            selectedCardBorderHex,
                            if (unit.isBlank()) "pts" else unit.trim(),
                            statsEnabled,
                            streaksEnabled,
                            pointTrackingEnabled,
                            statsVisualType,
                            showHighlights,
                            gVal,
                            allowGroupPointsOverflow
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4),
                    contentColor = if (isDark) Color(0xFF381E72) else Color.White
                ),
                shape = CircleShape,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .testTag("add_goal_confirm_button")
            ) {
                Text("Confirm", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    onDismiss()
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
                ),
                modifier = Modifier.testTag("add_goal_cancel_button")
            ) {
                Text("Cancel", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium))
            }
        }
    )

    // Milestone Add Single Dialog
    if (showAddMilestoneDialog) {
        var valueStr by remember { mutableStateOf("") }
        var selectedType by remember { mutableStateOf("dot") }
        var valError by remember { mutableStateOf(false) }
        var isIncreaseByMode by remember { mutableStateOf(false) }

        val allTypes = listOf("dot", "star", "heart", "diamond", "line", "flag", "danger", "cross")

        AlertDialog(
            onDismissRequest = {
                focusManager.clearFocus()
                keyboardController?.hide()
                showAddMilestoneDialog = false
            },
            containerColor = if (isDark) Color(0xFF2C2A30) else Color(0xFFFFFFFF),
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    "Add Milestone Checkpoint",
                    style = MaterialTheme.typography.titleMedium.copy(color = if (isDark) Color.White else Color(0xFF1D1B20)),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !isIncreaseByMode,
                            onClick = { isIncreaseByMode = false },
                            label = { Text("Exact Value", style = MaterialTheme.typography.labelMedium) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = isIncreaseByMode,
                            onClick = { isIncreaseByMode = true },
                            label = { Text("Increase By (+)", style = MaterialTheme.typography.labelMedium) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = valueStr,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.all { it.isDigit() }) {
                                valueStr = input
                                valError = (input.toIntOrNull() ?: 0) <= 0
                            }
                        },
                        label = { Text(if (isIncreaseByMode) "Increase From Previous/0 by" else "Milestone Point Value") },
                        placeholder = { Text(if (isIncreaseByMode) "+5" else "e.g. 5") },
                        isError = valError,
                        supportingText = {
                            if (valError) Text("Must be > 0")
                            else if (isIncreaseByMode) {
                                val lastVal = milestonesList.maxOfOrNull { it.value } ?: 0
                                val inc = valueStr.toIntOrNull() ?: 0
                                if (inc > 0) Text("Will create milestone at ${lastVal + inc} pts")
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = if (isDark) Color.White else Color(0xFF1D1B20),
                            unfocusedTextColor = if (isDark) Color.White else Color(0xFF1D1B20),
                            focusedBorderColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF21005D),
                            unfocusedBorderColor = if (isDark) Color(0xFF938F99) else Color(0xFF6750A4),
                            focusedLabelColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF21005D),
                            unfocusedLabelColor = if (isDark) Color(0xFFCAC4D0) else Color(0xFF6750A4)
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Column {
                        Text(
                            "Select Icon Style (8 Styles)",
                            style = MaterialTheme.typography.titleSmall.copy(color = if (isDark) Color.White else Color(0xFF1D1B20)),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            allTypes.forEach { type ->
                                val isSel = selectedType == type
                                Box(
                                    modifier = Modifier
                                        .weight(1f, fill = false)
                                        .width(62.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSel) {
                                                if (isDark) Color(0xFF381E72) else Color(0xFFE8DEF8)
                                            } else {
                                                if (isDark) Color(0xFF1E1C24) else Color(0xFFF1F1F1)
                                            }
                                        )
                                        .border(
                                            width = 1.5.dp,
                                            color = if (isSel) {
                                                if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
                                            } else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedType = type }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        MilestoneIconDisplay(
                                            type = type,
                                            tint = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            type.take(4).uppercase(),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) Color.White else Color(0xFF1D1B20)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val inputVal = valueStr.toIntOrNull() ?: 0
                        if (inputVal > 0) {
                            val finalVal = if (isIncreaseByMode) {
                                val lastVal = milestonesList.maxOfOrNull { it.value } ?: 0
                                lastVal + inputVal
                            } else inputVal

                            focusManager.clearFocus()
                            keyboardController?.hide()
                            milestonesList = (milestonesList + Milestone(finalVal, selectedType)).sortedBy { it.value }
                            showAddMilestoneDialog = false
                        } else {
                            valError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4),
                        contentColor = if (isDark) Color(0xFF381E72) else Color.White
                    )
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        showAddMilestoneDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
                    )
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Milestone Add Multiple Dialog
    if (showMultipleMilestonesDialog) {
        var mode by remember { mutableStateOf(0) } // 0 = Incremental, 1 = In Range
        var countStr by remember { mutableStateOf("5") }
        var startValStr by remember { mutableStateOf("5") }
        var stepValStr by remember { mutableStateOf("5") }
        var eachIncreaseByStr by remember { mutableStateOf("0") }
        var fromValStr by remember { mutableStateOf("100") }
        var toValStr by remember { mutableStateOf((maxPointsStr.toIntOrNull() ?: 200).toString()) }
        var weightPercent by remember { mutableStateOf(0f) } // -100% to +100%
        var selectedType by remember { mutableStateOf("dot") }

        val allTypes = listOf("dot", "star", "line", "flag", "cross", "diamond", "heart", "danger")

        val generatedPreview = remember(mode, countStr, startValStr, stepValStr, eachIncreaseByStr, fromValStr, toValStr, weightPercent) {
            val count = (countStr.toIntOrNull() ?: 0).coerceIn(1, 100)
            if (mode == 0) {
                val start = startValStr.toIntOrNull() ?: 0
                val step = stepValStr.toIntOrNull() ?: 0
                val eachInc = eachIncreaseByStr.toIntOrNull() ?: 0
                generateIncrementalMilestones(count, start, step, eachInc)
            } else {
                val from = fromValStr.toIntOrNull() ?: 0
                val to = toValStr.toIntOrNull() ?: 100
                generateRangeMilestones(count, from, to, weightPercent)
            }
        }

        AlertDialog(
            onDismissRequest = {
                focusManager.clearFocus()
                keyboardController?.hide()
                showMultipleMilestonesDialog = false
            },
            containerColor = if (isDark) Color(0xFF2C2A30) else Color(0xFFFFFFFF),
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    "Add multiple Milestone Checkpoints",
                    style = MaterialTheme.typography.titleMedium.copy(color = if (isDark) Color.White else Color(0xFF1D1B20)),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Mode Selector Tabs (Incremental vs In range)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { mode = 0 },
                            color = if (mode == 0) (if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4))
                                    else (if (isDark) Color(0xFF38353E) else Color(0xFFE8DEF8)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Incremental",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (mode == 0) (if (isDark) Color(0xFF381E72) else Color.White)
                                               else (if (isDark) Color.White else Color(0xFF1D1B20))
                                    )
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { mode = 1 },
                            color = if (mode == 1) (if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4))
                                    else (if (isDark) Color(0xFF38353E) else Color(0xFFE8DEF8)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "In range",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (mode == 1) (if (isDark) Color(0xFF381E72) else Color.White)
                                               else (if (isDark) Color.White else Color(0xFF1D1B20))
                                    )
                                )
                            }
                        }
                    }

                    if (mode == 0) {
                        // Incremental Mode
                        OutlinedTextField(
                            value = countStr,
                            onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) countStr = it },
                            label = { Text("# of milestones") },
                            placeholder = { Text("1 to 100") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = startValStr,
                            onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() || c == '-' }) startValStr = it },
                            label = { Text("Starting from") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = stepValStr,
                                onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() || c == '-' }) stepValStr = it },
                                label = { Text("Increase by") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = eachIncreaseByStr,
                                onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() || c == '-' }) eachIncreaseByStr = it },
                                label = { Text("Each increase by") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        // In Range Mode
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = fromValStr,
                                onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() || c == '-' }) fromValStr = it },
                                label = { Text("From") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = toValStr,
                                onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() || c == '-' }) toValStr = it },
                                label = { Text("To") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedTextField(
                            value = countStr,
                            onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) countStr = it },
                            label = { Text("# of milestones") },
                            placeholder = { Text("1 to 100") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Weight Distribution Slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Weight: ${if (weightPercent > 0) "+" else ""}${weightPercent.toInt()}%",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = if (isDark) Color.White else Color(0xFF1D1B20),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Slider(
                                value = weightPercent,
                                onValueChange = { weightPercent = it },
                                valueRange = -100f..100f,
                                colors = SliderDefaults.colors(
                                    thumbColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4),
                                    activeTrackColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
                                )
                            )

                            // Live Distribution Preview Bar
                            Text(
                                text = "Preview (${generatedPreview.size} milestones)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.LightGray else Color.DarkGray
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(26.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isDark) Color(0xFF1E1C24) else Color(0xFFE8DEF8))
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val w = size.width
                                    val h = size.height
                                    val minVal = (fromValStr.toIntOrNull() ?: 0).toFloat()
                                    val maxVal = (toValStr.toIntOrNull() ?: 100).toFloat().coerceAtLeast(minVal + 1)
                                    
                                    // Draw track baseline
                                    drawLine(
                                        color = if (isDark) Color.White.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.2f),
                                        start = androidx.compose.ui.geometry.Offset(0f, h / 2f),
                                        end = androidx.compose.ui.geometry.Offset(w, h / 2f),
                                        strokeWidth = 3.dp.toPx()
                                    )

                                    // Draw milestone dots
                                    generatedPreview.forEach { valTick ->
                                        val frac = ((valTick - minVal) / (maxVal - minVal)).coerceIn(0f, 1f)
                                        val x = frac * w
                                        drawCircle(
                                            color = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4),
                                            radius = 4.dp.toPx(),
                                            center = androidx.compose.ui.geometry.Offset(x, h / 2f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Icon Style Picker (2 rows of 4)
                    Column {
                        Text(
                            "Select Icon Style",
                            style = MaterialTheme.typography.titleSmall.copy(color = if (isDark) Color.White else Color(0xFF1D1B20)),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            allTypes.forEach { type ->
                                val isSel = selectedType == type
                                Box(
                                    modifier = Modifier
                                        .weight(1f, fill = false)
                                        .width(64.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSel) {
                                                if (isDark) Color(0xFF381E72) else Color(0xFFE8DEF8)
                                            } else {
                                                if (isDark) Color(0xFF1E1C24) else Color(0xFFF1F1F1)
                                            }
                                        )
                                        .border(
                                            width = 1.5.dp,
                                            color = if (isSel) {
                                                if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
                                            } else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedType = type }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        MilestoneIconDisplay(
                                            type = type,
                                            tint = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            type.uppercase(),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) Color.White else Color(0xFF1D1B20)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Preview numbers chips
                    if (generatedPreview.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            generatedPreview.take(10).forEach { pVal ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isDark) Color(0xFF381E72) else Color(0xFFE8DEF8)
                                ) {
                                    Text(
                                        text = "$pVal",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isDark) Color(0xFFD0BCFF) else Color(0xFF381E72),
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            if (generatedPreview.size > 10) {
                                Text(
                                    text = "+${generatedPreview.size - 10} more",
                                    style = MaterialTheme.typography.labelSmall.copy(color = if (isDark) Color.LightGray else Color.DarkGray)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (generatedPreview.isNotEmpty()) {
                            val newItems = generatedPreview.map { Milestone(it, selectedType) }
                            milestonesList = (milestonesList + newItems).distinctBy { it.value }.sortedBy { it.value }
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            showMultipleMilestonesDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4),
                        contentColor = if (isDark) Color(0xFF381E72) else Color.White
                    )
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        showMultipleMilestonesDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
                    )
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdjustGoalDialog(
    goal: Goal,
    isAddition: Boolean,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (amount: Int, adjustMax: Boolean) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var adjustMaxPoints by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Distinct colors for Add (Emerald) vs Subtract (Coral/Crimson)
    val actionAccentColor = if (isAddition) {
        if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
    } else {
        if (isDark) Color(0xFFE57373) else Color(0xFFC62828)
    }

    val actionBgColor = if (isAddition) {
        if (isDark) Color(0xFF1E3524) else Color(0xFFE8F5E9)
    } else {
        if (isDark) Color(0xFF381C1C) else Color(0xFFFFEBEE)
    }

    val buttonContentColor = if (isAddition) {
        if (isDark) Color(0xFF0F2617) else Color.White
    } else {
        if (isDark) Color(0xFF2E0909) else Color.White
    }

    AlertDialog(
        onDismissRequest = {
            focusManager.clearFocus()
            keyboardController?.hide()
            onDismiss()
        },
        containerColor = if (isDark) Color(0xFF1E1C24) else Color(0xFFF3EDF7),
        shape = RoundedCornerShape(28.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(actionBgColor)
                    .border(1.5.dp, actionAccentColor.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isAddition) "+" else "−",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = actionAccentColor
                    )
                )
            }
        },
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isAddition) "Quick Adjust: Add Points" else "Quick Adjust: Subtract Points",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge.copy(color = if (isDark) Color.White else Color(0xFF1D1B20)),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isAddition) "Adding to: ${goal.title}" else "Subtracting from: ${goal.title}",
                    style = MaterialTheme.typography.bodyMedium.copy(color = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F)),
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.all { it.isDigit() }) {
                            amountStr = input
                            val num = input.toIntOrNull() ?: 0
                            amountError = num <= 0
                        }
                    },
                    label = { Text(if (isAddition) "Points to add" else "Points to subtract") },
                    placeholder = { Text("5", color = Color(0xFF9E9E9E)) },
                    prefix = {
                        Text(
                            text = if (isAddition) "+ " else "− ",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = actionAccentColor
                            )
                        )
                    },
                    isError = amountError,
                    supportingText = if (amountError) {
                        { Text("Please enter an amount greater than 0") }
                    } else null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = if (isDark) Color.White else Color(0xFF1D1B20),
                        unfocusedTextColor = if (isDark) Color.White else Color(0xFF1D1B20),
                        focusedBorderColor = actionAccentColor,
                        unfocusedBorderColor = if (isDark) Color(0xFF938F99) else Color(0xFF6750A4),
                        focusedLabelColor = actionAccentColor,
                        unfocusedLabelColor = if (isDark) Color(0xFFCAC4D0) else Color(0xFF6750A4),
                        cursorColor = actionAccentColor
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("adjust_amount_input"),
                    singleLine = true
                )

                // Checkbox to adjust Max instead of Current
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { adjustMaxPoints = !adjustMaxPoints }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = adjustMaxPoints,
                        onCheckedChange = { adjustMaxPoints = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = actionAccentColor,
                            uncheckedColor = if (isDark) Color(0xFF938F99) else Color(0xFF6750A4),
                            checkmarkColor = if (isDark) Color(0xFF1E1C24) else Color.White
                        ),
                        modifier = Modifier.testTag("adjust_target_max_checkbox")
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isAddition) "Increase Max Points instead" else "Decrease Max Points instead",
                            style = MaterialTheme.typography.bodyMedium.copy(color = if (isDark) Color.White else Color(0xFF1D1B20)),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (isAddition) "Increase maximum goal target points." else "Decrease maximum goal target points.",
                            style = MaterialTheme.typography.bodySmall.copy(color = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F))
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toIntOrNull() ?: 0
                    if (amount > 0) {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        onConfirm(amount, adjustMaxPoints)
                    } else {
                        amountError = true
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = actionAccentColor,
                    contentColor = buttonContentColor
                ),
                shape = CircleShape,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .testTag("adjust_confirm_button")
            ) {
                Text(
                    text = if (isAddition) "Add Points" else "Subtract Points",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    onDismiss()
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F)
                ),
                modifier = Modifier.testTag("adjust_cancel_button")
            ) {
                Text("Cancel", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium))
            }
        }
    )
}

@Composable
fun CreditsDialog(
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("credits_dialog"),
        shape = RoundedCornerShape(28.dp),
        containerColor = if (isDark) Color(0xFF211F26) else Color(0xFFF3EDF7),
        icon = {
            Image(
                painter = painterResource(id = R.drawable.ic_flagpole_logo_1783178411745),
                contentDescription = "Flagpole Icon",
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.5.dp, if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4), RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
        },
        title = {
            Text(
                text = "Flagpole Credits",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color.White else Color(0xFF1D1B20)
                )
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "A beautifully animated progress and custom milestones tracker.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    ),
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                // Creator & Visionary: Baskes_EZ
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF2D2B33) else Color(0xFFE8DEF8)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Baskes_EZ",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color(0xFF21005D)
                            )
                        )
                        Text(
                            text = "Original Visionary & Idea Creator",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // AI Software Engineer: Gemini 3.5
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF23222A) else Color(0xFFEADBDB)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFFE57373) else Color(0xFFC62828),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Google Gemini AI",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color(0xFF3E2723)
                            )
                        )
                        Text(
                            text = "Lead AI Software Engineer",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = if (isDark) Color(0xFFE57373) else Color(0xFFC62828)
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4),
                    contentColor = if (isDark) Color(0xFF381E72) else Color.White
                ),
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .testTag("credits_close_button")
            ) {
                Text("Close", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium))
            }
        }
    )
}

@Composable
fun StatsOverlay(
    goal: Goal,
    isDark: Boolean,
    isLandscape: Boolean,
    landscapeColumns: Int,
    isExpanded: Boolean,
    onDismiss: () -> Unit,
    viewModel: GoalViewModel
) {
    val logsState = viewModel.getLogsForGoal(goal.id).collectAsState(initial = emptyList())
    val logs = logsState.value
    val allGroups by viewModel.allGroups.collectAsState(initial = emptyList())
    val groupForGoal = allGroups.find { it.id == goal.parentGroupId }
    val inheritedBorderColor = if (groupForGoal?.cardBorderHex != "NONE") groupForGoal?.cardBorderHex else null

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable(
                    onClick = onDismiss,
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isLandscape) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .clickable(enabled = false, onClick = {}),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            GoalCard(
                                goal = goal,
                                isDark = isDark,
                                isAnimationEnabled = false,
                                onIncrement = {},
                                onDecrement = {},
                                onHoldIncrement = {},
                                onHoldDecrement = {},
                                onDelete = {},
                                outlineColorHex = inheritedBorderColor
                            )
                            if (goal.milestonesStr.isNotBlank()) {
                                val milestones = parseMilestones(goal.milestonesStr)
                                if (milestones.isNotEmpty()) {
                                    val customColor = try { Color(android.graphics.Color.parseColor(goal.colorHex)) } catch (e: Exception) { Color(0xFF6750A4) }
                                    MilestoneProgressionSection(
                                        goal = goal,
                                        milestones = milestones,
                                        currentPoints = goal.currentPoints,
                                        customColor = customColor,
                                        isDark = isDark,
                                        outlineColorHex = inheritedBorderColor
                                    )
                                }
                            }
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            StatsCard(
                                goal = goal,
                                isDark = isDark,
                                logs = logs,
                                onDismiss = onDismiss,
                                outlineColorHex = inheritedBorderColor,
                                viewModel = viewModel,
                                showMilestones = false
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .clickable(enabled = false, onClick = {}),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        GoalCard(
                            goal = goal,
                            isDark = isDark,
                            isAnimationEnabled = false,
                            onIncrement = {},
                            onDecrement = {},
                            onHoldIncrement = {},
                            onHoldDecrement = {},
                            onDelete = {},
                            outlineColorHex = inheritedBorderColor
                        )
                        StatsCard(
                            goal = goal,
                            isDark = isDark,
                            logs = logs,
                            onDismiss = onDismiss,
                            outlineColorHex = inheritedBorderColor,
                            viewModel = viewModel,
                            showMilestones = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatsCard(
    goal: Goal,
    isDark: Boolean,
    logs: List<GoalLog>,
    onDismiss: () -> Unit,
    outlineColorHex: String? = null,
    viewModel: GoalViewModel? = null,
    showMilestones: Boolean = true
) {
    val customColor = try { Color(android.graphics.Color.parseColor(goal.colorHex)) } catch (e: Exception) { Color(0xFF6750A4) }
    val cardBgColor = getCardBackgroundColor(isDark, goal.secondaryColorHex)
    var selectedDays by remember { mutableStateOf(7) }
    var ignoreInactiveDays by remember { mutableStateOf(false) }
    var showFullScreenGraph by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        border = if (goal.cardBorderHex != "NONE") {
            val strokeColor = try { Color(android.graphics.Color.parseColor(goal.cardBorderHex)) } catch (e: Exception) { customColor }
            BorderStroke(2.dp, strokeColor)
        } else if (outlineColorHex != null && outlineColorHex != "NONE") {
            val strokeColor = try { Color(android.graphics.Color.parseColor(outlineColorHex)) } catch (e: Exception) { customColor }
            BorderStroke(2.dp, strokeColor)
        } else {
            if (isDark) BorderStroke(1.dp, Color(0xFF49454F)) else BorderStroke(1.dp, Color(0xFFCAC4D0))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Goal Stats & Insights",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF1D1B20)
                    )
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Stats",
                        tint = customColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (!goal.statsEnabled) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Stats tracking is disabled for this goal.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                val creationDateStr = sdf.format(java.util.Date(goal.createdAt))

                val daysSinceCreation = run {
                    val calToday = java.util.Calendar.getInstance().apply {
                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                        set(java.util.Calendar.MINUTE, 0)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }
                    val calCreated = java.util.Calendar.getInstance().apply {
                        timeInMillis = goal.createdAt
                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                        set(java.util.Calendar.MINUTE, 0)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }
                    val diffDays = ((calToday.timeInMillis - calCreated.timeInMillis) / (1000L * 60 * 60 * 24)).coerceAtLeast(0L) + 1L
                    maxOf(1L, diffDays)
                }
                val averagePerDay = "%.1f".format(goal.currentPoints.toDouble() / daysSinceCreation)

                val lastChangeStr = if (logs.isNotEmpty()) {
                    val sdfTime = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
                    sdfTime.format(java.util.Date(logs.last().timestamp))
                } else {
                    "Never"
                }

                val todayStart = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }.timeInMillis
                val pointsToday = logs.filter { it.timestamp >= todayStart }.sumOf { it.changeAmount }

                // Top stat: Group Value
                if (goal.parentGroupId != 0) {
                    val groupValStr = formatGroupPoint(goal.groupValue)
                    StatRow(label = "Group value", value = groupValStr, isDark = isDark)
                }

                // Top stat: Last Updated
                if (goal.pointTrackingEnabled) {
                    StatRow(label = "Last updated", value = lastChangeStr, isDark = isDark)
                }

                // Chart Section almost at top
                if (goal.pointTrackingEnabled) {
                    val lastDays = (0 until selectedDays).reversed().map { offset ->
                        val cal = java.util.Calendar.getInstance()
                        cal.add(java.util.Calendar.DAY_OF_YEAR, -offset)
                        cal
                    }
                    val dayLabels = lastDays.map { cal ->
                        val sdfDay = java.text.SimpleDateFormat("MM/dd", java.util.Locale.getDefault())
                        sdfDay.format(cal.time)
                    }
                    val dayValues = lastDays.map { cal ->
                        val start = cal.apply {
                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                            set(java.util.Calendar.MINUTE, 0)
                            set(java.util.Calendar.SECOND, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        val end = start + 24 * 60 * 60 * 1000
                        logs.filter { it.timestamp in start until end }.sumOf { it.changeAmount }
                    }

                    val cumulativePoints = mutableListOf<Float>()
                    var runningSum = goal.currentPoints.toFloat() - dayValues.sum()
                    dayValues.forEach { change ->
                        runningSum += change
                        cumulativePoints.add(runningSum)
                    }

                    var finalDayLabels = dayLabels
                    var finalDayValues = dayValues
                    var finalCumulativePoints: List<Float> = cumulativePoints
                    var finalDays = lastDays

                    if (ignoreInactiveDays) {
                        val activeIndices = dayValues.indices.filter { dayValues[it] != 0 }
                        if (activeIndices.isNotEmpty()) {
                            val keep = mutableSetOf<Int>()
                            for (idx in activeIndices) {
                                if (idx - 1 >= 0) keep.add(idx - 1)
                                keep.add(idx)
                            }
                            val indices = keep.sorted()
                            finalDayLabels = indices.map { dayLabels[it] }
                            finalDayValues = indices.map { dayValues[it] }
                            finalCumulativePoints = indices.map { cumulativePoints[it] }
                            finalDays = indices.map { lastDays[it] }
                        } else {
                            finalDayLabels = listOf("No change")
                            finalDayValues = listOf(0)
                            finalCumulativePoints = listOf(goal.currentPoints.toFloat())
                            finalDays = listOf(java.util.Calendar.getInstance())
                        }
                    }

                    val drawCount = finalDayValues.size

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Options Row for Period Selection
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val periods = listOf(7 to "7d", 14 to "14d", 30 to "30d", 90 to "3m")
                            periods.forEach { (days, label) ->
                                val isSelected = selectedDays == days
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) customColor.copy(alpha = 0.2f)
                                            else Color.Transparent
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) customColor else (if (isDark) Color(0xFF49454F) else Color(0xFFCAC4D0)),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedDays = days }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) customColor else (if (isDark) Color.White else Color(0xFF1D1B20))
                                        )
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (goal.statsVisualType == 0) "Progress (Bar)" else "Cumulative Trend (Graph)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, color = if (isDark) Color.White else Color(0xFF1D1B20))
                            )
                            IconButton(
                                onClick = { showFullScreenGraph = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ZoomIn,
                                    contentDescription = "Zoom Graph",
                                    tint = customColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        if (goal.statsVisualType == 0) {
                            Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                                val width = size.width
                                val height = size.height
                                val maxVal = maxOf(1f, finalDayValues.maxOf { kotlin.math.abs(it) }.toFloat())
                                val paddingLeft = 30f
                                val paddingRight = 10f
                                val paddingTop = 10f
                                val paddingBottom = 30f
                                val chartWidth = width - paddingLeft - paddingRight
                                val chartHeight = height - paddingTop - paddingBottom

                                drawLine(
                                    color = if (isDark) Color(0xFF49454F) else Color(0xFFCAC4D0),
                                    start = androidx.compose.ui.geometry.Offset(paddingLeft, height - paddingBottom),
                                    end = androidx.compose.ui.geometry.Offset(width - paddingRight, height - paddingBottom),
                                    strokeWidth = 2f
                                )

                                val barWidth = (chartWidth / drawCount) * 0.6f
                                val spacing = (chartWidth / drawCount) * 0.4f

                                for (i in 0 until drawCount) {
                                    val x = paddingLeft + i * (barWidth + spacing) + spacing / 2
                                    val valFloat = finalDayValues[i].toFloat()
                                    val barHeight = (kotlin.math.abs(valFloat) / maxVal) * chartHeight
                                    val y = height - paddingBottom - barHeight

                                    drawRoundRect(
                                        color = if (valFloat >= 0) customColor else Color.Red,
                                        topLeft = androidx.compose.ui.geometry.Offset(x, y),
                                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                                        cornerRadius = CornerRadius(4f, 4f)
                                    )

                                    drawContext.canvas.nativeCanvas.apply {
                                        val textPaint = android.graphics.Paint().apply {
                                            color = if (isDark) android.graphics.Color.WHITE else android.graphics.Color.BLACK
                                            textSize = 20f
                                            textAlign = android.graphics.Paint.Align.CENTER
                                            isAntiAlias = true
                                        }

                                        val shouldDrawLabel = when {
                                            drawCount <= 7 -> true
                                            drawCount <= 14 -> i % 2 == 0
                                            drawCount <= 30 -> i % 5 == 0
                                            else -> i % (drawCount / 6).coerceAtLeast(1) == 0
                                        }

                                        if (shouldDrawLabel && i < finalDayLabels.size) {
                                            drawText(
                                                finalDayLabels[i],
                                                x + barWidth / 2,
                                                height - 8f,
                                                textPaint
                                            )
                                        }

                                        val shouldDrawValue = when {
                                            drawCount <= 7 -> finalDayValues[i] != 0
                                            drawCount <= 14 -> finalDayValues[i] != 0 && i % 2 == 0
                                            else -> false
                                        }

                                        if (shouldDrawValue && i < finalDayValues.size) {
                                            drawText(
                                                "${if (finalDayValues[i] > 0) "+" else ""}${finalDayValues[i]}",
                                                x + barWidth / 2,
                                                maxOf(y - 6f, paddingTop),
                                                textPaint
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                                val width = size.width
                                val height = size.height
                                val paddingLeft = 30f
                                val paddingRight = 10f
                                val paddingTop = 10f
                                val paddingBottom = 30f
                                val chartWidth = width - paddingLeft - paddingRight
                                val chartHeight = height - paddingTop - paddingBottom

                                val minP = finalCumulativePoints.minOrNull() ?: 0f
                                val maxP = finalCumulativePoints.maxOrNull() ?: (minP + 1f)
                                val range = if (maxP == minP) 1f else (maxP - minP)

                                drawLine(
                                    color = if (isDark) Color(0xFF49454F) else Color(0xFFCAC4D0),
                                    start = androidx.compose.ui.geometry.Offset(paddingLeft, height - paddingBottom),
                                    end = androidx.compose.ui.geometry.Offset(width - paddingRight, height - paddingBottom),
                                    strokeWidth = 2f
                                )

                                val stepX = if (drawCount > 1) chartWidth / (drawCount - 1).toFloat() else chartWidth
                                val path = androidx.compose.ui.graphics.Path()

                                for (i in 0 until drawCount) {
                                    val x = paddingLeft + i * stepX
                                    val pointVal = finalCumulativePoints[i]
                                    val y = height - paddingBottom - ((pointVal - minP) / range) * chartHeight

                                    if (i == 0) {
                                        path.moveTo(x, y)
                                    } else {
                                        path.lineTo(x, y)
                                    }

                                    if (drawCount <= 30) {
                                        drawCircle(
                                            color = customColor,
                                            radius = if (drawCount <= 7) 6f else 4f,
                                            center = androidx.compose.ui.geometry.Offset(x, y)
                                        )
                                    }

                                    drawContext.canvas.nativeCanvas.apply {
                                        val textPaint = android.graphics.Paint().apply {
                                            color = if (isDark) android.graphics.Color.WHITE else android.graphics.Color.BLACK
                                            textSize = 20f
                                            textAlign = android.graphics.Paint.Align.CENTER
                                            isAntiAlias = true
                                        }

                                        val shouldDrawLabel = when {
                                            drawCount <= 7 -> true
                                            drawCount <= 14 -> i % 2 == 0
                                            drawCount <= 30 -> i % 5 == 0
                                            else -> i % (drawCount / 6).coerceAtLeast(1) == 0
                                        }

                                        if (shouldDrawLabel && i < finalDayLabels.size) {
                                            drawText(
                                                finalDayLabels[i],
                                                x,
                                                height - 8f,
                                                textPaint
                                            )
                                        }

                                        val shouldDrawValue = when {
                                            drawCount <= 7 -> true
                                            drawCount <= 14 -> i % 3 == 0
                                            drawCount <= 30 -> i % 10 == 0
                                            else -> i % (drawCount / 5).coerceAtLeast(1) == 0
                                        }

                                        if (shouldDrawValue && i < finalCumulativePoints.size) {
                                            drawText(
                                                "%.0f".format(pointVal),
                                                x,
                                                maxOf(y - 12f, paddingTop),
                                                textPaint
                                            )
                                        }
                                    }
                                }

                                drawPath(
                                    path = path,
                                    color = customColor,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                                )
                            }
                        }

                        // Ignore Inactive Days Checkbox below chart
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { ignoreInactiveDays = !ignoreInactiveDays }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Checkbox(
                                checked = ignoreInactiveDays,
                                onCheckedChange = { ignoreInactiveDays = it },
                                colors = CheckboxDefaults.colors(checkedColor = customColor),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Ignore inactive days",
                                style = MaterialTheme.typography.bodyMedium.copy(color = if (isDark) Color.White else Color(0xFF1D1B20))
                            )
                        }
                    }

                    if (showFullScreenGraph) {
                        FullScreenGraphDialog(
                            goal = goal,
                            isDark = isDark,
                            logs = logs,
                            selectedDaysInitial = selectedDays,
                            ignoreInactiveDaysInitial = ignoreInactiveDays,
                            onDismiss = { showFullScreenGraph = false },
                            customColor = customColor,
                            viewModel = viewModel
                        )
                    }
                }

                // Other Stats moved down (Created on, Average/day, Progress today, Current Streak)
                Spacer(modifier = Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatRow(label = "Created on", value = creationDateStr, isDark = isDark)
                    if (goal.pointTrackingEnabled) {
                        StatRow(label = "Average progress / day", value = "$averagePerDay ${goal.unit}", isDark = isDark)
                        StatRow(label = "Progress today", value = "${if (pointsToday >= 0) "+" else ""}$pointsToday ${goal.unit}", isDark = isDark)
                    }
                    if (goal.streaksEnabled) {
                        val streak = calculateStreak(logs)
                        StatRow(
                            label = "Current Streak 🔥",
                            value = "$streak ${if (streak == 1) "day" else "days"}",
                            valueColor = customColor,
                            isDark = isDark
                        )
                    }

                    // Milestone Progression Section (Categorized & Priority Sorted)
                    if (showMilestones && goal.milestonesStr.isNotBlank()) {
                        val milestones = parseMilestones(goal.milestonesStr)
                        if (milestones.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            MilestoneProgressionSection(
                                goal = goal,
                                milestones = milestones,
                                currentPoints = goal.currentPoints,
                                customColor = customColor,
                                isDark = isDark,
                                outlineColorHex = outlineColorHex
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MilestoneProgressionSection(
    goal: Goal,
    milestones: List<Milestone>,
    currentPoints: Int,
    customColor: Color,
    isDark: Boolean,
    outlineColorHex: String? = null
) {
    val cardBgColor = getCardBackgroundColor(isDark, goal.secondaryColorHex)
    val cardBorder = if (goal.cardBorderHex != "NONE") {
        val strokeColor = try { Color(android.graphics.Color.parseColor(goal.cardBorderHex)) } catch (e: Exception) { customColor }
        BorderStroke(2.dp, strokeColor)
    } else if (outlineColorHex != null && outlineColorHex != "NONE") {
        val strokeColor = try { Color(android.graphics.Color.parseColor(outlineColorHex)) } catch (e: Exception) { customColor }
        BorderStroke(2.dp, strokeColor)
    } else {
        if (isDark) null else borderStrokeForCard(goal.secondaryColorHex)
    }

    // Group by icon type and sort groups strictly by priority:
    // FLAG > STAR > HEART > DIAMOND > LINE > DOT > DANGER > CROSS
    val grouped: Map<String, List<Milestone>> = milestones.groupBy { it.type.lowercase().trim() }
    val sortedGroupKeys: List<String> = grouped.keys.sortedBy { k: String -> getMilestonePriority(k) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBgColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp),
        border = cardBorder
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Milestones Progression",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF1D1B20)
                    )
                )
                val totalMilestones = milestones.size
                val reachedMilestones = milestones.count { currentPoints >= it.value }
                Surface(
                    color = customColor.copy(alpha = if (isDark) 0.22f else 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "$reachedMilestones/$totalMilestones Done",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = customColor
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            sortedGroupKeys.forEach { typeKey ->
                val groupMilestones = grouped[typeKey]?.sortedBy { it.value } ?: emptyList()
                val totalCount = groupMilestones.size
                val achievedCount = groupMilestones.count { currentPoints >= it.value }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = getMilestoneTypeLabel(typeKey),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF1D1B20)
                        )
                    )

                    if (totalCount < 6) {
                        // Shows up to 5 individual milestone icons (achieved filled, unreached dimmed)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            groupMilestones.forEach { milestone ->
                                val isAchieved = currentPoints >= milestone.value
                                MilestoneIconDisplay(
                                    type = typeKey,
                                    tint = if (isAchieved) customColor else (if (isDark) Color(0xFF3E3A46) else Color(0xFFCAC4D0)),
                                    size = 22.dp
                                )
                            }
                        }
                    } else {
                        // Shows numbers instead for >= 6 milestones (e.g. 4/6 or 10/17 followed by icon)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$achievedCount/$totalCount",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color(0xFF1D1B20)
                                )
                            )
                            MilestoneIconDisplay(
                                type = typeKey,
                                tint = customColor,
                                size = 22.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatRow(
    label: String,
    value: String,
    valueColor: Color? = null,
    isDark: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(color = if (isDark) Color.LightGray else Color.DarkGray)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = valueColor ?: (if (isDark) Color.White else Color(0xFF1D1B20))
            )
        )
    }
}

fun calculateStreak(logs: List<GoalLog>): Int {
    if (logs.isEmpty()) return 0
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    val loggedDays = logs.filter { it.changeAmount != 0 }.map { sdf.format(java.util.Date(it.timestamp)) }.toSet()
    
    val today = java.util.Calendar.getInstance()
    val todayStr = sdf.format(today.time)
    
    val yesterday = java.util.Calendar.getInstance()
    yesterday.add(java.util.Calendar.DAY_OF_YEAR, -1)
    val yesterdayStr = sdf.format(yesterday.time)
    
    var current = if (loggedDays.contains(todayStr)) today else if (loggedDays.contains(yesterdayStr)) yesterday else return 0
    var streak = 0
    while (loggedDays.contains(sdf.format(current.time))) {
        streak++
        current.add(java.util.Calendar.DAY_OF_YEAR, -1)
    }
    return streak
}

fun saveBitmapToStorage(context: android.content.Context, bitmap: android.graphics.Bitmap, filename: String): android.net.Uri? {
    val resolver = context.contentResolver
    val contentValues = android.content.ContentValues().apply {
        put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
        put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/Flagpole")
            put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
        }
    }
    
    val imageUri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    if (imageUri != null) {
        try {
            resolver.openOutputStream(imageUri).use { out ->
                if (out != null) {
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                }
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(imageUri, contentValues, null, null)
            }
            return imageUri
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }
    return null
}

@Composable
fun ResetConfirmationDialog(
    title: String,
    subtitle: String,
    customColor: Color,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (ResetType) -> Unit
) {
    var selectedType by remember { mutableStateOf(ResetType.POINTS_ONLY) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(color = if (isDark) Color.LightGray else Color.DarkGray)
                )

                val options = listOf(
                    ResetType.POINTS_ONLY to "Reset current points only",
                    ResetType.BOTH to "Reset both current points and stats"
                )

                options.forEach { (type, label) ->
                    val isSelected = selectedType == type
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) customColor.copy(alpha = 0.12f) else Color.Transparent)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) customColor else (if (isDark) Color(0xFF3E3C40) else Color(0xFFE0E0E0)),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedType = type }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedType = type },
                            colors = RadioButtonDefaults.colors(selectedColor = customColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isDark) Color.White else Color(0xFF1D1B20)
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedType) },
                colors = ButtonDefaults.buttonColors(containerColor = customColor)
            ) {
                Text("Confirm", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = if (isDark) Color(0xFF1E1E1E) else Color.White
    )
}

@Composable
fun SnapshotDialog(
    onDismiss: () -> Unit,
    onCapture: (qualityPx: Int, splitParts: Int) -> Unit
) {
    var selectedQuality by remember { mutableStateOf(1080) }
    var splitSnapshot by remember { mutableStateOf(false) }
    var splitParts by remember { mutableStateOf(2) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Take Page Snapshot",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Select image resolution quality (width in pixels):",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                val options = listOf(
                    720 to "720px (Standard)",
                    1080 to "1080px (HD)",
                    1280 to "1280px (HD+)",
                    1600 to "1600px (Ultra Quality)"
                )
                
                options.forEach { (px, label) ->
                    val isSelected = selectedQuality == px
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedQuality = px }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedQuality = px }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { splitSnapshot = !splitSnapshot }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = splitSnapshot,
                        onCheckedChange = { splitSnapshot = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Split snapshot into multiple PNGs",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                    )
                }

                if (splitSnapshot) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Split into:",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "$splitParts images",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Slider(
                            value = splitParts.toFloat(),
                            onValueChange = { splitParts = it.toInt() },
                            valueRange = 2f..10f,
                            steps = 7,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Splits the long snapshot document into $splitParts separate images to avoid overly long images.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCapture(selectedQuality, if (splitSnapshot) splitParts else 1) }
            ) {
                Text("Capture & Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
fun FullScreenGraphDialog(
    goal: Goal,
    isDark: Boolean,
    logs: List<GoalLog>,
    selectedDaysInitial: Int,
    ignoreInactiveDaysInitial: Boolean,
    onDismiss: () -> Unit,
    customColor: Color,
    viewModel: GoalViewModel? = null
) {
    var selectedDays by remember { mutableStateOf(selectedDaysInitial) }
    var ignoreInactiveDays by remember { mutableStateOf(ignoreInactiveDaysInitial) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    val lastDays = (0 until selectedDays).reversed().map { offset ->
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -offset)
        cal
    }
    val dayLabels = lastDays.map { cal ->
        val sdfDay = java.text.SimpleDateFormat("MM/dd", java.util.Locale.getDefault())
        sdfDay.format(cal.time)
    }
    val dayValues = lastDays.map { cal ->
        val start = (cal.clone() as java.util.Calendar).apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val end = start + 24 * 60 * 60 * 1000
        logs.filter { it.timestamp in start until end }.sumOf { it.changeAmount }
    }

    val cumulativePoints = mutableListOf<Float>()
    var runningSum = goal.currentPoints.toFloat() - dayValues.sum()
    dayValues.forEach { change ->
        runningSum += change
        cumulativePoints.add(runningSum)
    }

    var finalDayLabels = dayLabels
    var finalDayValues = dayValues
    var finalCumulativePoints: List<Float> = cumulativePoints
    var finalDays = lastDays

    if (ignoreInactiveDays) {
        val activeIndices = dayValues.indices.filter { dayValues[it] != 0 }
        if (activeIndices.isNotEmpty()) {
            val keep = mutableSetOf<Int>()
            for (idx in activeIndices) {
                if (idx - 1 >= 0) keep.add(idx - 1)
                keep.add(idx)
            }
            val indices = keep.sorted()
            finalDayLabels = indices.map { dayLabels[it] }
            finalDayValues = indices.map { dayValues[it] }
            finalCumulativePoints = indices.map { cumulativePoints[it] }
            finalDays = indices.map { lastDays[it] }
        } else {
            finalDayLabels = listOf("No change")
            finalDayValues = listOf(0)
            finalCumulativePoints = listOf(goal.currentPoints.toFloat())
            finalDays = listOf(java.util.Calendar.getInstance())
        }
    }

    val drawCount = finalDayValues.size

    LaunchedEffect(drawCount) {
        selectedIndex = drawCount - 1
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Scaffold(
            containerColor = if (isDark) Color(0xFF121212) else Color(0xFFF5F5F5),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (isDark) Color.White else Color(0xFF1D1B20)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Fullscreen Stats - ${goal.title}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF1D1B20)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val periods = listOf(7 to "7d", 14 to "14d", 30 to "30d", 90 to "3m")
                    periods.forEach { (days, label) ->
                        val isSelected = selectedDays == days
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) customColor.copy(alpha = 0.2f)
                                    else Color.Transparent
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) customColor else (if (isDark) Color(0xFF49454F) else Color(0xFFCAC4D0)),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedDays = days }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) customColor else (if (isDark) Color.White else Color(0xFF1D1B20))
                                )
                            )
                        }
                    }
                }

                Text(
                    text = "Tap or drag on the graph to view specific day details.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (isDark) Color(0xFF1E1E1E) else Color.White)
                        .border(
                            width = 1.dp,
                            color = if (isDark) Color(0xFF2C2C2C) else Color(0xFFE0E0E0),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(16.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(drawCount) {
                                detectTapGestures { offset ->
                                    val x = offset.x
                                    val paddingLeft = 70f
                                    val paddingRight = 20f
                                    val chartWidth = size.width.toFloat() - paddingLeft - paddingRight
                                    if (drawCount > 0 && chartWidth > 0) {
                                        if (goal.statsVisualType == 0) {
                                            val barWidth = (chartWidth / drawCount) * 0.6f
                                            val spacing = (chartWidth / drawCount) * 0.4f
                                            var closestIdx = -1
                                            var minDistance = Float.MAX_VALUE
                                            for (i in 0 until drawCount) {
                                                val barX = paddingLeft + i * (barWidth + spacing) + spacing / 2 + barWidth / 2
                                                val dist = if (x >= barX) x - barX else barX - x
                                                if (dist < minDistance) {
                                                    minDistance = dist
                                                    closestIdx = i
                                                }
                                            }
                                            if (closestIdx in 0 until drawCount && minDistance < 80f) {
                                                selectedIndex = closestIdx
                                            }
                                        } else {
                                            val stepX = if (drawCount > 1) chartWidth / (drawCount - 1).toFloat() else chartWidth
                                            var closestIdx = -1
                                            var minDistance = Float.MAX_VALUE
                                            for (i in 0 until drawCount) {
                                                val pointX = paddingLeft + i * stepX
                                                val dist = if (x >= pointX) x - pointX else pointX - x
                                                if (dist < minDistance) {
                                                    minDistance = dist
                                                    closestIdx = i
                                                }
                                            }
                                            if (closestIdx in 0 until drawCount && minDistance < 80f) {
                                                selectedIndex = closestIdx
                                            }
                                        }
                                    }
                                }
                            }
                    ) {
                        val width = size.width
                        val height = size.height
                        val paddingLeft = 70f
                        val paddingRight = 20f
                        val paddingTop = 25f
                        val paddingBottom = 40f
                        val chartWidth = width - paddingLeft - paddingRight
                        val chartHeight = height - paddingTop - paddingBottom

                        drawLine(
                            color = if (isDark) Color(0xFF49454F) else Color(0xFFCAC4D0),
                            start = androidx.compose.ui.geometry.Offset(paddingLeft, paddingTop),
                            end = androidx.compose.ui.geometry.Offset(paddingLeft, height - paddingBottom),
                            strokeWidth = 2f
                        )

                        drawLine(
                            color = if (isDark) Color(0xFF49454F) else Color(0xFFCAC4D0),
                            start = androidx.compose.ui.geometry.Offset(paddingLeft, height - paddingBottom),
                            end = androidx.compose.ui.geometry.Offset(width - paddingRight, height - paddingBottom),
                            strokeWidth = 2f
                        )

                        val gridLinePaint = android.graphics.Paint().apply {
                            color = if (isDark) android.graphics.Color.GRAY else android.graphics.Color.DKGRAY
                            textSize = 18f
                            textAlign = android.graphics.Paint.Align.RIGHT
                            isAntiAlias = true
                        }

                        if (goal.statsVisualType == 0) {
                            val maxVal = maxOf(1f, finalDayValues.maxOf { kotlin.math.abs(it) }.toFloat())

                            val stepBar5 = when {
                                maxVal <= 15f -> 5
                                maxVal <= 35f -> 10
                                maxVal <= 75f -> 20
                                maxVal <= 150f -> 25
                                else -> (((maxVal / 3f) / 5f).toInt() * 5).coerceAtLeast(5)
                            }
                            val barGridValues = mutableListOf<Int>()
                            var bgVal = stepBar5
                            while (bgVal < maxVal.toInt() && barGridValues.size < 3) {
                                barGridValues.add(bgVal)
                                bgVal += stepBar5
                            }
                            for (v in barGridValues) {
                                val gy = height - paddingBottom - (v / maxVal) * chartHeight
                                if (gy in paddingTop..(height - paddingBottom)) {
                                    drawLine(
                                        color = if (isDark) Color(0xFF383838) else Color(0xFFE0E0E0),
                                        start = androidx.compose.ui.geometry.Offset(paddingLeft, gy),
                                        end = androidx.compose.ui.geometry.Offset(width - paddingRight, gy),
                                        strokeWidth = 1.5f,
                                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                                    )
                                    drawContext.canvas.nativeCanvas.drawText("$v", paddingLeft - 8f, gy + 6f, gridLinePaint)
                                }
                            }

                            val barWidth = (chartWidth / drawCount) * 0.6f
                            val spacing = (chartWidth / drawCount) * 0.4f

                            for (i in 0 until drawCount) {
                                val x = paddingLeft + i * (barWidth + spacing) + spacing / 2
                                val valFloat = finalDayValues[i].toFloat()
                                val barHeight = (kotlin.math.abs(valFloat) / maxVal) * chartHeight
                                val y = height - paddingBottom - barHeight

                                val isSelected = selectedIndex == i

                                drawRoundRect(
                                    color = if (isSelected) customColor else (if (valFloat >= 0) customColor.copy(alpha = 0.6f) else Color.Red.copy(alpha = 0.6f)),
                                    topLeft = androidx.compose.ui.geometry.Offset(x, y),
                                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                                    cornerRadius = CornerRadius(6f, 6f)
                                )

                                if (isSelected) {
                                    drawRoundRect(
                                        color = if (isDark) Color.White else Color.Black,
                                        topLeft = androidx.compose.ui.geometry.Offset(x - 2f, y - 2f),
                                        size = androidx.compose.ui.geometry.Size(barWidth + 4f, barHeight + 4f),
                                        cornerRadius = CornerRadius(6f, 6f),
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                                    )
                                }

                                drawContext.canvas.nativeCanvas.apply {
                                    val textPaint = android.graphics.Paint().apply {
                                        color = if (isDark) android.graphics.Color.WHITE else android.graphics.Color.BLACK
                                        textSize = 18f
                                        textAlign = android.graphics.Paint.Align.CENTER
                                        isAntiAlias = true
                                    }

                                    val shouldDrawLabel = when {
                                        drawCount <= 7 -> true
                                        drawCount <= 14 -> i % 2 == 0
                                        drawCount <= 30 -> i % 5 == 0
                                        else -> i % (drawCount / 6).coerceAtLeast(1) == 0
                                    }

                                    if (shouldDrawLabel && i < finalDayLabels.size) {
                                        drawText(
                                            finalDayLabels[i],
                                            x + barWidth / 2,
                                            height - 8f,
                                            textPaint
                                        )
                                    }

                                    val shouldDrawValue = when {
                                        drawCount <= 14 -> true
                                        drawCount <= 30 -> finalDayValues[i] != 0
                                        else -> finalDayValues[i] != 0 && i % 2 == 0
                                    }

                                    if (shouldDrawValue && i < finalDayValues.size) {
                                        val changeVal = finalDayValues[i]
                                        val valPaint = android.graphics.Paint().apply {
                                            color = if (isSelected) (if (isDark) android.graphics.Color.WHITE else android.graphics.Color.BLACK) else (if (isDark) android.graphics.Color.LTGRAY else android.graphics.Color.DKGRAY)
                                            textSize = 17f
                                            textAlign = android.graphics.Paint.Align.CENTER
                                            isAntiAlias = true
                                            typeface = if (isSelected) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
                                        }
                                        drawText(
                                            "${if (changeVal > 0) "+" else ""}$changeVal",
                                            x + barWidth / 2,
                                            maxOf(y - 8f, paddingTop + 6f),
                                            valPaint
                                        )
                                    }
                                }
                            }
                        } else {
                            val minP = finalCumulativePoints.minOrNull() ?: 0f
                            val maxP = finalCumulativePoints.maxOrNull() ?: (minP + 1f)
                            val range = (maxP - minP).coerceAtLeast(1f)

                            val spanP = range
                            val step5 = when {
                                spanP <= 15f -> 5
                                spanP <= 35f -> 10
                                spanP <= 75f -> 20
                                spanP <= 150f -> 25
                                spanP <= 300f -> 50
                                spanP <= 600f -> 100
                                else -> (((spanP / 3f) / 5f).toInt() * 5).coerceAtLeast(5)
                            }
                            val gridValues = mutableListOf<Int>()
                            var gVal = (kotlin.math.ceil(minP / step5) * step5).toInt()
                            if (gVal <= minP.toInt()) gVal += step5
                            while (gVal < maxP.toInt() && gridValues.size < 4) {
                                gridValues.add(gVal)
                                gVal += step5
                            }
                            val selectedGridLines = if (gridValues.size > 3) listOf(gridValues[0], gridValues[gridValues.size / 2], gridValues.last()) else gridValues

                            for (v in selectedGridLines) {
                                val gy = height - paddingBottom - ((v - minP) / range) * chartHeight
                                if (gy in paddingTop..(height - paddingBottom)) {
                                    drawLine(
                                        color = if (isDark) Color(0xFF383838) else Color(0xFFE0E0E0),
                                        start = androidx.compose.ui.geometry.Offset(paddingLeft, gy),
                                        end = androidx.compose.ui.geometry.Offset(width - paddingRight, gy),
                                        strokeWidth = 1.5f,
                                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                                    )
                                    drawContext.canvas.nativeCanvas.drawText("$v", paddingLeft - 8f, gy + 6f, gridLinePaint)
                                }
                            }

                            val stepX = if (drawCount > 1) chartWidth / (drawCount - 1).toFloat() else chartWidth
                            val path = androidx.compose.ui.graphics.Path()

                            for (i in 0 until drawCount) {
                                val x = paddingLeft + i * stepX
                                val pointVal = finalCumulativePoints[i]
                                val y = height - paddingBottom - ((pointVal - minP) / range) * chartHeight
                                if (i == 0) {
                                    path.moveTo(x, y)
                                } else {
                                    path.lineTo(x, y)
                                }
                            }

                            drawPath(
                                path = path,
                                color = customColor,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = 3.dp.toPx(),
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                                )
                            )

                            for (i in 0 until drawCount) {
                                val x = paddingLeft + i * stepX
                                val pointVal = finalCumulativePoints[i]
                                val y = height - paddingBottom - ((pointVal - minP) / range) * chartHeight

                                val isSelected = selectedIndex == i

                                drawCircle(
                                    color = if (isSelected) (if (isDark) Color.White else Color.Black) else customColor,
                                    radius = if (isSelected) 8f else 5f,
                                    center = androidx.compose.ui.geometry.Offset(x, y)
                                )

                                if (isSelected) {
                                    drawCircle(
                                        color = customColor,
                                        radius = 4f,
                                        center = androidx.compose.ui.geometry.Offset(x, y)
                                    )
                                }

                                drawContext.canvas.nativeCanvas.apply {
                                    val textPaint = android.graphics.Paint().apply {
                                        color = if (isDark) android.graphics.Color.WHITE else android.graphics.Color.BLACK
                                        textSize = 18f
                                        textAlign = android.graphics.Paint.Align.CENTER
                                        isAntiAlias = true
                                    }

                                    val shouldDrawLabel = when {
                                        drawCount <= 7 -> true
                                        drawCount <= 14 -> i % 2 == 0
                                        drawCount <= 30 -> i % 5 == 0
                                        else -> i % (drawCount / 6).coerceAtLeast(1) == 0
                                    }

                                    if (shouldDrawLabel && i < finalDayLabels.size) {
                                        drawText(
                                            finalDayLabels[i],
                                            x,
                                            height - 8f,
                                            textPaint
                                        )
                                    }

                                    val shouldDrawValue = when {
                                        drawCount <= 14 -> true
                                        drawCount <= 30 -> i % 2 == 0 || i == drawCount - 1
                                        else -> i % (drawCount / 8).coerceAtLeast(1) == 0 || i == drawCount - 1
                                    }

                                    if (shouldDrawValue && i < finalCumulativePoints.size) {
                                        val valPaint = android.graphics.Paint().apply {
                                            color = if (isSelected) (if (isDark) android.graphics.Color.WHITE else android.graphics.Color.BLACK) else (if (isDark) android.graphics.Color.LTGRAY else android.graphics.Color.DKGRAY)
                                            textSize = 17f
                                            textAlign = android.graphics.Paint.Align.CENTER
                                            isAntiAlias = true
                                            typeface = if (isSelected) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
                                        }
                                        drawText(
                                            "%.0f".format(pointVal),
                                            x,
                                            maxOf(y - 12f, paddingTop + 6f),
                                            valPaint
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0xFF1E1E1E) else Color.White)
                        .clickable { ignoreInactiveDays = !ignoreInactiveDays }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = ignoreInactiveDays,
                        onCheckedChange = { ignoreInactiveDays = it },
                        colors = CheckboxDefaults.colors(checkedColor = customColor),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ignore inactive days (no points change)",
                        style = MaterialTheme.typography.bodyMedium.copy(color = if (isDark) Color.White else Color(0xFF1D1B20))
                    )
                }

                selectedIndex?.let { idx ->
                    if (idx in 0 until drawCount) {
                        val cal = finalDays[idx]
                        val fullDateStr = java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.getDefault()).format(cal.time)
                        val change = finalDayValues[idx]
                        val cumulative = finalCumulativePoints[idx]
                        val previous = cumulative - change

                        val percentageChangeStr = if (previous != 0f) {
                            val pct = (change.toFloat() / previous) * 100
                            "${if (pct >= 0) "+" else ""}${"%.1f".format(pct)}%"
                        } else if (change != 0) {
                            "+100.0%"
                        } else {
                            "0.0%"
                        }

                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) Color(0xFF1E1E1E) else Color.White
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = if (isDark) Color(0xFF2C2C2C) else Color(0xFFE0E0E0),
                                    shape = RoundedCornerShape(24.dp)
                                )
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "Day Details",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = customColor
                                    )
                                )

                                StatRow(label = "Date", value = fullDateStr, isDark = isDark)
                                StatRow(
                                    label = "Points change",
                                    value = "${if (change > 0) "+" else ""}$change ${goal.unit}",
                                    valueColor = if (change > 0) customColor else if (change < 0) Color.Red else null,
                                    isDark = isDark
                                )
                                StatRow(
                                    label = "Percentage change",
                                    value = percentageChangeStr,
                                    valueColor = if (change > 0) customColor else if (change < 0) Color.Red else null,
                                    isDark = isDark
                                )
                                StatRow(
                                    label = "Total cumulative points",
                                    value = "%.0f %s".format(cumulative, goal.unit),
                                    isDark = isDark
                                )

                                var showIgnoreConfirmationDialog by remember { mutableStateOf(false) }

                                OutlinedButton(
                                    onClick = { showIgnoreConfirmationDialog = true },
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, if (isDark) Color(0xFF49454F) else Color(0xFFCAC4D0)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = if (isDark) Color(0xFFFF8A80) else Color(0xFFD32F2F)
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Ignore",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Ignore points change for this day")
                                }

                                if (showIgnoreConfirmationDialog) {
                                    val dayCal = finalDays[idx]
                                    val dayStart = (dayCal.clone() as java.util.Calendar).apply {
                                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                                        set(java.util.Calendar.MINUTE, 0)
                                        set(java.util.Calendar.SECOND, 0)
                                        set(java.util.Calendar.MILLISECOND, 0)
                                    }.timeInMillis
                                    val dayEnd = dayStart + 24 * 60 * 60 * 1000

                                    AlertDialog(
                                        onDismissRequest = { showIgnoreConfirmationDialog = false },
                                        title = {
                                            Text(
                                                text = "Ignore Points Change?",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                        },
                                        text = {
                                            Text(
                                                text = "Are you sure you want to ignore the points change for $fullDateStr? Only the points change stat will be +0 and the other stats for that day will stay the same.",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        },
                                        confirmButton = {
                                            Button(
                                                onClick = {
                                                    viewModel?.ignorePointsChangeForDay(goal.id, dayStart, dayEnd)
                                                    showIgnoreConfirmationDialog = false
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                                            ) {
                                                Text("Confirm", color = Color.White)
                                            }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { showIgnoreConfirmationDialog = false }) {
                                                Text("Cancel")
                                            }
                                        },
                                        shape = RoundedCornerShape(20.dp),
                                        containerColor = if (isDark) Color(0xFF1E1E1E) else Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GroupFullScreenGraphDialog(
    group: GoalGroup,
    isDark: Boolean,
    childGoals: List<Goal>,
    groupLogs: List<GoalLog>,
    selectedDaysInitial: Int,
    ignoreInactiveDaysInitial: Boolean = false,
    onDismiss: () -> Unit,
    customColor: Color
) {
    var selectedDays by remember { mutableStateOf(selectedDaysInitial) }
    var ignoreInactiveDays by remember { mutableStateOf(ignoreInactiveDaysInitial) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    val lastDays = (0 until selectedDays).reversed().map { offset ->
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -offset)
        cal
    }
    val dayLabels = lastDays.map { cal ->
        val sdfDay = java.text.SimpleDateFormat("MM/dd", java.util.Locale.US)
        sdfDay.format(cal.time)
    }

    val currentTotalGroupPoints = if (group.displayType == 1) {
        childGoals.sumOf {
            if (it.maxPoints > 0) {
                val fraction = it.currentPoints.toDouble() / it.maxPoints.toDouble()
                if (it.allowGroupPointsOverflow) fraction * it.groupValue else minOf(it.groupValue, fraction * it.groupValue)
            } else 0.0
        }
    } else {
        childGoals.sumOf { it.currentPoints }.toDouble()
    }

    val dayValues = lastDays.map { cal ->
        val s = (cal.clone() as java.util.Calendar).apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val e = s + 24 * 60 * 60 * 1000
        val logsInDay = groupLogs.filter { it.timestamp in s until e }
        logsInDay.sumOf { log ->
            val g = childGoals.find { it.id == log.goalId }
            if (group.displayType == 1 && g != null && g.maxPoints > 0) {
                log.changeAmount.toDouble() * (g.groupValue / g.maxPoints.toDouble())
            } else {
                log.changeAmount.toDouble()
            }
        }
    }

    val cumulativePoints = mutableListOf<Double>()
    var runningSum = currentTotalGroupPoints - dayValues.sum()
    dayValues.forEach { change ->
        runningSum += change
        cumulativePoints.add(runningSum)
    }

    var finalDayLabels = dayLabels
    var finalDayValues = dayValues
    var finalCumulativePoints: List<Double> = cumulativePoints
    var finalDays = lastDays

    if (ignoreInactiveDays) {
        val activeIndices = dayValues.indices.filter { dayValues[it] != 0.0 }
        if (activeIndices.isNotEmpty()) {
            val keep = mutableSetOf<Int>()
            for (idx in activeIndices) {
                if (idx - 1 >= 0) keep.add(idx - 1)
                keep.add(idx)
            }
            val indices = keep.sorted()
            finalDayLabels = indices.map { dayLabels[it] }
            finalDayValues = indices.map { dayValues[it] }
            finalCumulativePoints = indices.map { cumulativePoints[it] }
            finalDays = indices.map { lastDays[it] }
        } else {
            finalDayLabels = listOf(dayLabels.lastOrNull() ?: "Today")
            finalDayValues = listOf(0.0)
            finalCumulativePoints = listOf(currentTotalGroupPoints)
            finalDays = listOf(java.util.Calendar.getInstance())
        }
    }

    val drawCount = finalDayValues.size

    LaunchedEffect(drawCount) {
        selectedIndex = drawCount - 1
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Scaffold(
            containerColor = if (isDark) Color(0xFF121212) else Color(0xFFF5F5F5),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (isDark) Color.White else Color(0xFF1D1B20)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Fullscreen Stats - ${group.title}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF1D1B20)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Period Selection Pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDark) Color(0xFF1E1E1E) else Color(0xFFEFEFEF))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val periods = listOf(7 to "7d", 14 to "14d", 30 to "30d", 90 to "3m")
                    periods.forEach { (days, label) ->
                        val isSelected = selectedDays == days
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) customColor.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable { selectedDays = days }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) customColor else (if (isDark) Color.White else Color(0xFF1D1B20))
                                )
                            )
                        }
                    }
                }

                Text(
                    text = "Tap on the graph to view specific day details.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (isDark) Color(0xFF1E1E1E) else Color.White)
                        .border(
                            width = 1.dp,
                            color = if (isDark) Color(0xFF2C2C2C) else Color(0xFFE0E0E0),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(16.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(drawCount) {
                                detectTapGestures { offset ->
                                    val x = offset.x
                                    val paddingLeft = 70f
                                    val paddingRight = 20f
                                    val chartWidth = size.width.toFloat() - paddingLeft - paddingRight
                                    if (drawCount > 0 && chartWidth > 0) {
                                        val stepX = if (drawCount > 1) chartWidth / (drawCount - 1).toFloat() else chartWidth
                                        var closestIdx = -1
                                        var minDistance = Float.MAX_VALUE
                                        for (i in 0 until drawCount) {
                                            val pointX = paddingLeft + i * stepX
                                            val dist = if (x >= pointX) x - pointX else pointX - x
                                            if (dist < minDistance) {
                                                minDistance = dist
                                                closestIdx = i
                                            }
                                        }
                                        if (closestIdx in 0 until drawCount && minDistance < 80f) {
                                            selectedIndex = closestIdx
                                        }
                                    }
                                }
                            }
                    ) {
                        val width = size.width
                        val height = size.height
                        val paddingLeft = 70f
                        val paddingRight = 20f
                        val paddingTop = 25f
                        val paddingBottom = 40f
                        val chartWidth = width - paddingLeft - paddingRight
                        val chartHeight = height - paddingTop - paddingBottom

                        val minP = finalCumulativePoints.minOrNull() ?: 0.0
                        val maxP = finalCumulativePoints.maxOrNull() ?: 1.0
                        val rangeP = if (maxP - minP > 0.0001) (maxP - minP).toFloat() else 1f

                        val textPaint = android.graphics.Paint().apply {
                            color = if (isDark) android.graphics.Color.WHITE else android.graphics.Color.DKGRAY
                            textSize = 28f
                            isAntiAlias = true
                            textAlign = android.graphics.Paint.Align.RIGHT
                        }
                        val centerTextPaint = android.graphics.Paint().apply {
                            color = if (isDark) android.graphics.Color.WHITE else android.graphics.Color.DKGRAY
                            textSize = 26f
                            isAntiAlias = true
                            textAlign = android.graphics.Paint.Align.CENTER
                        }

                        // Grid lines and Y-axis values
                        val steps = 3
                        for (i in 0..steps) {
                            val y = paddingTop + chartHeight * (1f - i.toFloat() / steps)
                            val value = minP + (rangeP * i.toFloat() / steps)
                            drawLine(
                                color = if (isDark) Color(0xFF2C2B36) else Color(0xFFEEEEEE),
                                start = androidx.compose.ui.geometry.Offset(paddingLeft, y),
                                end = androidx.compose.ui.geometry.Offset(width - paddingRight, y),
                                strokeWidth = 1f
                            )
                            drawContext.canvas.nativeCanvas.drawText(
                                "%.1f".format(value),
                                paddingLeft - 10f,
                                y + 10f,
                                textPaint
                            )
                        }

                        if (drawCount > 0) {
                            val stepX = if (drawCount > 1) chartWidth / (drawCount - 1).toFloat() else chartWidth
                            val path = androidx.compose.ui.graphics.Path()

                            for (i in 0 until drawCount) {
                                val x = paddingLeft + i * stepX
                                val pointVal = finalCumulativePoints.getOrElse(i) { 0.0 }
                                val normY = ((pointVal - minP).toFloat() / rangeP).coerceIn(0f, 1f)
                                val y = paddingTop + chartHeight * (1f - normY)

                                if (i == 0) {
                                    path.moveTo(x, y)
                                } else {
                                    path.lineTo(x, y)
                                }

                                val isSelected = selectedIndex == i
                                drawCircle(
                                    color = if (isSelected) Color.White else customColor,
                                    radius = if (isSelected) 8f else 5f,
                                    center = androidx.compose.ui.geometry.Offset(x, y)
                                )
                                if (isSelected) {
                                    drawCircle(
                                        color = customColor,
                                        radius = 12f,
                                        center = androidx.compose.ui.geometry.Offset(x, y),
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                                    )
                                }

                                val shouldDrawLabel = when {
                                    drawCount <= 7 -> true
                                    drawCount <= 14 -> i % 2 == 0
                                    drawCount <= 30 -> i % 5 == 0
                                    else -> i % 10 == 0
                                }
                                if (shouldDrawLabel && i < finalDayLabels.size) {
                                    drawContext.canvas.nativeCanvas.drawText(
                                        finalDayLabels[i],
                                        x,
                                        height - 8f,
                                        centerTextPaint
                                    )
                                }
                            }

                            drawPath(
                                path = path,
                                color = customColor,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                            )
                        }
                    }
                }

                // Ignore Inactive Days Checkbox below chart
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { ignoreInactiveDays = !ignoreInactiveDays }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Checkbox(
                        checked = ignoreInactiveDays,
                        onCheckedChange = { ignoreInactiveDays = it },
                        colors = CheckboxDefaults.colors(checkedColor = customColor),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Ignore inactive days",
                        style = MaterialTheme.typography.bodyMedium.copy(color = if (isDark) Color.White else Color(0xFF1D1B20))
                    )
                }

                // Selected Day Details Card
                selectedIndex?.let { idx ->
                    if (idx in 0 until drawCount) {
                        val dayCal = finalDays.getOrElse(idx) { java.util.Calendar.getInstance() }
                        val sdfFull = java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.US)
                        val fullDateStr = sdfFull.format(dayCal.time)
                        val change = finalDayValues.getOrElse(idx) { 0.0 }
                        val cumulative = finalCumulativePoints.getOrElse(idx) { 0.0 }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) Color(0xFF1E1E1E) else Color.White
                            ),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF2C2C2C) else Color(0xFFE0E0E0))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Day Details",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = customColor
                                    )
                                )

                                StatRow(label = "Date", value = fullDateStr, isDark = isDark)
                                StatRow(
                                    label = "Points change",
                                    value = "${if (change > 0) "+" else ""}%.1f".format(change),
                                    valueColor = if (change > 0) customColor else if (change < 0) Color.Red else null,
                                    isDark = isDark
                                )
                                StatRow(
                                    label = "Total cumulative points",
                                    value = "%.1f".format(cumulative),
                                    isDark = isDark
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GroupStatsDialog(
    group: GoalGroup,
    isDark: Boolean,
    allGoals: List<Goal>,
    groupGoalStats: List<GroupGoalStats>,
    allLogs: List<GoalLog>,
    isLandscape: Boolean = false,
    onDismiss: () -> Unit,
    outlineColorHex: String? = null,
    isBrighterText: Boolean = false
) {
    val childGoals = remember(allGoals, group.id) {
        allGoals.filter { it.parentGroupId == group.id }
    }
    val groupGoalIds = remember(childGoals) {
        childGoals.map { it.id }.toSet()
    }
    val groupLogs = remember(allLogs, groupGoalIds) {
        allLogs.filter { it.goalId in groupGoalIds }
    }

    val customColor = remember(group.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(group.colorHex))
        } catch (e: Exception) {
            Color(0xFFE56C38)
        }
    }
    val cardBgColor = if (isDark) Color(0xFF1E1D24) else Color(0xFFFFFFFF)
    val cardBorderColor = if (isDark) Color(0xFF2C2B36) else Color(0xFFE0E0E0)

    val creationDateStr = remember(group.createdAt) {
        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US)
        sdf.format(java.util.Date(group.createdAt))
    }

    val periods = remember {
        listOf(7 to "7d", 14 to "14d", 30 to "30d", 90 to "3m")
    }
    var selectedDays by remember { mutableStateOf(7) }
    var ignoreInactiveDays by remember { mutableStateOf(false) }
    var showFullScreenGraph by remember { mutableStateOf(false) }

    val lastDays = remember(selectedDays) {
        (0 until selectedDays).reversed().map { offset ->
            val cal = java.util.Calendar.getInstance()
            cal.add(java.util.Calendar.DAY_OF_YEAR, -offset)
            cal
        }
    }
    val dayLabels = remember(lastDays) {
        val sdf = java.text.SimpleDateFormat("MM/dd", java.util.Locale.US)
        lastDays.map { sdf.format(it.time) }
    }

    val currentTotalGroupPoints = remember(childGoals, group.displayType) {
        if (group.displayType == 1) {
            childGoals.sumOf {
                if (it.maxPoints > 0) {
                    val fraction = it.currentPoints.toDouble() / it.maxPoints.toDouble()
                    if (it.allowGroupPointsOverflow) fraction * it.groupValue else minOf(it.groupValue, fraction * it.groupValue)
                } else 0.0
            }
        } else {
            childGoals.sumOf { it.currentPoints }.toDouble()
        }
    }

    val maxTotalGroupPoints = remember(childGoals, group.displayType) {
        if (group.displayType == 1) {
            childGoals.sumOf { it.groupValue }
        } else {
            childGoals.sumOf { it.maxPoints }.toDouble()
        }
    }

    val dayValues = remember(lastDays, groupLogs, childGoals, group.displayType) {
        lastDays.map { cal ->
            val s = (cal.clone() as java.util.Calendar).apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis
            val e = s + 24 * 60 * 60 * 1000
            val logsInDay = groupLogs.filter { it.timestamp in s until e }
            logsInDay.sumOf { log ->
                val g = childGoals.find { it.id == log.goalId }
                if (group.displayType == 1 && g != null && g.maxPoints > 0) {
                    log.changeAmount.toDouble() * (g.groupValue / g.maxPoints.toDouble())
                } else {
                    log.changeAmount.toDouble()
                }
            }
        }
    }

    val cumulativePoints = remember(currentTotalGroupPoints, dayValues) {
        val list = mutableListOf<Double>()
        var runningSum = currentTotalGroupPoints - dayValues.sum()
        dayValues.forEach { change ->
            runningSum += change
            list.add(runningSum)
        }
        list
    }

    var finalDayLabels: List<String> = dayLabels
    var finalDayValues: List<Double> = dayValues
    var finalCumulativePoints: List<Double> = cumulativePoints

    if (ignoreInactiveDays) {
        val activeIndices = dayValues.indices.filter { dayValues[it] != 0.0 }
        if (activeIndices.isNotEmpty()) {
            val keep = mutableSetOf<Int>()
            for (idx in activeIndices) {
                if (idx - 1 >= 0) keep.add(idx - 1)
                keep.add(idx)
            }
            val indices = keep.sorted()
            finalDayLabels = indices.map { dayLabels[it] }
            finalDayValues = indices.map { dayValues[it] }
            finalCumulativePoints = indices.map { cumulativePoints[it] }
        } else {
            finalDayLabels = listOf(dayLabels.lastOrNull() ?: "Today")
            finalDayValues = listOf(0.0)
            finalCumulativePoints = listOf(currentTotalGroupPoints)
        }
    }

    val daysSinceGroupCreation = remember(group.createdAt) {
        val calToday = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val calCreated = java.util.Calendar.getInstance().apply {
            timeInMillis = group.createdAt
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val diffDays = ((calToday.timeInMillis - calCreated.timeInMillis) / (1000L * 60 * 60 * 24)).coerceAtLeast(0L) + 1L
        maxOf(1L, diffDays)
    }
    val avgPointsPerDay = remember(currentTotalGroupPoints, daysSinceGroupCreation) {
        currentTotalGroupPoints / daysSinceGroupCreation.toDouble()
    }
    val avgPointsStr = remember(avgPointsPerDay) {
        String.format(java.util.Locale.US, "%.1f", avgPointsPerDay.coerceAtLeast(0.0))
    }
    val pointsToday = remember(dayValues) {
        dayValues.lastOrNull() ?: 0.0
    }
    val progressTodayStr = remember(pointsToday) {
        String.format(java.util.Locale.US, "%.1f", pointsToday)
    }
    val totalGroupPointsCountStr = remember(maxTotalGroupPoints) {
        formatGroupPoint(maxTotalGroupPoints)
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .clickable(
                    onClick = onDismiss,
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isLandscape) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.96f)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Left Column: GroupCard (shrunk) + Individual goals contribution Card
                    Column(
                        modifier = Modifier
                            .weight(0.48f)
                            .clickable(enabled = false) {},
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        GroupCard(
                            group = group,
                            isDark = isDark,
                            groupGoalStats = groupGoalStats,
                            allGoals = allGoals,
                            onEnter = {},
                            onEdit = {},
                            onDelete = {},
                            isExpanded = false,
                            isBrighterText = isBrighterText,
                            outlineColorHex = outlineColorHex,
                            onShowStats = null
                        )

                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBgColor),
                            border = BorderStroke(1.dp, cardBorderColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                IndividualGoalsContributionSection(
                                    childGoals = childGoals,
                                    isPointsTracking = group.displayType == 1,
                                    isDark = isDark,
                                    isBrighterText = isBrighterText,
                                    customColor = customColor,
                                    creationDateStr = creationDateStr
                                )
                            }
                        }
                    }

                    // Right Column: Group Stats Card
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBgColor),
                        border = BorderStroke(1.dp, cardBorderColor),
                        modifier = Modifier
                            .weight(0.52f)
                            .clickable(enabled = false) {}
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            GroupStatsCardHeader(
                                group = group,
                                customColor = customColor,
                                isDark = isDark,
                                isBrighterText = isBrighterText,
                                onDismiss = onDismiss
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Total goals count",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (isDark) Color(0xFFE0E0E0) else Color(0xFF333333)
                                    )
                                )
                                Text(
                                    text = "${childGoals.size}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark || isBrighterText) Color.White else Color(0xFF1D1B20)
                                    )
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Total group points count",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (isDark) Color(0xFFE0E0E0) else Color(0xFF333333)
                                    )
                                )
                                Text(
                                    text = totalGroupPointsCountStr,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark || isBrighterText) Color.White else Color(0xFF1D1B20)
                                    )
                                )
                            }

                            GroupStatsPeriodPills(
                                periods = periods,
                                selectedDays = selectedDays,
                                onSelectPeriod = { selectedDays = it },
                                customColor = customColor,
                                isDark = isDark
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Cumulative Trend (Graph)",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark || isBrighterText) Color.White else Color(0xFF1D1B20)
                                    )
                                )
                                IconButton(
                                    onClick = { showFullScreenGraph = true },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ZoomIn,
                                        contentDescription = "Zoom Graph",
                                        tint = customColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            GroupStatsGraph(
                                finalCumulativePoints = finalCumulativePoints,
                                finalDayLabels = finalDayLabels,
                                customColor = customColor,
                                isDark = isDark
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { ignoreInactiveDays = !ignoreInactiveDays }
                            ) {
                                Checkbox(
                                    checked = ignoreInactiveDays,
                                    onCheckedChange = { ignoreInactiveDays = it },
                                    colors = CheckboxDefaults.colors(checkedColor = customColor),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Ignore inactive days",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (isDark || isBrighterText) Color.White else Color(0xFF1D1B20)
                                    )
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Average group points/day",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (isDark) Color(0xFFE0E0E0) else Color(0xFF333333)
                                    )
                                )
                                Text(
                                    text = avgPointsStr,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark || isBrighterText) Color.White else Color(0xFF1D1B20)
                                    )
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Progress today",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (isDark) Color(0xFFE0E0E0) else Color(0xFF333333)
                                    )
                                )
                                Text(
                                    text = progressTodayStr,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark || isBrighterText) Color.White else Color(0xFF1D1B20)
                                    )
                                )
                            }
                        }
                    }
                }
            } else {
                // Portrait Mode
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top: GroupCard (shrink down)
                    GroupCard(
                        group = group,
                        isDark = isDark,
                        groupGoalStats = groupGoalStats,
                        allGoals = allGoals,
                        onEnter = {},
                        onEdit = {},
                        onDelete = {},
                        isExpanded = false,
                        isBrighterText = isBrighterText,
                        outlineColorHex = outlineColorHex,
                        onShowStats = null
                    )

                    // Bottom: Group Stats Card
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBgColor),
                        border = BorderStroke(1.dp, cardBorderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = false) {}
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            GroupStatsCardHeader(
                                group = group,
                                customColor = customColor,
                                isDark = isDark,
                                isBrighterText = isBrighterText,
                                onDismiss = onDismiss
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Total goals count",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (isDark) Color(0xFFE0E0E0) else Color(0xFF333333)
                                    )
                                )
                                Text(
                                    text = "${childGoals.size}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark || isBrighterText) Color.White else Color(0xFF1D1B20)
                                    )
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Total group points count",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (isDark) Color(0xFFE0E0E0) else Color(0xFF333333)
                                    )
                                )
                                Text(
                                    text = totalGroupPointsCountStr,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark || isBrighterText) Color.White else Color(0xFF1D1B20)
                                    )
                                )
                            }

                            GroupStatsPeriodPills(
                                periods = periods,
                                selectedDays = selectedDays,
                                onSelectPeriod = { selectedDays = it },
                                customColor = customColor,
                                isDark = isDark
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Cumulative Trend (Graph)",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark || isBrighterText) Color.White else Color(0xFF1D1B20)
                                    )
                                )
                                IconButton(
                                    onClick = { showFullScreenGraph = true },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ZoomIn,
                                        contentDescription = "Zoom Graph",
                                        tint = customColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            GroupStatsGraph(
                                finalCumulativePoints = finalCumulativePoints,
                                finalDayLabels = finalDayLabels,
                                customColor = customColor,
                                isDark = isDark
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { ignoreInactiveDays = !ignoreInactiveDays }
                            ) {
                                Checkbox(
                                    checked = ignoreInactiveDays,
                                    onCheckedChange = { ignoreInactiveDays = it },
                                    colors = CheckboxDefaults.colors(checkedColor = customColor),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Ignore inactive days",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (isDark || isBrighterText) Color.White else Color(0xFF1D1B20)
                                    )
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Average group points/day",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (isDark) Color(0xFFE0E0E0) else Color(0xFF333333)
                                    )
                                )
                                Text(
                                    text = avgPointsStr,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark || isBrighterText) Color.White else Color(0xFF1D1B20)
                                    )
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Progress today",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (isDark) Color(0xFFE0E0E0) else Color(0xFF333333)
                                    )
                                )
                                Text(
                                    text = progressTodayStr,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark || isBrighterText) Color.White else Color(0xFF1D1B20)
                                    )
                                )
                            }

                            HorizontalDivider(
                                color = if (isDark) Color(0xFF2C2B36) else Color(0xFFE0E0E0),
                                thickness = 1.dp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            IndividualGoalsContributionSection(
                                childGoals = childGoals,
                                isPointsTracking = group.displayType == 1,
                                isDark = isDark,
                                isBrighterText = isBrighterText,
                                customColor = customColor,
                                creationDateStr = creationDateStr
                            )
                        }
                    }
                }
            }
        }

        if (showFullScreenGraph) {
            GroupFullScreenGraphDialog(
                group = group,
                isDark = isDark,
                childGoals = childGoals,
                groupLogs = groupLogs,
                selectedDaysInitial = selectedDays,
                ignoreInactiveDaysInitial = ignoreInactiveDays,
                onDismiss = { showFullScreenGraph = false },
                customColor = customColor
            )
        }
    }
}

@Composable
private fun GroupStatsCardHeader(
    group: GoalGroup,
    customColor: Color,
    isDark: Boolean,
    isBrighterText: Boolean,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column {
            Text(
                text = group.title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isDark || isBrighterText) Color.White else Color(0xFF1D1B20)
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Stats for this group",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = customColor,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = customColor
            )
        }
    }
}

@Composable
private fun GroupStatsPeriodPills(
    periods: List<Pair<Int, String>>,
    selectedDays: Int,
    onSelectPeriod: (Int) -> Unit,
    customColor: Color,
    isDark: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        periods.forEach { (days, label) ->
            val isSelected = selectedDays == days
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) customColor.copy(alpha = 0.3f)
                        else if (isDark) Color(0xFF26252E) else Color(0xFFF0F0F2)
                    )
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) customColor else (if (isDark) Color(0xFF3E3D48) else Color(0xFFD6D6DA)),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onSelectPeriod(days) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) customColor else (if (isDark) Color(0xFFB0B0B8) else Color(0xFF555555))
                    )
                )
            }
        }
    }
}

@Composable
private fun GroupStatsGraph(
    finalCumulativePoints: List<Double>,
    finalDayLabels: List<String>,
    customColor: Color,
    isDark: Boolean
) {
    val drawCount = finalCumulativePoints.size
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
    ) {
        val width = size.width
        val height = size.height
        val paddingLeft = 28f
        val paddingRight = 28f
        val paddingTop = 32f
        val paddingBottom = 28f
        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom
        val baselineY = height - paddingBottom

        // Baseline horizontal line
        drawLine(
            color = if (isDark) Color(0xFF4A4955) else Color(0xFFD0D0D5),
            start = androidx.compose.ui.geometry.Offset(paddingLeft, baselineY),
            end = androidx.compose.ui.geometry.Offset(width - paddingRight, baselineY),
            strokeWidth = 1.5.dp.toPx()
        )

        if (drawCount > 0) {
            val minP = finalCumulativePoints.minOrNull() ?: 0.0
            val maxP = finalCumulativePoints.maxOrNull() ?: (minP + 1.0)
            val range = if (maxP == minP) 1.0 else (maxP - minP)

            val points = (0 until drawCount).map { i ->
                val x = if (drawCount == 1) {
                    width / 2
                } else {
                    paddingLeft + i * (chartWidth / (drawCount - 1))
                }
                val pVal = finalCumulativePoints[i]
                val y = baselineY - ((pVal - minP) / range).toFloat() * chartHeight
                androidx.compose.ui.geometry.Offset(x, y.coerceIn(paddingTop, baselineY))
            }

            // Draw line connecting dots
            if (points.size > 1) {
                val linePath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(points[0].x, points[0].y)
                    for (i in 1 until points.size) {
                        lineTo(points[i].x, points[i].y)
                    }
                }
                drawPath(
                    path = linePath,
                    color = customColor,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 3.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                        join = androidx.compose.ui.graphics.StrokeJoin.Round
                    )
                )
            }

            // Determine well-spaced indices for values and dates
            val valueIndices = if (drawCount <= 7) {
                (0 until drawCount).toSet()
            } else {
                // Greatly reduce the amount of numbers on the graph (only 4-5 well-spaced milestones)
                val step = (drawCount - 1) / 4.0
                setOf(
                    0,
                    (step).toInt(),
                    (step * 2).toInt(),
                    (step * 3).toInt(),
                    drawCount - 1
                )
            }

            val dateIndices = when {
                drawCount <= 7 -> (0 until drawCount).toSet()
                drawCount <= 14 -> (0 until drawCount).filter { it % 3 == 0 || it == drawCount - 1 }.toSet()
                drawCount <= 30 -> (0 until drawCount).filter { it % 6 == 0 || it == drawCount - 1 }.toSet()
                else -> (0 until drawCount).filter { it % (drawCount / 5).coerceAtLeast(1) == 0 || it == drawCount - 1 }.toSet()
            }

            // Draw dots and text
            drawContext.canvas.nativeCanvas.apply {
                val datePaint = android.graphics.Paint().apply {
                    color = if (isDark) android.graphics.Color.parseColor("#CAC4D0") else android.graphics.Color.parseColor("#49454F")
                    textSize = 11.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                val valPaint = android.graphics.Paint().apply {
                    color = if (isDark) android.graphics.Color.WHITE else android.graphics.Color.BLACK
                    textSize = 14.sp.toPx()
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }

                for (i in 0 until drawCount) {
                    val pt = points[i]
                    // Dot
                    drawCircle(
                        color = customColor,
                        radius = 4.5.dp.toPx(),
                        center = pt
                    )

                    // Value above dot (greatly reduced count & larger text)
                    if (i in valueIndices) {
                        val pointVal = finalCumulativePoints[i]
                        val valStr = if (pointVal == 0.0) "0" else String.format(java.util.Locale.US, "%.1f", pointVal)
                        val textX = pt.x.coerceIn(paddingLeft + 12f, width - paddingRight - 12f)
                        val textY = maxOf(pt.y - 14f, paddingTop + 20f)
                        drawText(valStr, textX, textY, valPaint)
                    }

                    // Date label below baseline
                    if (i in dateIndices && i < finalDayLabels.size) {
                        val dateX = pt.x.coerceIn(paddingLeft + 12f, width - paddingRight - 12f)
                        drawText(finalDayLabels[i], dateX, height - 6f, datePaint)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IndividualGoalsContributionSection(
    childGoals: List<Goal>,
    isPointsTracking: Boolean,
    isDark: Boolean,
    isBrighterText: Boolean,
    customColor: Color,
    creationDateStr: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Individual goals contribution",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = if (isDark || isBrighterText) Color.White else Color(0xFF1D1B20)
            )
        )

        if (childGoals.isEmpty()) {
            Text(
                text = "No goals in this group yet.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (isDark) Color.LightGray else Color.DarkGray
                ),
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                childGoals.forEach { goalItem ->
                    val goalColor = try {
                        Color(android.graphics.Color.parseColor(goalItem.colorHex))
                    } catch (e: Exception) {
                        customColor
                    }
                    val isCompleted = goalItem.maxPoints > 0 && goalItem.currentPoints >= goalItem.maxPoints

                    val textVal = if (isPointsTracking) {
                        val contribution = if (goalItem.maxPoints > 0) {
                            val fraction = goalItem.currentPoints.toDouble() / goalItem.maxPoints.toDouble()
                            if (goalItem.allowGroupPointsOverflow) fraction * goalItem.groupValue else minOf(goalItem.groupValue, fraction * goalItem.groupValue)
                        } else 0.0
                        if (contribution == 0.0) "0" else String.format(java.util.Locale.US, "%.1f", contribution)
                    } else {
                        val pct = if (goalItem.maxPoints > 0) {
                            ((goalItem.currentPoints.toDouble() / goalItem.maxPoints.toDouble()) * 100).toInt()
                        } else 0
                        "$pct%"
                    }

                    val textColor = if (isCompleted) {
                        getContrastingTextColor(goalColor)
                    } else {
                        if (isDark || isBrighterText) Color.White else Color(0xFF1D1B20)
                    }

                    Box(
                        modifier = Modifier
                            .size(width = 64.dp, height = 52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isCompleted) goalColor
                                else if (isDark) Color(0xFF22212B) else Color(0xFFF7F7F9)
                            )
                            .border(
                                border = BorderStroke(2.5.dp, goalColor),
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = textVal,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = if (textVal.length >= 4) 14.sp else 16.sp,
                                color = textColor
                            ),
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Created on",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (isDark) Color(0xFFB0B0B8) else Color(0xFF666666)
                )
            )
            Text(
                text = creationDateStr,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark || isBrighterText) Color.White else Color(0xFF1D1B20)
                )
            )
        }
    }
}

private fun getContrastingTextColor(backgroundColor: Color): Color {
    val luminance = 0.299 * backgroundColor.red + 0.587 * backgroundColor.green + 0.114 * backgroundColor.blue
    return if (luminance > 0.55) Color(0xFF1D1B20) else Color.White
}

// --- HSV Color Customizer Utilities ---
@Composable
fun HsvColorSliders(
    currentHex: String,
    onColorChanged: (String) -> Unit,
    isDark: Boolean
) {
    // Parse currentHex to initial HSV
    val initialHsv = remember(currentHex) {
        val hsv = FloatArray(3)
        try {
            if (currentHex.startsWith("#")) {
                android.graphics.Color.colorToHSV(android.graphics.Color.parseColor(currentHex), hsv)
            } else {
                android.graphics.Color.colorToHSV(0xFF6750A4.toInt(), hsv)
            }
        } catch (e: Exception) {
            android.graphics.Color.colorToHSV(0xFF6750A4.toInt(), hsv)
        }
        hsv
    }

    var h by remember(initialHsv) { mutableStateOf(initialHsv[0]) }
    var s by remember(initialHsv) { mutableStateOf(initialHsv[1]) }
    var v by remember(initialHsv) { mutableStateOf(initialHsv[2]) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(
                color = if (isDark) Color(0xFF2C2A30) else Color(0xFFF3EDF7),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Fine-tune Color (HSV): $currentHex",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = if (isDark) Color.White else Color(0xFF1D1B20)
            )
            // Color Preview Box
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(try { Color(android.graphics.Color.parseColor(currentHex)) } catch (e: Exception) { Color.Gray })
                    .border(1.dp, if (isDark) Color.White else Color.Black, CircleShape)
            )
        }

        // Hue Slider
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "H",
                style = MaterialTheme.typography.labelMedium,
                color = if (isDark) Color.LightGray else Color.DarkGray,
                modifier = Modifier.width(24.dp)
            )
            Slider(
                value = h,
                onValueChange = {
                    h = it
                    val newHex = String.format("#%06X", 0xFFFFFF and android.graphics.Color.HSVToColor(floatArrayOf(h, s, v)))
                    onColorChanged(newHex)
                },
                valueRange = 0f..360f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(h, 1f, 1f))),
                    activeTrackColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
                )
            )
            Text(
                text = "${h.toInt()}°",
                style = MaterialTheme.typography.labelMedium,
                color = if (isDark) Color.LightGray else Color.DarkGray,
                modifier = Modifier.width(36.dp),
                textAlign = TextAlign.End
            )
        }

        // Saturation Slider
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "S",
                style = MaterialTheme.typography.labelMedium,
                color = if (isDark) Color.LightGray else Color.DarkGray,
                modifier = Modifier.width(24.dp)
            )
            Slider(
                value = s,
                onValueChange = {
                    s = it
                    val newHex = String.format("#%06X", 0xFFFFFF and android.graphics.Color.HSVToColor(floatArrayOf(h, s, v)))
                    onColorChanged(newHex)
                },
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(h, s, 1f))),
                    activeTrackColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
                )
            )
            Text(
                text = "${(s * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = if (isDark) Color.LightGray else Color.DarkGray,
                modifier = Modifier.width(36.dp),
                textAlign = TextAlign.End
            )
        }

        // Value Slider
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "V",
                style = MaterialTheme.typography.labelMedium,
                color = if (isDark) Color.LightGray else Color.DarkGray,
                modifier = Modifier.width(24.dp)
            )
            Slider(
                value = v,
                onValueChange = {
                    v = it
                    val newHex = String.format("#%06X", 0xFFFFFF and android.graphics.Color.HSVToColor(floatArrayOf(h, s, v)))
                    onColorChanged(newHex)
                },
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(0f, 0f, v))),
                    activeTrackColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
                )
            )
            Text(
                text = "${(v * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = if (isDark) Color.LightGray else Color.DarkGray,
                modifier = Modifier.width(36.dp),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
fun CustomColorWheelIcon(
    isCustomActive: Boolean,
    customColorHex: String,
    onClick: () -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val customColorBrush = remember {
        androidx.compose.ui.graphics.Brush.sweepGradient(
            listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
        )
    }
    val parsedColor = remember(customColorHex) {
        try {
            Color(android.graphics.Color.parseColor(customColorHex))
        } catch (e: Exception) {
            Color(0xFF6750A4)
        }
    }
    val isLightColor = remember(customColorHex) {
        try {
            val hsv = FloatArray(3)
            android.graphics.Color.colorToHSV(android.graphics.Color.parseColor(customColorHex), hsv)
            hsv[2] >= 0.6f && hsv[1] <= 0.6f
        } catch (e: Exception) {
            false
        }
    }

    Box(
        modifier = modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(customColorBrush)
            .border(
                width = if (isCustomActive) 2.5.dp else 1.dp,
                color = if (isCustomActive) (if (isDark) Color.White else Color(0xFF1D1B20)) else (if (isDark) Color(0xFF49454F) else Color(0xFFCAC4D0)),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isCustomActive) {
            // The chosen custom color appears in the middle of the custom color icon
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(parsedColor)
                    .border(
                        width = 1.dp,
                        color = if (isLightColor) Color.Black.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.6f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected Custom Color",
                    tint = if (isLightColor) Color.Black else Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        } else {
            Icon(
                imageVector = Icons.Default.Palette,
                contentDescription = "Custom Color Selector",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun HsvPickerDialog(
    initialColorHex: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    isDark: Boolean
) {
    var tempColorHex by remember(initialColorHex) {
        mutableStateOf(
            if (initialColorHex.startsWith("#")) initialColorHex else "#6750A4"
        )
    }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF211F24) else Color(0xFFFEF7FF)
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with Title and 'X' close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Customize HSV Color",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isDark) Color.White else Color(0xFF1D1B20),
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = if (isDark) Color.LightGray else Color.DarkGray
                        )
                    }
                }

                // Color Preview Box and Slider Area
                HsvColorSliders(
                    currentHex = tempColorHex,
                    onColorChanged = { tempColorHex = it },
                    isDark = isDark
                )

                // Action Buttons (Confirm & Cancel)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Cancel",
                            color = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(tempColorHex) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4),
                            contentColor = if (isDark) Color(0xFF381E72) else Color.White
                        )
                    ) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}


