package com.korkutsoftware.hilalim.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface PrayerApiService {
    @GET("v1/timings")
    Call<PrayerResponse> getTimings(
            @Query("latitude") double latitude,
            @Query("longitude") double longitude,
            @Query("method") int method
    );

    @GET("v1/timingsByCity")
    Call<PrayerResponse> getTimingsByCity(
            @Query("city") String city,
            @Query("country") String country,
            @Query("method") int method
    );

    @GET("v1/timingsByAddress")
    Call<PrayerResponse> getTimingsByAddress(
            @Query("address") String address,
            @Query("method") int method
    );
}