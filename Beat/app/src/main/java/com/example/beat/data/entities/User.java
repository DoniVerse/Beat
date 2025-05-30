package com.example.beat.data.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Index;
import androidx.annotation.NonNull;

@Entity(tableName = "user", indices = {@Index(value = "email", unique = true)})
public class User {
    @PrimaryKey(autoGenerate = true)
    public int userId;

    @NonNull
    public String username;

    @NonNull
    public String email;

    @NonNull
    public String password;
}
