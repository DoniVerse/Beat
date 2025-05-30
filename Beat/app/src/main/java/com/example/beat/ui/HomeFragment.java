package com.example.beat.ui;

import static android.content.ContentValues.TAG;
import static android.content.Context.MODE_PRIVATE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.beat.ApiArtist;
import com.example.beat.ApiArtistAdapter;
import com.example.beat.ApiInterface;
import com.example.beat.CircularProfileView;
import com.example.beat.LoginActivity;
import com.example.beat.TrackAdapter;
import com.example.beat.mydata;
import com.example.beat.ui.PlayerActivity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beat.R;
import com.example.beat.Track;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

public class HomeFragment extends Fragment implements TrackAdapter.OnTrackClickListener {
    private RecyclerView recyclerView;
    private ApiInterface apiInterface;
    private SearchView searchView;
    private RecyclerView artistsRecyclerView;
    private RecyclerView tracksRecyclerView;
    private TrackAdapter trackAdapter;
    private View searchResultsLayout;
    private CircularProfileView profileView;
//    private TextView welcomeText;  // Removed welcome text
    private TextView tracks_header;
    private String userEmail;
    private String username;
    private static final String PREFS_NAME = "UserPrefs";  // Match LoginActivity's pref name
    private static final String KEY_EMAIL = "user_email";  // Match LoginActivity's key name
    private BottomNavigationView bottomNavigationView;
    private final List<ApiArtist> popularArtists = Arrays.asList(
            createArtist("Eminem", "https://e-cdns-images.dzcdn.net/images/artist/19cc38f9d69b352f718782e7a22f9c32/250x250-000000-80-0-0.jpg"),
            createArtist("Ed Sheeran", "https://e-cdns-images.dzcdn.net/images/artist/2a03401e091893ec8abd8f15426b1147/250x250-000000-80-0-0.jpg"),
            createArtist("Taylor Swift", "https://e-cdns-images.dzcdn.net/images/artist/8e45f6d855d66828fa80bc9bbb4935ae/250x250-000000-80-0-0.jpg"),
            createArtist("Drake", "https://e-cdns-images.dzcdn.net/images/artist/5d2fa7f140a6bdc2c864c3465a61fc71/250x250-000000-80-0-0.jpg"),
            createArtist("The Weeknd", "https://e-cdns-images.dzcdn.net/images/artist/033d460f704896c9caca89a1d753a137/250x250-000000-80-0-0.jpg"),
            createArtist("Rihanna", "https://e-cdns-images.dzcdn.net/images/artist/7d514d87a186c02657a8e88a84de36f2/250x250-000000-80-0-0.jpg")
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        
        // Initialize views


        artistsRecyclerView = view.findViewById(R.id.artists_recycler_view);
        tracksRecyclerView = view.findViewById(R.id.tracks_recycler_view);
//        welcomeText = view.findViewById(R.id.welcome_text);  // Removed welcome text
        searchView = view.findViewById(R.id.searchView);
        searchResultsLayout = view.findViewById(R.id.search_results_layout);
        profileView = view.findViewById(R.id.profile_view);
        SharedPreferences prefs = getActivity().getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        userEmail = prefs.getString(KEY_EMAIL, "");
        Log.d(TAG, "User email from prefs: " + userEmail);

        // Set profile circle letter
        if (!userEmail.isEmpty()) {
            String firstLetter = userEmail.substring(0, 1).toUpperCase();
            profileView.setLetter(firstLetter);
        } else {
            profileView.setLetter("?");
        }
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (!query.trim().isEmpty()) {
                    searchTracks(query);
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        
        // Set up RecyclerViews
        setupRecyclerViews();
        
        // Set up Deezer API
        setupDeezerApi();
        
        // Set up search
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchTracks(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        
        // Set up profile menu
        profileView.setOnClickListener(v -> showProfileMenu(v));
        
        return view;
    }

    private void setupRecyclerViews() {
        // Setup Artists RecyclerView
        artistsRecyclerView.setLayoutManager(new GridLayoutManager(requireActivity(), 2));
        ApiArtistAdapter artistAdapter = new ApiArtistAdapter(popularArtists, artist -> searchTracks(artist.getName()));
        artistsRecyclerView.setAdapter(artistAdapter);

        // Setup Tracks RecyclerView
        tracksRecyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));
        trackAdapter = new TrackAdapter(new ArrayList<>(), this);
        tracksRecyclerView.setAdapter(trackAdapter);
    }

    private void setupDeezerApi() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://deezerdevs-deezer.p.rapidapi.com/")
                .addConverterFactory(JacksonConverterFactory.create())
                .build();

        apiInterface = retrofit.create(ApiInterface.class);
    }

    private ApiArtist createArtist(String name, String pictureUrl) {
        ApiArtist artist = new ApiArtist();
        artist.setName(name);
        artist.setPictureMedium(pictureUrl);
        return artist;
    }

    public void onTrackClick(Track track) {
        if (track.getPreview() != null) {
            // Launch PlayerActivity
            Intent intent = new Intent(requireActivity(), PlayerActivity.class);
            intent.putExtra("title", track.getTitle());
            intent.putExtra("artist", track.getArtist().getName());
            intent.putExtra("albumArtUrl", track.getAlbum().getCover());
            intent.putExtra("streamUrl", track.getPreview());
            startActivity(intent);
        } else {
            Toast.makeText(requireActivity(), "No preview available for this track", Toast.LENGTH_SHORT).show();
        }
    }

//    private void setupRecyclerViews() {
//        // Setup Artists RecyclerView
//        artistsRecyclerView.setLayoutManager(new GridLayoutManager(requireActivity(), 2));
//        ApiArtistAdapter artistAdapter = new ApiArtistAdapter(popularArtists, artist -> searchTracks(artist.getName()));
//        artistsRecyclerView.setAdapter(artistAdapter);
//
//        // Setup Tracks RecyclerView
//        tracksRecyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));
//        trackAdapter = new TrackAdapter(new ArrayList<>(), this);
//        tracksRecyclerView.setAdapter(trackAdapter);
//    }

//    private void showCreatePlaylistDialog() {
//        CreatePlaylistDialogFragment dialog = new CreatePlaylistDialogFragment();
//        dialog.show(getSupportFragmentManager(), "create_playlist_dialog");
//    }

    private void showCreateAlbumDialog() {
        // TODO: Implement album creation dialog
        Toast.makeText(requireActivity(), "Create Album coming soon!", Toast.LENGTH_SHORT).show();
    }

    private void showCreateStationDialog() {
        // TODO: Implement station creation dialog
        Toast.makeText(requireActivity(), "Create Station coming soon!", Toast.LENGTH_SHORT).show();
    }

    private void showProfileMenu(View v) {
        PopupMenu popupMenu = new PopupMenu(requireActivity(), v);
        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.profile_menu, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menu_logout) {
                logout();
                return true;
            }
            return false;
        });

        popupMenu.show();
    }
    private void logout() {
        // Clear user session
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();

        // Navigate back to LoginActivity
        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
    private void searchTracks(String query) {
        if (query.trim().isEmpty()) {
            Toast.makeText(requireActivity(), "Please enter a search term", Toast.LENGTH_SHORT).show();
            return;
        }

        // Hide only the artists RecyclerView and welcome text
        artistsRecyclerView.setVisibility(View.GONE);
//        welcomeText.setVisibility(View.GONE);  // Removed welcome text
        searchResultsLayout.setVisibility(View.VISIBLE);
        tracks_header.setVisibility(View.GONE);
        // Keep tracksRecyclerView visible since it will show the search results
        tracksRecyclerView.setVisibility(View.VISIBLE);

        Call<mydata> retrofitData = apiInterface.searchTracks(query);
        retrofitData.enqueue(new Callback<mydata>() {
            @Override
            public void onResponse(Call<mydata> call, Response<mydata> response) {
                if (response.isSuccessful() && response.body() != null) {
                    mydata data = response.body();
                    if (data.getData() != null && !data.getData().isEmpty()) {
                        trackAdapter.updateTracks(data.getData());
                        Log.d(TAG, "Found " + data.getData().size() + " tracks");
                    } else {
                        Toast.makeText(requireActivity(), "No tracks found", Toast.LENGTH_SHORT).show();
                        trackAdapter.updateTracks(new ArrayList<>());
                    }
                } else {
                    String errorMessage = "Error: " + response.code();
                    Log.e(TAG, errorMessage);
                    Toast.makeText(requireActivity(), errorMessage, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<mydata> call, Throwable t) {
                Log.e(TAG, "Error fetching tracks", t);
                Toast.makeText(requireActivity(), "Error fetching tracks", Toast.LENGTH_SHORT).show();
            }
        });
    }
    public void onBackPressed() {
        if (searchResultsLayout.getVisibility() == View.VISIBLE) {
            // Show everything back
            tracks_header.setVisibility(View.VISIBLE);
            searchResultsLayout.setVisibility(View.GONE);
            artistsRecyclerView.setVisibility(View.VISIBLE);

            tracksRecyclerView.setVisibility(View.VISIBLE);
            searchView.setQuery("", false);
            searchView.clearFocus();
        } else {
            requireActivity().moveTaskToBack(true);
        }
    }
}
