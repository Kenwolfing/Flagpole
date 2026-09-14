package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY createdAt DESC")
    fun getAllGoals(): Flow<List<Goal>>

    @Query("SELECT * FROM goals WHERE parentGroupId = :parentGroupId ORDER BY createdAt DESC")
    fun getGoalsByParentGroupId(parentGroupId: Int): Flow<List<Goal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: Goal): Long

    @Update
    suspend fun updateGoal(goal: Goal)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoalById(id: Int)

    // Notes
    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE parentGroupId = :parentGroupId ORDER BY createdAt DESC")
    fun getNotesByParentGroupId(parentGroupId: Int): Flow<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Int)

    // Groups
    @Query("SELECT * FROM goal_groups ORDER BY createdAt DESC")
    fun getAllGroups(): Flow<List<GoalGroup>>

    @Query("SELECT * FROM goal_groups WHERE parentGroupId = :parentGroupId ORDER BY createdAt DESC")
    fun getGroupsByParentGroupId(parentGroupId: Int): Flow<List<GoalGroup>>

    @Query("SELECT * FROM goal_groups WHERE id = :id")
    fun getGroupByIdFlow(id: Int): Flow<GoalGroup?>

    @Query("""
        SELECT 
            parentGroupId, 
            COUNT(*) as totalGoals, 
            SUM(CASE WHEN currentPoints >= maxPoints THEN 1 ELSE 0 END) as completedGoals 
        FROM goals 
        GROUP BY parentGroupId
    """)
    fun getGroupGoalStats(): Flow<List<GroupGoalStats>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GoalGroup): Long

    @Update
    suspend fun updateGroup(group: GoalGroup)

    @Query("DELETE FROM goal_groups WHERE id = :id")
    suspend fun deleteGroupById(id: Int)

    @Query("DELETE FROM goals")
    suspend fun deleteAllGoals()

    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()

    @Query("DELETE FROM goal_groups")
    suspend fun deleteAllGroups()

    // Goal Logs
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoalLog(log: GoalLog): Long

    @Query("SELECT * FROM goal_logs WHERE goalId = :goalId ORDER BY timestamp ASC")
    fun getLogsForGoal(goalId: Int): Flow<List<GoalLog>>

    @Query("SELECT * FROM goal_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<GoalLog>>

    @Query("DELETE FROM goal_logs WHERE goalId = :goalId")
    suspend fun deleteLogsForGoal(goalId: Int)

    @Query("UPDATE goal_logs SET changeAmount = 0 WHERE goalId = :goalId AND timestamp >= :start AND timestamp < :end")
    suspend fun ignorePointsChangeForDay(goalId: Int, start: Long, end: Long)

    @Query("DELETE FROM goal_logs")
    suspend fun deleteAllGoalLogs()
}

