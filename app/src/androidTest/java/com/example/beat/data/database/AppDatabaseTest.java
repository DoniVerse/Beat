package com.example.beat.data.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.beat.data.dao.MusicDao;
import com.example.beat.data.entities.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class AppDatabaseTest {
    private AppDatabase database;
    private MusicDao musicDao;

    @Before
    public void createDb() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase.class
        ).allowMainThreadQueries() // Only for testing
        .build();
        musicDao = database.musicDao();
    }

    @After
    public void closeDb() {
        database.close();
    }

    @Test
    public void testUserOperations() {
        // Create user
        User user = new User();
        user.username = "test_user";
        user.email = "test@example.com";
        user.password = "password123";
        long userId = musicDao.insertUser(user);
        assertTrue(userId > 0);

        // Verify user can be retrieved
        User retrievedUser = musicDao.getUserByEmailAndPassword(user.email, user.password);
        assertNotNull(retrievedUser);
        assertEquals(user.username, retrievedUser.username);
    }

    @Test
    public void testArtistOperations() {
        // Create artist
        Artist artist = new Artist();
        artist.name = "Test Artist";
        long artistId = musicDao.insertArtist(artist);
        assertTrue(artistId > 0);

        // Get all artists
        List<Artist> artists = musicDao.getAllArtists();
        assertNotNull(artists);
        assertTrue(artists.size() > 0);
    }

    @Test
    public void testAlbumOperations() {
        // Create album
        Album album = new Album();
        album.name = "Test Album";
        album.releaseYear = "2025";
        long albumId = musicDao.insertAlbum(album);
        assertTrue(albumId > 0);

        // Get all albums
        List<Album> albums = musicDao.getAllAlbums();
        assertNotNull(albums);
        assertTrue(albums.size() > 0);
    }

    @Test
    public void testSongOperations() {
        // Create user
        User user = new User();
        user.username = "song_test";
        user.email = "song@example.com";
        user.password = "password123";
        long userId = musicDao.insertUser(user);

        // Create artist
        Artist artist = new Artist();
        artist.name = "Song Artist";
        long artistId = musicDao.insertArtist(artist);

        // Create album
        Album album = new Album();
        album.name = "Song Album";
        album.releaseYear = "2025";
        long albumId = musicDao.insertAlbum(album);

        // Create song
        LocalSong song = new LocalSong();
        song.title = "Test Song";
        song.filePath = "/test/song.mp3";
        song.userId = (int) userId;
        song.artistId = (int) artistId;
        song.albumId = (int) albumId;
        long songId = musicDao.insertSong(song);
        assertTrue(songId > 0);

        // Verify song is associated with user
        List<LocalSong> userSongs = musicDao.getSongsByUser((int) userId);
        assertNotNull(userSongs);
        assertTrue(userSongs.size() > 0);
    }

    @Test
    public void testPlaylistOperations() {
        // Create user
        User user = new User();
        user.username = "playlist_test";
        user.email = "playlist@example.com";
        user.password = "password123";
        long userId = musicDao.insertUser(user);

        // Create playlist
        Playlist playlist = new Playlist();
        playlist.name = "Test Playlist";
        playlist.userId = (int) userId;
        long playlistId = musicDao.insertPlaylist(playlist);
        assertTrue(playlistId > 0);

        // Create song
        LocalSong song = new LocalSong();
        song.title = "Playlist Song";
        song.filePath = "/test/song.mp3";
        song.userId = (int) userId;
        long songId = musicDao.insertSong(song);
        assertTrue(songId > 0);

        // Add song to playlist
        PlaylistSongCrossRef crossRef = new PlaylistSongCrossRef();
        crossRef.playlistId = (int) playlistId;
        crossRef.songId = (int) songId;
        musicDao.insertPlaylistSongCrossRef(crossRef);

        // Verify relationship
        List<PlaylistSongCrossRef> playlistSongs = musicDao.getPlaylistSongs((int) playlistId);
        assertNotNull(playlistSongs);
        assertTrue(playlistSongs.size() > 0);
    }

    @Test
    public void testRelationshipQueries() {
        // Create user
        User user = new User();
        user.username = "relationship_test";
        user.email = "relationship@example.com";
        user.password = "password123";
        long userId = musicDao.insertUser(user);

        // Create playlist
        Playlist playlist = new Playlist();
        playlist.name = "Test Playlist";
        playlist.userId = (int) userId;
        long playlistId = musicDao.insertPlaylist(playlist);

        // Create song
        LocalSong song = new LocalSong();
        song.title = "Relationship Song";
        song.filePath = "/test/song.mp3";
        song.userId = (int) userId;
        long songId = musicDao.insertSong(song);

        // Add song to playlist
        PlaylistSongCrossRef crossRef = new PlaylistSongCrossRef();
        crossRef.playlistId = (int) playlistId;
        crossRef.songId = (int) songId;
        musicDao.insertPlaylistSongCrossRef(crossRef);

        // Test PlaylistWithSongs relationship
        PlaylistWithSongs playlistWithSongs = musicDao.getPlaylistWithSongs((int) playlistId);
        assertNotNull(playlistWithSongs);
        assertNotNull(playlistWithSongs.songs);
        assertTrue(playlistWithSongs.songs.size() > 0);

        // Test UserWithPlaylists relationship
        UserWithPlaylists userWithPlaylists = musicDao.getUserWithPlaylists((int) userId);
        assertNotNull(userWithPlaylists);
        assertNotNull(userWithPlaylists.playlists);
        assertTrue(userWithPlaylists.playlists.size() > 0);
    }
}
