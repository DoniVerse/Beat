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
                MusicDao musicDao = database.musicDao();
                UserWithPlaylists userWithPlaylists = musicDao.getUserWithPlaylists(userId);
                if (userWithPlaylists != null) {
                    List<Playlist> playlists = userWithPlaylists.playlists;
                    List<PlaylistWithSongs> playlistWithSongs = new ArrayList<>();
                    
                    for (Playlist playlist : playlists) {
                        PlaylistWithSongs playlistWithSongsObj = new PlaylistWithSongs();
                        playlistWithSongsObj.playlist = playlist;
                        playlistWithSongsObj.songs = new ArrayList<>(); // Start with empty songs list
                        playlistWithSongs.add(playlistWithSongsObj);
                    }

                    requireActivity().runOnUiThread(() -> {
                        playlistAdapter.updatePlaylists(playlistWithSongs);
                    });
                } else {
                    // User not found or no playlists
                    requireActivity().runOnUiThread(() -> {
                        TextView errorText = new TextView(getContext());
                        errorText.setText("No playlists found");
                        errorText.setGravity(android.view.Gravity.CENTER);
                        errorText.setLayoutParams(new RecyclerView.LayoutParams(
                            RecyclerView.LayoutParams.MATCH_PARENT,
                            RecyclerView.LayoutParams.WRAP_CONTENT
                        ));
                        recyclerView.setLayoutManager(null);
                        recyclerView.setAdapter(null);
                        recyclerView.addView(errorText);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() -> {
                    TextView errorText = new TextView(getContext());
                    errorText.setText("Error loading playlists: " + e.getMessage());
                    errorText.setGravity(android.view.Gravity.CENTER);
                    errorText.setLayoutParams(new RecyclerView.LayoutParams(
                        RecyclerView.LayoutParams.MATCH_PARENT,
                        RecyclerView.LayoutParams.WRAP_CONTENT
                    ));
                    recyclerView.setLayoutManager(null);
                    recyclerView.setAdapter(null);
                    recyclerView.addView(errorText);
                });
            }
        }).start();
    }

    public void refreshPlaylists() {
        loadPlaylists();
    }
}
