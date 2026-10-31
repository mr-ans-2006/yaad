package io.github.mrans2006.yaad.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/** The database file itself ("yaad.db"). It lives inside the app's private storage on the phone. */
@Database(entities = [Shot::class, Ignored::class], version = 1, exportSchema = false)
abstract class YaadDatabase : RoomDatabase() {
    abstract fun shots(): ShotDao

    companion object {
        fun create(context: Context): YaadDatabase =
            Room.databaseBuilder(context, YaadDatabase::class.java, "yaad.db").build()
    }
}
