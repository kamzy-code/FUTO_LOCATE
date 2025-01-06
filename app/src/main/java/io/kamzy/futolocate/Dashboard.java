package io.kamzy.futolocate;

import static io.kamzy.futolocate.Tools.Tools.prepGetRequestWithoutBody;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import io.kamzy.futolocate.Models.Landmarks;
import okhttp3.OkHttpClient;
import okhttp3.Response;

public class Dashboard extends AppCompatActivity {

    MapView mapView;
    Context ctx;
    BottomNavigationView bottomNav;
    OkHttpClient client = new OkHttpClient();
    String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ctx = this;
        mapView = findViewById(R.id.map_view);
        bottomNav = findViewById(R.id.bottom_navigation);
        token = getIntent().getStringExtra("token");


//        Initialize Map
        Configuration.getInstance().setUserAgentValue(getPackageName());
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(17.0);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        GeoPoint futoLocation = new GeoPoint(5.3792, 6.9974);
        mapView.getController().setCenter(futoLocation);

//        get Landmarks from DB
        try {
            getAllLandmarkAPI("api/landmark", token, mapView);
        } catch (IOException | JSONException e) {
            throw new RuntimeException(e);
        }

        // Floating Action Button
        findViewById(R.id.fab_locate).setOnClickListener(v -> {
            // Handle FAB click
        });

        findViewById(R.id.fab_navigate).setOnClickListener(v -> {
            // Handle FAB click
        });

        // Bottom Navigation
        bottomNav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_explore) {
                    // Handle Explore tab
                    return true;
                } else if (itemId == R.id.nav_landmarks) {
                    // Handle Landmark tab
                    return true;
                } else if (itemId == R.id.history) {
                    // Handle History tab
                    return true;
                } else if (itemId == R.id.profile) {
                    // Handle Profile tab
                    return true;
                }
                return false;
            }
        });

    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }






private void getAllLandmarkAPI (String endpoint, String authToken, MapView map) throws IOException, JSONException {
        new Thread(()->{
            try(Response response = client.newCall(prepGetRequestWithoutBody(endpoint, authToken)).execute()){
                int statusCode = response.code();
                Log.i("statusCode", String.valueOf(statusCode));
                if (response.isSuccessful()){
                    JSONArray responseBody = new JSONArray(response.body().string());
                    List<Landmarks> allLandmarks = parseLandmarksUsingGson(String.valueOf(responseBody));
                    runOnUiThread(()->{
                        for (Landmarks landmark : allLandmarks){
                            Log.i("Landmarks", "Name: "+ landmark.getName()+ " Lat: " + landmark.getLatitude()+
                                    " Long: " + landmark.getLongitude());
                            addMarkerToMap(map, landmark.getName(), landmark.getLatitude(), landmark.getLongitude());
                        }
                    });
                }
            }catch (IOException | JSONException e){
                throw new RuntimeException(e);
            }

        }).start();
    }


// convert JSONArray landmarks to List<Landmarks>
    public List<Landmarks> parseLandmarksUsingGson(String jsonArrayString) {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<Landmarks>>() {}.getType();
        return gson.fromJson(jsonArrayString, listType);
    }



    // Method to add landmarks from the DB to the Map
    private void addMarkerToMap(MapView displayedMap, String name, double latitude, double longitude) {
        Marker marker = new Marker(displayedMap);
        marker.setPosition(new GeoPoint(latitude, longitude));
        marker.setTitle(name); // This shows a title when the marker is tapped
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM); // Adjusts the marker's position
        displayedMap.getOverlays().add(marker); // Adds the marker to the map
        displayedMap.invalidate(); // Refreshes the map
    }
}