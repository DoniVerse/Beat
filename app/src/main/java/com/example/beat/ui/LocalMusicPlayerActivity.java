package com.example.beat.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.beat.R;
import com.example.beat.data.entities.LocalSong;
import com.example.beat.services.MusicService;

import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class LocalMusicPlayerActivity extends AppCompatActivity {
    private TextView songTitle, currentTimeView, totalTimeView;
    private SeekBar seekBar;
    private ImageButton playPauseBtn, prevBtn, nextBtn, shuffleBtn, repeatBtn;
    private ImageView albumArt;

    private List<LocalSong> songList;
    private int currentPosition = 0;
    private boolean isShuffle = false;
    private boolean isRepeat = false;
    private Handler handler = new Handler();
    private MusicService musicService;
    private boolean serviceBound = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.LocalBinder binder = (MusicService.LocalBinder) service;
            musicService = binder.getService();
            serviceBound = true;
            setupPlayer();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    private final Runnable updateSeekBarRunnable = new Runnable() {
        @Override
        public void run() {
            if (serviceBound && musicService != null) {
                int currentPosition = musicService.getCurrentPosition();
                seekBar.setProgress(currentPosition);
                currentTimeView.setText(formatTime(currentPosition));
                handler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_local);

        initializeViews();
        getIntentData();
        bindMusicService();
        setupClickListeners();
    }

    private void initializeViews() {
        songTitle = findViewById(R.id.song_title);
        seekBar = findViewById(R.id.seekBar);
        playPauseBtn = findViewById(R.id.play_pause_btn);
        prevBtn = findViewById(R.id.prev_btn);
        nextBtn = findViewById(R.id.next_btn);
        shuffleBtn = findViewById(R.id.shuffle_btn);
        repeatBtn = findViewById(R.id.repeat_btn);
        currentTimeView = findViewById(R.id.current_time);
        totalTimeView = findViewById(R.id.total_time);
        albumArt = findViewById(R.id.album_art);
    }

    private void getIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            songList = intent.getParcelableArrayListExtra("SONG_LIST");
            currentPosition = intent.getIntExtra("POSITION", 0);
        }
    }

    private void bindMusicService() {
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        startService(intent);
    }

    private void setupClickListeners() {
        playPauseBtn.setOnClickListener(v -> togglePlayPause());
        prevBtn.setOnClickListener(v -> playPrevious());
        nextBtn.setOnClickListener(v -> playNext());
        shuffleBtn.setOnClickListener(v -> toggleShuffle());
        repeatBtn.setOnClickListener(v -> toggleRepeat());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && serviceBound) {
                    musicService.seekTo(progress);
                    currentTimeView.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                handler.removeCallbacks(updateSeekBarRunnable);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (serviceBound) {
                    handler.post(updateSeekBarRunnable);
                }
            }
        });
    }

    private void setupPlayer() {
        if (serviceBound && songList != null && !songList.isEmpty()) {
            musicService.setupPlayer(songList, currentPosition);
            updateSongInfo();
            handler.post(updateSeekBarRunnable);
        }
    }

    private void updateSongInfo() {
        if (songList != null && currentPosition < songList.size()) {
            LocalSong currentSong = songList.get(currentPosition);
            songTitle.setText(currentSong.getTitle());
            if (serviceBound) {
                int duration = musicService.getDuration();
                seekBar.setMax(duration);
                totalTimeView.setText(formatTime(duration));
                playPauseBtn.setImageResource(
                    musicService.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play
                );
            }
        }
    }

    private String formatTime(int milliseconds) {
        return String.format(Locale.getDefault(), "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(milliseconds),
                TimeUnit.MILLISECONDS.toSeconds(milliseconds) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds)));
    }

    private void togglePlayPause() {
        if (serviceBound) {
            musicService.togglePlayPause();
            playPauseBtn.setImageResource(
                musicService.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play
            );
        }
    }

    private void playPrevious() {
        if (serviceBound) {
            musicService.playPrevious();
            updateSongInfo();
        }
    }

    private void playNext() {
        if (serviceBound) {
            if (isShuffle) {
                currentPosition = new Random().nextInt(songList.size());
                musicService.setupPlayer(songList, currentPosition);
            } else {
                musicService.playNext();
            }
            updateSongInfo();
        }
    }

    private void toggleShuffle() {
        isShuffle = !isShuffle;
        shuffleBtn.setImageResource(isShuffle ? R.drawable.ic_shuffle_on : R.drawable.ic_shuffle);
    }

    private void toggleRepeat() {
        isRepeat = !isRepeat;
        repeatBtn.setImageResource(isRepeat ? R.drawable.ic_repeat_on : R.drawable.ic_repeat);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateSeekBarRunnable);
        if (serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
        }
    }
}
