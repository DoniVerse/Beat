package com.example.beat.data;

import android.content.Context;
import android.util.Log;

import com.example.beat.data.dao.MusicDao;
import com.example.beat.data.entities.Album;
import com.example.beat.data.entities.Artist;
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
        try {
            // First get or create a default user
            User defaultUser = getOrCreateDefaultUser();
            if (defaultUser == null) {
                Log.e(TAG, "Failed to create default user");
                return -1;
            }

            // Set the user ID for the song
            song.setUserId(defaultUser.userId);
            Log.d(TAG, "Setting user ID: " + defaultUser.userId + " for song: " + song.getTitle());

            // Check if song already exists for this user
            if (musicDao.countSongsForUser(song.getFilePath(), song.getUserId()) > 0) {
                Log.i(TAG, "Song already exists for user: " + song.getFilePath());
                return -1;
            }

            // Extract metadata from song title
            String[] metadata = extractSongMetadata(song.getTitle(), song);
            String artistName = metadata[0];
            String albumName = metadata[1];

            // Create artist if it doesn't exist
            if (song.getArtistId() == null) {
                if (artistName == null || artistName.isEmpty()) {
                    artistName = "Unknown Artist";
                }
                
                Artist artist = new Artist();
                artist.name = artistName;
                long artistId = musicDao.insertArtist(artist);
                
                if (artistId <= 0) {
                    Log.e(TAG, "Failed to insert artist: " + artistName);
                    return -1;
                }
                
                song.setArtistId((int) artistId);
                Log.d(TAG, "Created artist: " + artistName + " with ID: " + artistId);
            }

            // Create album if it doesn't exist
            if (song.getAlbumId() == null) {
                if (albumName == null || albumName.isEmpty()) {
                    albumName = "Unknown Album";
                }
                
                Album album = new Album();
                album.name = albumName;
                album.artistId = song.getArtistId();
                
                long albumId = musicDao.insertAlbum(album);
                
                if (albumId <= 0) {
                    Log.e(TAG, "Failed to insert album: " + albumName);
                    return -1;
                }
                
                song.setAlbumId((int) albumId);
                Log.d(TAG, "Created album: " + albumName + " with ID: " + albumId);
            }

            // Insert the song
            long songId = musicDao.insertSong(song);
            if (songId <= 0) {
                Log.e(TAG, "Failed to insert song: " + song.getTitle());
                return -1;
            }
            
            Log.d(TAG, "Successfully inserted song: " + song.getTitle() + " with ID: " + songId);
            return songId;
            
        } catch (Exception e) {
            Log.e(TAG, "Error inserting song: " + song.getTitle(), e);
            return -1;
        }
    }

    /**
     * Extract artist and album name from song title using various patterns
     * @param title The song title
     * @return Array containing [artistName, albumName]
     */


    private String[] extractSongMetadata(String title, LocalSong song) {
        String artistName = null;
        String albumName = null;
        
        try {
            // Try different patterns to extract metadata
            // Pattern 1: Artist - Album - Song
            if (title.contains(" - ")) {
                String[] parts = title.split(" - ", 3);
                if (parts.length >= 2) {
                    artistName = parts[0];
                    if (parts.length == 3) {
                        albumName = parts[1];
                    }
                }
            }
            
            // If no album name found, try to extract from file path
            if (albumName == null || albumName.isEmpty()) {
                String filePath = song.getFilePath();
                if (filePath != null) {
                    int lastSlash = filePath.lastIndexOf('/');
                    if (lastSlash != -1) {
                        int secondLastSlash = filePath.lastIndexOf('/', lastSlash - 1);
                        if (secondLastSlash != -1) {
                            albumName = filePath.substring(secondLastSlash + 1, lastSlash);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            Log.w(TAG, "Error extracting metadata from title: " + title, e);
        }
        
        return new String[]{artistName, albumName};
    }

    /**
     * Get or create a default user if none exists
     * @return The default user
     */
    private User getOrCreateDefaultUser() {
        try {
            // Try to get an existing user
            User existingUser = musicDao.getUserByEmailAndPassword("default@beat.com", "default");
            if (existingUser != null) {
                Log.d(TAG, "Found existing default user");
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
                Log.d(TAG, "Created new default user with ID: " + userId);
                return newUser;
            } else {
                Log.e(TAG, "Failed to insert default user");
                return null;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating default user", e);
            return null;
        }
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
