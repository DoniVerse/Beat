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
import com.google.android.material.imageview.ShapeableImageView;
import android.net.Uri;

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
        holder.bind(album);
    }

    @Override
    public int getItemCount() {
        return albums != null ? albums.size() : 0;
    }

    class AlbumViewHolder extends RecyclerView.ViewHolder {
        private final TextView albumName;
        private final TextView songCount;
        private final ShapeableImageView albumArt;

        public AlbumViewHolder(@NonNull View itemView) {
            super(itemView);
            albumName = itemView.findViewById(R.id.album_name);
            songCount = itemView.findViewById(R.id.song_count);
            albumArt = itemView.findViewById(R.id.album_art);
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    AlbumWithSongs clickedAlbum = albums.get(position);
                    // Navigate to album songs fragment
                    AlbumSongsFragment fragment = AlbumSongsFragment.newInstance(clickedAlbum);
                    ((MainActivity) itemView.getContext()).navigateToFragment(fragment);
                }
            });
        }

        public void bind(AlbumWithSongs album) {
            // Load album art if available, otherwise use default
            if (album.album != null && album.album.albumArtUri != null) {
                albumArt.setImageURI(Uri.parse(album.album.albumArtUri));
            } else {
                albumArt.setImageResource(R.drawable.default_artist);
            }
        }
    }
}
