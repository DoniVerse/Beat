package com.example.beat.data.entities;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "artist")
public class Artist implements Parcelable {
    @PrimaryKey(autoGenerate = true)
    public int artistId;
    public String name;
    public int songCount;
    public String artistArtUri;

    // Required empty constructor for Room
    public Artist() {}

    // Constructor for creating new artist
    public Artist(String name) {
        this.name = name;
    }

    // Required for Parcelable
    protected Artist(Parcel in) {
        artistId = in.readInt();
        name = in.readString();
        songCount = in.readInt();
        artistArtUri = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(artistId);
        dest.writeString(name);
        dest.writeInt(songCount);
        dest.writeString(artistArtUri);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Artist> CREATOR = new Creator<Artist>() {
        @Override
        public Artist createFromParcel(Parcel in) {
            return new Artist(in);
        }

        @Override
        public Artist[] newArray(int size) {
            return new Artist[size];
        }
    };
}
