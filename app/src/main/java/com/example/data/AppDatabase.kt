package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE goals ADD COLUMN cardBorderHex TEXT NOT NULL DEFAULT 'NONE'")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE goals ADD COLUMN unit TEXT NOT NULL DEFAULT 'pts'")
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE goals ADD COLUMN statsEnabled INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE goals ADD COLUMN streaksEnabled INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE goals ADD COLUMN pointTrackingEnabled INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE goals ADD COLUMN statsVisualType INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE goals ADD COLUMN showHighlights INTEGER NOT NULL DEFAULT 1")
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `goal_logs` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `goalId` INTEGER NOT NULL, 
                `pointsBefore` INTEGER NOT NULL, 
                `pointsAfter` INTEGER NOT NULL, 
                `changeAmount` INTEGER NOT NULL, 
                `timestamp` INTEGER NOT NULL
            )
        """)
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE goals ADD COLUMN allowGroupPointsOverflow INTEGER NOT NULL DEFAULT 1")
    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE goals ADD COLUMN showMilestoneProgressBar INTEGER NOT NULL DEFAULT 1")
    }
}

@Database(entities = [Goal::class, Note::class, GoalGroup::class, GoalLog::class], version = 12, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun goalDao(): GoalDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "progress_tracker_db"
                )
                .addMigrations(MIGRATION_5_6, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_10_11, MIGRATION_11_12)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
