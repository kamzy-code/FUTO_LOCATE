package io.kamzy.futolocate.Models;

import io.kamzy.futolocate.enums.ModeOfTransport;

public class Routes {
    private int route_id;
    private double distance;
    private String path_coordinates;
    private int estimated_time;
    private ModeOfTransport modeOfTransport;


    public Routes(int route_id, double distance, String path_coordinates, int estimated_time, ModeOfTransport modeOfTransport) {
        this.route_id = route_id;
        this.distance = distance;
        this.path_coordinates = path_coordinates;
        this.estimated_time = estimated_time;
        this.modeOfTransport = modeOfTransport;
    }

    public int getRoute_id() {
        return route_id;
    }

    public void setRoute_id(int route_id) {
        this.route_id = route_id;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public String getPath_coordinates() {
        return path_coordinates;
    }

    public void setPath_coordinates(String path_coordinates) {
        this.path_coordinates = path_coordinates;
    }

    public int getEstimated_time() {
        return estimated_time;
    }

    public void setEstimated_time(int estimated_time) {
        this.estimated_time = estimated_time;
    }

    public ModeOfTransport getModeOfTransport() {
        return modeOfTransport;
    }

    public void setModeOfTransport(ModeOfTransport modeOfTransport) {
        this.modeOfTransport = modeOfTransport;
    }
}
