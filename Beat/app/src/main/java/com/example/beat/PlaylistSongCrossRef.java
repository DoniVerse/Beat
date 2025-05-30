package com.example.beat;

import androidx.room.Entity;
    @Entity(primaryKeys = {"playlistId", "songId"})
    public class PlaylistSongCrossRef {
        public int playlistId;
        public int songId;

        public PlaylistSongCrossRef(int playlistId, int songId) {
            this.playlistId = playlistId;
            this.songId = songId;
        }
    }


