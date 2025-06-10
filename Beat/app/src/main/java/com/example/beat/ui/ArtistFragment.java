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
import com.example.beat.adapter.ArtistWithSongsAdapter;
import com.example.beat.data.database.AppDatabase;
import com.example.beat.data.entities.Artist;
import com.example.beat.data.entities.ArtistWithSongs;

import java.util.ArrayList;
import java.util.List;

public class ArtistFragment extends Fragment {
    private RecyclerView recyclerView;
    private ArtistWithSongsAdapter artistAdapter;
    private int userId;
    // Make database reference transient to avoid serialization issues
    private transient AppDatabase database;
    private List<ArtistWithSongs> allArtists;  // Store all artists for filtering

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
        artistAdapter = new ArtistWithSongsAdapter();
        recyclerView.setAdapter(artistAdapter);
        loadArtists();
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
                if (allArtists != null) {
                    List<ArtistWithSongs> filteredArtists = filterArtists(newText);
                    artistAdapter.updateArtists(filteredArtists);
                }
                return true;
            }
        });
    }

    private List<ArtistWithSongs> filterArtists(String query) {
        if (query.isEmpty()) {
            return new ArrayList<>(allArtists);
        }
        query = query.toLowerCase();
        List<ArtistWithSongs> filteredList = new ArrayList<>();
        for (ArtistWithSongs artist : allArtists) {
            if (artist.artist.name.toLowerCase().contains(query) ||
                (artist.artist.artistId != 0 && String.valueOf(artist.artist.artistId).contains(query))) {
                filteredList.add(artist);
            }
        }
        return filteredList;
    }

    private void loadArtists() {
        final List<ArtistWithSongs> artists = new ArrayList<>();

        // Get artists for user and manually build ArtistWithSongs objects
        List<Artist> dbArtists = database.musicDao().getArtistsForUser(userId);
        if (dbArtists != null) {
            for (Artist artist : dbArtists) {
                ArtistWithSongs artistWithSongs = new ArtistWithSongs();
                artistWithSongs.artist = artist;
                artistWithSongs.songs = database.musicDao().getSongsByArtistAndUser(artist.artistId, userId);
                artists.add(artistWithSongs);
            }
        }
        allArtists = new ArrayList<>(artists);
        artistAdapter.updateArtists(artists);
    }

    public void refreshArtists() {
        loadArtists();
    }

    @Override
    public void onResume() {
        super.onResume();
    }
}
