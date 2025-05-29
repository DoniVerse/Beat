package com.example.beat.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beat.R;
import com.example.beat.adapter.SongAdapter;
import com.example.beat.data.database.AppDatabase;
import com.example.beat.data.entities.LocalSong;
import com.example.beat.data.dao.MusicDao;

import java.util.ArrayList;
import java.util.List;

public class LocalSongsFragment extends Fragment {
    private AppDatabase database;
    private MusicDao musicDao;
    private int userId;
    private RecyclerView recyclerView;
    private SongAdapter songAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userId = requireActivity().getSharedPreferences("UserPrefs", 0)
                .getInt("userId", -1);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_local_songs, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.recycler_view);
        setupRecyclerView();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        songAdapter = new SongAdapter(new ArrayList<>());
        recyclerView.setAdapter(songAdapter);
        loadSongs();
    }

    private void loadSongs() {
        new Thread(() -> {
            try {
                database = AppDatabase.getInstance(requireContext());
                musicDao = database.musicDao();

                List<LocalSong> songs = musicDao.getSongsByUser(userId);
                if (songs == null) {
                    songs = new ArrayList<>();
                }
                
                final List<LocalSong> finalSongs = new ArrayList<>(songs);
                requireActivity().runOnUiThread(() -> {
                    songAdapter.updateSongs(finalSongs);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void refreshSongs() {
        loadSongs();
    }
}
