package com.example.beat.data.entities;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.room.Embedded;
import androidx.room.Relation;
import java.io.Serializable;
import java.util.List;

public class ArtistWithSongs implements Parcelable {
    @Embedded
    public Artist artist;

    @Relation(
        parentColumn = "artistId",
        entityColumn = "artistId"
    )
    public List<LocalSong> songs;

    // Default constructor required by Room
    public ArtistWithSongs() {
    }

    // Constructor for creating new instances
    public ArtistWithSongs(Artist artist, List<LocalSong> songs) {
        this.artist = artist;
        this.songs = songs;
    }

    // Protected constructor for Parcelable
    protected ArtistWithSongs(Parcel in) {
        artist = in.readParcelable(Artist.class.getClassLoader());
        songs = in.createTypedArrayList(LocalSong.CREATOR);
    }

    public static final Creator<ArtistWithSongs> CREATOR = new Parcelable.Creator<ArtistWithSongs>() {
        @Override
        public ArtistWithSongs createFromParcel(Parcel in) {
            return new ArtistWithSongs(in);
        }

        @Override
        public ArtistWithSongs[] newArray(int size) {
            return new ArtistWithSongs[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(artist, flags);
        dest.writeTypedList(songs);
    }
}
