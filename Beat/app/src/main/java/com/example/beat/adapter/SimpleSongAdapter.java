package com.example.beat.adapter;

import android.app.AlertDialog;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beat.R;
import com.example.beat.data.entities.LocalSong;
import com.example.beat.ui.PlayerActivityWithService;
// import com.example.beat.utils.PlaylistHelper; // TODO: Add when PlaylistHelper is available

import java.util.ArrayList;
import java.util.List;

public class SimpleSongAdapter extends RecyclerView.Adapter<SimpleSongAdapter.SongViewHolder> {
    private List<LocalSong> songs;
    private OnSongActionListener actionListener;

    public interface OnSongActionListener {
        void onDeleteSong(LocalSong song, int position);
        void onAddSongToPlaylist(LocalSong song);
    }

    public SimpleSongAdapter() {
        this.songs = new ArrayList<>();
    }

    public SimpleSongAdapter(List<LocalSong> songs) {
        this.songs = songs != null ? new ArrayList<>(songs) : new ArrayList<>();
    }

    public void updateSongs(List<LocalSong> newSongs) {
        this.songs.clear();
        this.songs.addAll(newSongs);
        notifyDataSetChanged();
    }

    public void setOnSongActionListener(OnSongActionListener actionListener) {
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_song, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        LocalSong song = songs.get(position);
        holder.bind(song);
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    class SongViewHolder extends RecyclerView.ViewHolder {
        private final TextView songTitle;
        private final TextView songArtist;
        private final ImageButton btnOptions;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            songTitle = itemView.findViewById(R.id.song_title);
            songArtist = itemView.findViewById(R.id.song_artist);
            btnOptions = itemView.findViewById(R.id.btn_options);
            
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    LocalSong clickedSong = songs.get(position);
                    // Use PlayerActivityWithService for background playback
                    Intent intent = new Intent(itemView.getContext(), PlayerActivityWithService.class);
                    intent.putExtra("title", clickedSong.getTitle());
                    intent.putExtra("artist", clickedSong.getArtistId() != null ? clickedSong.getArtistId() : "Unknown Artist");
                    intent.putExtra("albumArtUrl", ""); // No album art for now
                    intent.putExtra("streamUrl", clickedSong.getFilePath()); // Use file path for local songs

                    // Pass the full song list for playlist navigation
                    java.util.ArrayList<String> songList = new java.util.ArrayList<>();
                    java.util.ArrayList<String> titleList = new java.util.ArrayList<>();
                    java.util.ArrayList<String> artistList = new java.util.ArrayList<>();
                    java.util.ArrayList<String> albumArtList = new java.util.ArrayList<>();

                    for (LocalSong song : songs) {
                        songList.add(song.getFilePath());
                        titleList.add(song.getTitle());
                        artistList.add(song.getArtistId() != null ? String.valueOf(song.getArtistId()) : "Unknown Artist");
                        albumArtList.add(""); // No album art for now
                    }

                    intent.putStringArrayListExtra("SONG_LIST", songList);
                    intent.putStringArrayListExtra("TITLE_LIST", titleList);
                    intent.putStringArrayListExtra("ARTIST_LIST", artistList);
                    intent.putStringArrayListExtra("ALBUM_ART_LIST", albumArtList);
                    intent.putExtra("POSITION", position);

                    itemView.getContext().startActivity(intent);
                }
            });

            // Set up option menu click listener
            btnOptions.setOnClickListener(v -> {
                // First show a simple dialog to test if button is working
                new AlertDialog.Builder(v.getContext())
                    .setTitle("DEBUG")
                    .setMessage("Option button clicked! Button is working.")
                    .setPositiveButton("OK", (dialog, which) -> {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            showOptionsMenu(v, songs.get(position), position);
                        }
                    })
                    .show();
            });
        }

        private void showOptionsMenu(View view, LocalSong song, int position) {
            try {
                PopupMenu popup = new PopupMenu(view.getContext(), view);
                popup.getMenuInflater().inflate(R.menu.song_options_menu, popup.getMenu());

                popup.setOnMenuItemClickListener(item -> {
                    int itemId = item.getItemId();
                    if (itemId == R.id.action_delete_song) {
                        // Delete Song
                        new AlertDialog.Builder(view.getContext())
                            .setTitle("Delete Song")
                            .setMessage("Are you sure you want to delete \"" + song.getTitle() + "\"?")
                            .setPositiveButton("Delete", (dialog, which) -> {
                                if (actionListener != null) {
                                    actionListener.onDeleteSong(song, position);
                                } else {
                                    // Show simple dialog instead of toast
                                    new AlertDialog.Builder(view.getContext())
                                        .setTitle("Success")
                                        .setMessage("Song deleted: " + song.getTitle())
                                        .setPositiveButton("OK", null)
                                        .show();
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                        return true;
                    } else if (itemId == R.id.action_add_to_playlist) {
                        // Add to Playlist
                        if (actionListener != null) {
                            actionListener.onAddSongToPlaylist(song);
                        } else {
                            // Show simple dialog instead of toast
                            new AlertDialog.Builder(view.getContext())
                                .setTitle("Success")
                                .setMessage("Add to playlist: " + song.getTitle())
                                .setPositiveButton("OK", null)
                                .show();
                        }
                        return true;
                    }
                    return false;
                });

                popup.show();
            } catch (Exception e) {
                // If PopupMenu fails, show error message
                new AlertDialog.Builder(view.getContext())
                    .setTitle("Error")
                    .setMessage("Could not show menu: " + e.getMessage())
                    .setPositiveButton("OK", null)
                    .show();
            }
        }

        private void showDeleteConfirmation(LocalSong song, int position) {
            new AlertDialog.Builder(itemView.getContext())
                .setTitle("Delete Song")
                .setMessage("Are you sure you want to delete \"" + song.getTitle() + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (actionListener != null) {
                        actionListener.onDeleteSong(song, position);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
        }

        public void bind(LocalSong song) {
            songTitle.setText(song.getTitle());
            if (song.getArtistId() != null) {
                songArtist.setText("Artist: " + song.getArtistId());
            } else {
                songArtist.setText("No Artist");
            }
        }
    }
}
