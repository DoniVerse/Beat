package com.example.beat.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.beat.data.dao.MusicDao;
import com.example.beat.data.entities.*;

@Database(entities = {
        LocalSong.class,
        Artist.class,
        Album.class,
        User.class,
        LocalVideo.class,
        Playlist.class,
        PlaylistSong.class
}, version = 3, exportSchema = false)
@TypeConverters({Converters.class})

public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Remove the old unique index if it exists
            database.execSQL("DROP INDEX IF EXISTS index_local_song_title_filePath_artistId_userId");
            // Create the new unique index
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_local_song_filePath_userId ON local_song (filePath, userId)");
            
            // Add logging to verify migration
            database.execSQL("INSERT INTO room_master_table (id, identity_hash) VALUES(42, 'd2043471d8c3f1752d87c7294fecdfb3')");
        }
    };

    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Add albumArtUri column to album table
            database.execSQL("ALTER TABLE album ADD COLUMN albumArtUri TEXT");
            // Add songCount and artistArtUri columns to artist table
            database.execSQL("ALTER TABLE artist ADD COLUMN songCount INTEGER DEFAULT 0");
            database.execSQL("ALTER TABLE artist ADD COLUMN artistArtUri TEXT");
            
            // Add logging to verify migration
            database.execSQL("INSERT INTO room_master_table (id, identity_hash) VALUES(42, '5847d9b7f27d5a985cf4f20cec42733b')");
        }
    };

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase.class,
                    "beat_database"
            )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build();
        }
        return instance;
    }

    public abstract MusicDao musicDao();
}
