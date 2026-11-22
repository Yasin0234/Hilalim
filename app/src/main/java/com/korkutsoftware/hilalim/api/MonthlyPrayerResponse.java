package com.korkutsoftware.hilalim.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class MonthlyPrayerResponse {
    @SerializedName("data")
    public List<PrayerResponse.Data> data;
}
