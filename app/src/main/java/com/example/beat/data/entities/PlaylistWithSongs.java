package com.example.beat.data.entities;

import androidx.room.Embedded;
import androidx.room.Relation;
import java.util.List;
import java.io.Serializable;

public class PlaylistWithSongs implements Serializable {
    @Embedded public Playlist playlist;

    @Relation(
        parentColumn = "playlistId",
        entity = LocalSong.class,
        entityColumn = "songId"
    )
    public List<LocalSong> songs;
}
