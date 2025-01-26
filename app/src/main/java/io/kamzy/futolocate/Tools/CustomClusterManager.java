package io.kamzy.futolocate.Tools;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.kamzy.futolocate.ExploreFragment;
import io.kamzy.futolocate.Models.Landmarks;

public class CustomClusterManager {
    private final MapView mapView;
    private final int clusterRadius; // Radius in pixels for clustering
    private final String token;
    ExploreFragment exploreFragment = new ExploreFragment();

    public CustomClusterManager(MapView mapView, int clusterRadius, String token) {
        this.mapView = mapView;
        this.clusterRadius = clusterRadius;
        this.token = token;
    }

    // Main method to handle clustering with Landmark objects
    public void addClusteredMarkers(List<Landmarks> landmarks) {


        int currentZoomLevel = (int) mapView.getZoomLevelDouble();
        int dynamicRadius = calculateDynamicClusterRadius((int) currentZoomLevel);

        // Create a map of GeoPoints and their corresponding Landmarks
        Map<GeoPoint, Landmarks> geoPointToLandmarkMap = new HashMap<>();
        for (Landmarks landmark : landmarks) {
            GeoPoint geoPoint = new GeoPoint(landmark.getLatitude(), landmark.getLongitude());
            geoPointToLandmarkMap.put(geoPoint, landmark);
        }

        // Perform clustering
        Map<GeoPoint, List<GeoPoint>> clusters = clusterLocations(new ArrayList<>(geoPointToLandmarkMap.keySet()), dynamicRadius);

        // Clear existing overlays
        mapView.getOverlays().clear();

        // Add markers or clusters to the map
        for (Map.Entry<GeoPoint, List<GeoPoint>> cluster : clusters.entrySet()) {
            GeoPoint clusterCenter = cluster.getKey();
            List<GeoPoint> clusterPoints = cluster.getValue();

            if (clusterPoints.size() == 1) {
                GeoPoint location = clusterPoints.get(0);
                Landmarks landmark = geoPointToLandmarkMap.get(location);
                addMarker(location, landmark.getName());
            } else {
                addClusterMarker(clusterCenter, clusterPoints.size());
            }
        }

        // Add clusterer overlay to the map
        mapView.invalidate();
    }

    private Map<GeoPoint, List<GeoPoint>> clusterLocations(List<GeoPoint> locations, int radius) {
        Map<GeoPoint, List<GeoPoint>> clusters = new HashMap<>();

        for (GeoPoint location : locations) {
            boolean addedToCluster = false;

            for (GeoPoint clusterCenter : clusters.keySet()) {
                if (location.distanceToAsDouble(clusterCenter) < radius) {
                    clusters.get(clusterCenter).add(location);
                    addedToCluster = true;
                    break;
                }
            }

            if (!addedToCluster) {
                List<GeoPoint> clusterPoints = new ArrayList<>();
                clusterPoints.add(location);
                clusters.put(location, clusterPoints);
            }
        }

        return clusters;
    }

    private void addMarker(GeoPoint location, String name) {
        // Set up the Paint for the text
        Paint paint = new Paint();
        paint.setColor(android.graphics.Color.BLACK);
        paint.setTextSize(30); // Adjust text size as needed
        paint.setTextAlign(Paint.Align.LEFT);

        // Measure the text
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        float textWidth = paint.measureText(name);
        float textHeight = fontMetrics.bottom - fontMetrics.top;

        // Add padding around the text
        int padding = 20; // Adjust padding as needed
        int bitmapWidth = (int) (textWidth + padding * 2);
        int bitmapHeight = (int) (textHeight + padding * 2);

        // Create a Bitmap with enough space for the text
        Bitmap bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Draw the text on the Bitmap
        canvas.drawText(name, padding, padding - fontMetrics.ascent, paint);

        // Create a Drawable from the Bitmap
        BitmapDrawable drawable = new BitmapDrawable(mapView.getContext().getResources(), bitmap);

//        add the marker to the map
        // Create the Marker
        Marker marker = new Marker(mapView);
        marker.setPosition(location);
        marker.setIcon(drawable);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_TOP); // Center the text above the marker
        marker.setTitle(name); // This shows a title when the marker is tapped
        mapView.getOverlays().add(marker); // Adds the marker to the map

        mapView.invalidate(); // Refreshes the map

        marker.setOnMarkerClickListener((marker1, mapView1) -> {
//            exploreFragment.showLandmarkClickDialogue(mapView,name, location.getLatitude(), location.getLongitude(), token);
            return true;
        });
    }

    private void addClusterMarker(GeoPoint clusterCenter, int clusterSize) {
        Marker clusterMarker = new Marker(mapView);
        clusterMarker.setPosition(clusterCenter);
        clusterMarker.setTitle("Cluster with " + clusterSize + " locations");
        clusterMarker.setSubDescription("Zoom in to view individual locations.");
        clusterMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapView.getOverlays().add(clusterMarker);
    }

    private int calculateDynamicClusterRadius(int zoomLevel) {
        if (zoomLevel > 18) return 50;  // High zoom level = smaller cluster radius
        if (zoomLevel > 15) return 100; // Medium zoom level = medium cluster radius
        return 150;                     // Low zoom level = larger cluster radius
    }



}
