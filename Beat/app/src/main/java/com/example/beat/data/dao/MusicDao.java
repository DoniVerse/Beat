package com.example.beat.data.dao;

import androidx.room.*;
import androidx.room.Dao;

import com.example.beat.data.entities.*;
import java.util.List;

@Dao
public interface MusicDao {
    // User operations
    @Insert
    long insertUser(User user);

    @Query("SELECT * FROM user WHERE email = :email AND password = :password")
    User getUserByEmailAndPassword(String email, String password);

    @Query("SELECT * FROM user WHERE email = :email")
    User getUserByEmail(String email);

    @Query("SELECT * FROM user WHERE userId = :userId")
    User getUserById(int userId);

    // Artist operations
    @Insert
    long insertArtist(Artist artist);

    @Query("SELECT * FROM artist")
    List<Artist> getAllArtists();

    @Query("SELECT * FROM artist WHERE artistId = :artistId")
    Artist getArtistById(int artistId);

    @Transaction
    @Query("SELECT DISTINCT artist.* FROM artist " +
            "JOIN local_song ON artist.artistId = local_song.artistId " +
            "WHERE local_song.userId = :userId " +
            "ORDER BY artist.name ASC")
    List<ArtistWithSongs> getArtistsByUser(int userId);

    @Query("SELECT * FROM artist WHERE name = :name")
    Artist getArtistByName(String name);

    // Album operations
    @Insert
    long insertAlbum(Album album);

    @Query("SELECT * FROM album")
    List<Album> getAllAlbums();

    @Query("SELECT * FROM album WHERE name = :name AND artistId = :artistId")
    Album getAlbumByName(String name, int artistId);

    // LocalSong operations
    @Insert
    long insertSong(LocalSong song);

    @Query("SELECT * FROM local_song WHERE userId = :userId")
    List<LocalSong> getSongsByUser(int userId);

    @Query("SELECT COUNT(*) FROM local_song WHERE filePath = :filePath AND userId = :userId")
    int countSongsForUser(String filePath, int userId);

    @Query("SELECT * FROM album WHERE albumId IN " +
            "(SELECT DISTINCT albumId FROM local_song WHERE userId = :userId) " +
            "ORDER BY name ASC")
    List<AlbumWithSongs> getAlbumsByUser(int userId);

    @Query("SELECT * FROM local_song " +
            "JOIN album ON local_song.albumId = album.albumId " +
            "WHERE local_song.albumId = :albumId AND local_song.userId = :userId " +
            "ORDER BY local_song.title ASC")
    List<LocalSong> getSongsByAlbumAndUser(int albumId, int userId);

    @Query("SELECT * FROM local_song WHERE artistId = :artistId AND userId = :userId")
    List<LocalSong> getSongsByArtistAndUser(int artistId, int userId);




    // Playlist operations
    @Insert
    long insertPlaylist(Playlist playlist);

    @Query("SELECT * FROM playlist WHERE userId = :userId")
    List<Playlist> getPlaylistsByUser(int userId);

    // LocalVideo operations
    @Insert
    long insertVideo(LocalVideo video);

    @Query("SELECT * FROM local_video WHERE userId = :userId")
    List<LocalVideo> getVideosByUser(int userId);

    // Relationship queries
    @Transaction
    @Query("SELECT * FROM playlist WHERE playlistId = :playlistId")
    PlaylistWithSongs getPlaylistWithSongs(int playlistId);

    @Transaction
    @Query("SELECT * FROM user WHERE userId = :userId")
    UserWithPlaylists getUserWithPlaylists(int userId);
}
