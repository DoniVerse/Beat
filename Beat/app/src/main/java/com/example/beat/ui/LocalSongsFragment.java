package com.example.beat.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.SearchView.OnQueryTextListener;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beat.R;
import com.example.beat.adapter.SongAdapter;
import com.example.beat.data.database.AppDatabase;
import com.example.beat.data.entities.Album;
import com.example.beat.data.entities.Artist;
import com.example.beat.data.entities.LocalSong;
import com.example.beat.data.dao.MusicDao;

import java.util.ArrayList;
import java.util.List;

public class LocalSongsFragment extends Fragment implements OnQueryTextListener {
    private AppDatabase database;
    private MusicDao musicDao;
    private int userId;
    private RecyclerView recyclerView;
    private SongAdapter songAdapter;
    private SearchView searchView;
    private List<LocalSong> allSongs = new ArrayList<>();

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
        View view = inflater.inflate(R.layout.fragment_local_songs, container, false);
        searchView = view.findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(this);
        return view;
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
                
                allSongs = new ArrayList<>(songs); // Store all songs for filtering
                final List<LocalSong> finalSongs = new ArrayList<>(allSongs);
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

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (newText == null || newText.isEmpty()) {
            // Show all songs when search is cleared
            songAdapter.updateSongs(allSongs);
            return true;
        }

        // Filter songs based on search query
        List<LocalSong> filteredSongs = new ArrayList<>();
        for (LocalSong song : allSongs) {
            // Search by title
            if (song.getTitle() != null && song.getTitle().toLowerCase().contains(newText.toLowerCase())) {
                filteredSongs.add(song);
                continue;
            }

            // Search by artist name using artistId
            if (song.getArtistId() > 0) {
                Artist artist = musicDao.getArtistById(song.getArtistId());
                if (artist != null && artist.name != null && 
                    artist.name.toLowerCase().contains(newText.toLowerCase())) {
                    filteredSongs.add(song);
                    continue;
                }
            }

            // Search by album name using albumId
            if (song.getAlbumId() > 0) {
                Album album = musicDao.getAlbumById(song.getAlbumId());
                if (album != null && album.name != null && 
                    album.name.toLowerCase().contains(newText.toLowerCase())) {
                    filteredSongs.add(song);
                }
            }
        }
        songAdapter.updateSongs(filteredSongs);
        return true;
    }
}
