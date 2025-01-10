package io.kamzy.futolocate;

import static io.kamzy.futolocate.Tools.Tools.baseURL;
import static io.kamzy.futolocate.Tools.Tools.prepGetRequestWithoutBody;

import android.content.Context;
import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
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
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import io.kamzy.futolocate.Models.Landmarks;
import io.kamzy.futolocate.Models.Routes;
import io.kamzy.futolocate.enums.ModeOfTransport;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Dashboard extends AppCompatActivity {

    MapView mapView;
    Context ctx;
    BottomNavigationView bottomNav;
    OkHttpClient client = new OkHttpClient();
    String token, fromLocation, toLocation;
    TextInputLayout searchTextInputLayout, fromTextInputLayout, toTextInputLayout;
    TextInputEditText searchEditText, fromEditText, toEditText;
    Marker searchMarker, fromMarker, toMarker, currentLocationMarker;
    Polyline routeLine;
    private static final int LOCATION_PERMISSION_REQUEST = 100;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    GeoPoint futoLocation;

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
        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);



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
        futoLocation = new GeoPoint(5.3792, 6.9974);
        mapView.getController().setCenter(futoLocation);

//        get Landmarks from DB
        try {
            getAllLandmarkAPI("api/landmark", token, mapView);
        } catch (IOException | JSONException e) {
            throw new RuntimeException(e);
        }


        setupLocationCallback();     // Initialize the callback

        // Floating Action Button
        findViewById(R.id.fab_locate).setOnClickListener(v -> {
            requestLocationPermission(); // Ensure permissions are granted
            startLocationUpdates();      // Start receiving location updates
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

//        normal search action button
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH){
                String query = searchEditText.getText().toString().trim();
                if (!query.isEmpty()){
                    performSearch(query, token, mapView, searchMarker -> {});
                    return true;
                }
            }
            return false;
        });

//      Navigate action
        fromEditText.setOnEditorActionListener((v, actionId, event) -> {
            fromLocation = fromEditText.getText().toString();
            toLocation = toEditText.getText().toString();

            if (!fromLocation.isEmpty() && !toLocation.isEmpty()) {
                performFromLocate(fromLocation, token, mapView, fromMarker -> {
                    performToLocate(toLocation, token, mapView, toMarker -> {
                        if (fromMarker != null && toMarker != null) {
                            GeoPoint fromLocationData = fromMarker.getPosition();
                            GeoPoint toLocationData = toMarker.getPosition();

                            try {
                                getRouteAPI(
                                        "api/navigation/routes/create",
                                        String.valueOf(fromLocationData.getLatitude()),
                                        String.valueOf(fromLocationData.getLongitude()),
                                        String.valueOf(toLocationData.getLatitude()),
                                        String.valueOf(toLocationData.getLongitude()),
                                        String.valueOf(ModeOfTransport.walking),
                                        token,
                                        mapView
                                );
                            } catch (IOException | JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                });

                return true;
            }
            return false;
        });

        toEditText.setOnEditorActionListener((v, actionId, event) -> {
            fromLocation = fromEditText.getText().toString();
            toLocation = toEditText.getText().toString();

            if (!fromLocation.isEmpty() && !toLocation.isEmpty()) {
                performFromLocate(fromLocation, token, mapView, fromMarker -> {
                    performToLocate(toLocation, token, mapView, toMarker -> {
                        if (fromMarker != null && toMarker != null) {
                            GeoPoint fromLocationData = fromMarker.getPosition();
                            GeoPoint toLocationData = toMarker.getPosition();

                            try {
                                getRouteAPI(
                                        "api/navigation/routes/create",
                                        String.valueOf(fromLocationData.getLatitude()),
                                        String.valueOf(fromLocationData.getLongitude()),
                                        String.valueOf(toLocationData.getLatitude()),
                                        String.valueOf(toLocationData.getLongitude()),
                                        String.valueOf(ModeOfTransport.walking),
                                        token,
                                        mapView
                                );
                            } catch (IOException | JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                });

                return true;
            }
            return false;
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




//  APIs
//  get all landmarks from DB
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
                            addDBlandmarksToMap(map, landmark.getName(), landmark.getLatitude(), landmark.getLongitude());
                        }
                    });
                }
            }catch (IOException | JSONException e){
                throw new RuntimeException(e);
            }
        }).start();
    }

//      perform local search
    private void searchLandmarkAPI(String endpoint, String query, String authToken, MapView m, String action, OnMarkerAddedCallback callback) throws IOException, JSONException, NullPointerException{
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
                            performExternalSearchQuery(query, m, action, callback);
                            return; // Exit the method or handle the error appropriately
                        }else {
                                JSONObject responseBody =  new JSONObject(r);
                                Landmarks l = parseJSONObjectUsingGson(String.valueOf(responseBody));
                                switch (action){
                                    case "search" :
                                        runOnUiThread(() -> {
                                            Log.i("Name: ", l.getName());
                                            // Center map on found location
                                            GeoPoint geoPoint = new GeoPoint(l.getLatitude(), l.getLongitude());
                                            // Optionally add a marker
                                            addSearchMarkerToMap(m, l.getName(), l.getLatitude(), l.getLongitude(), callback);
                                            m.getController().setCenter(geoPoint);
                                            m.getController().setZoom(18.0);
                                        });
                                        break;
                                    case "fromLocate":
                                        runOnUiThread(() -> {
                                            Log.i("Name: ", l.getName());
                                            // add a marker
                                            addfromMarkerToMap(m, l.getName(), l.getLatitude(), l.getLongitude(), callback);
                                        });
                                        break;
                                    case "toLocate":
                                        runOnUiThread(() -> {
                                            Log.i("Name: ", l.getName());
                                            // add a marker
                                            addtoMarkerToMap(m, l.getName(), l.getLatitude(), l.getLongitude(), callback);
                                        });
                                        break;
                                }
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
    private void performExternalSearchQuery (String query, MapView map, String action, OnMarkerAddedCallback callback) throws IOException{
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
                    String r = response.body() != null ? response.body().string() : "[]";
                    Log.i("External Search", r);
                    if (r.equals("[]")){
                        Log.i("Search Result", "couldn't find location: " + query);
                        switch (action){
                            case "search" :
                                runOnUiThread(() -> {
                                    map.getOverlays().remove(searchMarker);
                                });
                                break;
                            case "fromLocate":
                            case "toLocate":
                                runOnUiThread(() -> {
                                    map.getOverlays().remove(fromMarker);
                                    map.getOverlays().remove(toMarker);
                                });
                                break;
                        }

                    } else {
                        try {
                            JSONArray results = new JSONArray(r);
                            if (results.length() > 0) {
                                JSONObject location = results.getJSONObject(0);
                                double lat = location.getDouble("lat");
                                double lon = location.getDouble("lon");

                                switch (action){
                                    case "search" :
                                        runOnUiThread(() -> {
                                            Log.i("Name: ", query);
                                            GeoPoint geoPoint = new GeoPoint(lat, lon);
                                            addSearchMarkerToMap(map, query, lat, lon, callback);
                                            map.getController().setCenter(geoPoint);
                                            map.getController().setZoom(18.0);

                                        });
                                        break;
                                    case "fromLocate":
                                        runOnUiThread(() -> {
                                            Log.i("Name: ", query);
                                            // add a marker
                                            addfromMarkerToMap(map, query, lat, lon, callback);
                                        });
                                        break;
                                    case "toLocate":
                                        runOnUiThread(() -> {
                                            Log.i("Name: ", query);
                                            // add a marker
                                            addtoMarkerToMap(map, query, lat, lon, callback);
                                        });
                                        break;
                                }

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

    private void getRouteAPI (String endpoint, String startLat, String startLong, String endLat, String endLong, String transport, String authToken, MapView m) throws IOException, JSONException{
        FormBody.Builder requestParams = new FormBody.Builder()
                .add("startLat", startLat)
                .add("startLong", startLong)
                .add("endLat", endLat)
                .add("endLong", endLong)
                .add("modeOfTransport", transport);

        RequestBody requestBody = requestParams.build();

        Request request = new Request.Builder()
                .url(baseURL + endpoint)
                .addHeader("Authorization", "Bearer "+ authToken)
                .post(requestBody)
                .build();

        new Thread(()->{
            try(Response response = client.newCall(request).execute()){
               int statusCode = response.code();
                Log.i("statusCode", String.valueOf(statusCode));
                if (response.isSuccessful()){
                    String responseBody = response.body() != null ? response.body().string() : "null";
                    if (responseBody.equals("null")){
                        Log.i("Route", "No route found");
                    }else {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        Routes routes = parseJSONObjecttoRoute(jsonResponse.toString());
                        runOnUiThread(()->{
                            Log.i("Route Data", routes.getPath_coordinates());
                            Type listType = new TypeToken<List<List<Double>>>() {}.getType();
                            List<List<Double>> routeData = new Gson().fromJson(routes.getPath_coordinates(), listType);

                            // Step 1: Parse the route data into GeoPoints
                            List<GeoPoint> geoPoints = new ArrayList<>();
                            for (List<Double> point : routeData) {
                                double longitude = point.get(0); // OSRM gives [longitude, latitude]
                                double latitude = point.get(1);
                                geoPoints.add(new GeoPoint(latitude, longitude)); // GeoPoint expects [latitude, longitude]
                            }

                            // Step 2: Create a Polyline
                            if (routeLine != null){
                                mapView.getOverlays().remove(routeLine);
                                routeLine = null;
                            }
                            routeLine = new Polyline();
                            routeLine.setPoints(geoPoints);
                            routeLine.setColor(Color.BLUE);  // Set the line color
                            routeLine.setWidth(8.0f);        // Set the line width

                            // Step 3: Add the Polyline to the MapView
                            m.getOverlayManager().add(routeLine);

                            BoundingBox boundingBox = BoundingBox.fromGeoPoints(geoPoints);
                            mapView.zoomToBoundingBox(boundingBox, true);

                            // Refresh the MapView to display the route
                            m.invalidate();
                        });
                    }
                }

            } catch (IOException | JSONException e) {
                throw new RuntimeException(e);
            }
        }).start();

    }





//    TOOLS
// convert JSONArray landmarks to List<Landmarks>
    private List<Landmarks> parseJSONArrayUsingGson(String jsonArrayString) {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<Landmarks>>() {}.getType();
        return gson.fromJson(jsonArrayString, listType);
    }

    // convert JSONObject landmark to Landmark object
    private Landmarks parseJSONObjectUsingGson(String jsonObjectString) {
        Gson gson = new Gson();
        Type type = new TypeToken<Landmarks>() {}.getType();
        return gson.fromJson(jsonObjectString, type);
    }

    // convert JSONObject to Route object
    private Routes parseJSONObjecttoRoute(String jsonObjectString) {
        Gson gson = new Gson();
        Type type = new TypeToken<Routes>() {}.getType();
        return gson.fromJson(jsonObjectString, type);
    }

    // Method to add landmarks from the DB to the Map
    private void addDBlandmarksToMap(MapView displayedMap, String name, double latitude, double longitude) {
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
        marker.setPosition(new GeoPoint(latitude, longitude));
        marker.setIcon(drawable);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_TOP); // Center the text above the marker
        marker.setTitle(name); // This shows a title when the marker is tapped
        displayedMap.getOverlays().add(marker); // Adds the marker to the map

        displayedMap.invalidate(); // Refreshes the map
    }

    // Method to perform normal search
    private void performSearch (String query, String token, MapView map, OnMarkerAddedCallback callback){
        try {
            searchLandmarkAPI("api/landmark/search", query, token, map, "search", callback);
        } catch (IOException | JSONException | NullPointerException e) {
            throw new RuntimeException(e);
        }
    }

    // Method to perform "From" location search
    private void performFromLocate (String query, String token, MapView map, OnMarkerAddedCallback callback){
        try {
            searchLandmarkAPI("api/landmark/search", query, token, map, "fromLocate", callback);
        } catch (IOException | JSONException | NullPointerException e) {
            throw new RuntimeException(e);
        }
    }

    // Method to perform "To" location search
    private void performToLocate (String query, String token, MapView map, OnMarkerAddedCallback callback){
        try {
            searchLandmarkAPI("api/landmark/search", query, token, map, "toLocate", callback);
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

//    Methods for adding markers
    private void addSearchMarkerToMap(MapView mapView, String name, double latitude, double longitude, OnMarkerAddedCallback callback) {
        if (searchMarker != null) {
            mapView.getOverlays().remove(searchMarker);
        }

        if (fromMarker != null) {
            mapView.getOverlays().remove(fromMarker);
        }

        if (toMarker != null) {
            mapView.getOverlays().remove(toMarker);
        }
        if (routeLine != null){
            mapView.getOverlays().remove(routeLine);
            routeLine = null;
        }

        // Remove the old marker, if it exists
        if (currentLocationMarker != null) {
            mapView.getOverlays().remove(currentLocationMarker);
        }


        searchMarker = new Marker(mapView);
        searchMarker.setPosition(new GeoPoint(latitude, longitude));
        searchMarker.setTitle(name);
        searchMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapView.getOverlays().add(searchMarker);
        mapView.invalidate(); // Refresh the map

        // Notify the callback
        if (callback != null) {
            callback.onMarkerAdded(fromMarker);
        }
    }

    private void addfromMarkerToMap(MapView mapView, String name, double latitude, double longitude, OnMarkerAddedCallback callback) {
        if (fromMarker != null) {
            mapView.getOverlays().remove(fromMarker);
        }

        if (searchMarker != null) {
            mapView.getOverlays().remove(searchMarker);
        }

        // Remove the old marker, if it exists
        if (currentLocationMarker != null) {
            mapView.getOverlays().remove(currentLocationMarker);
        }

        fromMarker = new Marker(mapView);
        fromMarker.setPosition(new GeoPoint(latitude, longitude));
        fromMarker.setTitle(name);
        fromMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapView.getOverlays().add(fromMarker);
        mapView.invalidate(); // Refresh the map

        // Notify the callback
        if (callback != null) {
            callback.onMarkerAdded(fromMarker);
        }
    }

    private void addtoMarkerToMap(MapView mapView, String name, double latitude, double longitude, OnMarkerAddedCallback callback) {
        if (toMarker != null) {
            mapView.getOverlays().remove(toMarker);
        }

        if (searchMarker != null) {
            mapView.getOverlays().remove(searchMarker);
        }

        // Remove the old marker, if it exists
        if (currentLocationMarker != null) {
            mapView.getOverlays().remove(currentLocationMarker);
        }


        toMarker = new Marker(mapView);
        toMarker.setPosition(new GeoPoint(latitude, longitude));
        toMarker.setTitle(name);
        toMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapView.getOverlays().add(toMarker);
        mapView.invalidate(); // Refresh the map

        // Notify the callback
        if (callback != null) {
            callback.onMarkerAdded(toMarker);
        }
    }

    public interface OnMarkerAddedCallback {
        void onMarkerAdded(Marker marker);
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
        } else {
            getCurrentLocation();
            startLocationUpdates();
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            updateLocationOnMap(location);
                        } else {
                            Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void startLocationUpdates() {
        // Create a LocationRequest instance using the Builder
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2000) // Minimum update interval
                .build();

        // Check location permission before requesting updates
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } else {
            // Request location permission if not already granted
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
        }
    }

    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        // Handle location updates
                        Log.d("Location Update", "Lat: " + location.getLatitude() + ", Lng: " + location.getLongitude());
                    }
                }
            }
        };
    }

    private void updateLocationOnMap(Location location) {
        GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());

        // Remove the old marker, if it exists
        if (currentLocationMarker != null) {
            mapView.getOverlays().remove(currentLocationMarker);
        }

        // Add new marker for current location
        currentLocationMarker = new Marker(mapView);
        currentLocationMarker.setPosition(geoPoint);
        currentLocationMarker.setTitle("You are here");
        currentLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapView.getOverlays().add(currentLocationMarker);

        // Center the map on the current location
        mapView.getController().setCenter(geoPoint);
        mapView.invalidate(); // Refresh the map
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop location updates to conserve battery
        if (fusedLocationClient != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

}