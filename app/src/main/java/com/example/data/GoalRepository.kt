package com.example.data

import kotlinx.coroutines.flow.Flow

class GoalRepository(private val goalDao: GoalDao) {
    val allGoals: Flow<List<Goal>> = goalDao.getAllGoals()
    val allNotes: Flow<List<Note>> = goalDao.getAllNotes()
    val allGroups: Flow<List<GoalGroup>> = goalDao.getAllGroups()

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
}

