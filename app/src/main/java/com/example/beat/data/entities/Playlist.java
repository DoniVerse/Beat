package com.example.beat.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.example.beat.data.Converters;

import java.util.List;

@Entity(tableName = "playlist")
public class Playlist {
    @PrimaryKey(autoGenerate = true)
    public int playlistId;
    public String name;
    public int userId;
    
    @TypeConverters(Converters.class)
    public List<Integer> songIds;
}
