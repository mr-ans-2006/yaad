package io.github.mrans2006.yaad.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * DAO = "Data Access Object". It is the list of questions we can ask the database.
 * Room writes the actual code for each one from the SQL in @Query.
 */
@Dao
interface ShotDao {
    /** All screenshots, newest first. A Flow re-sends the list every time the table changes. */
    @Query("SELECT * FROM shots ORDER BY takenAt DESC")
    fun observeAll(): Flow<List<Shot>>

    @Query("SELECT * FROM shots WHERE id = :id")
    fun observe(id: Long): Flow<Shot?>

    @Query("SELECT * FROM shots WHERE id = :id")
    suspend fun get(id: Long): Shot?

    @Query("SELECT * FROM shots WHERE status = 0 ORDER BY takenAt DESC LIMIT :limit")
    suspend fun pending(limit: Int): List<Shot>

    @Query("SELECT COUNT(*) FROM shots WHERE status = 0")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM shots WHERE status = 0")
    suspend fun pendingCount(): Int

    @Query("SELECT id, text, tags, vectors FROM shots WHERE status = 1")
    suspend fun searchRows(): List<SearchRow>

    @Query("SELECT mediaId FROM shots WHERE mediaId IS NOT NULL")
    suspend fun knownMediaIds(): List<Long>

    @Query("SELECT mediaId FROM ignored")
    suspend fun ignoredMediaIds(): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(shots: List<Shot>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(shot: Shot): Long

    @Update
    suspend fun update(shot: Shot)

    @Query("DELETE FROM shots WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM shots WHERE mediaId IN (:mediaIds)")
    suspend fun deleteByMediaIds(mediaIds: List<Long>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun ignore(ignored: Ignored)

    @Query("UPDATE shots SET status = 0, error = NULL WHERE status = 2")
    suspend fun retryFailed()
}
