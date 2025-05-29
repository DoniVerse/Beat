package com.example.beat;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;

public interface ApiInterface {

    @GET("/search")
    @Headers({
            "x-rapidapi-key: 6d00891479mshcac0a21e8ba6eb3p1557dfjsna1e33e64159b",
            "x-rapidapi-host: deezerdevs-deezer.p.rapidapi.com"
    })
    Call<mydata> searchTracks(@Query("q") String query);
}
