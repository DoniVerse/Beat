package com.example.beat.data;

import android.content.Context;
import android.util.Log;

import com.example.beat.data.dao.MusicDao;
import com.example.beat.data.entities.LocalSong;
import com.example.beat.data.entities.User;
import com.example.beat.data.database.AppDatabase;

import java.util.List;

public class SongManager {
    private static final String TAG = "SongManager";
    private final MusicDao musicDao;
    private final Context context;

    public SongManager(Context context) {
        this.context = context;
        this.musicDao = AppDatabase.getInstance(context).musicDao();
    }

    /**
     * Insert a song into the database with proper user association
     * @param song The song to be inserted
     * @return The ID of the inserted song, or -1 if insertion failed
     */
    public long insertSongWithUser(LocalSong song) {
        // First get or create a default user
        User defaultUser = getOrCreateDefaultUser();
        if (defaultUser == null) {
            Log.e(TAG, "Failed to create default user");
            return -1;
        }

        // Set the user ID for the song
        song.setUserId(defaultUser.userId);

        // Check if song already exists for this user
        if (musicDao.countSongsForUser(song.getFilePath(), song.getUserId()) > 0) {
            Log.i(TAG, "Song already exists for user: " + song.getFilePath());
            return -1;
        }

        // Insert the song
        return musicDao.insertSong(song);
    }

    /**
     * Get or create a default user if none exists
     * @return The default user
     */
    private User getOrCreateDefaultUser() {
        // Try to get an existing user
        User existingUser = musicDao.getUserByEmailAndPassword("default@beat.com", "default");
        if (existingUser != null) {
            return existingUser;
        }

        // Create a new default user
        User newUser = new User();
        newUser.email = "default@beat.com";
        newUser.password = "default"; // In production, this should be hashed
        newUser.username = "Default User";
        
        long userId = musicDao.insertUser(newUser);
        if (userId > 0) {
            newUser.userId = (int) userId;
            return newUser;
        }
        
        return null;
    }

    /**
     * Get all songs for the default user
     * @return List of songs
     */
    public List<LocalSong> getSongsForDefaultUser() {
        User defaultUser = getOrCreateDefaultUser();
        if (defaultUser != null) {
            return musicDao.getSongsByUser(defaultUser.userId);
        }
        return null;
    }
}
