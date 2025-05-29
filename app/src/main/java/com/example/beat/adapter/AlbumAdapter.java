package com.example.beat.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beat.MainActivity;
import com.example.beat.R;
import com.example.beat.data.entities.AlbumWithSongs;
import com.example.beat.data.entities.LocalSong;
import com.example.beat.ui.AlbumSongsFragment;

import java.util.List;

public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder> {
    private List<AlbumWithSongs> albums;

    public AlbumAdapter(List<AlbumWithSongs> albums) {
        this.albums = albums;
    }

    public void updateAlbums(List<AlbumWithSongs> albums) {
        this.albums = albums;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AlbumViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_album, parent, false);
        return new AlbumViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlbumViewHolder holder, int position) {
        AlbumWithSongs album = albums.get(position);
        holder.albumName.setText(album.album.name);
        int songCount = album.songs != null ? album.songs.size() : 0;
        holder.songCount.setText(String.format("%d songs", songCount));
        // No need to bind adapter here, it's handled in ViewHolder constructor
    }

    @Override
    public int getItemCount() {
        return albums != null ? albums.size() : 0;
    }

    static class AlbumViewHolder extends RecyclerView.ViewHolder {
        TextView albumName;
        TextView songCount;

        AlbumViewHolder(@NonNull View itemView) {
            super(itemView);
            albumName = itemView.findViewById(R.id.album_name);
            songCount = itemView.findViewById(R.id.song_count);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    RecyclerView parentRecyclerView = (RecyclerView) itemView.getParent();
                    AlbumAdapter adapter = (AlbumAdapter) parentRecyclerView.getAdapter();
                    AlbumWithSongs album = adapter.albums.get(position);
                    AlbumSongsFragment fragment = AlbumSongsFragment.newInstance(album);
                    ((MainActivity) itemView.getContext()).navigateToFragment(fragment);
                }
            });
        }


    }}
