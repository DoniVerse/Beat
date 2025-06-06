package com.example.beat.models;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "local_songs")
public class LocalSong {
    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "title")
    private String title;

    @ColumnInfo(name = "artist_id")
    private long artistId;

    @ColumnInfo(name = "album_id")
    private long albumId;

    @ColumnInfo(name = "file_path")
    private String filePath;

    @ColumnInfo(name = "album_art_uri")
    private String albumArtUri;

    // Constructor
    public LocalSong(String title, long artistId, long albumId, String filePath, String albumArtUri) {
        this.title = title;
        this.artistId = artistId;
        this.albumId = albumId;
        this.filePath = filePath;
        this.albumArtUri = albumArtUri;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getArtistId() {
        return artistId;
    }

    public void setArtistId(long artistId) {
        this.artistId = artistId;
    }

    public long getAlbumId() {
        return albumId;
    }

    public void setAlbumId(long albumId) {
        this.albumId = albumId;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getAlbumArtUri() {
        return albumArtUri;
    }

    public void setAlbumArtUri(String albumArtUri) {
        this.albumArtUri = albumArtUri;
    }

    // Default constructor required by Room
    public LocalSong() {
    }
} 