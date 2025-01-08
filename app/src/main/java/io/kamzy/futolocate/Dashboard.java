package io.kamzy.futolocate;

import static io.kamzy.futolocate.Tools.Tools.baseURL;
import static io.kamzy.futolocate.Tools.Tools.prepGetRequestWithoutBody;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import io.kamzy.futolocate.Models.Landmarks;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Dashboard extends AppCompatActivity {

    MapView mapView;
    Context ctx;
    BottomNavigationView bottomNav;
    OkHttpClient client = new OkHttpClient();
    String token;
    TextInputLayout searchTextInputLayout, fromTextInputLayout, toTextInputLayout;
    TextInputEditText searchEditText, fromEditText, toEditText;

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
        searchTextInputLayout = findViewById(R.id.searchTextInputLayout);
        fromTextInputLayout = findViewById(R.id.fromTextInputLayout);
        toTextInputLayout = findViewById(R.id.toTextInputLayout);
        searchEditText = findViewById(R.id.searchEditText);
        fromEditText = findViewById(R.id.fromLocationEditText);
        toEditText = findViewById(R.id.toLocationEditText);



//        Initialize Map
        Configuration.getInstance().setUserAgentValue(getPackageName());
        // Configure osmdroid
        File cacheDir = new File(getCacheDir(), "osmdroid_tiles");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        Configuration.getInstance().setOsmdroidTileCache(cacheDir);

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
            if (fromTextInputLayout.getVisibility() == View.GONE && toTextInputLayout.getVisibility() == View.GONE) {
                // Show navigation fields
                searchTextInputLayout.setVisibility(View.GONE);
                fromTextInputLayout.setVisibility(View.VISIBLE);
                toTextInputLayout.setVisibility(View.VISIBLE);
            } else {
                // Revert to normal search field
                searchTextInputLayout.setVisibility(View.VISIBLE);
                fromTextInputLayout.setVisibility(View.GONE);
                toTextInputLayout.setVisibility(View.GONE);
            }
        });

//        normal search action
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH){
                String query = searchEditText.getText().toString().trim();
                if (!query.isEmpty()){
                    performSearch(query, token, mapView);
                    return true;
                }
            } return false;
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
                    List<Landmarks> allLandmarks = parseJSONArrayUsingGson(String.valueOf(responseBody));
                    runOnUiThread(()->{
                        for (Landmarks landmark : allLandmarks){
                            Log.i("Landmarks", "Name: "+ landmark.getName()+ " Lat: " + landmark.getLatitude()+
                                    " Long: " + landmark.getLongitude() + " Cat: " + landmark.getCategory());
                            addMarkerToMap(map, landmark.getName(), landmark.getLatitude(), landmark.getLongitude());
                        }
                    });
                }
            }catch (IOException | JSONException e){
                throw new RuntimeException(e);
            }
        }).start();
    }


    private void searchLandmarkAPI(String endpoint, String query, String authToken, MapView m) throws IOException, JSONException, NullPointerException{
        new Thread(() -> {
//            Build the URL with query parameters
            HttpUrl url = HttpUrl.parse(baseURL+endpoint)
                    .newBuilder()
                    .addQueryParameter("query", query)
                    .build();

            // Build the GET request
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer "+ authToken)
                    .get()
                    .build();
            try(Response response = client.newCall(request).execute()){
                int statusCode = response.code();
                Log.i("statusCode", String.valueOf(statusCode));
                if (response.isSuccessful()){
                        String r = response.body() != null ? response.body().string() : "null";
                        if (r.equals("null")) {
                            performExternalSearchQuery(query, m);
                            return; // Exit the method or handle the error appropriately
                        }else {
                                JSONObject responseBody =  new JSONObject(r);
                                Landmarks l = parseJSONObjectUsingGson(String.valueOf(responseBody));
                                runOnUiThread(() -> {
                                    Log.i("Name: ", l.getName());
                                    // Center map on found location
                                    GeoPoint geoPoint = new GeoPoint(l.getLatitude(), l.getLongitude());
                                    m.getController().setCenter(geoPoint);
                                    m.getController().setZoom(18.0);
                                    // Optionally add a marker
                                    addMarkerToMap(m, l.getName(), l.getLatitude(), l.getLongitude());
                                });
                        }
                } else {
                    Log.e("API Error", "Couldn't perform search");
                }
            }catch (IOException | JSONException | NullPointerException  e){
                throw new RuntimeException(e);
            }
        }).start();
    }


//    perform External search query

    private void performExternalSearchQuery (String query, MapView map) throws IOException{
        OkHttpClient client1 = getUnsafeOkHttpClient();
        String url = "https://nominatim.openstreetmap.org/search?format=json&q=" + Uri.encode(query);

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "FUTOLocate/1.0 (udochukwu.chikezie20191152552@futo.edu.ng)")
                .build();

        new Thread(()->{
            try(Response response = client1.newCall(request).execute()) {
                int statusCode = response.code();
                Log.i("statusCode", String.valueOf(statusCode));
                if (response.isSuccessful()){
                    String r = response.body() != null ? response.body().string() : "null";
                    if (r.equals("null")){
                        Log.i("Search Result", "No external location found");
                    } else {
                        try {
                            JSONArray results = new JSONArray(r);
                            if (results.length() > 0) {
                                JSONObject location = results.getJSONObject(0);
                                double lat = location.getDouble("lat");
                                double lon = location.getDouble("lon");

                                runOnUiThread(() -> {
                                    GeoPoint geoPoint = new GeoPoint(lat, lon);
                                    map.getController().setCenter(geoPoint);
                                    map.getController().setZoom(18.0);
                                    addMarkerToMap(map, query, lat, lon);
                                });
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    Log.i("API Error", "Couldn't perform External Search");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            ;
        }).start();
    }

// convert JSONArray landmarks to List<Landmarks>
    private List<Landmarks> parseJSONArrayUsingGson(String jsonArrayString) {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<Landmarks>>() {}.getType();
        return gson.fromJson(jsonArrayString, listType);
    }


    // convert JSONObject landmarks to List<Landmarks>
    private Landmarks parseJSONObjectUsingGson(String jsonObjectString) {
        Gson gson = new Gson();
        Type type = new TypeToken<Landmarks>() {}.getType();
        return gson.fromJson(jsonObjectString, type);
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

    private void performSearch (String query, String token, MapView map){
        try {
            searchLandmarkAPI("api/landmark/search", query, token, map);
        } catch (IOException | JSONException | NullPointerException e) {
            throw new RuntimeException(e);
        }
    }

    OkHttpClient getUnsafeOkHttpClient() {
        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {}

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {}

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
            builder.hostnameVerifier((hostname, session) -> true);
            builder.connectTimeout(30, TimeUnit.SECONDS) // Increase connection timeout
                    .readTimeout(30, TimeUnit.SECONDS)    // Increase read timeout
                    .writeTimeout(30, TimeUnit.SECONDS)   // Increase write timeout
                    .build();


            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}