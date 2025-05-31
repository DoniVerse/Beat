package com.example.beat.viewmodel;

import android.app.Application;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.beat.R;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class PlayerViewModel extends AndroidViewModel {
    private static final String TAG = "PlayerViewModel";

    private final MutableLiveData<String> trackTitle = new MutableLiveData<>();
    private final MutableLiveData<String> artistName = new MutableLiveData<>();
    private final MutableLiveData<String> albumArtUrl = new MutableLiveData<>();
    private final MutableLiveData<Integer> currentPosition = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> totalDuration = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> isPlaying = new MutableLiveData<>(false);
    private final MutableLiveData<String> formattedCurrentTime = new MutableLiveData<>("00:00");
    private final MutableLiveData<String> formattedTotalTime = new MutableLiveData<>("00:00");
    private final MutableLiveData<Integer> playPauseButtonRes = new MutableLiveData<>(R.drawable.ic_play_arrow);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    private MediaPlayer mediaPlayer;
    private Handler handler;
    private Runnable updateSeekBarRunnable;
    private String currentStreamUrl = null;
    private boolean isLocal = false;

    public PlayerViewModel(@NonNull Application application) {
        super(application);
        handler = new Handler(Looper.getMainLooper());
        Log.d(TAG, "PlayerViewModel initialized");
    }

    public void loadTrack(Intent intent) {
        if (intent == null) {
            Log.e(TAG, "Null intent received");
            error.setValue("Invalid track data");
            return;
        }

        try {
            String title = intent.getStringExtra("title");
            String artist = intent.getStringExtra("artist");
            String artUrl = intent.getStringExtra("albumArtUrl");
            String streamUrl = intent.getStringExtra("streamUrl");
            isLocal = intent.getBooleanExtra("isLocal", false);

            Log.d(TAG, "Loading " + (isLocal ? "local" : "API") + " track: " + title);

            if (streamUrl == null || streamUrl.isEmpty()) {
                Log.e(TAG, "Invalid stream URL");
                error.setValue("Invalid stream URL");
                return;
            }

            trackTitle.setValue(title != null ? title : "Unknown Title");
            artistName.setValue(artist != null ? artist : "Unknown Artist");
            albumArtUrl.setValue(artUrl);

            if (!streamUrl.equals(currentStreamUrl)) {
                currentStreamUrl = streamUrl;
                setupMediaPlayer(streamUrl);
            } else if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                updatePlaybackState(false);
                updateProgress(mediaPlayer.getCurrentPosition());
                totalDuration.setValue(mediaPlayer.getDuration());
                formattedTotalTime.setValue(formatTime(mediaPlayer.getDuration()));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading track: " + e.getMessage());
            error.setValue("Error loading track: " + e.getMessage());
        }
    }

    private void setupMediaPlayer(String streamUrl) {
        try {
            releasePlayer(); // Clean up existing player

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(getApplication(), Uri.parse(streamUrl));
            
            mediaPlayer.setOnPreparedListener(mp -> {
                try {
                    updatePlaybackState(true);
                    totalDuration.setValue(mp.getDuration());
                    formattedTotalTime.setValue(formatTime(mp.getDuration()));
                    startProgressUpdate();
                    mp.start();
                    Log.d(TAG, "MediaPlayer prepared and started");
                } catch (Exception e) {
                    Log.e(TAG, "Error in onPrepared: " + e.getMessage());
                    error.setValue("Error starting playback");
                }
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                String errorMsg = "MediaPlayer error " + what + ": " + extra;
                Log.e(TAG, errorMsg);
                error.setValue(errorMsg);
                updatePlaybackState(false);
                return true;
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                Log.d(TAG, "Playback completed");
                updatePlaybackState(false);
                stopProgressUpdate();
            });

            mediaPlayer.prepareAsync();
            Log.d(TAG, "Starting async preparation of MediaPlayer");
        } catch (IOException e) {
            Log.e(TAG, "Error setting up MediaPlayer: " + e.getMessage());
            error.setValue("Error loading track: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in setupMediaPlayer: " + e.getMessage());
            error.setValue("Unexpected error: " + e.getMessage());
        }
    }

    private void updatePlaybackState(boolean playing) {
        try {
            isPlaying.setValue(playing);
            playPauseButtonRes.setValue(playing ? R.drawable.ic_pause : R.drawable.ic_play);
            Log.d(TAG, "Playback state updated: " + (playing ? "playing" : "paused"));
        } catch (Exception e) {
            Log.e(TAG, "Error updating playback state: " + e.getMessage());
        }
    }

    public void togglePlayPause() {
        if (mediaPlayer == null) {
            Log.e(TAG, "MediaPlayer is null in togglePlayPause");
            return;
        }

        try {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                stopProgressUpdate();
                Log.d(TAG, "Playback paused");
            } else {
                mediaPlayer.start();
                startProgressUpdate();
                Log.d(TAG, "Playback started");
            }
            updatePlaybackState(mediaPlayer.isPlaying());
        } catch (Exception e) {
            Log.e(TAG, "Error in togglePlayPause: " + e.getMessage());
            error.setValue("Error controlling playback");
        }
    }

    public void seekTo(int position) {
        if (mediaPlayer == null) {
            Log.e(TAG, "MediaPlayer is null in seekTo");
            return;
        }
        try {
            mediaPlayer.seekTo(position);
            Log.d(TAG, "Seeked to position: " + position);
        } catch (Exception e) {
            Log.e(TAG, "Error in seekTo: " + e.getMessage());
            error.setValue("Error seeking track");
        }
    }

    private void startProgressUpdate() {
        stopProgressUpdate();
        updateSeekBarRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        updateProgress(mediaPlayer.getCurrentPosition());
                        handler.postDelayed(this, 1000);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in progress update: " + e.getMessage());
                }
            }
        };
        handler.post(updateSeekBarRunnable);
        Log.d(TAG, "Progress updates started");
    }

    private void stopProgressUpdate() {
        if (updateSeekBarRunnable != null) {
            handler.removeCallbacks(updateSeekBarRunnable);
            Log.d(TAG, "Progress updates stopped");
        }
    }

    private void updateProgress(int progress) {
        try {
            currentPosition.setValue(progress);
            formattedCurrentTime.setValue(formatTime(progress));
        } catch (Exception e) {
            Log.e(TAG, "Error updating progress: " + e.getMessage());
        }
    }

    public void releasePlayer() {
        try {
            stopProgressUpdate();
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
                mediaPlayer = null;
                Log.d(TAG, "MediaPlayer released");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error releasing player: " + e.getMessage());
        }
    }

    public String formatTime(int milliseconds) {
        try {
            return String.format(Locale.getDefault(), "%d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(milliseconds),
                TimeUnit.MILLISECONDS.toSeconds(milliseconds) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds))
            );
        } catch (Exception e) {
            Log.e(TAG, "Error formatting time: " + e.getMessage());
            return "00:00";
        }
    }

    @Override
    protected void onCleared() {
        try {
            super.onCleared();
            releasePlayer();
            handler = null;
            Log.d(TAG, "ViewModel cleared");
        } catch (Exception e) {
            Log.e(TAG, "Error in onCleared: " + e.getMessage());
        }
    }

    // Getters for LiveData
    public LiveData<String> getTrackTitle() { return trackTitle; }
    public LiveData<String> getArtistName() { return artistName; }
    public LiveData<String> getAlbumArtUrl() { return albumArtUrl; }
    public LiveData<Integer> getCurrentPosition() { return currentPosition; }
    public LiveData<Integer> getTotalDuration() { return totalDuration; }
    public LiveData<Boolean> getIsPlaying() { return isPlaying; }
    public LiveData<String> getFormattedCurrentTime() { return formattedCurrentTime; }
    public LiveData<String> getFormattedTotalTime() { return formattedTotalTime; }
    public LiveData<Integer> getPlayPauseButtonRes() { return playPauseButtonRes; }
    public LiveData<String> getError() { return error; }
}



