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

public class LocalMusicPlayerActivity extends AppCompatActivity {
    private TextView songTitle;
    private SeekBar seekBar;
    private ImageButton playPauseBtn, prevBtn, nextBtn, shuffleBtn, repeatBtn;

    private LocalSong currentSong;
    private List<LocalSong> songList;
    private int currentPosition = 0;
    private MediaPlayer mediaPlayer;
    private Handler handler = new Handler();
    private boolean isPlaying = false;
    private boolean isShuffle = false;
    private boolean isRepeat = false;

    private Runnable updateSeekBarRunnable = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                seekBar.setProgress(mediaPlayer.getCurrentPosition());
                handler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_local);

        songTitle = findViewById(R.id.song_title);
        seekBar = findViewById(R.id.seekBar);
        playPauseBtn = findViewById(R.id.play_pause_btn);
        prevBtn = findViewById(R.id.prev_btn);
        nextBtn = findViewById(R.id.next_btn);
        shuffleBtn = findViewById(R.id.shuffle_btn);
        repeatBtn = findViewById(R.id.repeat_btn);

        Intent intent = getIntent();
        if (intent != null) {
            songList = intent.getParcelableArrayListExtra("SONG_LIST");
            currentPosition = intent.getIntExtra("POSITION", 0);

            if (songList != null && !songList.isEmpty()) {
                currentSong = songList.get(currentPosition);
                setupPlayer();
            }
        }

        playPauseBtn.setOnClickListener(v -> togglePlayPause());
        prevBtn.setOnClickListener(v -> playPrevious());
        nextBtn.setOnClickListener(v -> playNext());
        shuffleBtn.setOnClickListener(v -> toggleShuffle());
        repeatBtn.setOnClickListener(v -> toggleRepeat());
    }

    private void updateSongInfo() {
        if (currentSong != null) {
            songTitle.setText(currentSong.getTitle());
            if (mediaPlayer != null) {
                seekBar.setMax(mediaPlayer.getDuration());
            }
        }
    }

    private void setupPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }

        if (currentSong != null) {
            String filePath = currentSong.getFilePath();

            try {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setDataSource(this, Uri.parse(filePath));
                mediaPlayer.prepare();
                mediaPlayer.start();
                isPlaying = true;
                playPauseBtn.setImageResource(R.drawable.ic_pause);
                updateSongInfo();
                seekBar.setProgress(0);
                handler.post(updateSeekBarRunnable);

                mediaPlayer.setOnCompletionListener(mp -> {
                    if (isRepeat) {
                        setupPlayer();
                    } else {
                        playNext();
                    }
                });

                seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (fromUser && mediaPlayer != null) {
                            mediaPlayer.seekTo(progress);
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        handler.removeCallbacks(updateSeekBarRunnable);
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        handler.post(updateSeekBarRunnable);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                songTitle.setText("Error playing song");
                if (mediaPlayer != null) {
                    mediaPlayer.release();
                    mediaPlayer = null;
                }
            }
        }
    }

    private void togglePlayPause() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                isPlaying = false;
                playPauseBtn.setImageResource(R.drawable.ic_play);
            } else {
                mediaPlayer.start();
                isPlaying = true;
                playPauseBtn.setImageResource(R.drawable.ic_pause);
            }
        }
    }

    private void playPrevious() {
        if (songList != null && !songList.isEmpty()) {
            currentPosition = (currentPosition - 1 + songList.size()) % songList.size();
            currentSong = songList.get(currentPosition);
            setupPlayer();
        }
    }

    private void playNext() {
        if (songList != null && !songList.isEmpty()) {
            if (isShuffle) {
                currentPosition = new Random().nextInt(songList.size());
            } else {
                currentPosition = (currentPosition + 1) % songList.size();
            }
            currentSong = songList.get(currentPosition);
            setupPlayer();
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
        if (mediaPlayer != null) {
            mediaPlayer.release();
            handler.removeCallbacks(updateSeekBarRunnable);
        }
    }
}
