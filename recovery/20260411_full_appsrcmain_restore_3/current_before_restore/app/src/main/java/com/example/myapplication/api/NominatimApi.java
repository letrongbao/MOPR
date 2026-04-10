package com.example.myapplication.api;

import com.example.myapplication.domain.NominatimResult;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;

public interface NominatimApi {

    @Headers("User-Agent: NhaTroApp/1.0")
    @GET("search")
    Call<List<NominatimResult>> searchAddress(
            @Query("q") String query,
            @Query("format") String format,
            @Query("limit") int limit
    );
}
