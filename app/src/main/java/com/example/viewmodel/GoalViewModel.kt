package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Goal
import com.example.data.Note
import com.example.data.GoalGroup
import com.example.data.GoalRepository
import com.example.data.GoalLog
import com.example.ui.DashboardItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.example.data.GroupGoalStats
import kotlinx.coroutines.launch
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class ResetType {
    POINTS_ONLY,
    STATS_ONLY,
    BOTH
}

sealed class CopiedItem {
    data class CopiedGoal(val goal: Goal) : CopiedItem()
    data class CopiedNote(val note: Note) : CopiedItem()
    data class CopiedGroup(val group: GoalGroup) : CopiedItem()
}

class GoalViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: GoalRepository
    private val prefs = application.getSharedPreferences("goal_tracker_prefs", android.content.Context.MODE_PRIVATE)

    private val _isDarkTheme = kotlinx.coroutines.flow.MutableStateFlow(prefs.getBoolean("is_dark_theme", false))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme

    fun setDarkTheme(enabled: Boolean) {
        _isDarkTheme.value = enabled
        prefs.edit().putBoolean("is_dark_theme", enabled).apply()
    }

    private val _landscapeColumns = kotlinx.coroutines.flow.MutableStateFlow(prefs.getInt("landscape_columns", 1))
    val landscapeColumns: StateFlow<Int> = _landscapeColumns

    fun setLandscapeColumns(columns: Int) {
        _landscapeColumns.value = columns
        prefs.edit().putInt("landscape_columns", columns).apply()
    }

    private val _isAnimationEnabled = kotlinx.coroutines.flow.MutableStateFlow(prefs.getBoolean("is_animation_enabled", true))
    val isAnimationEnabled: StateFlow<Boolean> = _isAnimationEnabled

    fun setAnimationEnabled(enabled: Boolean) {
        _isAnimationEnabled.value = enabled
        prefs.edit().putBoolean("is_animation_enabled", enabled).apply()
    }

    private val _isAestheticDashboardEnabled = kotlinx.coroutines.flow.MutableStateFlow(prefs.getBoolean("is_aesthetic_dashboard_enabled", true))
    val isAestheticDashboardEnabled: StateFlow<Boolean> = _isAestheticDashboardEnabled

    fun setAestheticDashboardEnabled(enabled: Boolean) {
        _isAestheticDashboardEnabled.value = enabled
        prefs.edit().putBoolean("is_aesthetic_dashboard_enabled", enabled).apply()
    }

    private val _animationDurationSeconds = kotlinx.coroutines.flow.MutableStateFlow(prefs.getInt("animation_duration_seconds", 3))
    val animationDurationSeconds: StateFlow<Int> = _animationDurationSeconds

    fun setAnimationDurationSeconds(seconds: Int) {
        _animationDurationSeconds.value = seconds
        prefs.edit().putInt("animation_duration_seconds", seconds).apply()
    }

    private val _animationEasingType = kotlinx.coroutines.flow.MutableStateFlow(prefs.getInt("animation_easing_type", 2)) // 2 = Constant (Linear)
    val animationEasingType: StateFlow<Int> = _animationEasingType

    fun setAnimationEasingType(type: Int) {
        _animationEasingType.value = type
        prefs.edit().putInt("animation_easing_type", type).apply()
    }

    private val _dashboardBackgroundUri = kotlinx.coroutines.flow.MutableStateFlow(prefs.getString("dashboard_background_uri", null))
    val dashboardBackgroundUri: StateFlow<String?> = _dashboardBackgroundUri

    fun setDashboardBackgroundUri(uri: String?) {
        viewModelScope.launch {
            val localUri = withContext(Dispatchers.IO) {
                saveAndResizeImage(uri)
            }
            _dashboardBackgroundUri.value = localUri
            if (localUri != null) {
                prefs.edit().putString("dashboard_background_uri", localUri).apply()
            } else {
                prefs.edit().remove("dashboard_background_uri").apply()
            }
        }
    }

    private val _expandedItemIds = kotlinx.coroutines.flow.MutableStateFlow<Set<String>>(prefs.getStringSet("expanded_items", emptySet()) ?: emptySet())
    val expandedItemIds: StateFlow<Set<String>> = _expandedItemIds

    fun toggleItemExpanded(itemId: String) {
        val current = _expandedItemIds.value.toMutableSet()
        if (current.contains(itemId)) {
            current.remove(itemId)
        } else {
            current.add(itemId)
        }
        _expandedItemIds.value = current
        prefs.edit().putStringSet("expanded_items", current).apply()
    }

    init {
        val database = AppDatabase.getDatabase(application)
        repository = GoalRepository(database.goalDao())
    }

    private val _isBrighterTextEnabled = kotlinx.coroutines.flow.MutableStateFlow(prefs.getBoolean("is_brighter_text_enabled", false))
    val isBrighterTextEnabled: StateFlow<Boolean> = _isBrighterTextEnabled

    fun setBrighterTextEnabled(enabled: Boolean) {
        _isBrighterTextEnabled.value = enabled
        prefs.edit().putBoolean("is_brighter_text_enabled", enabled).apply()
    }

    private val _isLoading = kotlinx.coroutines.flow.MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _copiedItem = kotlinx.coroutines.flow.MutableStateFlow<CopiedItem?>(null)
    val copiedItem: StateFlow<CopiedItem?> = _copiedItem

    fun copyGoal(goal: Goal) {
        _copiedItem.value = CopiedItem.CopiedGoal(goal)
    }

    fun copyNote(note: Note) {
        _copiedItem.value = CopiedItem.CopiedNote(note)
    }

    fun copyGroup(group: GoalGroup) {
        _copiedItem.value = CopiedItem.CopiedGroup(group)
    }

    fun clearCopiedItem() {
        _copiedItem.value = null
    }

    fun pasteCopiedItem(targetGroupId: Int) {
        val itemToPaste = _copiedItem.value ?: return
        viewModelScope.launch {
            val allGoalsInTarget = repository.getGoalsByParentGroupId(targetGroupId).first()
            val allNotesInTarget = repository.getNotesByParentGroupId(targetGroupId).first()
            val allGroupsInTarget = repository.getGroupsByParentGroupId(targetGroupId).first()

            val maxCreatedAt = (allGoalsInTarget.map { it.createdAt } +
                    allNotesInTarget.map { it.createdAt } +
                    allGroupsInTarget.map { it.createdAt }).maxOrNull() ?: System.currentTimeMillis()
            val newCreatedAt = maxOf(System.currentTimeMillis(), maxCreatedAt + 1000)

            when (itemToPaste) {
                is CopiedItem.CopiedGoal -> {
                    val newGoal = itemToPaste.goal.copy(
                        id = 0,
                        parentGroupId = targetGroupId,
                        createdAt = newCreatedAt
                    )
                    val newGoalId = repository.insert(newGoal).toInt()

                    val logs = repository.getLogsForGoal(itemToPaste.goal.id).first()
                    logs.forEach { log ->
                        repository.insertGoalLog(
                            log.copy(id = 0, goalId = newGoalId)
                        )
                    }
                }
                is CopiedItem.CopiedNote -> {
                    val newNote = itemToPaste.note.copy(
                        id = 0,
                        parentGroupId = targetGroupId,
                        imageUri = itemToPaste.note.imageUri,
                        createdAt = newCreatedAt
                    )
                    repository.insertNote(newNote)
                }
                is CopiedItem.CopiedGroup -> {
                    val allGoalsList = repository.allGoals.first()
                    val allNotesList = repository.allNotes.first()
                    val allGroupsList = repository.allGroups.first()

                    val newGroup = itemToPaste.group.copy(
                        id = 0,
                        parentGroupId = targetGroupId,
                        createdAt = newCreatedAt
                    )
                    val newGroupId = repository.insertGroup(newGroup).toInt()
                    val oldGroupId = itemToPaste.group.id

                    allGoalsList.filter { it.parentGroupId == oldGroupId }.forEach { childGoal ->
                        val newChildGoal = childGoal.copy(
                            id = 0,
                            parentGroupId = newGroupId,
                            createdAt = System.currentTimeMillis()
                        )
                        val newChildGoalId = repository.insert(newChildGoal).toInt()
                        val childLogs = repository.getLogsForGoal(childGoal.id).first()
                        childLogs.forEach { log ->
                            repository.insertGoalLog(log.copy(id = 0, goalId = newChildGoalId))
                        }
                    }

                    allNotesList.filter { it.parentGroupId == oldGroupId }.forEach { childNote ->
                        repository.insertNote(
                            childNote.copy(
                                id = 0,
                                parentGroupId = newGroupId,
                                imageUri = childNote.imageUri,
                                createdAt = System.currentTimeMillis()
                            )
                        )
                    }

                    allGroupsList.filter { it.parentGroupId == oldGroupId }.forEach { childSubGroup ->
                        copySubGroupRecursive(childSubGroup, newGroupId, allGoalsList, allNotesList, allGroupsList)
                    }
                }
            }

            _copiedItem.value = null
        }
    }

    private suspend fun copySubGroupRecursive(
        subGroup: GoalGroup,
        newParentId: Int,
        allGoalsList: List<Goal>,
        allNotesList: List<Note>,
        allGroupsList: List<GoalGroup>
    ) {
        val newSubGroupId = repository.insertGroup(
            subGroup.copy(id = 0, parentGroupId = newParentId, createdAt = System.currentTimeMillis())
        ).toInt()
        val oldSubGroupId = subGroup.id

        allGoalsList.filter { it.parentGroupId == oldSubGroupId }.forEach { childGoal ->
            val newGoal = childGoal.copy(
                id = 0,
                parentGroupId = newSubGroupId,
                createdAt = System.currentTimeMillis()
            )
            val newGoalId = repository.insert(newGoal).toInt()
            val logs = repository.getLogsForGoal(childGoal.id).first()
            logs.forEach { log ->
                repository.insertGoalLog(log.copy(id = 0, goalId = newGoalId))
            }
        }

        allNotesList.filter { it.parentGroupId == oldSubGroupId }.forEach { childNote ->
            repository.insertNote(
                childNote.copy(
                    id = 0,
                    parentGroupId = newSubGroupId,
                    imageUri = childNote.imageUri,
                    createdAt = System.currentTimeMillis()
                )
            )
        }

        allGroupsList.filter { it.parentGroupId == oldSubGroupId }.forEach { childSubSubGroup ->
            copySubGroupRecursive(childSubSubGroup, newSubGroupId, allGoalsList, allNotesList, allGroupsList)
        }
    }

    private val _groupNavigationStack = kotlinx.coroutines.flow.MutableStateFlow<List<Int>>(emptyList())
    val groupNavigationStack: StateFlow<List<Int>> = _groupNavigationStack

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentGroupId: StateFlow<Int> = _groupNavigationStack
        .map { it.lastOrNull() ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val goals: StateFlow<List<Goal>> = currentGroupId
        .flatMapLatest { groupId ->
            _isLoading.value = true
            repository.getGoalsByParentGroupId(groupId)
        }
        .onEach { _isLoading.value = false }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val notes: StateFlow<List<Note>> = currentGroupId
        .flatMapLatest { groupId ->
            _isLoading.value = true
            repository.getNotesByParentGroupId(groupId)
        }
        .onEach { _isLoading.value = false }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val groups: StateFlow<List<GoalGroup>> = currentGroupId
        .flatMapLatest { groupId ->
            _isLoading.value = true
            repository.getGroupsByParentGroupId(groupId)
        }
        .onEach { _isLoading.value = false }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeGroup: StateFlow<GoalGroup?> = currentGroupId
        .flatMapLatest { groupId ->
            if (groupId == 0) {
                flowOf(null)
            } else {
                repository.getGroupByIdFlow(groupId)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val groupGoalStats: StateFlow<List<GroupGoalStats>> = repository.groupGoalStats
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allGoals: StateFlow<List<Goal>> = repository.allGoals
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allGroups: StateFlow<List<GoalGroup>> = repository.allGroups
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allLogs: StateFlow<List<GoalLog>> = repository.allLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun ignorePointsChangeForDay(goalId: Int, start: Long, end: Long) {
        viewModelScope.launch {
            repository.ignorePointsChangeForDay(goalId, start, end)
        }
    }

    fun enterGroup(id: Int) {
        _groupNavigationStack.value = _groupNavigationStack.value + id
    }

    fun goBack() {
        val current = _groupNavigationStack.value
        if (current.isNotEmpty()) {
            _groupNavigationStack.value = current.dropLast(1)
        }
    }

    fun addGoal(
        title: String,
        description: String,
        currentPoints: Int,
        maxPoints: Int,
        allowExceed: Boolean,
        colorHex: String,
        secondaryColorHex: String,
        milestonesStr: String,
        barBehavior: Int,
        hidePassedMilestones: Boolean,
        showRemainingCount: Boolean,
        showMilestoneProgressBar: Boolean = true,
        progressType: Int = 0,
        meterThickness: Float = 14f,
        dotsPerRow: Int = 8,
        parentGroupId: Int = 0,
        cardBorderHex: String = "NONE",
        unit: String = "pts",
        statsEnabled: Boolean = true,
        streaksEnabled: Boolean = false,
        pointTrackingEnabled: Boolean = true,
        statsVisualType: Int = 0,
        showHighlights: Boolean = true,
        groupValue: Double = 1.0,
        allowGroupPointsOverflow: Boolean = true
    ) {
        viewModelScope.launch {
            val finalMax = maxOf(1, maxPoints)
            val finalCurrent = if (!allowExceed) minOf(maxOf(0, currentPoints), finalMax) else maxOf(0, currentPoints)
            val goal = Goal(
                title = title,
                description = description,
                currentPoints = finalCurrent,
                maxPoints = finalMax,
                allowExceed = allowExceed,
                colorHex = colorHex,
                secondaryColorHex = secondaryColorHex,
                milestonesStr = milestonesStr,
                barBehavior = barBehavior,
                hidePassedMilestones = hidePassedMilestones,
                showRemainingCount = showRemainingCount,
                showMilestoneProgressBar = showMilestoneProgressBar,
                progressType = progressType,
                meterThickness = meterThickness,
                dotsPerRow = dotsPerRow,
                parentGroupId = parentGroupId,
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
            val goalId = repository.insert(goal).toInt()
            if (finalCurrent > 0 && statsEnabled && pointTrackingEnabled) {
                repository.insertGoalLog(
                    GoalLog(
                        goalId = goalId,
                        pointsBefore = 0,
                        pointsAfter = finalCurrent,
                        changeAmount = finalCurrent,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    private suspend fun updateGoalWithLogging(oldGoal: Goal, newGoal: Goal) {
        if (oldGoal.currentPoints != newGoal.currentPoints && newGoal.statsEnabled && newGoal.pointTrackingEnabled) {
            val diff = newGoal.currentPoints - oldGoal.currentPoints
            repository.insertGoalLog(
                GoalLog(
                    goalId = newGoal.id,
                    pointsBefore = oldGoal.currentPoints,
                    pointsAfter = newGoal.currentPoints,
                    changeAmount = diff,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
        repository.update(newGoal)
    }

    fun updateGoal(goal: Goal) {
        viewModelScope.launch {
            val existing = goals.value.find { it.id == goal.id }
            if (existing != null) {
                updateGoalWithLogging(existing, goal)
            } else {
                repository.update(goal)
            }
        }
    }

    fun duplicateGoal(goal: Goal) {
        viewModelScope.launch {
            val duplicate = goal.copy(id = 0, createdAt = System.currentTimeMillis())
            repository.insert(duplicate)
        }
    }

    fun resetGoal(goal: Goal, type: ResetType = ResetType.POINTS_ONLY) {
        viewModelScope.launch {
            when (type) {
                ResetType.POINTS_ONLY -> {
                    updateGoalWithLogging(goal, goal.copy(currentPoints = 0))
                }
                ResetType.STATS_ONLY -> {
                    repository.deleteLogsForGoal(goal.id)
                    repository.update(goal.copy(createdAt = System.currentTimeMillis()))
                }
                ResetType.BOTH -> {
                    repository.deleteLogsForGoal(goal.id)
                    repository.update(goal.copy(currentPoints = 0, createdAt = System.currentTimeMillis()))
                }
            }
        }
    }

    fun completeGoal(goal: Goal) {
        viewModelScope.launch {
            updateGoalWithLogging(goal, goal.copy(currentPoints = goal.maxPoints))
        }
    }

    fun incrementPoints(goal: Goal) {
        viewModelScope.launch {
            val newPoints = goal.currentPoints + 1
            if (!goal.allowExceed && newPoints > goal.maxPoints) {
                // Cannot exceed max points
                return@launch
            }
            updateGoalWithLogging(goal, goal.copy(currentPoints = newPoints))
        }
    }

    fun decrementPoints(goal: Goal) {
        viewModelScope.launch {
            val newPoints = maxOf(0, goal.currentPoints - 1)
            updateGoalWithLogging(goal, goal.copy(currentPoints = newPoints))
        }
    }

    fun adjustGoal(goal: Goal, isAddition: Boolean, amount: Int, adjustMax: Boolean) {
        viewModelScope.launch {
            if (adjustMax) {
                val change = if (isAddition) amount else -amount
                val newMax = maxOf(1, goal.maxPoints + change)
                val newCurrent = if (!goal.allowExceed) minOf(goal.currentPoints, newMax) else goal.currentPoints
                val updatedGoal = goal.copy(maxPoints = newMax, currentPoints = newCurrent)
                updateGoalWithLogging(goal, updatedGoal)
            } else {
                val change = if (isAddition) amount else -amount
                var newCurrent = goal.currentPoints + change
                newCurrent = maxOf(0, newCurrent)
                if (!goal.allowExceed && newCurrent > goal.maxPoints) {
                    newCurrent = goal.maxPoints
                }
                updateGoalWithLogging(goal, goal.copy(currentPoints = newCurrent))
            }
        }
    }

    fun getLogsForGoal(goalId: Int): kotlinx.coroutines.flow.Flow<List<GoalLog>> {
        return repository.getLogsForGoal(goalId)
    }

    fun deleteGoal(goal: Goal) {
        viewModelScope.launch {
            repository.deleteLogsForGoal(goal.id)
            repository.deleteById(goal.id)
        }
    }

    // Notes CRUD
    fun addNote(
        text: String,
        textSize: Float,
        colorHex: String,
        secondaryColorHex: String,
        imageUri: String?,
        parentGroupId: Int = 0,
        cardBorderHex: String = "NONE"
    ) {
        viewModelScope.launch {
            val localUri = withContext(Dispatchers.IO) {
                saveAndResizeImage(imageUri)
            }
            val note = Note(
                text = text,
                textSize = textSize,
                colorHex = colorHex,
                secondaryColorHex = secondaryColorHex,
                imageUri = localUri,
                parentGroupId = parentGroupId,
                cardBorderHex = cardBorderHex
            )
            repository.insertNote(note)
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch {
            val localUri = withContext(Dispatchers.IO) {
                saveAndResizeImage(note.imageUri)
            }
            repository.updateNote(note.copy(imageUri = localUri))
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.deleteNoteById(note.id)
        }
    }

    fun duplicateNote(note: Note) {
        viewModelScope.launch {
            repository.insertNote(note.copy(id = 0, createdAt = System.currentTimeMillis()))
        }
    }

    // Groups CRUD & Complex Actions
    fun addGroup(
        title: String,
        description: String,
        colorHex: String,
        secondaryColorHex: String,
        imageUri: String?,
        applyBg: Boolean,
        outlineMainColor: Boolean,
        parentGroupId: Int = 0,
        cardBorderHex: String = "NONE",
        displayType: Int = 0
    ) {
        viewModelScope.launch {
            val localUri = withContext(Dispatchers.IO) {
                saveAndResizeImage(imageUri)
            }
            val group = GoalGroup(
                title = title,
                description = description,
                colorHex = colorHex,
                secondaryColorHex = secondaryColorHex,
                imageUri = localUri,
                applyBg = applyBg,
                outlineMainColor = outlineMainColor,
                parentGroupId = parentGroupId,
                cardBorderHex = cardBorderHex,
                displayType = displayType
            )
            repository.insertGroup(group)
        }
    }

    fun updateGroup(group: GoalGroup) {
        viewModelScope.launch {
            val localUri = withContext(Dispatchers.IO) {
                saveAndResizeImage(group.imageUri)
            }
            repository.updateGroup(group.copy(imageUri = localUri))
        }
    }

    private fun saveAndResizeImage(uriString: String?): String? {
        if (uriString.isNullOrBlank()) return null
        val context = getApplication<Application>()
        // If it's already a file inside our own files directory, don't copy it again
        if (uriString.contains(context.filesDir.absolutePath)) {
            return uriString
        }

        return try {
            val uri = Uri.parse(uriString)
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                // First pass to decode size
                BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri), null, options)
                
                // Calculate sample size to avoid loading huge bitmap into memory
                val maxDim = 1024
                var scale = 1
                if (options.outHeight > maxDim || options.outWidth > maxDim) {
                    scale = Math.pow(2.0, Math.ceil(Math.log(maxDim.toDouble() / Math.max(options.outHeight, options.outWidth).toDouble()) / Math.log(0.5))).toInt()
                }
                
                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = scale
                }
                val srcBitmap = BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri), null, decodeOptions)
                
                if (srcBitmap != null) {
                    val finalMaxDim = 1024f
                    val width = srcBitmap.width
                    val height = srcBitmap.height
                    val ratio = width.toFloat() / height.toFloat()
                    
                    val (newWidth, newHeight) = if (width > finalMaxDim || height > finalMaxDim) {
                        if (ratio > 1) {
                            Pair(finalMaxDim.toInt(), (finalMaxDim / ratio).toInt())
                        } else {
                            Pair((finalMaxDim * ratio).toInt(), finalMaxDim.toInt())
                        }
                    } else {
                        Pair(width, height)
                    }
                    
                    val scaledBitmap = if (newWidth != width || newHeight != height) {
                        Bitmap.createScaledBitmap(srcBitmap, newWidth, newHeight, true)
                    } else {
                        srcBitmap
                    }
                    
                    val imagesDir = File(context.filesDir, "app_images")
                    if (!imagesDir.exists()) {
                        imagesDir.mkdirs()
                    }
                    val fileName = "img_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().take(6)}.jpg"
                    val destFile = File(imagesDir, fileName)
                    
                    FileOutputStream(destFile).use { out ->
                        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                    }
                    
                    if (scaledBitmap != srcBitmap) {
                        scaledBitmap.recycle()
                    }
                    srcBitmap.recycle()
                    
                    Uri.fromFile(destFile).toString()
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            uriString
        }
    }

    fun deleteGroup(group: GoalGroup) {
        viewModelScope.launch {
            repository.deleteGroupById(group.id)
        }
    }

    fun duplicateGroup(group: GoalGroup) {
        viewModelScope.launch {
            val allGoalsList = repository.allGoals.first()
            val allNotesList = repository.allNotes.first()
            val allGroupsList = repository.allGroups.first()

            val newGroupId = repository.insertGroup(group.copy(id = 0, createdAt = System.currentTimeMillis()))
            val oldGroupId = group.id
            
            // Duplicate goals inside this group
            allGoalsList.filter { it.parentGroupId == oldGroupId }.forEach { childGoal ->
                repository.insert(childGoal.copy(id = 0, parentGroupId = newGroupId.toInt(), createdAt = System.currentTimeMillis()))
            }
            
            // Duplicate notes inside this group
            allNotesList.filter { it.parentGroupId == oldGroupId }.forEach { childNote ->
                repository.insertNote(childNote.copy(id = 0, parentGroupId = newGroupId.toInt(), createdAt = System.currentTimeMillis()))
            }
            
            // Duplicate sub-groups inside this group
            allGroupsList.filter { it.parentGroupId == oldGroupId }.forEach { childSubGroup ->
                duplicateSubGroupRecursive(childSubGroup, newGroupId.toInt(), allGoalsList, allNotesList, allGroupsList)
            }
        }
    }

    private suspend fun duplicateSubGroupRecursive(
        subGroup: GoalGroup, 
        newParentId: Int,
        allGoalsList: List<Goal>,
        allNotesList: List<Note>,
        allGroupsList: List<GoalGroup>
    ) {
        val newSubGroupId = repository.insertGroup(subGroup.copy(id = 0, parentGroupId = newParentId, createdAt = System.currentTimeMillis()))
        val oldSubGroupId = subGroup.id
        
        allGoalsList.filter { it.parentGroupId == oldSubGroupId }.forEach { childGoal ->
            repository.insert(childGoal.copy(id = 0, parentGroupId = newSubGroupId.toInt(), createdAt = System.currentTimeMillis()))
        }
        
        allNotesList.filter { it.parentGroupId == oldSubGroupId }.forEach { childNote ->
            repository.insertNote(childNote.copy(id = 0, parentGroupId = newSubGroupId.toInt(), createdAt = System.currentTimeMillis()))
        }
        
        allGroupsList.filter { it.parentGroupId == oldSubGroupId }.forEach { childSubSubGroup ->
            duplicateSubGroupRecursive(childSubSubGroup, newSubGroupId.toInt(), allGoalsList, allNotesList, allGroupsList)
        }
    }

    fun resetGroupGoals(groupId: Int, type: ResetType = ResetType.POINTS_ONLY) {
        viewModelScope.launch {
            val allGoalsList = repository.allGoals.first()
            val allGroupsList = repository.allGroups.first()

            val allGoalIdsToReset = mutableSetOf<Int>()
            findGoalIdsInGroupRecursive(groupId, allGoalIdsToReset, allGoalsList, allGroupsList)
            
            allGoalsList.forEach { goal ->
                if (allGoalIdsToReset.contains(goal.id)) {
                    when (type) {
                        ResetType.POINTS_ONLY -> {
                            updateGoalWithLogging(goal, goal.copy(currentPoints = 0))
                        }
                        ResetType.STATS_ONLY -> {
                            repository.deleteLogsForGoal(goal.id)
                            repository.update(goal.copy(createdAt = System.currentTimeMillis()))
                        }
                        ResetType.BOTH -> {
                            repository.deleteLogsForGoal(goal.id)
                            repository.update(goal.copy(currentPoints = 0, createdAt = System.currentTimeMillis()))
                        }
                    }
                }
            }
        }
    }

    fun completeGroupGoals(groupId: Int) {
        viewModelScope.launch {
            val allGoalsList = repository.allGoals.first()
            val allGroupsList = repository.allGroups.first()

            val allGoalIdsToComplete = mutableSetOf<Int>()
            findGoalIdsInGroupRecursive(groupId, allGoalIdsToComplete, allGoalsList, allGroupsList)
            
            allGoalsList.forEach { goal ->
                if (allGoalIdsToComplete.contains(goal.id)) {
                    updateGoalWithLogging(goal, goal.copy(currentPoints = goal.maxPoints))
                }
            }
        }
    }

    private fun findGoalIdsInGroupRecursive(
        groupId: Int, 
        resultSet: MutableSet<Int>,
        allGoalsList: List<Goal>,
        allGroupsList: List<GoalGroup>
    ) {
        allGoalsList.filter { it.parentGroupId == groupId }.forEach { resultSet.add(it.id) }
        allGroupsList.filter { it.parentGroupId == groupId }.forEach { subGroup ->
            findGoalIdsInGroupRecursive(subGroup.id, resultSet, allGoalsList, allGroupsList)
        }
    }

    fun moveItemUp(itemId: Int, itemType: String, currentGroupId: Int) {
        moveItem(itemId, itemType, true, currentGroupId)
    }

    fun moveItemDown(itemId: Int, itemType: String, currentGroupId: Int) {
        moveItem(itemId, itemType, false, currentGroupId)
    }

    fun moveItemToTop(itemId: Int, itemType: String, currentGroupId: Int) {
        viewModelScope.launch {
            val currentGoals = goals.value.filter { it.parentGroupId == currentGroupId }.map { DashboardItem.GoalItem(it) }
            val currentNotes = notes.value.filter { it.parentGroupId == currentGroupId }.map { DashboardItem.NoteItem(it) }
            val currentGroups = groups.value.filter { it.parentGroupId == currentGroupId }.map { DashboardItem.GroupItem(it) }
            val list = (currentGoals + currentNotes + currentGroups).sortedByDescending { it.createdAt }
            if (list.isEmpty()) return@launch

            val item = list.find { 
                it.id == itemId && when (it) {
                    is DashboardItem.GoalItem -> itemType == "goal"
                    is DashboardItem.NoteItem -> itemType == "note"
                    is DashboardItem.GroupItem -> itemType == "group"
                }
            } ?: return@launch

            val maxCreatedAt = list.maxOf { it.createdAt }
            updateItemCreatedAt(item, maxCreatedAt + 1000)
        }
    }

    fun moveItemToBottom(itemId: Int, itemType: String, currentGroupId: Int) {
        viewModelScope.launch {
            val currentGoals = goals.value.filter { it.parentGroupId == currentGroupId }.map { DashboardItem.GoalItem(it) }
            val currentNotes = notes.value.filter { it.parentGroupId == currentGroupId }.map { DashboardItem.NoteItem(it) }
            val currentGroups = groups.value.filter { it.parentGroupId == currentGroupId }.map { DashboardItem.GroupItem(it) }
            val list = (currentGoals + currentNotes + currentGroups).sortedByDescending { it.createdAt }
            if (list.isEmpty()) return@launch

            val item = list.find { 
                it.id == itemId && when (it) {
                    is DashboardItem.GoalItem -> itemType == "goal"
                    is DashboardItem.NoteItem -> itemType == "note"
                    is DashboardItem.GroupItem -> itemType == "group"
                }
            } ?: return@launch

            val minCreatedAt = list.minOf { it.createdAt }
            updateItemCreatedAt(item, minCreatedAt - 1000)
        }
    }

    fun moveItemBeforeTarget(sourceId: Int, sourceType: String, targetId: Int, targetType: String, currentGroupId: Int) {
        viewModelScope.launch {
            val currentGoals = goals.value.filter { it.parentGroupId == currentGroupId }.map { DashboardItem.GoalItem(it) }
            val currentNotes = notes.value.filter { it.parentGroupId == currentGroupId }.map { DashboardItem.NoteItem(it) }
            val currentGroups = groups.value.filter { it.parentGroupId == currentGroupId }.map { DashboardItem.GroupItem(it) }
            val list = (currentGoals + currentNotes + currentGroups).sortedByDescending { it.createdAt }.toMutableList()
            
            val sourceIndex = list.indexOfFirst { 
                it.id == sourceId && when (it) {
                    is DashboardItem.GoalItem -> sourceType == "goal"
                    is DashboardItem.NoteItem -> sourceType == "note"
                    is DashboardItem.GroupItem -> sourceType == "group"
                }
            }
            val targetIndex = list.indexOfFirst { 
                it.id == targetId && when (it) {
                    is DashboardItem.GoalItem -> targetType == "goal"
                    is DashboardItem.NoteItem -> targetType == "note"
                    is DashboardItem.GroupItem -> targetType == "group"
                }
            }
            if (sourceIndex == -1 || targetIndex == -1 || sourceIndex == targetIndex) return@launch
            
            val sourceItem = list.removeAt(sourceIndex)
            val newTargetIndex = list.indexOfFirst { 
                it.id == targetId && when (it) {
                    is DashboardItem.GoalItem -> targetType == "goal"
                    is DashboardItem.NoteItem -> targetType == "note"
                    is DashboardItem.GroupItem -> targetType == "group"
                }
            }
            if (newTargetIndex == -1) return@launch
            
            list.add(newTargetIndex, sourceItem)
            
            val now = System.currentTimeMillis()
            list.forEachIndexed { idx, item ->
                val newCreatedAt = now - idx * 10000
                updateItemCreatedAt(item, newCreatedAt)
            }
        }
    }

    private fun moveItem(itemId: Int, itemType: String, up: Boolean, currentGroupId: Int) {
        viewModelScope.launch {
            val currentGoals = goals.value.filter { it.parentGroupId == currentGroupId }.map { DashboardItem.GoalItem(it) }
            val currentNotes = notes.value.filter { it.parentGroupId == currentGroupId }.map { DashboardItem.NoteItem(it) }
            val currentGroups = groups.value.filter { it.parentGroupId == currentGroupId }.map { DashboardItem.GroupItem(it) }
            val list = (currentGoals + currentNotes + currentGroups).sortedByDescending { it.createdAt }.toMutableList()
            
            val index = list.indexOfFirst { 
                it.id == itemId && when (it) {
                    is DashboardItem.GoalItem -> itemType == "goal"
                    is DashboardItem.NoteItem -> itemType == "note"
                    is DashboardItem.GroupItem -> itemType == "group"
                }
            }
            if (index == -1) return@launch
            
            val targetIndex = if (up) index - 1 else index + 1
            if (targetIndex in list.indices) {
                val itemA = list[index]
                val itemB = list[targetIndex]
                
                val timeA = itemA.createdAt
                val timeB = itemB.createdAt
                
                val newTimeA = if (timeA == timeB) timeB + 1 else timeB
                val newTimeB = if (timeA == timeB) timeA - 1 else timeA

                updateItemCreatedAt(itemA, newTimeA)
                updateItemCreatedAt(itemB, newTimeB)
            }
        }
    }

    private suspend fun updateItemCreatedAt(item: DashboardItem, newCreatedAt: Long) {
        when (item) {
            is DashboardItem.GoalItem -> {
                repository.update(item.goal.copy(createdAt = newCreatedAt))
            }
            is DashboardItem.NoteItem -> {
                repository.updateNote(item.note.copy(createdAt = newCreatedAt))
            }
            is DashboardItem.GroupItem -> {
                repository.updateGroup(item.group.copy(createdAt = newCreatedAt))
            }
        }
    }

    fun exportBackup(uri: Uri, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val context = getApplication<Application>()
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        java.util.zip.ZipOutputStream(java.io.BufferedOutputStream(os)).use { zos ->
                            // 1. Write the backup_data.json entry
                            val allGoalsList = repository.allGoals.first()
                            val allNotesList = repository.allNotes.first()
                            val allGroupsList = repository.allGroups.first()

                            val json = org.json.JSONObject().apply {
                                // Settings
                                put("settings", org.json.JSONObject().apply {
                                    put("is_dark_theme", prefs.getBoolean("is_dark_theme", false))
                                    put("landscape_columns", prefs.getInt("landscape_columns", 1))
                                    put("is_animation_enabled", prefs.getBoolean("is_animation_enabled", true))
                                    put("is_aesthetic_dashboard_enabled", prefs.getBoolean("is_aesthetic_dashboard_enabled", true))
                                    put("animation_duration_seconds", prefs.getInt("animation_duration_seconds", 3))
                                    put("animation_easing_type", prefs.getInt("animation_easing_type", 2))
                                    put("dashboard_background_uri", prefs.getString("dashboard_background_uri", null))
                                })
                                
                                // Goals
                                val goalsArray = org.json.JSONArray()
                                for (g in allGoalsList) {
                                    goalsArray.put(org.json.JSONObject().apply {
                                        put("id", g.id)
                                        put("title", g.title)
                                        put("description", g.description)
                                        put("currentPoints", g.currentPoints)
                                        put("maxPoints", g.maxPoints)
                                        put("allowExceed", g.allowExceed)
                                        put("colorHex", g.colorHex)
                                        put("secondaryColorHex", g.secondaryColorHex)
                                        put("milestonesStr", g.milestonesStr)
                                        put("barBehavior", g.barBehavior)
                                        put("hidePassedMilestones", g.hidePassedMilestones)
                                        put("showRemainingCount", g.showRemainingCount)
                                        put("parentGroupId", g.parentGroupId)
                                        put("progressType", g.progressType)
                                        put("meterThickness", g.meterThickness.toDouble())
                                        put("dotsPerRow", g.dotsPerRow)
                                        put("unit", g.unit)
                                        put("cardBorderHex", g.cardBorderHex)
                                        put("statsEnabled", g.statsEnabled)
                                        put("streaksEnabled", g.streaksEnabled)
                                        put("pointTrackingEnabled", g.pointTrackingEnabled)
                                        put("statsVisualType", g.statsVisualType)
                                        put("showHighlights", g.showHighlights)
                                        put("groupValue", g.groupValue)
                                        put("allowGroupPointsOverflow", g.allowGroupPointsOverflow)
                                        put("isExpanded", _expandedItemIds.value.contains("goal_${g.id}"))
                                        put("createdAt", g.createdAt)
                                    })
                                }
                                put("goals", goalsArray)

                                // Notes
                                val notesArray = org.json.JSONArray()
                                for (n in allNotesList) {
                                    notesArray.put(org.json.JSONObject().apply {
                                        put("id", n.id)
                                        put("text", n.text)
                                        put("textSize", n.textSize.toDouble())
                                        put("colorHex", n.colorHex)
                                        put("secondaryColorHex", n.secondaryColorHex)
                                        put("imageUri", n.imageUri)
                                        put("parentGroupId", n.parentGroupId)
                                        put("createdAt", n.createdAt)
                                    })
                                }
                                put("notes", notesArray)

                                // Groups
                                val groupsArray = org.json.JSONArray()
                                for (gr in allGroupsList) {
                                    groupsArray.put(org.json.JSONObject().apply {
                                        put("id", gr.id)
                                        put("title", gr.title)
                                        put("description", gr.description)
                                        put("colorHex", gr.colorHex)
                                        put("secondaryColorHex", gr.secondaryColorHex)
                                        put("imageUri", gr.imageUri)
                                        put("applyBg", gr.applyBg)
                                        put("outlineMainColor", gr.outlineMainColor)
                                        put("parentGroupId", gr.parentGroupId)
                                        put("cardBorderHex", gr.cardBorderHex)
                                        put("displayType", gr.displayType)
                                        put("createdAt", gr.createdAt)
                                    })
                                }
                                put("groups", groupsArray)
                            }

                            // Add backup_data.json to zip
                            val jsonBytes = json.toString(4).toByteArray(Charsets.UTF_8)
                            val jsonEntry = java.util.zip.ZipEntry("backup_data.json")
                            zos.putNextEntry(jsonEntry)
                            zos.write(jsonBytes)
                            zos.closeEntry()

                            // 2. Write the images
                            val imagesDir = File(context.filesDir, "app_images")
                            if (imagesDir.exists() && imagesDir.isDirectory) {
                                val files = imagesDir.listFiles()
                                if (files != null) {
                                    for (file in files) {
                                        if (file.isFile) {
                                            try {
                                                val entry = java.util.zip.ZipEntry("images/${file.name}")
                                                zos.putNextEntry(entry)
                                                file.inputStream().use { fis ->
                                                    fis.copyTo(zos)
                                                }
                                                zos.closeEntry()
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onError(e.localizedMessage ?: "Unknown error")
                }
            }
        }
    }

    fun importBackup(uri: Uri, replaceAll: Boolean = true, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val restoredExpandedItems = mutableSetOf<String>()
            try {
                withContext(Dispatchers.IO) {
                    val context = getApplication<Application>()
                    val imagesDir = File(context.filesDir, "app_images")
                    if (!imagesDir.exists()) {
                        imagesDir.mkdirs()
                    }

                    var jsonString: String? = null
                    
                    // Open the zip and extract files
                    context.contentResolver.openInputStream(uri)?.use { isStream ->
                        java.util.zip.ZipInputStream(java.io.BufferedInputStream(isStream)).use { zis ->
                            var entry = zis.nextEntry
                            while (entry != null) {
                                if (!entry.isDirectory) {
                                    if (entry.name == "backup_data.json") {
                                        jsonString = zis.bufferedReader(Charsets.UTF_8).readText()
                                    } else if (entry.name.startsWith("images/")) {
                                        val fileName = entry.name.substringAfter("images/")
                                        if (fileName.isNotEmpty()) {
                                            val destFile = File(imagesDir, fileName)
                                            destFile.outputStream().use { fos ->
                                                zis.copyTo(fos)
                                            }
                                        }
                                    }
                                }
                                zis.closeEntry()
                                entry = zis.nextEntry
                            }
                        }
                    }

                    if (jsonString == null) {
                        throw Exception("Invalid backup file: backup_data.json not found")
                    }

                    val json = org.json.JSONObject(jsonString!!)
                    
                    // 1. Process settings (only if replacing all)
                    if (replaceAll) {
                        val settingsObj = json.optJSONObject("settings")
                        if (settingsObj != null) {
                            val isDark = settingsObj.optBoolean("is_dark_theme", false)
                            val columns = settingsObj.optInt("landscape_columns", 1)
                            val animEnabled = settingsObj.optBoolean("is_animation_enabled", true)
                            val aestheticEnabled = settingsObj.optBoolean("is_aesthetic_dashboard_enabled", true)
                            val animDuration = settingsObj.optInt("animation_duration_seconds", 3)
                            val animEasing = settingsObj.optInt("animation_easing_type", 2)
                            
                            _isDarkTheme.value = isDark
                            _landscapeColumns.value = columns
                            _isAnimationEnabled.value = animEnabled
                            _isAestheticDashboardEnabled.value = aestheticEnabled
                            _animationDurationSeconds.value = animDuration
                            _animationEasingType.value = animEasing

                            val editor = prefs.edit()
                            editor.clear()
                            editor.putBoolean("is_dark_theme", isDark)
                            editor.putInt("landscape_columns", columns)
                            editor.putBoolean("is_animation_enabled", animEnabled)
                            editor.putBoolean("is_aesthetic_dashboard_enabled", aestheticEnabled)
                            editor.putInt("animation_duration_seconds", animDuration)
                            editor.putInt("animation_easing_type", animEasing)
                            
                            val bgUri = settingsObj.optString("dashboard_background_uri", "")
                            if (bgUri.isNotEmpty()) {
                                val filename = bgUri.substringAfterLast("/")
                                val newFile = File(imagesDir, filename)
                                if (newFile.exists()) {
                                    val localUri = Uri.fromFile(newFile).toString()
                                    _dashboardBackgroundUri.value = localUri
                                    editor.putString("dashboard_background_uri", localUri)
                                }
                            }
                            editor.apply()
                        }

                        // Clear Database
                        repository.deleteAllGoals()
                        repository.deleteAllNotes()
                        repository.deleteAllGroups()
                    }

                    val groupIdMap = mutableMapOf<Int, Int>()

                    // 2. Restore GoalGroups
                    val groupsArray = json.optJSONArray("groups")
                    if (groupsArray != null) {
                        for (i in 0 until groupsArray.length()) {
                            val obj = groupsArray.getJSONObject(i)
                            val oldId = obj.optInt("id")
                            val oldParentId = obj.optInt("parentGroupId", 0)
                            
                            // Map old imageUri to new local path if present
                            var imgUri: String? = if (obj.isNull("imageUri")) null else obj.optString("imageUri")
                            if (imgUri != null && imgUri != "null" && imgUri.isNotEmpty()) {
                                val filename = imgUri.substringAfterLast("/")
                                val imgFile = File(imagesDir, filename)
                                if (imgFile.exists()) {
                                    imgUri = Uri.fromFile(imgFile).toString()
                                }
                            } else {
                                imgUri = null
                            }

                            val mappedParentId = if (replaceAll) oldParentId else (if (oldParentId != 0) groupIdMap[oldParentId] ?: 0 else 0)

                            val group = GoalGroup(
                                id = if (replaceAll) oldId else 0,
                                title = obj.optString("title"),
                                description = obj.optString("description"),
                                colorHex = obj.optString("colorHex", "#6750A4"),
                                secondaryColorHex = obj.optString("secondaryColorHex", "DEFAULT"),
                                imageUri = imgUri,
                                applyBg = obj.optBoolean("applyBg", true),
                                outlineMainColor = obj.optBoolean("outlineMainColor", false),
                                parentGroupId = mappedParentId,
                                cardBorderHex = obj.optString("cardBorderHex", "NONE"),
                                displayType = obj.optInt("displayType", 0),
                                createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                            )
                            val newGroupId = repository.insertGroup(group).toInt()
                            if (!replaceAll) {
                                groupIdMap[oldId] = newGroupId
                            }
                        }
                    }

                    // 3. Restore Goals
                    val goalsArray = json.optJSONArray("goals")
                    if (goalsArray != null) {
                        for (i in 0 until goalsArray.length()) {
                            val obj = goalsArray.getJSONObject(i)
                            val oldId = obj.optInt("id")
                            val oldParentId = obj.optInt("parentGroupId", 0)
                            val mappedParentId = if (replaceAll) oldParentId else (if (oldParentId != 0) groupIdMap[oldParentId] ?: 0 else 0)

                            val goal = Goal(
                                id = if (replaceAll) oldId else 0,
                                title = obj.optString("title"),
                                description = obj.optString("description"),
                                currentPoints = obj.optInt("currentPoints"),
                                maxPoints = obj.optInt("maxPoints"),
                                allowExceed = obj.optBoolean("allowExceed"),
                                colorHex = obj.optString("colorHex", "#6750A4"),
                                secondaryColorHex = obj.optString("secondaryColorHex", "DEFAULT"),
                                milestonesStr = obj.optString("milestonesStr", ""),
                                barBehavior = obj.optInt("barBehavior", 0),
                                hidePassedMilestones = obj.optBoolean("hidePassedMilestones", false),
                                showRemainingCount = obj.optBoolean("showRemainingCount", true),
                                parentGroupId = mappedParentId,
                                progressType = obj.optInt("progressType", 0),
                                meterThickness = obj.optDouble("meterThickness", 14.0).toFloat(),
                                dotsPerRow = obj.optInt("dotsPerRow", 8),
                                cardBorderHex = obj.optString("cardBorderHex", "NONE"),
                                unit = obj.optString("unit", "pts"),
                                statsEnabled = obj.optBoolean("statsEnabled", true),
                                streaksEnabled = obj.optBoolean("streaksEnabled", false),
                                pointTrackingEnabled = obj.optBoolean("pointTrackingEnabled", true),
                                statsVisualType = obj.optInt("statsVisualType", 0),
                                showHighlights = obj.optBoolean("showHighlights", true),
                                groupValue = obj.optDouble("groupValue", 1.0),
                                allowGroupPointsOverflow = obj.optBoolean("allowGroupPointsOverflow", true),
                                createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                            )
                            val newGoalId = repository.insert(goal).toInt()
                            val keyId = if (replaceAll) oldId else newGoalId
                            if (obj.optBoolean("isExpanded", false)) {
                                restoredExpandedItems.add("goal_$keyId")
                            }
                        }
                    }

                    // 4. Restore Notes
                    val notesArray = json.optJSONArray("notes")
                    if (notesArray != null) {
                        for (i in 0 until notesArray.length()) {
                            val obj = notesArray.getJSONObject(i)
                            val oldId = obj.optInt("id")
                            val oldParentId = obj.optInt("parentGroupId", 0)
                            val mappedParentId = if (replaceAll) oldParentId else (if (oldParentId != 0) groupIdMap[oldParentId] ?: 0 else 0)
                            
                            var imgUri: String? = if (obj.isNull("imageUri")) null else obj.optString("imageUri")
                            if (imgUri != null && imgUri != "null" && imgUri.isNotEmpty()) {
                                val filename = imgUri.substringAfterLast("/")
                                val imgFile = File(imagesDir, filename)
                                if (imgFile.exists()) {
                                    imgUri = Uri.fromFile(imgFile).toString()
                                }
                            } else {
                                imgUri = null
                            }

                            val note = Note(
                                id = if (replaceAll) oldId else 0,
                                text = obj.optString("text"),
                                textSize = obj.optDouble("textSize", 14.0).toFloat(),
                                colorHex = obj.optString("colorHex", "#6750A4"),
                                secondaryColorHex = obj.optString("secondaryColorHex", "DEFAULT"),
                                imageUri = imgUri,
                                parentGroupId = mappedParentId,
                                cardBorderHex = obj.optString("cardBorderHex", "NONE"),
                                createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                            )
                            repository.insertNote(note)
                        }
                    }
                }
                if (replaceAll && restoredExpandedItems.isNotEmpty()) {
                    prefs.edit().putStringSet("expanded_items", restoredExpandedItems).apply()
                    _expandedItemIds.value = restoredExpandedItems
                } else if (!replaceAll && restoredExpandedItems.isNotEmpty()) {
                    val merged = _expandedItemIds.value.toMutableSet().apply { addAll(restoredExpandedItems) }
                    prefs.edit().putStringSet("expanded_items", merged).apply()
                    _expandedItemIds.value = merged
                }
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onError(e.localizedMessage ?: "Unknown error")
                }
            }
        }
    }
}
