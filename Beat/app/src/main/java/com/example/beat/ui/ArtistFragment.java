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
import com.example.beat.adapter.ArtistWithSongsAdapter;
import com.example.beat.data.database.AppDatabase;
import com.example.beat.data.entities.Artist;
import com.example.beat.data.entities.ArtistWithSongs;

import java.util.ArrayList;
import java.util.List;

public class ArtistFragment extends Fragment implements OnQueryTextListener {
    private RecyclerView recyclerView;
    private ArtistWithSongsAdapter artistAdapter;
    private int userId;
    private AppDatabase database;

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
        View view = inflater.inflate(R.layout.fragment_artist, container, false);
        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        SearchView searchView = view.findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(this);
        setupRecyclerView();
        return view;
    }

    private void setupRecyclerView() {
        artistAdapter = new ArtistWithSongsAdapter();
        recyclerView.setAdapter(artistAdapter);
        loadArtists();
    }

    private void loadArtists() {
        new Thread(() -> {
            try {
                database = AppDatabase.getInstance(getContext());
                List<ArtistWithSongs> artists = database.musicDao().getArtistsByUser(userId);
                // Store all artists for filtering
                allArtists = new ArrayList<>(artists);
                requireActivity().runOnUiThread(() -> {
                    artistAdapter.updateArtists(artists);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void refreshArtists() {
        loadArtists();
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (newText == null || newText.isEmpty()) {
            // Show all artists when search is cleared
            artistAdapter.updateArtists(allArtists);
            return true;
        }

        // Filter artists based on search query
        List<ArtistWithSongs> filteredArtists = new ArrayList<>();
        for (ArtistWithSongs artist : allArtists) {
            if (artist.artist != null && artist.artist.name != null && 
                artist.artist.name.toLowerCase().contains(newText.toLowerCase())) {
                filteredArtists.add(artist);
            }
        }
        artistAdapter.updateArtists(filteredArtists);
        return true;
    }

    private List<ArtistWithSongs> allArtists = new ArrayList<>();

    @Override
    public void onResume() {
        super.onResume();
    }
}
