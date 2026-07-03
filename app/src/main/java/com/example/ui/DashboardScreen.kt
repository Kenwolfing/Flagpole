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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.snap
import com.example.R
import com.example.data.*
import com.example.viewmodel.GoalViewModel

// Milestone models for handling local UI parsing
data class Milestone(
    val value: Int,
    val type: String // "dot", "star", "line"
)

fun parseMilestones(str: String): List<Milestone> {
    if (str.isBlank()) return emptyList()
    return str.split(";").mapNotNull { part ->
        val subparts = part.split(":")
        if (subparts.size == 2) {
            val value = subparts[0].toIntOrNull()
            if (value != null) {
                Milestone(value, subparts[1])
            } else null
        } else null
    }.sortedBy { it.value }
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
        // Blend baseColor subtly on top of dark surface
        val tintAlpha = 0.12f
        Color(
            red = (1f - tintAlpha) * 0.12f + tintAlpha * baseColor.red,
            green = (1f - tintAlpha) * 0.11f + tintAlpha * baseColor.green,
            blue = (1f - tintAlpha) * 0.15f + tintAlpha * baseColor.blue,
            alpha = 1f
        )
    } else {
        // Soft bright pastel version of baseColor
        val tintAlpha = 0.08f
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

    val isDark by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val isAnimationEnabled by viewModel.isAnimationEnabled.collectAsStateWithLifecycle()
    val landscapeColumns by viewModel.landscapeColumns.collectAsStateWithLifecycle()
    val animationEasingType by viewModel.animationEasingType.collectAsStateWithLifecycle()

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var currentTab by remember { mutableStateOf(0) } // 0 = Goals, 1 = Settings/Options

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editTargetGoal by remember { mutableStateOf<Goal?>(null) }
    var showLandscapeMenu by remember { mutableStateOf(false) }
    var showAnimationEasingMenu by remember { mutableStateOf(false) }
    var showAdjustDialog by remember { mutableStateOf(false) }
    var adjustTargetGoal by remember { mutableStateOf<Goal?>(null) }
    var isAdjustmentAddition by remember { mutableStateOf(true) }

    // Notes dialog triggers
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var showEditNoteDialog by remember { mutableStateOf(false) }
    var editTargetNote by remember { mutableStateOf<Note?>(null) }

    // Groups dialog triggers
    var showAddGroupDialog by remember { mutableStateOf(false) }
    var showEditGroupDialog by remember { mutableStateOf(false) }
    var editTargetGroup by remember { mutableStateOf<GoalGroup?>(null) }

    var showFabOptions by remember { mutableStateOf(false) }
    var isSortingModeEnabled by remember { mutableStateOf(false) }

    val currentGroupId = groupNavigationStack.lastOrNull() ?: 0
    val activeGroup = groups.find { it.id == currentGroupId }

    val dashboardItems = remember(goals, notes, groups, currentGroupId) {
        val list = mutableListOf<DashboardItem>()
        list.addAll(goals.filter { it.parentGroupId == currentGroupId }.map { DashboardItem.GoalItem(it) })
        list.addAll(notes.filter { it.parentGroupId == currentGroupId }.map { DashboardItem.NoteItem(it) })
        list.addAll(groups.filter { it.parentGroupId == currentGroupId }.map { DashboardItem.GroupItem(it) })
        list.sortByDescending { it.createdAt }
        list
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
        floatingActionButton = {
            if (currentTab == 0) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (showFabOptions) {
                        ExtendedFloatingActionButton(
                            onClick = {
                                showAddNoteDialog = true
                                showFabOptions = false
                            },
                            icon = { Icon(Icons.Default.Edit, contentDescription = "New Note") },
                            text = { Text("New Note") },
                            containerColor = if (isDark) Color(0xFFE8DEF8) else Color(0xFFECE6F0),
                            contentColor = if (isDark) Color(0xFF21005D) else Color(0xFF1D1B20),
                            modifier = Modifier.testTag("add_note_fab")
                        )
                        ExtendedFloatingActionButton(
                            onClick = {
                                showAddGroupDialog = true
                                showFabOptions = false
                            },
                            icon = { Icon(Icons.Default.Folder, contentDescription = "New Group") },
                            text = { Text("New Group") },
                            containerColor = if (isDark) Color(0xFFE8DEF8) else Color(0xFFECE6F0),
                            contentColor = if (isDark) Color(0xFF21005D) else Color(0xFF1D1B20),
                            modifier = Modifier.testTag("add_group_fab")
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4),
                        contentColor = if (isDark) Color(0xFF381E72) else Color.White,
                        tonalElevation = 6.dp,
                        modifier = Modifier
                            .testTag("add_goal_fab")
                            .clip(CircleShape)
                            .combinedClickable(
                                onLongClick = {
                                    showFabOptions = !showFabOptions
                                },
                                onClick = {
                                    if (showFabOptions) {
                                        showFabOptions = false
                                    } else {
                                        showAddDialog = true
                                    }
                                }
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Goal")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("New Goal", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (currentTab == 0 && activeGroup != null && activeGroup.imageUri != null && activeGroup.applyBg) {
                AsyncImage(
                    model = activeGroup.imageUri,
                    contentDescription = "Group Background Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.1f
                )
            }

            Column(
                modifier = Modifier.fillMaxSize()
            ) {
            if (currentTab == 0) {
                // High Density Header with Integrated Hero Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Progress",
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

                    // Decorative miniature preview and toggle sorting button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
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

                        Card(
                            modifier = Modifier.size(60.dp),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_hero_banner_1783079996500),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (currentGroupId != 0 && activeGroup != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
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
                            group = activeGroup,
                            isDark = isDark,
                            allGoals = goals,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Accomplishments summary
                if (currentGroupId == 0 && goals.isNotEmpty()) {
                    val totalGoals = goals.size
                    val completedGoals = goals.count { it.currentPoints >= it.maxPoints }
                    val totalProgress = goals.sumOf { it.currentPoints }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
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
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // List of Goals, Notes, and Groups
                if (dashboardItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(16.dp),
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
                } else {
                    if (isLandscape && landscapeColumns == 2) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
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
                                    when (item) {
                                        is DashboardItem.GroupItem -> androidx.compose.foundation.lazy.grid.GridItemSpan(2)
                                        else -> androidx.compose.foundation.lazy.grid.GridItemSpan(1)
                                    }
                                }
                            ) { item ->
                                val outlineColor = if (activeGroup?.outlineMainColor == true) activeGroup.colorHex else null
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    if (isSortingModeEnabled) {
                                        var dragAccumulator by remember(item) { mutableStateOf(0f) }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp, bottom = 8.dp)
                                                .pointerInput(item) {
                                                    detectDragGestures(
                                                        onDragStart = { dragAccumulator = 0f },
                                                        onDrag = { change, dragAmount ->
                                                            change.consume()
                                                            dragAccumulator += dragAmount.y
                                                            if (dragAccumulator > 80f) {
                                                                when (item) {
                                                                    is DashboardItem.GoalItem -> viewModel.moveItemDown(item.goal.id, "goal", currentGroupId)
                                                                    is DashboardItem.NoteItem -> viewModel.moveItemDown(item.note.id, "note", currentGroupId)
                                                                    is DashboardItem.GroupItem -> viewModel.moveItemDown(item.group.id, "group", currentGroupId)
                                                                }
                                                                dragAccumulator = 0f
                                                            } else if (dragAccumulator < -80f) {
                                                                when (item) {
                                                                    is DashboardItem.GoalItem -> viewModel.moveItemUp(item.goal.id, "goal", currentGroupId)
                                                                    is DashboardItem.NoteItem -> viewModel.moveItemUp(item.note.id, "note", currentGroupId)
                                                                    is DashboardItem.GroupItem -> viewModel.moveItemUp(item.group.id, "group", currentGroupId)
                                                                }
                                                                dragAccumulator = 0f
                                                            }
                                                        }
                                                    )
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(0.9f)
                                                    .height(6.dp)
                                                    .background(
                                                        color = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4),
                                                        shape = RoundedCornerShape(3.dp)
                                                    )
                                            )
                                        }
                                    }
                                    when (item) {
                                        is DashboardItem.GoalItem -> {
                                            GoalCard(
                                                goal = item.goal,
                                                isDark = isDark,
                                                isAnimationEnabled = isAnimationEnabled,
                                                animationEasingType = animationEasingType,
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
                                                onReset = { viewModel.resetGoal(item.goal) },
                                                onComplete = { viewModel.completeGoal(item.goal) },
                                                onDelete = { viewModel.deleteGoal(item.goal) },
                                                outlineColorHex = outlineColor
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
                                                onDelete = { viewModel.deleteNote(item.note) },
                                                outlineColorHex = outlineColor
                                            )
                                        }
                                        is DashboardItem.GroupItem -> {
                                            GroupCard(
                                                group = item.group,
                                                isDark = isDark,
                                                allGoals = goals,
                                                onEnter = { viewModel.enterGroup(item.group.id) },
                                                onEdit = {
                                                    editTargetGroup = item.group
                                                    showEditGroupDialog = true
                                                },
                                                onDuplicate = { viewModel.duplicateGroup(item.group) },
                                                onResetGoals = { viewModel.resetGroupGoals(item.group.id) },
                                                onCompleteGoals = { viewModel.completeGroupGoals(item.group.id) },
                                                onDelete = { viewModel.deleteGroup(item.group) },
                                                outlineColorHex = outlineColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
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
                                val outlineColor = if (activeGroup?.outlineMainColor == true) activeGroup.colorHex else null
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    if (isSortingModeEnabled) {
                                        var dragAccumulator by remember(item) { mutableStateOf(0f) }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp, bottom = 8.dp)
                                                .pointerInput(item) {
                                                    detectDragGestures(
                                                        onDragStart = { dragAccumulator = 0f },
                                                        onDrag = { change, dragAmount ->
                                                            change.consume()
                                                            dragAccumulator += dragAmount.y
                                                            if (dragAccumulator > 80f) {
                                                                when (item) {
                                                                    is DashboardItem.GoalItem -> viewModel.moveItemDown(item.goal.id, "goal", currentGroupId)
                                                                    is DashboardItem.NoteItem -> viewModel.moveItemDown(item.note.id, "note", currentGroupId)
                                                                    is DashboardItem.GroupItem -> viewModel.moveItemDown(item.group.id, "group", currentGroupId)
                                                                }
                                                                dragAccumulator = 0f
                                                            } else if (dragAccumulator < -80f) {
                                                                when (item) {
                                                                    is DashboardItem.GoalItem -> viewModel.moveItemUp(item.goal.id, "goal", currentGroupId)
                                                                    is DashboardItem.NoteItem -> viewModel.moveItemUp(item.note.id, "note", currentGroupId)
                                                                    is DashboardItem.GroupItem -> viewModel.moveItemUp(item.group.id, "group", currentGroupId)
                                                                }
                                                                dragAccumulator = 0f
                                                            }
                                                        }
                                                    )
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(0.9f)
                                                    .height(6.dp)
                                                    .background(
                                                        color = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4),
                                                        shape = RoundedCornerShape(3.dp)
                                                    )
                                            )
                                        }
                                    }
                                    when (item) {
                                        is DashboardItem.GoalItem -> {
                                            GoalCard(
                                                goal = item.goal,
                                                isDark = isDark,
                                                isAnimationEnabled = isAnimationEnabled,
                                                animationEasingType = animationEasingType,
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
                                                onReset = { viewModel.resetGoal(item.goal) },
                                                onComplete = { viewModel.completeGoal(item.goal) },
                                                onDelete = { viewModel.deleteGoal(item.goal) },
                                                outlineColorHex = outlineColor
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
                                                onDelete = { viewModel.deleteNote(item.note) },
                                                outlineColorHex = outlineColor
                                            )
                                        }
                                        is DashboardItem.GroupItem -> {
                                            GroupCard(
                                                group = item.group,
                                                isDark = isDark,
                                                allGoals = goals,
                                                onEnter = { viewModel.enterGroup(item.group.id) },
                                                onEdit = {
                                                    editTargetGroup = item.group
                                                    showEditGroupDialog = true
                                                },
                                                onDuplicate = { viewModel.duplicateGroup(item.group) },
                                                onResetGoals = { viewModel.resetGroupGoals(item.group.id) },
                                                onCompleteGoals = { viewModel.completeGroupGoals(item.group.id) },
                                                onDelete = { viewModel.deleteGroup(item.group) },
                                                outlineColorHex = outlineColor
                                            )
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
                }
            }
        }
        }

        // Add New Goal Dialog
        if (showAddDialog) {
            AddGoalDialog(
                isDark = isDark,
                onDismiss = { showAddDialog = false },
                onConfirm = { title, description, current, max, allowExceed, colorHex, secondaryColorHex, milestonesStr, barBehavior, hidePassed, showRemaining ->
                    viewModel.addGoal(
                        title, description, current, max, allowExceed, colorHex, secondaryColorHex, milestonesStr, barBehavior, hidePassed, showRemaining, currentGroupId
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
                onConfirm = { text, textSize, colorHex, secondaryColorHex, imageUri ->
                    viewModel.addNote(text, textSize, colorHex, secondaryColorHex, imageUri, currentGroupId)
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
                onConfirm = { text, textSize, colorHex, secondaryColorHex, imageUri ->
                    viewModel.updateNote(
                        editTargetNote!!.copy(
                            text = text,
                            textSize = textSize,
                            colorHex = colorHex,
                            secondaryColorHex = secondaryColorHex,
                            imageUri = imageUri
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
                onConfirm = { title, description, colorHex, secondaryColorHex, imageUri, applyBg, outlineMainColor ->
                    viewModel.addGroup(
                        title, description, colorHex, secondaryColorHex, imageUri, applyBg, outlineMainColor, currentGroupId
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
                onConfirm = { title, description, colorHex, secondaryColorHex, imageUri, applyBg, outlineMainColor ->
                    viewModel.updateGroup(
                        editTargetGroup!!.copy(
                            title = title,
                            description = description,
                            colorHex = colorHex,
                            secondaryColorHex = secondaryColorHex,
                            imageUri = imageUri,
                            applyBg = applyBg,
                            outlineMainColor = outlineMainColor
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
                onDismiss = {
                    showEditDialog = false
                    editTargetGoal = null
                },
                onConfirm = { title, description, current, max, allowExceed, colorHex, secondaryColorHex, milestonesStr, barBehavior, hidePassed, showRemaining ->
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
                            showRemainingCount = showRemaining
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
    }
}

@Composable
fun GoalCard(
    goal: Goal,
    isDark: Boolean,
    isAnimationEnabled: Boolean = true,
    animationEasingType: Int = 2,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onHoldIncrement: () -> Unit,
    onHoldDecrement: () -> Unit,
    onEdit: () -> Unit = {},
    onDuplicate: () -> Unit = {},
    onReset: () -> Unit = {},
    onComplete: () -> Unit = {},
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    outlineColorHex: String? = null
) {
    var showCardMenu by remember { mutableStateOf(false) }

    var lastPoints by remember(goal.id) { mutableStateOf(goal.currentPoints) }
    var animateThisChange by remember(goal.id) { mutableStateOf(false) }

    if (goal.currentPoints != lastPoints) {
        val delta = kotlin.math.abs(goal.currentPoints - lastPoints)
        animateThisChange = isAnimationEnabled && (delta != 1)
        lastPoints = goal.currentPoints
    }

    val easing = when (animationEasingType) {
        0 -> androidx.compose.animation.core.CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
        1 -> androidx.compose.animation.core.CubicBezierEasing(0.8f, 0.0f, 1.0f, 1.0f)
        else -> androidx.compose.animation.core.LinearEasing
    }

    val animatedPoints by animateFloatAsState(
        targetValue = goal.currentPoints.toFloat(),
        animationSpec = if (animateThisChange) {
            tween(durationMillis = 5000, easing = easing)
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

    val progressRatio = if (goal.maxPoints > 0) {
        minOf(1f, (if (isAnimationEnabled) animatedPoints else goal.currentPoints.toFloat()) / goal.maxPoints.toFloat())
    } else 0f
    val percentage = (progressRatio * 100).toInt()
    val isCompleted = displayPoints >= goal.maxPoints

    val customColor = remember(goal.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(goal.colorHex))
        } catch (e: Exception) {
            Color(0xFF6750A4)
        }
    }

    val milestones = remember(goal.milestonesStr) { parseMilestones(goal.milestonesStr) }

    // Math calculation for different behaviors
    val rangeStart: Float
    val rangeEnd: Float
    val progressFraction: Float

    when (goal.barBehavior) {
        1 -> { // ALL_MILESTONES (show everything)
            rangeStart = 0f
            val maxMilestone = milestones.maxOfOrNull { it.value } ?: 0
            rangeEnd = maxOf(goal.maxPoints, maxMilestone).toFloat()
            progressFraction = if (rangeEnd > 0) goal.currentPoints.toFloat() / rangeEnd else 0f
        }
        2 -> { // UP_TO_NEXT
            rangeStart = 0f
            val nextMilestone = milestones.firstOrNull { it.value > goal.currentPoints }?.value ?: goal.maxPoints
            rangeEnd = nextMilestone.toFloat()
            progressFraction = if (rangeEnd > 0) goal.currentPoints.toFloat() / rangeEnd else 0f
        }
        3 -> { // BETWEEN_LAST_AND_NEXT
            val lastMilestone = milestones.lastOrNull { it.value <= goal.currentPoints }?.value ?: 0
            val nextMilestone = milestones.firstOrNull { it.value > goal.currentPoints }?.value ?: goal.maxPoints
            rangeStart = lastMilestone.toFloat()
            rangeEnd = nextMilestone.toFloat()
            val denom = rangeEnd - rangeStart
            progressFraction = if (denom > 0) (goal.currentPoints - lastMilestone).toFloat() / denom else 0f
        }
        else -> { // BEFORE_GOAL (default)
            rangeStart = 0f
            rangeEnd = goal.maxPoints.toFloat()
            progressFraction = if (rangeEnd > 0) goal.currentPoints.toFloat() / rangeEnd else 0f
        }
    }

    val animatedProgressFraction by animateFloatAsState(
        targetValue = progressFraction.coerceIn(0f, 1f),
        animationSpec = if (animateThisChange) {
            tween(durationMillis = 5000, easing = easing)
        } else {
            snap()
        },
        label = "GoalProgressAnimation"
    )

    val cardBgColor = getCardBackgroundColor(isDark, goal.secondaryColorHex)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("goal_card_${goal.id}"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBgColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp),
        border = if (outlineColorHex != null) {
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
                    // Status Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isDark) Color(0xFF381E72) else Color(0xFFE8DEF8),
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isCompleted) "Completed" else "Active",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFEADDFF) else Color(0xFF1D192B),
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
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

            // Interactive Controls Row
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
                                text = "/ ${goal.maxPoints} pts",
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
                                val nextMilestoneVal = milestones.firstOrNull { it.value > displayPoints }?.value ?: goal.maxPoints
                                Text(
                                    text = "Next: $nextMilestoneVal pts",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = customColor,
                                        fontSize = 12.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                            }
                            Text(
                                text = if (isCompleted) "100%" else "$percentage%",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (goal.barBehavior == 2 || goal.barBehavior == 3) customColor.copy(alpha = 0.7f) else customColor,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Progress Bar container overlaying milestones
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BoxWithConstraints(
                            modifier = Modifier
                                .weight(1f)
                                .height(24.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            val barWidth = maxWidth

                            // Progress Bar
                            LinearProgressIndicator(
                                progress = { animatedProgressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                color = customColor,
                                trackColor = if (isDark) Color(0xFF333038) else Color(0xFFE6E0E9)
                            )

                            // Render Milestones checkpoints
                            milestones.forEach { milestone ->
                                val isPassed = milestone.value <= displayPoints
                                val isVisible = when (goal.barBehavior) {
                                    0 -> milestone.value <= goal.maxPoints
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

                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .offset(x = offset - 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        when (milestone.type) {
                                            "star" -> {
                                                Icon(
                                                    imageVector = Icons.Filled.Star,
                                                    contentDescription = null,
                                                    tint = if (isPassed) Color.White else customColor,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                            }
                                            "line" -> {
                                                Box(
                                                    modifier = Modifier
                                                        .width(2.5.dp)
                                                        .height(12.dp)
                                                        .background(if (isPassed) Color.White else customColor.copy(alpha = 0.85f))
                                                )
                                            }
                                            else -> { // "dot"
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isPassed) Color.White else customColor)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Render the main Goal target (maxPoints) as a bigger dot!
                            val goalVisible = when (goal.barBehavior) {
                                0 -> true
                                1 -> true
                                2 -> goal.maxPoints <= rangeEnd
                                3 -> goal.maxPoints >= rangeStart && goal.maxPoints <= rangeEnd
                                else -> false
                            }

                            if (goalVisible) {
                                val goalFraction = when (goal.barBehavior) {
                                    3 -> {
                                        val denom = rangeEnd - rangeStart
                                        if (denom > 0) (goal.maxPoints - rangeStart) / denom else 0f
                                    }
                                    else -> {
                                        if (rangeEnd > 0) goal.maxPoints.toFloat() / rangeEnd else 1f
                                    }
                                }.coerceIn(0f, 1f)

                                val goalOffset = barWidth * goalFraction

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .offset(x = goalOffset - 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(customColor)
                                            .border(2.dp, Color.White, CircleShape)
                                    )
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
    Surface(
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier
            .size(48.dp)
            .testTag(testTag)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
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
    outlineColorHex: String? = null,
    modifier: Modifier = Modifier
) {
    var showCardMenu by remember { mutableStateOf(false) }
    val cardBgColor = getCardBackgroundColor(isDark, note.secondaryColorHex)
    val mainColor = try {
        Color(android.graphics.Color.parseColor(note.colorHex))
    } catch (e: Exception) {
        if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
    }

    val borderModifier = if (outlineColorHex != null) {
        val outColor = try {
            Color(android.graphics.Color.parseColor(outlineColorHex))
        } catch (e: Exception) {
            mainColor
        }
        Modifier.border(2.dp, outColor, RoundedCornerShape(16.dp))
    } else Modifier

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .then(borderModifier)
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

@Composable
fun GroupCard(
    group: GoalGroup,
    isDark: Boolean,
    allGoals: List<Goal>,
    onEnter: () -> Unit,
    onEdit: () -> Unit = {},
    onDuplicate: () -> Unit = {},
    onResetGoals: () -> Unit = {},
    onCompleteGoals: () -> Unit = {},
    onDelete: () -> Unit,
    outlineColorHex: String? = null,
    modifier: Modifier = Modifier
) {
    var showCardMenu by remember { mutableStateOf(false) }
    val cardBgColor = getCardBackgroundColor(isDark, group.secondaryColorHex)
    val mainColor = try {
        Color(android.graphics.Color.parseColor(group.colorHex))
    } catch (e: Exception) {
        if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
    }

    val borderModifier = if (outlineColorHex != null) {
        val outColor = try {
            Color(android.graphics.Color.parseColor(outlineColorHex))
        } catch (e: Exception) {
            mainColor
        }
        Modifier.border(2.dp, outColor, RoundedCornerShape(16.dp))
    } else Modifier

    val immediateGoals = allGoals.filter { it.parentGroupId == group.id }
    val totalGoals = immediateGoals.size
    val completedGoals = immediateGoals.count { it.currentPoints >= it.maxPoints }

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .then(borderModifier)
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

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
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
                            text = { Text("Delete Group") },
                            onClick = {
                                showCardMenu = false
                                onDelete()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red) }
                        )
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
                    text = "$completedGoals/$totalGoals Goals completed",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = mainColor,
                        fontSize = 18.sp
                    ),
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                val progressFraction = if (totalGoals > 0) completedGoals.toFloat() / totalGoals else 0f
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
    allGoals: List<Goal>,
    modifier: Modifier = Modifier
) {
    val cardBgColor = getCardBackgroundColor(isDark, group.secondaryColorHex)
    val mainColor = try {
        Color(android.graphics.Color.parseColor(group.colorHex))
    } catch (e: Exception) {
        if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
    }

    val immediateGoals = allGoals.filter { it.parentGroupId == group.id }
    val totalGoals = immediateGoals.size
    val completedGoals = immediateGoals.count { it.currentPoints >= it.maxPoints }

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
                    text = "$completedGoals/$totalGoals Goals completed",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = mainColor,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                val progressFraction = if (totalGoals > 0) completedGoals.toFloat() / totalGoals else 0f
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
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
        imageUri: String?
    ) -> Unit
) {
    var text by remember { mutableStateOf(noteToEdit?.text ?: "") }
    var textSize by remember { mutableStateOf(noteToEdit?.textSize ?: 14f) }
    var selectedColorHex by remember { mutableStateOf(noteToEdit?.colorHex ?: "#6750A4") }
    var selectedSecondaryColorHex by remember { mutableStateOf(noteToEdit?.secondaryColorHex ?: "DEFAULT") }
    var selectedImageUri by remember { mutableStateOf<String?>(noteToEdit?.imageUri) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF1E1C24) else Color(0xFFF3EDF7),
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = if (noteToEdit != null) "Edit Note" else "Add New Note",
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.titleLarge.copy(color = if (isDark) Color.White else Color(0xFF1D1B20))
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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

                Column {
                    Text(
                        text = "Pick Note Accent Color",
                        style = MaterialTheme.typography.titleSmall.copy(color = if (isDark) Color.White else Color(0xFF1D1B20)),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    ) {
                        val colorsList = listOf("#6750A4", "#0288D1", "#2E7D32", "#EF6C00", "#C2185B", "#00796B")
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
                    }
                }

                Column {
                    Text(
                        text = "Pick Card Background (Secondary Color)",
                        style = MaterialTheme.typography.titleSmall.copy(color = if (isDark) Color.White else Color(0xFF1D1B20)),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
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

                        val colorsList = listOf("#6750A4", "#0288D1", "#2E7D32", "#EF6C00", "#C2185B", "#00796B")
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
                    }
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
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        text,
                        textSize,
                        selectedColorHex,
                        selectedSecondaryColorHex,
                        selectedImageUri
                    )
                },
                enabled = text.isNotBlank()
            ) {
                Text("Confirm", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
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
        outlineMainColor: Boolean
    ) -> Unit
) {
    var title by remember { mutableStateOf(groupToEdit?.title ?: "") }
    var description by remember { mutableStateOf(groupToEdit?.description ?: "") }
    var selectedColorHex by remember { mutableStateOf(groupToEdit?.colorHex ?: "#6750A4") }
    var selectedSecondaryColorHex by remember { mutableStateOf(groupToEdit?.secondaryColorHex ?: "DEFAULT") }
    var selectedImageUri by remember { mutableStateOf<String?>(groupToEdit?.imageUri) }
    var applyBg by remember { mutableStateOf(groupToEdit?.applyBg ?: true) }
    var outlineMainColor by remember { mutableStateOf(groupToEdit?.outlineMainColor ?: false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF1E1C24) else Color(0xFFF3EDF7),
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = if (groupToEdit != null) "Edit Group" else "Add New Group",
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.titleLarge.copy(color = if (isDark) Color.White else Color(0xFF1D1B20))
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text(
                        text = "Pick Group Accent Color",
                        style = MaterialTheme.typography.titleSmall.copy(color = if (isDark) Color.White else Color(0xFF1D1B20)),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    ) {
                        val colorsList = listOf("#6750A4", "#0288D1", "#2E7D32", "#EF6C00", "#C2185B", "#00796B")
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
                    }
                }

                Column {
                    Text(
                        text = "Pick Card Background (Secondary Color)",
                        style = MaterialTheme.typography.titleSmall.copy(color = if (isDark) Color.White else Color(0xFF1D1B20)),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
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

                        val colorsList = listOf("#6750A4", "#0288D1", "#2E7D32", "#EF6C00", "#C2185B", "#00796B")
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
                    }
                }

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
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        title,
                        description,
                        selectedColorHex,
                        selectedSecondaryColorHex,
                        selectedImageUri,
                        applyBg,
                        outlineMainColor
                    )
                },
                enabled = title.isNotBlank()
            ) {
                Text("Confirm", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGoalDialog(
    isDark: Boolean,
    goalToEdit: Goal? = null,
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
        showRemainingCount: Boolean
    ) -> Unit
) {
    var title by remember { mutableStateOf(goalToEdit?.title ?: "") }
    var description by remember { mutableStateOf(goalToEdit?.description ?: "") }
    var currentPointsStr by remember { mutableStateOf(goalToEdit?.currentPoints?.toString() ?: "") }
    var maxPointsStr by remember { mutableStateOf(goalToEdit?.maxPoints?.toString() ?: "") }
    var allowExceed by remember { mutableStateOf(goalToEdit?.allowExceed ?: false) }
    var selectedColorHex by remember { mutableStateOf(goalToEdit?.colorHex ?: "#6750A4") }
    var selectedSecondaryColorHex by remember { mutableStateOf(goalToEdit?.secondaryColorHex ?: "DEFAULT") }

    // Milestones Tab state
    var milestonesList by remember { mutableStateOf(goalToEdit?.let { parseMilestones(it.milestonesStr) } ?: listOf<Milestone>()) }
    var barBehavior by remember { mutableStateOf(goalToEdit?.barBehavior ?: 0) }
    var hidePassedMilestones by remember { mutableStateOf(goalToEdit?.hidePassedMilestones ?: false) }
    var showRemainingCount by remember { mutableStateOf(goalToEdit?.showRemainingCount ?: true) }

    var activeTab by remember { mutableStateOf(0) } // 0 = General, 1 = Milestones
    var showAddMilestoneDialog by remember { mutableStateOf(false) }

    var titleError by remember { mutableStateOf(false) }
    var maxPointsError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
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
                        text = { Text("Milestones", style = MaterialTheme.typography.labelMedium) }
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

                    // Main color picker
                    Column {
                        Text(
                            text = "Pick Goal Accent Color",
                            style = MaterialTheme.typography.titleSmall.copy(color = if (isDark) Color.White else Color(0xFF1D1B20)),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        ) {
                            val colorsList = listOf("#6750A4", "#0288D1", "#2E7D32", "#EF6C00", "#C2185B", "#00796B")
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
                        }
                    }

                    // Secondary color picker
                    Column {
                        Text(
                            text = "Pick Card Background (Secondary Color)",
                            style = MaterialTheme.typography.titleSmall.copy(color = if (isDark) Color.White else Color(0xFF1D1B20)),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
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

                            val colorsList = listOf("#6750A4", "#0288D1", "#2E7D32", "#EF6C00", "#C2185B", "#00796B")
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
                        }
                    }
                } else {
                    // Milestones tab
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = { showAddMilestoneDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4),
                                contentColor = if (isDark) Color(0xFF381E72) else Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Milestone")
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
                                                val icon = when (milestone.type) {
                                                    "star" -> Icons.Default.Star
                                                    "line" -> Icons.Default.Menu
                                                    else -> Icons.Default.Circle
                                                }
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = null,
                                                    tint = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4),
                                                    modifier = Modifier.size(16.dp)
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
                            showRemainingCount
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
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
                ),
                modifier = Modifier.testTag("add_goal_cancel_button")
            ) {
                Text("Cancel", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium))
            }
        }
    )

    // Milestone Add Dialog
    if (showAddMilestoneDialog) {
        var valueStr by remember { mutableStateOf("") }
        var selectedType by remember { mutableStateOf("dot") }
        var valError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddMilestoneDialog = false },
            containerColor = if (isDark) Color(0xFF2C2A30) else Color(0xFFFFFFFF),
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    "Add Milestone Checkpoint",
                    style = MaterialTheme.typography.titleMedium.copy(color = if (isDark) Color.White else Color(0xFF1D1B20))
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = valueStr,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.all { it.isDigit() }) {
                                valueStr = input
                                valError = (input.toIntOrNull() ?: 0) <= 0
                            }
                        },
                        label = { Text("Milestone Point Value") },
                        placeholder = { Text("e.g. 5") },
                        isError = valError,
                        supportingText = {
                            if (valError) Text("Must be > 0")
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
                            "Select Icon Style",
                            style = MaterialTheme.typography.titleSmall.copy(color = if (isDark) Color.White else Color(0xFF1D1B20)),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val types = listOf("dot", "star", "line")
                            types.forEach { type ->
                                val isSel = selectedType == type
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
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
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        val icon = when (type) {
                                            "star" -> Icons.Default.Star
                                            "line" -> Icons.Default.Menu
                                            else -> Icons.Default.Circle
                                        }
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            type.uppercase(),
                                            style = MaterialTheme.typography.labelSmall.copy(
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
                        val value = valueStr.toIntOrNull() ?: 0
                        if (value > 0) {
                            milestonesList = (milestonesList + Milestone(value, selectedType)).sortedBy { it.value }
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
                    onClick = { showAddMilestoneDialog = false },
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

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF1E1C24) else Color(0xFFF3EDF7),
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = "Quick Adjust",
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.titleLarge.copy(color = if (isDark) Color.White else Color(0xFF1D1B20))
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Adjusting goal: ${goal.title}",
                    style = MaterialTheme.typography.bodyMedium.copy(color = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F)),
                    fontWeight = FontWeight.Normal
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.all { it.isDigit() }) {
                            amountStr = input
                            val num = input.toIntOrNull() ?: 0
                            amountError = num <= 0
                        }
                    },
                    label = { Text("Value to add/subtract") },
                    placeholder = { Text("5", color = Color(0xFF9E9E9E)) },
                    isError = amountError,
                    supportingText = {
                        if (amountError) {
                            Text("Please enter an amount greater than 0")
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
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = adjustMaxPoints,
                        onCheckedChange = { adjustMaxPoints = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4),
                            uncheckedColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4),
                            checkmarkColor = if (isDark) Color(0xFF381E72) else Color.White
                        ),
                        modifier = Modifier.testTag("adjust_target_max_checkbox")
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Adjust Max Points instead",
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
                        onConfirm(amount, adjustMaxPoints)
                    } else {
                        amountError = true
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4),
                    contentColor = if (isDark) Color(0xFF381E72) else Color.White
                ),
                shape = CircleShape,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .testTag("adjust_confirm_button")
            ) {
                Text("Confirm", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
                ),
                modifier = Modifier.testTag("adjust_cancel_button")
            ) {
                Text("Cancel", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium))
            }
        }
    )
}
