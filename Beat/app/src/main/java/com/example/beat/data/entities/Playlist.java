package com.example.beat.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.example.beat.data.Converters;

import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "playlist")
public class Playlist {
    @PrimaryKey(autoGenerate = true)
    public int playlistId;
    public String name;
    public int userId;
    @TypeConverters(Converters.class)
    private List<Integer> songIds = new ArrayList<>();
    
    public Playlist() {
        this.songIds = new ArrayList<>();
    }
    
    public Playlist(int playlistId, String name, int userId) {
        this.playlistId = playlistId;
        this.name = name;
        this.userId = userId;
        this.songIds = new ArrayList<>();
    }
    
    public int getPlaylistId() {
        return playlistId;
    }

    public void setPlaylistId(int playlistId) {
        this.playlistId = playlistId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Integer> getSongIds() {
        return songIds;
    }

    public void setSongIds(List<Integer> songIds) {
        this.songIds = songIds;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }
}
