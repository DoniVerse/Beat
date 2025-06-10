package com.example.beat.scanner;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import com.example.beat.data.dao.MusicDao;
import com.example.beat.data.entities.Album;
import com.example.beat.data.entities.Artist;
import com.example.beat.data.entities.LocalSong;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MediaStoreScanner {
    private final Context context;
    private final MusicDao musicDao;
    private final int userId;

    public MediaStoreScanner(Context context, MusicDao musicDao, int userId) {
        this.context = context;
        this.musicDao = musicDao;
        this.userId = userId;
    }

    private Album getOrCreateAlbum(String albumName, String year, int artistId) {
        Album album = musicDao.getAlbumByName(albumName, artistId);
        if (album != null) return album;

        Album newAlbum = new Album();
        newAlbum.name = albumName;
        newAlbum.artistId = artistId;
        newAlbum.releaseYear = year;

        try {
            long albumId = musicDao.insertAlbum(newAlbum);
            newAlbum.albumId = (int) albumId;
            Log.d("AlbumInsert", "Created new album: " + albumName + " with ID: " + albumId);
            return newAlbum;
        } catch (Exception e) {
            Log.e("AlbumInsert", "Error inserting album: " + albumName + ", Error: " + e.getMessage());
            return null;
        }
    }

    private Artist getOrCreateArtist(String artistName) {
        Artist artist = musicDao.getArtistByName(artistName);
        if (artist != null) return artist;

        Artist newArtist = new Artist(artistName);
        try {
            long artistId = musicDao.insertArtist(newArtist);
            newArtist.artistId = (int) artistId;
            return newArtist;
        } catch (Exception e) {
            Log.e("ArtistInsert", "Error inserting artist: " + artistName + ", Error: " + e.getMessage());
            return null;
        }
    }

    private Set<String> processedPaths = new HashSet<>();

    public void scanMusicFiles() {
        // Clear processed paths from previous scan
        processedPaths.clear();

        ContentResolver contentResolver = context.getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.ALBUM_ID  // Add album ID to get album art
        };

        // Add filter to only get valid audio files
        String selection = MediaStore.Audio.Media.IS_MUSIC + "=1";
        Cursor cursor = contentResolver.query(uri, projection, selection, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            int titleIndex = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int pathIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
            int artistIndex = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int albumIndex = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
            int durationIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
            int sizeIndex = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE);
            int albumIdIndex = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);

            int insertedCount = 0;
            int totalCount = 0;

            do {
                totalCount++;
                String title = cursor.getString(titleIndex);
                String filePath = cursor.getString(pathIndex);
                String artistName = cursor.getString(artistIndex);
                String albumName = cursor.getString(albumIndex);
                long duration = cursor.getLong(durationIndex);
                long size = cursor.getLong(sizeIndex);
                long albumId = cursor.getLong(albumIdIndex);

                // Get album art URI - try multiple methods
                String albumArtUri = null;

                // Method 1: Try MediaStore album art (most reliable)
                if (albumId > 0) {
                    Uri albumArtContentUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"), albumId);
                    albumArtUri = albumArtContentUri.toString();
                    Log.d("AlbumArt", "✅ Found MediaStore album art for '" + title + "' (Album ID: " + albumId + "): " + albumArtUri);
                }

                // Method 2: If no MediaStore album art, try to use file path for embedded art extraction
                if (albumArtUri == null && filePath != null) {
                    // Store file path as backup for embedded art extraction
                    // We'll use this in the UI layer with a different approach
                    albumArtUri = "file://" + filePath; // Mark as file-based for later processing
                    Log.d("AlbumArt", "🔄 Using file path for embedded art extraction: '" + title + "' -> " + filePath);
                }

                if (albumArtUri == null) {
                    Log.d("AlbumArt", "❌ No album art available for '" + title + "' (Album ID: " + albumId + ", Album: '" + albumName + "')");
                }

                Log.d("SongScan", "Processing song: " + title + " (" + filePath + ")");
                Log.d("SongScan", "Artist: " + artistName + ", Album: " + albumName);
                Log.d("SongScan", "Duration: " + duration + "ms, Size: " + size + "bytes");

                // Skip invalid files (too short or too small)
                if (duration < 1000 || size < 1000) {
                    Log.d("SongInsert", "Skipping invalid file: " + title + " (too short/small)");
                    continue;
                }

                if (filePath == null || title == null) {
                    Log.d("SongInsert", "Skipping invalid file: null title or path");
                    continue;
                }

                // Skip if we've already processed this file in this scan
                if (processedPaths.contains(filePath)) {
                    Log.d("SongInsert", "Skipping already processed: " + title);
                    continue;
                }

                try {
                    Artist artist = getOrCreateArtist(artistName);
                    if (artist == null) {
                        Log.e("SongInsert", "Failed to create artist: " + artistName);
                        continue;
                    }

                    Album album = getOrCreateAlbum(albumName, "", artist.artistId);
                    if (album == null) {
                        Log.e("SongInsert", "Failed to create album: " + albumName);
                        continue;
                    }

                    LocalSong song = new LocalSong();
                    song.setTitle(title);
                    song.setFilePath(filePath);
                    song.setUserId(userId);
                    song.setArtistId(artist.artistId);
                    song.setAlbumId(album.albumId);
                    song.setAlbumArtUri(albumArtUri); // Set the album art URI

                    // Check if song exists with this file path and user using a more efficient method
                    int songCount = musicDao.countSongsForUser(filePath, userId);
                    if (songCount > 0) {
                        Log.d("SongInsert", "Found existing song: " + title + " (user: " + userId + ")");
                        continue;
                    }

                    // Add to processed paths only if we're going to insert it
                    processedPaths.add(filePath);
                    try {
                        musicDao.insertSong(song);
                        insertedCount++;
                        Log.d("SongInsert", "Successfully inserted: " + title);
                    } catch (Exception e) {
                        Log.e("SongInsert", "Error inserting song: " + title + " - " + e.getMessage());
                    } finally {
                        processedPaths.add(filePath);
                    }
                } catch (Exception e) {
                    Log.e("SongInsert", "Error processing song: " + title + " - " + e.getMessage());
                    continue;
                }
            } while (cursor.moveToNext());

            Log.d("MediaScan", "Total found: " + totalCount);
            Log.d("MediaScan", "Total inserted: " + insertedCount);
            Log.d("MediaScan", "Total skipped: " + (totalCount - insertedCount));
            cursor.close();
        } else {
            Log.d("MediaScan", "No music files found.");
        }
    }
}
