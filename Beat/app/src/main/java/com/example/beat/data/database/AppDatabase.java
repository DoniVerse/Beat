package com.example.beat.data.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.example.beat.data.dao.MusicDao;
import com.example.beat.data.dao.PlaylistDao;
import com.example.beat.data.entities.*;

import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {
        User.class,
        Artist.class,
        Album.class,
        LocalSong.class,
        Playlist.class,
        LocalVideo.class,
        PlaylistSong.class
    },
    version = 4,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;
    private static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Add unique index on email column
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_user_email ON user (email)");
        }
    };

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Add userId column to playlist table
            database.execSQL("ALTER TABLE playlist ADD COLUMN user_id INTEGER NOT NULL DEFAULT 0");
            // Create playlist_song table
            database.execSQL("CREATE TABLE IF NOT EXISTS playlist_song " +
                    "(id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "playlist_id INTEGER NOT NULL, " +
                    "song_id INTEGER NOT NULL, " +
                    "FOREIGN KEY(playlist_id) REFERENCES playlist(playlist_id), " +
                    "FOREIGN KEY(song_id) REFERENCES local_song(song_id))");
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "beat_database"
                    ).allowMainThreadQueries()
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return instance;
    }

    public abstract MusicDao musicDao();
    public abstract PlaylistDao playlistDao();
}
