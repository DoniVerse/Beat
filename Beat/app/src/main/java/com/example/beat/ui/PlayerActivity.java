package com.example.beat.ui;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.beat.R;
import com.example.beat.viewmodel.PlayerViewModel;
import com.google.android.material.imageview.ShapeableImageView;

public class PlayerActivity extends AppCompatActivity {

    private PlayerViewModel playerViewModel;
    private ShapeableImageView albumArtImageView;
    private TextView trackTitleTextView;
    private TextView artistNameTextView;
    private SeekBar seekBar;
    private TextView currentTimeTextView;
    private TextView totalTimeTextView;
    private ImageButton playPauseButton;
    private ImageButton previousButton; // Keep for potential future use
    private ImageButton nextButton;     // Keep for potential future use
    private ImageButton shuffleButton;  // Keep for potential future use
    private ImageButton repeatButton;   // Keep for potential future use
    private ImageButton backButton;
    private boolean isUserSeeking = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        // Initialize ViewModel
        playerViewModel = new ViewModelProvider(this).get(PlayerViewModel.class);

        initializeViews();
        setupObservers();
        setupClickListeners();

        // Load track data into ViewModel
        if (savedInstanceState == null) { // Load only once
            playerViewModel.loadTrack(getIntent());
        }
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
        shuffleButton = findViewById(R.id.shuffleButton);

        backButton = findViewById(R.id.backButton);
    }

    private void setupObservers() {
        playerViewModel.getTrackTitle().observe(this, title -> trackTitleTextView.setText(title));
        playerViewModel.getArtistName().observe(this, artist -> artistNameTextView.setText(artist));
        playerViewModel.getAlbumArtUrl().observe(this, url -> {
            if (url != null && !url.isEmpty()) {
                Glide.with(this)
                        .load(url)
                        .placeholder(R.drawable.default_album_art) // Use new default drawable
                        .error(R.drawable.default_album_art)     // Use new default drawable on error
                        .into(albumArtImageView);
            } else {
                // Load default placeholder if URL is null or empty
                Glide.with(this)
                        .load(R.drawable.default_album_art)
                        .into(albumArtImageView);
            }
        });

        playerViewModel.getTotalDuration().observe(this, duration -> seekBar.setMax(duration));
        playerViewModel.getCurrentPosition().observe(this, position -> {
            if (!isUserSeeking) {
                seekBar.setProgress(position);
            }
        });

        playerViewModel.getFormattedCurrentTime().observe(this, time -> currentTimeTextView.setText(time));
        playerViewModel.getFormattedTotalTime().observe(this, time -> totalTimeTextView.setText(time));

        playerViewModel.getPlayPauseButtonRes().observe(this, resId -> playPauseButton.setImageResource(resId));

        // Observe isPlaying if needed for other UI changes, e.g., animations
        // playerViewModel.getIsPlaying().observe(this, playing -> { /* Update UI based on playing state */ });
    }

    private void setupClickListeners() {
        playPauseButton.setOnClickListener(v -> playerViewModel.togglePlayPause());
        backButton.setOnClickListener(v -> finish());

        // Add listeners for other controls if functionality is implemented in ViewModel
        // previousButton.setOnClickListener(v -> playerViewModel.previousTrack());
        // nextButton.setOnClickListener(v -> playerViewModel.nextTrack());
        // shuffleButton.setOnClickListener(v -> playerViewModel.toggleShuffle());
        // repeatButton.setOnClickListener(v -> playerViewModel.toggleRepeat());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int userProgress = 0;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    userProgress = progress;
                    // Update current time text immediately while scrubbing
                    currentTimeTextView.setText(playerViewModel.formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isUserSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isUserSeeking = false;
                playerViewModel.seekTo(userProgress);
            }
        });
    }

    // No need for onDestroy cleanup for MediaPlayer as it's handled in ViewModel's onCleared
}

