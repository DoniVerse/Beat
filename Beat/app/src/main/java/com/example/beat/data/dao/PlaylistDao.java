package com.example.beat.data.dao;

import androidx.room.*;

import com.example.beat.data.entities.LocalSong;
import com.example.beat.data.entities.Playlist;
import com.example.beat.data.entities.PlaylistSong;
import com.example.beat.data.entities.PlaylistWithSongs;

import java.util.List;

@Dao
public interface PlaylistDao {
    @Insert
    long insert(Playlist playlist);

    @Insert
    void insertPlaylistSong(PlaylistSong playlistSong);

    @Query("SELECT * FROM playlist WHERE userId = :userId")
    List<Playlist> getPlaylistsByUser(int userId);

    @Query("SELECT * FROM playlist WHERE playlistId = :playlistId")
    Playlist getPlaylistById(int playlistId);

    @Delete
    void delete(Playlist playlist);

    @Update
    void update(Playlist playlist);

    @Query("SELECT * FROM playlist_song WHERE playlistId = :playlistId")
    List<PlaylistSong> getPlaylistSongs(int playlistId);

    @Query("SELECT * FROM local_song WHERE songId IN (SELECT songId FROM playlist_song WHERE playlistId = :playlistId)")
    List<LocalSong> getSongsForPlaylist(int playlistId);

    @Transaction
    @Query("SELECT * FROM playlist WHERE playlistId = :playlistId")
    PlaylistWithSongs getPlaylistWithSongs(int playlistId);

    @Transaction
    @Query("SELECT * FROM playlist WHERE userId = :userId")
    List<PlaylistWithSongs> getUserPlaylistsWithSongs(int userId);

    @Query("SELECT COUNT(*) > 0 FROM playlist_song WHERE playlistId = :playlistId AND songId = :songId")
    boolean isSongInPlaylist(int playlistId, int songId);

    @Query("DELETE FROM playlist_song WHERE playlistId = :playlistId")
    void deletePlaylistSongs(int playlistId);
}
