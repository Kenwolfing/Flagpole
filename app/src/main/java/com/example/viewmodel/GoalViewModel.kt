package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Goal
import com.example.data.Note
import com.example.data.GoalGroup
import com.example.data.GoalRepository
import com.example.ui.DashboardItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

    private val _animationEasingType = kotlinx.coroutines.flow.MutableStateFlow(prefs.getInt("animation_easing_type", 2)) // 2 = Constant (Linear)
    val animationEasingType: StateFlow<Int> = _animationEasingType

    fun setAnimationEasingType(type: Int) {
        _animationEasingType.value = type
        prefs.edit().putInt("animation_easing_type", type).apply()
    }

    init {
        val database = AppDatabase.getDatabase(application)
        repository = GoalRepository(database.goalDao())
    }

    val goals: StateFlow<List<Goal>> = repository.allGoals
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val notes: StateFlow<List<Note>> = repository.allNotes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val groups: StateFlow<List<GoalGroup>> = repository.allGroups
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _groupNavigationStack = kotlinx.coroutines.flow.MutableStateFlow<List<Int>>(emptyList())
    val groupNavigationStack: StateFlow<List<Int>> = _groupNavigationStack

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
        parentGroupId: Int = 0
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
                parentGroupId = parentGroupId
            )
            repository.insert(goal)
        }
    }

    fun updateGoal(goal: Goal) {
        viewModelScope.launch {
            repository.update(goal)
        }
    }

    fun duplicateGoal(goal: Goal) {
        viewModelScope.launch {
            val duplicate = goal.copy(id = 0, createdAt = System.currentTimeMillis())
            repository.insert(duplicate)
        }
    }

    fun resetGoal(goal: Goal) {
        viewModelScope.launch {
            repository.update(goal.copy(currentPoints = 0))
        }
    }

    fun completeGoal(goal: Goal) {
        viewModelScope.launch {
            repository.update(goal.copy(currentPoints = goal.maxPoints))
        }
    }

    fun incrementPoints(goal: Goal) {
        viewModelScope.launch {
            val newPoints = goal.currentPoints + 1
            if (!goal.allowExceed && newPoints > goal.maxPoints) {
                // Cannot exceed max points
                return@launch
            }
            repository.update(goal.copy(currentPoints = newPoints))
        }
    }

    fun decrementPoints(goal: Goal) {
        viewModelScope.launch {
            val newPoints = maxOf(0, goal.currentPoints - 1)
            repository.update(goal.copy(currentPoints = newPoints))
        }
    }

    fun adjustGoal(goal: Goal, isAddition: Boolean, amount: Int, adjustMax: Boolean) {
        viewModelScope.launch {
            if (adjustMax) {
                val change = if (isAddition) amount else -amount
                val newMax = maxOf(1, goal.maxPoints + change)
                val newCurrent = if (!goal.allowExceed) minOf(goal.currentPoints, newMax) else goal.currentPoints
                repository.update(goal.copy(maxPoints = newMax, currentPoints = newCurrent))
            } else {
                val change = if (isAddition) amount else -amount
                var newCurrent = goal.currentPoints + change
                newCurrent = maxOf(0, newCurrent)
                if (!goal.allowExceed && newCurrent > goal.maxPoints) {
                    newCurrent = goal.maxPoints
                }
                repository.update(goal.copy(currentPoints = newCurrent))
            }
        }
    }

    fun deleteGoal(goal: Goal) {
        viewModelScope.launch {
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
        parentGroupId: Int = 0
    ) {
        viewModelScope.launch {
            val note = Note(
                text = text,
                textSize = textSize,
                colorHex = colorHex,
                secondaryColorHex = secondaryColorHex,
                imageUri = imageUri,
                parentGroupId = parentGroupId
            )
            repository.insertNote(note)
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch {
            repository.updateNote(note)
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
        parentGroupId: Int = 0
    ) {
        viewModelScope.launch {
            val group = GoalGroup(
                title = title,
                description = description,
                colorHex = colorHex,
                secondaryColorHex = secondaryColorHex,
                imageUri = imageUri,
                applyBg = applyBg,
                outlineMainColor = outlineMainColor,
                parentGroupId = parentGroupId
            )
            repository.insertGroup(group)
        }
    }

    fun updateGroup(group: GoalGroup) {
        viewModelScope.launch {
            repository.updateGroup(group)
        }
    }

    fun deleteGroup(group: GoalGroup) {
        viewModelScope.launch {
            repository.deleteGroupById(group.id)
        }
    }

    fun duplicateGroup(group: GoalGroup) {
        viewModelScope.launch {
            val newGroupId = repository.insertGroup(group.copy(id = 0, createdAt = System.currentTimeMillis()))
            val oldGroupId = group.id
            
            // Duplicate goals inside this group
            goals.value.filter { it.parentGroupId == oldGroupId }.forEach { childGoal ->
                repository.insert(childGoal.copy(id = 0, parentGroupId = newGroupId.toInt(), createdAt = System.currentTimeMillis()))
            }
            
            // Duplicate notes inside this group
            notes.value.filter { it.parentGroupId == oldGroupId }.forEach { childNote ->
                repository.insertNote(childNote.copy(id = 0, parentGroupId = newGroupId.toInt(), createdAt = System.currentTimeMillis()))
            }
            
            // Duplicate sub-groups inside this group
            groups.value.filter { it.parentGroupId == oldGroupId }.forEach { childSubGroup ->
                duplicateSubGroupRecursive(childSubGroup, newGroupId.toInt())
            }
        }
    }

    private suspend fun duplicateSubGroupRecursive(subGroup: GoalGroup, newParentId: Int) {
        val newSubGroupId = repository.insertGroup(subGroup.copy(id = 0, parentGroupId = newParentId, createdAt = System.currentTimeMillis()))
        val oldSubGroupId = subGroup.id
        
        goals.value.filter { it.parentGroupId == oldSubGroupId }.forEach { childGoal ->
            repository.insert(childGoal.copy(id = 0, parentGroupId = newSubGroupId.toInt(), createdAt = System.currentTimeMillis()))
        }
        
        notes.value.filter { it.parentGroupId == oldSubGroupId }.forEach { childNote ->
            repository.insertNote(childNote.copy(id = 0, parentGroupId = newSubGroupId.toInt(), createdAt = System.currentTimeMillis()))
        }
        
        groups.value.filter { it.parentGroupId == oldSubGroupId }.forEach { childSubSubGroup ->
            duplicateSubGroupRecursive(childSubSubGroup, newSubGroupId.toInt())
        }
    }

    fun resetGroupGoals(groupId: Int) {
        viewModelScope.launch {
            val allGoalIdsToReset = mutableSetOf<Int>()
            findGoalIdsInGroupRecursive(groupId, allGoalIdsToReset)
            
            goals.value.forEach { goal ->
                if (allGoalIdsToReset.contains(goal.id)) {
                    repository.update(goal.copy(currentPoints = 0))
                }
            }
        }
    }

    fun completeGroupGoals(groupId: Int) {
        viewModelScope.launch {
            val allGoalIdsToComplete = mutableSetOf<Int>()
            findGoalIdsInGroupRecursive(groupId, allGoalIdsToComplete)
            
            goals.value.forEach { goal ->
                if (allGoalIdsToComplete.contains(goal.id)) {
                    repository.update(goal.copy(currentPoints = goal.maxPoints))
                }
            }
        }
    }

    private fun findGoalIdsInGroupRecursive(groupId: Int, resultSet: MutableSet<Int>) {
        goals.value.filter { it.parentGroupId == groupId }.forEach { resultSet.add(it.id) }
        groups.value.filter { it.parentGroupId == groupId }.forEach { subGroup ->
            findGoalIdsInGroupRecursive(subGroup.id, resultSet)
        }
    }

    fun moveItemUp(itemId: Int, itemType: String, currentGroupId: Int) {
        moveItem(itemId, itemType, true, currentGroupId)
    }

    fun moveItemDown(itemId: Int, itemType: String, currentGroupId: Int) {
        moveItem(itemId, itemType, false, currentGroupId)
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
}
