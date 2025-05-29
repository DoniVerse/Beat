package com.example.beat.data.entities;

import androidx.room.Embedded;
import androidx.room.Relation;
import java.util.List;

public class UserWithPlaylists {
    @Embedded public User user;

    @Relation(
        parentColumn = "userId",
        entityColumn = "userId"
    )
    public List<Playlist> playlists;
}
