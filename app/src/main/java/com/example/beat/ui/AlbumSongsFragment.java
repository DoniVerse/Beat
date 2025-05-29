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
import com.example.beat.data.entities.AlbumWithSongs;
import com.example.beat.data.entities.LocalSong;

import java.util.List;

public class AlbumSongsFragment extends Fragment {
    private TextView albumName;
    private RecyclerView recyclerView;
    private SongAdapter songAdapter;
    private AlbumWithSongs album;

    public static AlbumSongsFragment newInstance(AlbumWithSongs album) {
        AlbumSongsFragment fragment = new AlbumSongsFragment();
        Bundle args = new Bundle();
        args.putParcelable("album", album);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_album_songs, container, false);
        albumName = view.findViewById(R.id.album_name);
        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        setupRecyclerView();
        return view;
    }

    private void setupRecyclerView() {
        if (getArguments() != null) {
            album = getArguments().getParcelable("album");
            if (album != null) {
                albumName.setText(album.album.name);
                songAdapter = new SongAdapter(album.songs);
                recyclerView.setAdapter(songAdapter);
            }
        }
    }

    public void updateAlbum(AlbumWithSongs album) {
        if (songAdapter != null) {
            this.album = album;
            albumName.setText(album.album.name);
            songAdapter.updateSongs(album.songs);
        }
    }
}
