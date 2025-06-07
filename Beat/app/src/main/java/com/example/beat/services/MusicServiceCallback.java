package com.example.beat.services;

/**
 * Interface for receiving callbacks from MusicService
 */
public interface MusicServiceCallback {
    /**
     * Called when the next song should be played
     */
    void onNext();

    /**
     * Called when the previous song should be played
     */
    void onPrevious();
} 