package com.example.beat.viewmodel;

import android.app.Application;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.beat.R;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class PlayerViewModel extends AndroidViewModel {

    private final MutableLiveData<String> trackTitle = new MutableLiveData<>();
    private final MutableLiveData<String> artistName = new MutableLiveData<>();
    private final MutableLiveData<String> albumArtUrl = new MutableLiveData<>();
    private final MutableLiveData<Integer> currentPosition = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> totalDuration = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> isPlaying = new MutableLiveData<>(false);
    private final MutableLiveData<String> formattedCurrentTime = new MutableLiveData<>("00:00");
    private final MutableLiveData<String> formattedTotalTime = new MutableLiveData<>("00:00");
    private final MutableLiveData<Integer> playPauseButtonRes = new MutableLiveData<>(R.drawable.ic_play_arrow); // Use custom drawable

    private MediaPlayer mediaPlayer;
    private Handler handler;
    private Runnable updateSeekBarRunnable;
    private String currentStreamUrl = null;

    public PlayerViewModel(@NonNull Application application) {
        super(application);
        handler = new Handler(Looper.getMainLooper());
    }

    public LiveData<String> getTrackTitle() { return trackTitle; }
    public LiveData<String> getArtistName() { return artistName; }
    public LiveData<String> getAlbumArtUrl() { return albumArtUrl; }
    public LiveData<Integer> getCurrentPosition() { return currentPosition; }
    public LiveData<Integer> getTotalDuration() { return totalDuration; }
    public LiveData<Boolean> getIsPlaying() { return isPlaying; }
    public LiveData<String> getFormattedCurrentTime() { return formattedCurrentTime; }
    public LiveData<String> getFormattedTotalTime() { return formattedTotalTime; }
    public LiveData<Integer> getPlayPauseButtonRes() { return playPauseButtonRes; }

    public void loadTrack(Intent intent) {
        String title = intent.getStringExtra("title");
        String artist = intent.getStringExtra("artist");
        String artUrl = intent.getStringExtra("albumArtUrl");
        String streamUrl = intent.getStringExtra("streamUrl");

        trackTitle.setValue(title);
        artistName.setValue(artist);
        albumArtUrl.setValue(artUrl);

        if (streamUrl != null && !streamUrl.equals(currentStreamUrl)) {
            currentStreamUrl = streamUrl;
            setupMediaPlayer(streamUrl);
        } else if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            // If same track, just update UI state but don't restart
            updatePlaybackState(false);
            updateProgress(mediaPlayer.getCurrentPosition());
            totalDuration.setValue(mediaPlayer.getDuration());
            formattedTotalTime.setValue(formatTime(mediaPlayer.getDuration()));
        }
    }

    private void setupMediaPlayer(String streamUrl) {
        releaseMediaPlayer(); // Release existing player first
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(getApplication(), Uri.parse(streamUrl));
            mediaPlayer.prepareAsync();

            mediaPlayer.setOnPreparedListener(mp -> {
                totalDuration.setValue(mp.getDuration());
                formattedTotalTime.setValue(formatTime(mp.getDuration()));
                startPlayback();
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                updatePlaybackState(false);
                currentPosition.setValue(0);
                formattedCurrentTime.setValue(formatTime(0));
                stopSeekBarUpdate();
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                // Handle errors appropriately
                releaseMediaPlayer();
                updatePlaybackState(false);
                return true; // Indicate error was handled
            });

        } catch (IOException e) {
            e.printStackTrace();
            releaseMediaPlayer();
            // Handle initialization error
        }
    }

    private void startPlayback() {
        if (mediaPlayer != null) {
            mediaPlayer.start();
            updatePlaybackState(true);
            startSeekBarUpdate();
        }
    }

    public void togglePlayPause() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                updatePlaybackState(false);
                stopSeekBarUpdate();
            } else {
                // Check if prepared, might need to prepare if stopped due to error or completion
                mediaPlayer.start();
                updatePlaybackState(true);
                startSeekBarUpdate();
            }
        }
    }

    public void seekTo(int position) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(position);
            updateProgress(position);
        }
    }

    private void updatePlaybackState(boolean playing) {
        isPlaying.setValue(playing);
        playPauseButtonRes.setValue(playing ? R.drawable.ic_pause : R.drawable.ic_play_arrow); // Use custom drawables
    }

    private void startSeekBarUpdate() {
        stopSeekBarUpdate(); // Ensure only one runnable is active
        updateSeekBarRunnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    updateProgress(mediaPlayer.getCurrentPosition());
                    handler.postDelayed(this, 1000);
                }
            }
        };
        handler.post(updateSeekBarRunnable);
    }

    private void stopSeekBarUpdate() {
        if (updateSeekBarRunnable != null) {
            handler.removeCallbacks(updateSeekBarRunnable);
        }
    }

    private void updateProgress(int position) {
        currentPosition.setValue(position);
        formattedCurrentTime.setValue(formatTime(position));
    }

    public String formatTime(int milliseconds) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) -
                TimeUnit.MINUTES.toSeconds(minutes);
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    private void releaseMediaPlayer() {
        stopSeekBarUpdate();
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.reset();
                mediaPlayer.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mediaPlayer = null;
        }
        updatePlaybackState(false);
        currentPosition.setValue(0);
        formattedCurrentTime.setValue(formatTime(0));
        currentStreamUrl = null; // Reset stream URL on release
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        releaseMediaPlayer();
        handler = null; // Avoid leaks
    }
}

