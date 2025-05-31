package com.example.beat.ui;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
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
    private ImageButton previousButton;
    private ImageButton nextButton;
    private ImageButton shuffleButton;
    private ImageButton repeatButton;
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
        if (savedInstanceState == null && getIntent() != null) {
            try {
                playerViewModel.loadTrack(getIntent());
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error loading track: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
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
        repeatButton = findViewById(R.id.repeatButton);
        backButton = findViewById(R.id.backButton);
    }

    private void setupObservers() {
        playerViewModel.getTrackTitle().observe(this, title -> 
            trackTitleTextView.setText(title != null ? title : "Unknown Title"));
        
        playerViewModel.getArtistName().observe(this, artist -> 
            artistNameTextView.setText(artist != null ? artist : "Unknown Artist"));
        
        playerViewModel.getAlbumArtUrl().observe(this, url -> {
            try {
                if (url != null && !url.isEmpty()) {
                    Glide.with(this)
                            .load(url)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .placeholder(R.drawable.default_album_art)
                            .error(R.drawable.default_album_art)
                            .into(albumArtImageView);
                } else {
                    Glide.with(this)
                            .load(R.drawable.default_album_art)
                            .into(albumArtImageView);
                }
            } catch (Exception e) {
                e.printStackTrace();
                albumArtImageView.setImageResource(R.drawable.default_album_art);
            }
        });

        playerViewModel.getTotalDuration().observe(this, duration -> {
            if (duration != null) seekBar.setMax(duration);
        });
        
        playerViewModel.getCurrentPosition().observe(this, position -> {
            if (!isUserSeeking && position != null) {
                seekBar.setProgress(position);
            }
        });

        playerViewModel.getFormattedCurrentTime().observe(this, time -> 
            currentTimeTextView.setText(time != null ? time : "00:00"));
        
        playerViewModel.getFormattedTotalTime().observe(this, time -> 
            totalTimeTextView.setText(time != null ? time : "00:00"));

        playerViewModel.getPlayPauseButtonRes().observe(this, resId -> {
            if (resId != null) playPauseButton.setImageResource(resId);
        });

        playerViewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupClickListeners() {
        playPauseButton.setOnClickListener(v -> playerViewModel.togglePlayPause());
        backButton.setOnClickListener(v -> finish());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int userProgress = 0;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    userProgress = progress;
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        playerViewModel.releasePlayer();
    }
}

