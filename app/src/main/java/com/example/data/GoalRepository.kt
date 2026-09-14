package com.example.data

import kotlinx.coroutines.flow.Flow

class GoalRepository(private val goalDao: GoalDao) {
    val allGoals: Flow<List<Goal>> = goalDao.getAllGoals()
    val allNotes: Flow<List<Note>> = goalDao.getAllNotes()
    val allGroups: Flow<List<GoalGroup>> = goalDao.getAllGroups()

    fun getGoalsByParentGroupId(parentGroupId: Int): Flow<List<Goal>> = goalDao.getGoalsByParentGroupId(parentGroupId)
    fun getNotesByParentGroupId(parentGroupId: Int): Flow<List<Note>> = goalDao.getNotesByParentGroupId(parentGroupId)
    fun getGroupsByParentGroupId(parentGroupId: Int): Flow<List<GoalGroup>> = goalDao.getGroupsByParentGroupId(parentGroupId)
    fun getGroupByIdFlow(id: Int): Flow<GoalGroup?> = goalDao.getGroupByIdFlow(id)
    val groupGoalStats: Flow<List<GroupGoalStats>> = goalDao.getGroupGoalStats()

    suspend fun insert(goal: Goal) = goalDao.insertGoal(goal)
    suspend fun update(goal: Goal) = goalDao.updateGoal(goal)
    suspend fun deleteById(id: Int) = goalDao.deleteGoalById(id)

    // Notes
    suspend fun insertNote(note: Note) = goalDao.insertNote(note)
    suspend fun updateNote(note: Note) = goalDao.updateNote(note)
    suspend fun deleteNoteById(id: Int) = goalDao.deleteNoteById(id)

    // Groups
    suspend fun insertGroup(group: GoalGroup) = goalDao.insertGroup(group)
    suspend fun updateGroup(group: GoalGroup) = goalDao.updateGroup(group)
    suspend fun deleteGroupById(id: Int) = goalDao.deleteGroupById(id)

    suspend fun deleteAllGoals() = goalDao.deleteAllGoals()
    suspend fun deleteAllNotes() = goalDao.deleteAllNotes()
    suspend fun deleteAllGroups() = goalDao.deleteAllGroups()

    // Goal Logs
    fun getLogsForGoal(goalId: Int): Flow<List<GoalLog>> = goalDao.getLogsForGoal(goalId)
    val allLogs: Flow<List<GoalLog>> = goalDao.getAllLogs()
    suspend fun insertGoalLog(log: GoalLog) = goalDao.insertGoalLog(log)
    suspend fun deleteLogsForGoal(goalId: Int) = goalDao.deleteLogsForGoal(goalId)
    suspend fun ignorePointsChangeForDay(goalId: Int, start: Long, end: Long) = goalDao.ignorePointsChangeForDay(goalId, start, end)
    suspend fun deleteAllGoalLogs() = goalDao.deleteAllGoalLogs()
}

