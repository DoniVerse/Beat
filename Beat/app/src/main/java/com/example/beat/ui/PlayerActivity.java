package com.example.beat.ui;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.beat.R;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class PlayerActivity extends AppCompatActivity {
    private ImageView albumArtImageView;
    private TextView trackTitleTextView;
    private TextView artistNameTextView;
    private SeekBar seekBar;
    private TextView currentTimeTextView;
    private TextView totalTimeTextView;
    private ImageButton playPauseButton;
    private ImageButton previousButton;
    private ImageButton nextButton;
    private ImageButton backButton;

    private MediaPlayer mediaPlayer;
    private Handler handler;
    private Runnable updateSeekBar;
    private boolean isPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        initializeViews();
        setupClickListeners();
        loadTrackData();
        setupMediaPlayer();
    }

    private void initializeViews() {
        albumArtImageView = findViewById(R.id.albumArtImageView);
        trackTitleTextView = findViewById(R.id.trackTitleTextView);
        artistNameTextView = findViewById(R.id.artistNameTextView);
        seekBar = findViewById(R.id.seekBar);
        currentTimeTextView = findViewById(R.id.currentTimeTextView);
        totalTimeTextView = findViewById(R.id.totalTimeTextView);
        playPauseButton = findViewById(R.id.playPauseButton);
        previousButton = findViewById(R.id.previousButton);
        nextButton = findViewById(R.id.nextButton);
        backButton = findViewById(R.id.backButton);

        handler = new Handler();
    }

    private void setupClickListeners() {
        playPauseButton.setOnClickListener(v -> togglePlayPause());
        backButton.setOnClickListener(v -> finish());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer.seekTo(progress);
                    updateCurrentTimeText(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void loadTrackData() {
        Intent intent = getIntent();
        String title = intent.getStringExtra("title");
        String artist = intent.getStringExtra("artist");
        String albumArtUrl = intent.getStringExtra("albumArtUrl");

        trackTitleTextView.setText(title);
        artistNameTextView.setText(artist);

        if (albumArtUrl != null && !albumArtUrl.isEmpty()) {
            Glide.with(this)
                    .load(albumArtUrl)
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(albumArtImageView);
        }
    }

    private void setupMediaPlayer() {
        String streamUrl = getIntent().getStringExtra("streamUrl");
        if (streamUrl == null || streamUrl.isEmpty()) {
            return;
        }

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(streamUrl);
            mediaPlayer.prepareAsync();

            mediaPlayer.setOnPreparedListener(mp -> {
                int duration = mp.getDuration();
                seekBar.setMax(duration);
                totalTimeTextView.setText(formatTime(duration));
                startPlayback();
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                playPauseButton.setImageResource(android.R.drawable.ic_media_play);
                seekBar.setProgress(0);
                currentTimeTextView.setText(formatTime(0));
                isPlaying = false;
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startPlayback() {
        mediaPlayer.start();
        isPlaying = true;
        playPauseButton.setImageResource(android.R.drawable.ic_media_pause);
        startSeekBarUpdate();
    }

    private void togglePlayPause() {
        if (mediaPlayer != null) {
            if (isPlaying) {
                mediaPlayer.pause();
                playPauseButton.setImageResource(android.R.drawable.ic_media_play);
            } else {
                mediaPlayer.start();
                playPauseButton.setImageResource(android.R.drawable.ic_media_pause);
                startSeekBarUpdate();
            }
            isPlaying = !isPlaying;
        }
    }

    private void startSeekBarUpdate() {
        updateSeekBar = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && isPlaying) {
                    int currentPosition = mediaPlayer.getCurrentPosition();
                    seekBar.setProgress(currentPosition);
                    updateCurrentTimeText(currentPosition);
                    handler.postDelayed(this, 1000);
                }
            }
        };
        handler.post(updateSeekBar);
    }

    private void updateCurrentTimeText(int milliseconds) {
        currentTimeTextView.setText(formatTime(milliseconds));
    }

    private String formatTime(int milliseconds) {
        return String.format(Locale.getDefault(), "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(milliseconds),
                TimeUnit.MILLISECONDS.toSeconds(milliseconds) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds)));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (handler != null) {
            handler.removeCallbacks(updateSeekBar);
        }
    }
}