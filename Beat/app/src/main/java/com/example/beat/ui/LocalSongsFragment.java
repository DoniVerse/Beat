package com.example.beat.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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
    private List<LocalSong> allSongs;  // Store all songs for filtering

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
        setupSearchView(view);
        setupRecyclerView();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        songAdapter = new SongAdapter(new ArrayList<>());
        recyclerView.setAdapter(songAdapter);
        loadSongs();
    }

    private void setupSearchView(View view) {
        androidx.appcompat.widget.SearchView searchView = view.findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (allSongs != null) {
                    List<LocalSong> filteredSongs = filterSongs(newText);
                    songAdapter.updateSongs(filteredSongs);
                }
                return true;
            }
        });
    }

    private List<LocalSong> filterSongs(String query) {
        if (query.isEmpty()) {
            return new ArrayList<>(allSongs);
        }
        query = query.toLowerCase();
        List<LocalSong> filteredList = new ArrayList<>();
        for (LocalSong song : allSongs) {
            if (song.getTitle().toLowerCase().contains(query) ||
                (song.getArtistId() != null && String.valueOf(song.getArtistId()).contains(query))) {
                filteredList.add(song);
            }
        }
        return filteredList;
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

                allSongs = new ArrayList<>(songs);  // Store all songs
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
