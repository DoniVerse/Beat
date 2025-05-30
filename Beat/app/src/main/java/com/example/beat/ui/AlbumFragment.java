package com.example.beat.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.SearchView.OnQueryTextListener;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beat.R;
import com.example.beat.adapter.AlbumAdapter;
import com.example.beat.data.database.AppDatabase;
import com.example.beat.data.entities.AlbumWithSongs;
import com.example.beat.data.entities.LocalSong;

import java.util.ArrayList;
import java.util.List;

public class AlbumFragment extends Fragment implements OnQueryTextListener {
    private RecyclerView recyclerView;
    private AlbumAdapter albumAdapter;
    private AppDatabase database;
    private int userId;

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
        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        SearchView searchView = view.findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(this);
        setupRecyclerView();
        return view;
    }

    private void setupRecyclerView() {
        albumAdapter = new AlbumAdapter(new ArrayList<>());
        recyclerView.setAdapter(albumAdapter);
        loadAlbums();
    }

    private void loadAlbums() {
        new Thread(() -> {
            try {
                database = AppDatabase.getInstance(getContext());
                List<AlbumWithSongs> albums = database.musicDao().getAlbumsByUser(userId);
                // Store all albums for filtering
                allAlbums = new ArrayList<>(albums);
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

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (newText == null || newText.isEmpty()) {
            // Show all albums when search is cleared
            albumAdapter.updateAlbums(allAlbums);
            return true;
        }

        // Filter albums based on search query
        List<AlbumWithSongs> filteredAlbums = new ArrayList<>();
        for (AlbumWithSongs album : allAlbums) {
            if (album.album != null && album.album.name != null && 
                album.album.name.toLowerCase().contains(newText.toLowerCase())) {
                filteredAlbums.add(album);
            }
        }
        albumAdapter.updateAlbums(filteredAlbums);
        return true;
    }

    private List<AlbumWithSongs> allAlbums = new ArrayList<>();
}
