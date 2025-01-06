package io.kamzy.futolocate.Models;

import io.kamzy.futolocate.enums.LandmarkCategory;

public class Landmarks {
    int landmark_id;
    String name;
    String description;
    double latitude;
    double longitude;
    LandmarkCategory category;
    String photo_url;

    public Landmarks(int landmark_id, String name, String description, double latitude, double longitude, LandmarkCategory category, String photo_url) {
        this.landmark_id = landmark_id;
        this.name = name;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
        this.category = category;
        this.photo_url = photo_url;
    }

    public int getLandmark_id() {
        return landmark_id;
    }

    public void setLandmark_id(int landmark_id) {
        this.landmark_id = landmark_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public LandmarkCategory getCategory() {
        return category;
    }

    public void setCategory(LandmarkCategory category) {
        this.category = category;
    }

    public String getPhoto_url() {
        return photo_url;
    }

    public void setPhoto_url(String photo_url) {
        this.photo_url = photo_url;
    }
}
