package com.korkutsoftware.hilalim.model;

import java.util.List;

public class CityData {
    private String city;
    private List<String> districts;

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public List<String> getDistricts() {
        return districts;
    }

    public void setDistricts(List<String> districts) {
        this.districts = districts;
    }
}