package com.example.beat.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.beat.data.database.AppDatabase;
import com.example.beat.data.dao.MusicDao;
import com.example.beat.data.dao.PlaylistDao;
import com.example.beat.data.entities.LocalSong;
import com.example.beat.data.entities.Playlist;
import com.example.beat.data.entities.PlaylistSong;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel for LocalMusicPlayerActivity following MVVM architecture
 * Handles music playback state and playlist operations
 */
public class LocalMusicViewModel extends AndroidViewModel {
    private static final String TAG = "LocalMusicViewModel";
    
    private final MusicDao musicDao;
    private final PlaylistDao playlistDao;
    private final ExecutorService executor;
    
    // Playback state
    private final MutableLiveData<LocalSong> currentSong = new MutableLiveData<>();
    private final MutableLiveData<List<LocalSong>> playlist = new MutableLiveData<>();
    private final MutableLiveData<Integer> currentPosition = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> isPlaying = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isShuffleEnabled = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isRepeatEnabled = new MutableLiveData<>(false);
    
    // UI state
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    
    public LocalMusicViewModel(@NonNull Application application) {
        super(application);
        AppDatabase database = AppDatabase.getInstance(application);
        musicDao = database.musicDao();
        playlistDao = database.playlistDao();
        executor = Executors.newFixedThreadPool(2);
    }
    
    // Getters for LiveData
    public LiveData<LocalSong> getCurrentSong() { return currentSong; }
    public LiveData<List<LocalSong>> getPlaylist() { return playlist; }
    public LiveData<Integer> getCurrentPosition() { return currentPosition; }
    public LiveData<Boolean> getIsPlaying() { return isPlaying; }
    public LiveData<Boolean> getIsShuffleEnabled() { return isShuffleEnabled; }
    public LiveData<Boolean> getIsRepeatEnabled() { return isRepeatEnabled; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    
    /**
     * Set the current playlist and song
     */
    public void setPlaylist(List<LocalSong> songs, int position) {
        playlist.setValue(songs);
        currentPosition.setValue(position);
        if (songs != null && position >= 0 && position < songs.size()) {
            currentSong.setValue(songs.get(position));
        }
    }
    
    /**
     * Play next song in playlist
     */
    public void playNext() {
        List<LocalSong> songs = playlist.getValue();
        Integer pos = currentPosition.getValue();
        
        if (songs != null && pos != null) {
            int nextPos = (pos + 1) % songs.size();
            currentPosition.setValue(nextPos);
            currentSong.setValue(songs.get(nextPos));
        }
    }
    
    /**
     * Play previous song in playlist
     */
    public void playPrevious() {
        List<LocalSong> songs = playlist.getValue();
        Integer pos = currentPosition.getValue();
        
        if (songs != null && pos != null) {
            int prevPos = pos > 0 ? pos - 1 : songs.size() - 1;
            currentPosition.setValue(prevPos);
            currentSong.setValue(songs.get(prevPos));
        }
    }
    
    /**
     * Toggle play/pause state
     */
    public void togglePlayPause() {
        Boolean playing = isPlaying.getValue();
        isPlaying.setValue(playing == null ? true : !playing);
    }
    
    /**
     * Toggle shuffle mode
     */
    public void toggleShuffle() {
        Boolean shuffle = isShuffleEnabled.getValue();
        isShuffleEnabled.setValue(shuffle == null ? true : !shuffle);
    }
    
    /**
     * Toggle repeat mode
     */
    public void toggleRepeat() {
        Boolean repeat = isRepeatEnabled.getValue();
        isRepeatEnabled.setValue(repeat == null ? true : !repeat);
    }
    
    /**
     * Add current song to a playlist
     */
    public void addToPlaylist(int playlistId) {
        LocalSong song = currentSong.getValue();
        if (song != null) {
            executor.execute(() -> {
                try {
                    PlaylistSong playlistSong = new PlaylistSong();
                    playlistSong.setPlaylistId(playlistId);
                    playlistSong.setSongId(song.getSongId());
                    playlistDao.insertPlaylistSong(playlistSong);
                } catch (Exception e) {
                    errorMessage.postValue("Error adding song to playlist: " + e.getMessage());
                }
            });
        }
    }
    
    /**
     * Get all playlists for a user
     */
    public LiveData<List<Playlist>> getUserPlaylists(int userId) {
        MutableLiveData<List<Playlist>> playlistsLiveData = new MutableLiveData<>();
        executor.execute(() -> {
            try {
                List<Playlist> playlists = playlistDao.getPlaylistsByUser(userId);
                playlistsLiveData.postValue(playlists);
            } catch (Exception e) {
                errorMessage.postValue("Error loading playlists: " + e.getMessage());
            }
        });
        return playlistsLiveData;
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
