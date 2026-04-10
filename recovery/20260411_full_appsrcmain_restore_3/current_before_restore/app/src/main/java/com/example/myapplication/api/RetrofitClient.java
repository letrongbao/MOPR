package com.example.myapplication.api;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static final String BASE_URL = "https://nominatim.openstreetmap.org/";
    private static volatile Retrofit retrofit;
    private static volatile NominatimApi nominatimApi;

    public static Retrofit getInstance() {
        if (retrofit == null) {
            synchronized (RetrofitClient.class) {
                if (retrofit == null) {
                    retrofit = new Retrofit.Builder()
                            .baseUrl(BASE_URL)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();
                }
            }
        }
        return retrofit;
    }

    public static NominatimApi getNominatimApi() {
        if (nominatimApi == null) {
            synchronized (RetrofitClient.class) {
                if (nominatimApi == null) {
                    nominatimApi = getInstance().create(NominatimApi.class);
                }
            }
        }
        return nominatimApi;
    }
}
