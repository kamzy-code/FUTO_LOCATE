package io.kamzy.futolocate.Tools;

import java.util.List;

import io.kamzy.futolocate.Models.Events;
import io.kamzy.futolocate.Models.Landmarks;
import io.kamzy.futolocate.Models.Users;

public class DataManager {
    private static DataManager instance;
    private List<Landmarks> allLandmarks;
    private Users users;
    String token;
    List<Events> allEvents;

    private DataManager() {}

    public static synchronized DataManager getInstance() {
        if (instance == null) {
            instance = new DataManager();
        }
        return instance;
    }

    public List<Landmarks> getAllLandmarks() {
        return allLandmarks;
    }

    public void setAllLandmarks(List<Landmarks> allLandmarks) {
        this.allLandmarks = allLandmarks;
    }

    public Users getUsers() {
        return users;
    }

    public void setUsers(Users users) {
        this.users = users;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public List<Events> getAllEvents() {
        return allEvents;
    }

    public void setAllEvents(List<Events> allEvents) {
        this.allEvents = allEvents;
    }
}
