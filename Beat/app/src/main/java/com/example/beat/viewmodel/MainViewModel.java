package com.example.beat.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.beat.data.database.AppDatabase;
import com.example.beat.data.dao.MusicDao;
import com.example.beat.data.entities.LocalSong;
import com.example.beat.scanner.MediaStoreScanner;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel for MainActivity following MVVM architecture
 * Handles data operations and business logic for the main screen
 */
public class MainViewModel extends AndroidViewModel {
    private static final String TAG = "MainViewModel";
    
    private final MusicDao musicDao;
    private final ExecutorService executor;
    
    // LiveData for UI state
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> hasPermissions = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> scanProgress = new MutableLiveData<>(0);
    
    public MainViewModel(@NonNull Application application) {
        super(application);
        AppDatabase database = AppDatabase.getInstance(application);
        musicDao = database.musicDao();
        executor = Executors.newFixedThreadPool(2);
    }
    
    // Getters for LiveData
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getHasPermissions() { return hasPermissions; }
    public LiveData<Integer> getScanProgress() { return scanProgress; }
    
    /**
     * Start media scanning process
     */
    public void startMediaScan(int userId) {
        isLoading.setValue(true);
        executor.execute(() -> {
            try {
                MediaStoreScanner scanner = new MediaStoreScanner(
                    getApplication(), 
                    musicDao, 
                    userId
                );
                scanner.scanMusicFiles();
                
                // Update UI on completion
                isLoading.postValue(false);
                scanProgress.postValue(100);
                
            } catch (Exception e) {
                isLoading.postValue(false);
                errorMessage.postValue("Error scanning music files: " + e.getMessage());
            }
        });
    }
    
    /**
     * Update permissions status
     */
    public void setPermissionsGranted(boolean granted) {
        hasPermissions.setValue(granted);
    }
    
    /**
     * Get all local songs for a user
     */
    public LiveData<List<LocalSong>> getUserSongs(int userId) {
        MutableLiveData<List<LocalSong>> songsLiveData = new MutableLiveData<>();
        executor.execute(() -> {
            try {
                List<LocalSong> songs = musicDao.getSongsByUser(userId);
                songsLiveData.postValue(songs);
            } catch (Exception e) {
                errorMessage.postValue("Error loading songs: " + e.getMessage());
            }
        });
        return songsLiveData;
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
