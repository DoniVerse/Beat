package com.example.beat.ui;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.beat.R;
import com.example.beat.data.entities.LocalSong;

import java.util.List;
import java.util.Random;

public class PlayerActivityLocal extends AppCompatActivity {
    private TextView titleView;
    private ImageButton playPauseBtn, nextBtn, prevBtn, shuffleBtn, repeatBtn;
    private SeekBar seekBar;
    private MediaPlayer mediaPlayer;
    private Handler handler = new Handler();
    private List<LocalSong> songs;
    private int position = 0;
    private boolean isShuffle = false;
    private boolean isRepeat = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_local);

        titleView = findViewById(R.id.song_title);
        playPauseBtn = findViewById(R.id.play_pause_btn);
        nextBtn = findViewById(R.id.next_btn);
        prevBtn = findViewById(R.id.prev_btn);
        shuffleBtn = findViewById(R.id.shuffle_btn);

        seekBar = findViewById(R.id.seekBar);

        Intent intent = getIntent();
        songs = (List<LocalSong>) intent.getSerializableExtra("song_list");
        position = intent.getIntExtra("position", 0);

        titleView = findViewById(R.id.song_title);
        playPauseBtn = findViewById(R.id.play_pause_btn);
        nextBtn = findViewById(R.id.next_btn);
        prevBtn = findViewById(R.id.prev_btn);
        shuffleBtn = findViewById(R.id.shuffle_btn);

        seekBar = findViewById(R.id.seekBar);

        songs = (List<LocalSong>) getIntent().getSerializableExtra("song_list");
        position = getIntent().getIntExtra("position", 0);

        setupPlayer();

        playPauseBtn.setOnClickListener(v -> togglePlayPause());
        nextBtn.setOnClickListener(v -> playNext());
        prevBtn.setOnClickListener(v -> playPrevious());
        shuffleBtn.setOnClickListener(v -> isShuffle = !isShuffle);
        repeatBtn.setOnClickListener(v -> isRepeat = !isRepeat);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) mediaPlayer.seekTo(progress);
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void setupPlayer() {
        if (mediaPlayer != null) mediaPlayer.release();

        String path = songs.get(position).getFilePath();
        mediaPlayer = MediaPlayer.create(this, Uri.parse(path));
        titleView.setText(songs.get(position).getTitle());
        mediaPlayer.start();
        playPauseBtn.setImageResource(R.drawable.ic_pause);

        seekBar.setMax(mediaPlayer.getDuration());

        mediaPlayer.setOnCompletionListener(mp -> {
            if (isRepeat) setupPlayer();
            else playNext();
        });

        handler.post(updateSeekBarRunnable);
    }

    private void togglePlayPause() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            playPauseBtn.setImageResource(R.drawable.ic_play);
        } else {
            mediaPlayer.start();
            playPauseBtn.setImageResource(R.drawable.ic_pause);
        }
    }

    private void playNext() {
        if (isShuffle) position = new Random().nextInt(songs.size());
        else position = (position + 1) % songs.size();
        setupPlayer();
    }

    private void playPrevious() {
        position = (position - 1 + songs.size()) % songs.size();
        setupPlayer();
    }

    private final Runnable updateSeekBarRunnable = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null) {
                seekBar.setProgress(mediaPlayer.getCurrentPosition());
                handler.postDelayed(this, 500);
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            handler.removeCallbacks(updateSeekBarRunnable);
            mediaPlayer.release();
        }
    }
}
