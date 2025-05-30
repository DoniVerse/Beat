package com.example.beat.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beat.R;
import com.example.beat.adapter.PlaylistAdapter;
import com.example.beat.data.dao.MusicDao;
import com.example.beat.data.dao.PlaylistDao;
import com.example.beat.data.database.AppDatabase;
import com.example.beat.data.entities.Playlist;
import com.example.beat.data.entities.PlaylistWithSongs;
import com.example.beat.data.entities.UserWithPlaylists;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class PlaylistFragment extends Fragment {
    private RecyclerView recyclerView;
    private PlaylistAdapter playlistAdapter;
    private int userId;
    private AppDatabase database;

    private void createNewPlaylist(String name) {
        new Thread(() -> {
            try {
                MusicDao musicDao = database.musicDao();
                Playlist playlist = new Playlist();
                playlist.name = name;
                playlist.userId = userId;
                musicDao.insertPlaylist(playlist);
                
                // Refresh the playlists list
                requireActivity().runOnUiThread(() -> {
                    loadPlaylists();
                });
            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Error creating playlist: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userId = requireActivity().getSharedPreferences("UserPrefs", 0)
                .getInt("userId", -1);
        database = AppDatabase.getInstance(getContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playlist, container, false);
        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        playlistAdapter = new PlaylistAdapter();
        playlistAdapter.setUserId(userId);
        recyclerView.setAdapter(playlistAdapter);
        
        // Add menu button
        FloatingActionButton fab = view.findViewById(R.id.fab_add_playlist);
        if (fab != null) {
            fab.setOnClickListener(v -> {
                EditText input = new EditText(getContext());
                new AlertDialog.Builder(getContext())
                    .setTitle("Create New Playlist")
                    .setMessage("Enter playlist name:")
                    .setView(input)
                    .setPositiveButton("Create", (dialog, which) -> {
                        String name = input.getText().toString();
                        if (!name.isEmpty()) {
                            createNewPlaylist(name);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            });
        }
        
        loadPlaylists();
        return view;
    }

    private void loadPlaylists() {
        new Thread(() -> {
            try {
                PlaylistDao playlistDao = database.playlistDao();
                List<PlaylistWithSongs> playlists = playlistDao.getUserPlaylistsWithSongs(userId);
                
                requireActivity().runOnUiThread(() -> {
                    playlistAdapter.updatePlaylists(playlists);
                });
            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Error loading playlists: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    playlistAdapter.updatePlaylists(new ArrayList<>());
                });
            }
        }).start();
    }

    public void refreshPlaylists() {
        loadPlaylists();
    }
}
