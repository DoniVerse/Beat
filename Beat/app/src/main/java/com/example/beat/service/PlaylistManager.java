package com.example.beat.service;

import android.util.Log;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

public class PlaylistManager {
    private static final String TAG = "PlaylistManager";
    private static PlaylistManager instance;
    
    // Playlist data
    private List<String> songList;
    private List<String> titleList;
    private List<String> artistList;
    private List<String> albumArtList;
    private int currentPosition = 0;
    
    // Player state
    private boolean isShuffleEnabled = false;
    private boolean isRepeatEnabled = false;
    
    // Current track info
    private String currentStreamUrl = "";
    private String currentTitle = "";
    private String currentArtist = "";
    private String currentAlbumArt = "";
    
    // Listener for track changes
    public interface PlaylistListener {
        void onTrackChanged(String streamUrl, String title, String artist, String albumArt);
    }
    
    private PlaylistListener listener;
    
    private PlaylistManager() {
        // Private constructor for singleton
    }
    
    public static PlaylistManager getInstance() {
        if (instance == null) {
            instance = new PlaylistManager();
        }
        return instance;
    }
    
    public void setPlaylistListener(PlaylistListener listener) {
        this.listener = listener;
    }
    
    public void setPlaylist(List<String> songList, List<String> titleList, 
                           List<String> artistList, List<String> albumArtList, 
                           int currentPosition) {
        this.songList = new ArrayList<>(songList);
        this.titleList = new ArrayList<>(titleList);
        this.artistList = new ArrayList<>(artistList);
        this.albumArtList = new ArrayList<>(albumArtList);
        this.currentPosition = currentPosition;
        
        Log.d(TAG, "Playlist set with " + songList.size() + " songs, current position: " + currentPosition);
        
        // Update current track info
        updateCurrentTrackInfo();
    }
    
    public void setCurrentTrack(String streamUrl, String title, String artist, String albumArt) {
        this.currentStreamUrl = streamUrl;
        this.currentTitle = title;
        this.currentArtist = artist;
        this.currentAlbumArt = albumArt;
        
        Log.d(TAG, "Current track set: " + title);
    }
    
    public boolean hasPlaylist() {
        return songList != null && !songList.isEmpty();
    }
    
    public boolean canGoNext() {
        return hasPlaylist() || isRepeatEnabled;
    }
    
    public boolean canGoPrevious() {
        return hasPlaylist() || currentPosition > 0;
    }
    
    public void playNext() {
        Log.d(TAG, "playNext called. hasPlaylist: " + hasPlaylist());
        
        if (!hasPlaylist()) {
            // No playlist, just restart current song if repeat is enabled
            if (isRepeatEnabled && listener != null) {
                listener.onTrackChanged(currentStreamUrl, currentTitle, currentArtist, currentAlbumArt);
            }
            return;
        }
        
        if (isShuffleEnabled) {
            // Random song selection
            Random random = new Random();
            currentPosition = random.nextInt(songList.size());
            Log.d(TAG, "Shuffle mode - going to random song at position: " + currentPosition);
        } else {
            currentPosition = (currentPosition + 1) % songList.size();
            Log.d(TAG, "Going to next song at position: " + currentPosition);
        }
        
        updateCurrentTrackInfo();
        notifyTrackChanged();
    }
    
    public void playPrevious() {
        Log.d(TAG, "playPrevious called. hasPlaylist: " + hasPlaylist());
        
        if (!hasPlaylist()) {
            // No playlist, just restart current song
            if (listener != null) {
                listener.onTrackChanged(currentStreamUrl, currentTitle, currentArtist, currentAlbumArt);
            }
            return;
        }
        
        currentPosition = (currentPosition - 1 + songList.size()) % songList.size();
        Log.d(TAG, "Going to previous song at position: " + currentPosition);
        
        updateCurrentTrackInfo();
        notifyTrackChanged();
    }
    
    private void updateCurrentTrackInfo() {
        if (hasPlaylist() && currentPosition >= 0 && currentPosition < songList.size()) {
            currentStreamUrl = songList.get(currentPosition);
            currentTitle = titleList != null && currentPosition < titleList.size() ?
                          titleList.get(currentPosition) : "Unknown Track";
            currentArtist = artistList != null && currentPosition < artistList.size() ?
                           artistList.get(currentPosition) : "Unknown Artist";
            currentAlbumArt = albumArtList != null && currentPosition < albumArtList.size() ?
                             albumArtList.get(currentPosition) : "";
            
            Log.d(TAG, "Updated current track info: " + currentTitle + " at position " + currentPosition);
        }
    }
    
    private void notifyTrackChanged() {
        if (listener != null) {
            listener.onTrackChanged(currentStreamUrl, currentTitle, currentArtist, currentAlbumArt);
        }
    }
    
    // Getters and setters
    public String getCurrentStreamUrl() { return currentStreamUrl; }
    public String getCurrentTitle() { return currentTitle; }
    public String getCurrentArtist() { return currentArtist; }
    public String getCurrentAlbumArt() { return currentAlbumArt; }
    public int getCurrentPosition() { return currentPosition; }
    
    public boolean isShuffleEnabled() { return isShuffleEnabled; }
    public void setShuffleEnabled(boolean enabled) { 
        this.isShuffleEnabled = enabled;
        Log.d(TAG, "Shuffle " + (enabled ? "enabled" : "disabled"));
    }
    
    public boolean isRepeatEnabled() { return isRepeatEnabled; }
    public void setRepeatEnabled(boolean enabled) { 
        this.isRepeatEnabled = enabled;
        Log.d(TAG, "Repeat " + (enabled ? "enabled" : "disabled"));
    }
    
    public int getPlaylistSize() {
        return hasPlaylist() ? songList.size() : 0;
    }

    public void setCurrentPosition(int position) {
        if (hasPlaylist() && position >= 0 && position < songList.size()) {
            this.currentPosition = position;
            updateCurrentTrackInfo();
            Log.d(TAG, "Current position set to: " + position);
        } else {
            Log.w(TAG, "Invalid position or no playlist: " + position);
        }
    }
}
