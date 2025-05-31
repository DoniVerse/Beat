package com.example.beat.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beat.R;
import com.example.beat.adapter.SongAdapter;
import com.example.beat.data.database.AppDatabase;
import com.example.beat.data.entities.PlaylistWithSongs;
import com.example.beat.data.entities.LocalSong;

import java.util.ArrayList;
import java.util.List;

public class PlaylistSongsFragment extends Fragment {
    private TextView playlistName;
    private RecyclerView recyclerView;
    private SongAdapter songAdapter;
    private PlaylistWithSongs playlist;

    public static PlaylistSongsFragment newInstance(PlaylistWithSongs playlist) {
        PlaylistSongsFragment fragment = new PlaylistSongsFragment();
        Bundle args = new Bundle();
        args.putSerializable("playlist", playlist);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playlist_songs, container, false);
        playlistName = view.findViewById(R.id.playlist_name);
        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        // Get playlist from arguments
        if (getArguments() != null) {
            playlist = (PlaylistWithSongs) getArguments().getSerializable("playlist");
            if (playlist != null) {
                playlistName.setText(playlist.playlist.name);
                loadPlaylistSongs();
            }
        }
        
        return view;
    }

    private void loadPlaylistSongs() {
        if (playlist == null || getContext() == null) return;

        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(getContext());
                List<LocalSong> songs = db.musicDao().getPlaylistSongs(playlist.playlist.getPlaylistId());
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (songs != null && !songs.isEmpty()) {
                            songAdapter = new SongAdapter(songs);
                            recyclerView.setAdapter(songAdapter);
                        } else {
                            Toast.makeText(getContext(), "No songs in playlist", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Error loading playlist songs", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        }).start();
    }

    public void updatePlaylist(PlaylistWithSongs playlist) {
        if (songAdapter != null) {
            this.playlist = playlist;
            playlistName.setText(playlist.playlist.name);
            songAdapter.updateSongs(playlist.songs);
        }
    }
}
