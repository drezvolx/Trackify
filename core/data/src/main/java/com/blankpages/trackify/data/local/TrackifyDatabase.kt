package com.blankpages.trackify.data.local

import android.content.Context
import android.util.Log
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.blankpages.trackify.domain.BackupPayload
import com.blankpages.trackify.domain.CourierNote
import com.blankpages.trackify.domain.DailyLog
import com.blankpages.trackify.domain.NotePhoto
import com.blankpages.trackify.domain.SavedLocation
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackifyDao {
    @Upsert
    suspend fun upsertLog(log: DailyLog)

    @Delete
    suspend fun deleteLog(log: DailyLog)

    @Query("SELECT * FROM daily_logs WHERE date = :date LIMIT 1")
    fun getLogByDate(date: String): Flow<DailyLog?>

    @Query("SELECT * FROM daily_logs WHERE date LIKE :yearMonth || '%' ORDER BY date DESC")
    fun getLogsByMonth(yearMonth: String): Flow<List<DailyLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: SavedLocation)

    @Update
    suspend fun updateLocation(location: SavedLocation)

    @Delete
    suspend fun deleteLocation(location: SavedLocation)

    @Query("UPDATE courier_notes SET linkedLocationId = NULL WHERE linkedLocationId = :locationId")
    suspend fun unlinkNotesFromLocation(locationId: Int)

    @Query("SELECT * FROM saved_locations ORDER BY id DESC")
    fun getAllLocations(): Flow<List<SavedLocation>>

    @Upsert
    suspend fun upsertNote(note: CourierNote): Long

    @Delete
    suspend fun deleteNote(note: CourierNote)

    @Query("SELECT * FROM courier_notes ORDER BY isResolved ASC, isPinned DESC, id DESC")
    fun getAllNotes(): Flow<List<CourierNote>>

    @Query("SELECT * FROM note_photos ORDER BY noteId ASC, sortOrder ASC, id ASC")
    fun getAllNotePhotos(): Flow<List<NotePhoto>>

    @Query("SELECT * FROM note_photos WHERE noteId = :noteId ORDER BY sortOrder ASC, id ASC")
    suspend fun notePhotos(noteId: Int): List<NotePhoto>

    @Query("SELECT * FROM note_photos ORDER BY noteId ASC, sortOrder ASC, id ASC")
    suspend fun dumpNotePhotos(): List<NotePhoto>

    @Upsert
    suspend fun upsertNotePhotos(photos: List<NotePhoto>)

    @Query("DELETE FROM note_photos WHERE noteId = :noteId")
    suspend fun deleteNotePhotos(noteId: Int)

    @Query("DELETE FROM note_photos")
    suspend fun clearNotePhotos()

    @Query("SELECT * FROM daily_logs")
    suspend fun dumpLogs(): List<DailyLog>

    @Query("SELECT * FROM saved_locations")
    suspend fun dumpLocations(): List<SavedLocation>

    @Query("SELECT * FROM courier_notes")
    suspend fun dumpNotes(): List<CourierNote>

    @Query("DELETE FROM daily_logs")
    suspend fun clearLogs()

    @Query("DELETE FROM saved_locations")
    suspend fun clearLocations()

    @Query("DELETE FROM courier_notes")
    suspend fun clearNotes()

    @Upsert
    suspend fun upsertLogs(logs: List<DailyLog>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocations(locations: List<SavedLocation>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<CourierNote>)

    /** Restore = ganti penuh, bukan tambah. Data yang tidak ada di backup ikut terhapus. */
    @Transaction
    suspend fun restoreSnapshot(payload: BackupPayload) {
        clearLogs()
        clearLocations()
        clearNotePhotos()
        clearNotes()
        if (payload.logs.isNotEmpty()) upsertLogs(payload.logs)
        if (payload.locations.isNotEmpty()) insertLocations(payload.locations)
        if (payload.notes.isNotEmpty()) insertNotes(payload.notes)
        if (payload.notePhotos.isNotEmpty()) upsertNotePhotos(payload.notePhotos.take(5 * payload.notes.size))
    }
}

private val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `daily_logs_new` (" +
                "`date` TEXT NOT NULL, " +
                "`isWorkingDay` INTEGER NOT NULL, " +
                "`packagesCarried` INTEGER NOT NULL, " +
                "`packagesDelivered` INTEGER NOT NULL, " +
                "`ratePerPackage` REAL NOT NULL, " +
                "`bonus` REAL NOT NULL, " +
                "`expense` REAL NOT NULL, " +
                "`notes` TEXT NOT NULL, " +
                "PRIMARY KEY(`date`))",
        )
        db.execSQL(
            "INSERT INTO `daily_logs_new` " +
                "(`date`, `isWorkingDay`, `packagesCarried`, `packagesDelivered`, " +
                "`ratePerPackage`, `bonus`, `expense`, `notes`) " +
                "SELECT `date`, `isWorkingDay`, `packagesCarried`, `packagesDelivered`, " +
                "`ratePerPackage`, `bonus`, `expense`, `notes` " +
                "FROM `daily_logs`",
        )
        db.execSQL("DROP TABLE `daily_logs`")
        db.execSQL("ALTER TABLE `daily_logs_new` RENAME TO `daily_logs`")
    }
}

private val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `saved_locations` ADD COLUMN `photoPath` TEXT NOT NULL DEFAULT ''")
    }
}

private val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `note_photos` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`noteId` INTEGER NOT NULL, " +
                "`path` TEXT NOT NULL, " +
                "`sortOrder` INTEGER NOT NULL DEFAULT 0, " +
                "FOREIGN KEY(`noteId`) REFERENCES `courier_notes`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_photos_noteId` ON `note_photos` (`noteId`)")
    }
}

@Database(
    entities = [DailyLog::class, SavedLocation::class, CourierNote::class, NotePhoto::class],
    version = 8,
    exportSchema = false,
)
abstract class TrackifyDatabase : RoomDatabase() {
    abstract fun dao(): TrackifyDao

    companion object {
        @Volatile
        private var instance: TrackifyDatabase? = null

        fun get(context: Context): TrackifyDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                TrackifyDatabase::class.java,
                "trackify_offline_db",
            )
                .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                .fallbackToDestructiveMigration(true)
                .build()
                .also { instance = it }
                .also { Log.d("TrackifyDatabase", "Database initialized") }
        }
    }
}
