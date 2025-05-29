package com.example.beat.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "user")
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
