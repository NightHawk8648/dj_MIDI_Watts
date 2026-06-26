package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "presets")
data class Preset(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val bpm: Int = 120,
    val faderLow: Float = 0.5f,
    val faderMid: Float = 0.5f,
    val faderHigh: Float = 0.5f,
    val faderVocal: Float = 0.5f,
    val faderSub: Float = 0.5f,
    val strobeSpeed: Int = 60,
    val fogDensity: Float = 0.0f,
    val themeColorGlow: String = "#00FFCC", // HTML color hex representing the Neon LED glow
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface PresetDao {
    @Query("SELECT * FROM presets ORDER BY timestamp DESC")
    fun getAllPresets(): Flow<List<Preset>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: Preset)

    @Delete
    suspend fun deletePreset(preset: Preset)
}

@Database(entities = [Preset::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun presetDao(): PresetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "presets_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
