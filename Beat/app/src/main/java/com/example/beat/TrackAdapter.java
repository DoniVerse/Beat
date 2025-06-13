package com.example.beat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.TrackViewHolder> {
    private List<Track> tracks;
    private OnTrackClickListener listener;

    public interface OnTrackClickListener {
        void onTrackClick(Track track);
    }

    public TrackAdapter(List<Track> tracks, OnTrackClickListener listener) {
        this.tracks = tracks;
        this.listener = listener;
    }

    public void updateTracks(List<Track> newTracks) {
        this.tracks = newTracks;
        notifyDataSetChanged();
    }

    // ✅ ADD: Method to get current tracks list
    public List<Track> getTracks() {
        return tracks;
    }

    @NonNull
    @Override
    public TrackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_2, parent, false);
        return new TrackViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrackViewHolder holder, int position) {
        Track track = tracks.get(position);
        holder.titleView.setText(track.getTitle());
        holder.artistView.setText(track.getArtist() != null ? track.getArtist().getName() : "Unknown Artist");

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTrackClick(track);
            }
        });
    }

    @Override
    public int getItemCount() {
        return tracks != null ? tracks.size() : 0;
    }

    static class TrackViewHolder extends RecyclerView.ViewHolder {
        TextView titleView;
        TextView artistView;

        TrackViewHolder(View itemView) {
            super(itemView);
            titleView = itemView.findViewById(android.R.id.text1);
            artistView = itemView.findViewById(android.R.id.text2);
        }
    }
}