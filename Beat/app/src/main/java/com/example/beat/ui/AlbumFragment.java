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
import com.example.beat.adapter.AlbumAdapter;
import com.example.beat.data.database.AppDatabase;
import com.example.beat.data.entities.Album;
import com.example.beat.data.entities.AlbumWithSongs;
import com.example.beat.data.entities.LocalSong;

import java.util.ArrayList;
import java.util.List;

public class AlbumFragment extends Fragment {
    private RecyclerView recyclerView;
    private AlbumAdapter albumAdapter;
    private AppDatabase database;
    private int userId;
    private List<AlbumWithSongs> allAlbums;  // Store all albums for filtering

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userId = requireActivity().getSharedPreferences("UserPrefs", 0)
                .getInt("userId", -1);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_album, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        setupSearchView(view);
        setupRecyclerView();
    }

    private void setupRecyclerView() {
        albumAdapter = new AlbumAdapter(new ArrayList<>());
        recyclerView.setAdapter(albumAdapter);
        loadAlbums();
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
                if (allAlbums != null) {
                    List<AlbumWithSongs> filteredAlbums = filterAlbums(newText);
                    albumAdapter.updateAlbums(filteredAlbums);
                }
                return true;
            }
        });
    }

    private List<AlbumWithSongs> filterAlbums(String query) {
        if (query.isEmpty()) {
            return new ArrayList<>(allAlbums);
        }
        query = query.toLowerCase();
        List<AlbumWithSongs> filteredList = new ArrayList<>();
        for (AlbumWithSongs album : allAlbums) {
            if (album.album.name.toLowerCase().contains(query) ||
                (album.album.artistId != 0 && String.valueOf(album.album.artistId).contains(query))) {
                filteredList.add(album);
            }
        }
        return filteredList;
    }

    private void loadAlbums() {
        new Thread(() -> {
            try {
                database = AppDatabase.getInstance(getContext());
                final List<AlbumWithSongs> albums = new ArrayList<>();

                // Get albums for user and manually build AlbumWithSongs objects
                List<Album> dbAlbums = database.musicDao().getAlbumsForUser(userId);
                if (dbAlbums != null) {
                    for (Album album : dbAlbums) {
                        AlbumWithSongs albumWithSongs = new AlbumWithSongs();
                        albumWithSongs.album = album;
                        albumWithSongs.songs = database.musicDao().getSongsByAlbumAndUser(album.albumId, userId);
                        albums.add(albumWithSongs);
                    }
                }
                allAlbums = new ArrayList<>(albums);  // Store all albums
                requireActivity().runOnUiThread(() -> {
                    albumAdapter.updateAlbums(albums);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void refreshAlbums() {
        loadAlbums();
    }
}
