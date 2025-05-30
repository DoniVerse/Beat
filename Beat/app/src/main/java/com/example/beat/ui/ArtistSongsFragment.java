package com.example.beat.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beat.R;
import com.example.beat.adapter.SongAdapter;
import com.example.beat.data.entities.ArtistWithSongs;

import java.util.List;

public class ArtistSongsFragment extends Fragment {
    private TextView artistName;
    private RecyclerView recyclerView;
    private SongAdapter songAdapter;
    private ArtistWithSongs artist;

    public static ArtistSongsFragment newInstance(ArtistWithSongs artist) {
        ArtistSongsFragment fragment = new ArtistSongsFragment();
        Bundle args = new Bundle();
        args.putParcelable("artist", artist);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_artist_songs, container, false);
        artistName = view.findViewById(R.id.artist_name);
        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        setupRecyclerView();
        return view;
    }

    private void setupRecyclerView() {
        if (getArguments() != null) {
            artist = getArguments().getParcelable("artist");
            if (artist != null) {
                artistName.setText(artist.artist.name);
                songAdapter = new SongAdapter(artist.songs);
                recyclerView.setAdapter(songAdapter);
            }
        }
    }

    public void updateArtist(ArtistWithSongs artist) {
        if (songAdapter != null) {
            this.artist = artist;
            artistName.setText(artist.artist.name);
            songAdapter.updateSongs(artist.songs);
        }
    }
}
