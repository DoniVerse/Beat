package com.example.beat.data.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;
import androidx.room.Index;

@Entity(tableName = "local_video",
    foreignKeys = @ForeignKey(entity = User.class, parentColumns = "userId", childColumns = "userId", onDelete = ForeignKey.CASCADE),
    indices = {@Index("userId")}
)
public class LocalVideo {
    @PrimaryKey(autoGenerate = true)
    public int videoId;

    public String title;
    public String filePath;

    public int userId;
}
