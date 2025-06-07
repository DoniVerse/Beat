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
import com.example.beat.data.entities.PlaylistWithSongs;
import com.example.beat.data.entities.LocalSong;

import java.util.List;

public class PlaylistSongsFragment extends Fragment {
    private TextView playlistName;
    private RecyclerView recyclerView;
    private SongAdapter songAdapter;
    // Make complex object transient to avoid serialization issues
    private transient PlaylistWithSongs playlist;

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
        setupRecyclerView();
        return view;
    }

    private void setupRecyclerView() {
        if (getArguments() != null) {
            playlist = (PlaylistWithSongs) getArguments().getSerializable("playlist");
            if (playlist != null) {
                playlistName.setText(playlist.playlist.name);
                // Pass playlist context to SongAdapter
                songAdapter = new SongAdapter(playlist.songs, "PLAYLIST_SONGS", playlist.playlist.playlistId);
                recyclerView.setAdapter(songAdapter);
            }
        }
    }

    public void updatePlaylist(PlaylistWithSongs playlist) {
        if (songAdapter != null) {
            this.playlist = playlist;
            playlistName.setText(playlist.playlist.name);
            songAdapter.updateSongs(playlist.songs);
        }
    }
}
