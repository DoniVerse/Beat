package com.example.beat.service;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;

public class BackgroundMusicService extends Service {
    private static final String TAG = "BackgroundMusicService";
    
    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;
    private String currentTrackTitle = "";
    private String currentArtist = "";

    private final IBinder binder = new MusicBinder();

    public class MusicBinder extends Binder {
        public BackgroundMusicService getService() {
            return BackgroundMusicService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "BackgroundMusicService created");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; // Service will be restarted if killed
    }

    public void playMusic(String streamUrl, String title, String artist, String albumArt) {
        currentTrackTitle = title != null ? title : "Unknown Track";
        currentArtist = artist != null ? artist : "Unknown Artist";

        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }

            mediaPlayer = new MediaPlayer();
            
            // Handle both local files and URLs
            if (streamUrl.startsWith("http://") || streamUrl.startsWith("https://")) {
                // For URLs (like Deezer previews)
                mediaPlayer.setDataSource(streamUrl);
            } else {
                // For local files
                mediaPlayer.setDataSource(streamUrl);
            }
            
            mediaPlayer.prepareAsync();

            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                isPlaying = true;
                Log.d(TAG, "Music started playing: " + currentTrackTitle);
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                isPlaying = false;
                Log.d(TAG, "Music completed");
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer error: " + what + ", " + extra);
                isPlaying = false;
                return true;
            });

        } catch (IOException e) {
            Log.e(TAG, "Error setting up MediaPlayer", e);
        }
    }

    public void pauseMusic() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPlaying = false;
            Log.d(TAG, "Music paused");
        }
    }

    public void resumeMusic() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            isPlaying = true;
            Log.d(TAG, "Music resumed");
        }
    }

    public void stopMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        isPlaying = false;
        Log.d(TAG, "Music stopped");
    }

    public boolean isPlaying() {
        return isPlaying && mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public int getCurrentPosition() {
        if (mediaPlayer != null) {
            return mediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    public int getDuration() {
        if (mediaPlayer != null) {
            return mediaPlayer.getDuration();
        }
        return 0;
    }

    public void seekTo(int position) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(position);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        Log.d(TAG, "BackgroundMusicService destroyed");
    }
}
