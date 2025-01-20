package io.kamzy.futolocate;

import static io.kamzy.futolocate.Tools.Tools.baseURL;
import static io.kamzy.futolocate.Tools.Tools.prepGetRequestWithoutBody;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
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
import io.kamzy.futolocate.Models.Users;
import io.kamzy.futolocate.Tools.EmailSharedViewModel;
import io.kamzy.futolocate.Tools.TokenSharedViewModel;
import io.kamzy.futolocate.enums.ModeOfTransport;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ExploreFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ExploreFragment extends Fragment {

    MapView mapView;
    Context ctx;
    OkHttpClient client;
    String token, fromLocation, toLocation, getLocationAction;
    TextInputLayout searchTextInputLayout, fromTextInputLayout, toTextInputLayout;
    TextInputEditText searchEditText, fromEditText, toEditText;
    Marker searchMarker, fromMarker, toMarker, currentLocationMarker;
    Polyline routeLine;
    private static final int LOCATION_PERMISSION_REQUEST = 100;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    GeoPoint futoLocation;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public ExploreFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ExploreFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ExploreFragment newInstance(String param1, String param2) {
        ExploreFragment fragment = new ExploreFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_explore, container, false);

        mapView = view.findViewById(R.id.map_view);
        searchTextInputLayout = view.findViewById(R.id.searchTextInputLayout);
        fromTextInputLayout = view.findViewById(R.id.fromTextInputLayout);
        toTextInputLayout = view.findViewById(R.id.toTextInputLayout);
        searchEditText = view.findViewById(R.id.searchEditText);
        fromEditText = view.findViewById(R.id.fromLocationEditText);
        toEditText = view.findViewById(R.id.toLocationEditText);
        client = new OkHttpClient();
        ctx = requireContext();


        getLocationAction = "default";
        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(ctx);


        //        Initialize Map
        Configuration.getInstance().setUserAgentValue(requireActivity().getPackageName());
        // Configure osmdroid
        File cacheDir = new File(requireActivity().getCacheDir(), "osmdroid_tiles");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        Configuration.getInstance().setOsmdroidTileCache(cacheDir);

        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(17.0);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        futoLocation = new GeoPoint(5.3792, 6.9974);
        mapView.getController().setCenter(futoLocation);

        // Add the long press listener
        addLongPressListener();

        // Inflate the layout for this fragment
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TokenSharedViewModel tokenSharedViewModel = new ViewModelProvider(requireActivity()).get(TokenSharedViewModel.class);
        tokenSharedViewModel.getData().observe(getViewLifecycleOwner(), value -> {
            token = value;


//        get Landmarks from DB
            try {
                getAllLandmarkAPI("api/landmark", token, mapView);
            } catch (IOException | JSONException e) {
                throw new RuntimeException(e);
            }

            setupLocationCallback(getLocationAction);     // Initialize the callback
            requestLocationPermission(getLocationAction); // Ensure permissions are granted
            startLocationUpdates();      // Start receiving location updates

            // Floating Action Button
            view.findViewById(R.id.fab_locate).setOnClickListener(v -> {
                getLocationAction = "fabLocateClick";
                setupLocationCallback(getLocationAction);
                requestLocationPermission(getLocationAction); // Ensure permissions are granted
//            startLocationUpdates();      // Start receiving location updates
            });

            view.findViewById(R.id.fab_navigate).setOnClickListener(v -> {
                // Handle FAB click
                if (fromTextInputLayout.getVisibility() == View.GONE && toTextInputLayout.getVisibility() == View.GONE) {
                    // Show navigation fields
                    searchTextInputLayout.setVisibility(View.GONE);
                    fromTextInputLayout.setVisibility(View.VISIBLE);
                    toTextInputLayout.setVisibility(View.VISIBLE);
                    fromEditText.setText(R.string.your_location);
                } else {
                    // Revert to normal search field
                    searchTextInputLayout.setVisibility(View.VISIBLE);
                    fromTextInputLayout.setVisibility(View.GONE);
                    toTextInputLayout.setVisibility(View.GONE);
                }
            });

//        normal search action button
            searchEditText.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String query = searchEditText.getText().toString().trim();
                    Log.i("Search", "Query: " + query);
                    if (!query.isEmpty()) {
                        performSearch(query, token, mapView, searchMarker -> {
                        });
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
                    if (fromLocation.equals("Your Location")) {
                        requestLocationPermission("fromNavigate");
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
                    } else if (toLocation.equals("Your Location")) {
                        requestLocationPermission("toNavigate");
                        performFromLocate(fromLocation, token, mapView, fromMarker -> {
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
                    } else {
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

                    }

                    return true;
                }
                return false;
            });

            toEditText.setOnEditorActionListener((v, actionId, event) -> {
                fromLocation = fromEditText.getText().toString();
                toLocation = toEditText.getText().toString();

                if (!fromLocation.isEmpty() && !toLocation.isEmpty()) {
                    if (fromLocation.equals("Your Location")) {
                        requestLocationPermission("fromNavigate");
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
                    } else if (toLocation.equals("Your Location")) {
                        requestLocationPermission("toNavigate");
                        performFromLocate(fromLocation, token, mapView, fromMarker -> {
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
                    } else {
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

                    }

                    return true;
                }
                return false;
            });
        });

    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onResume() {
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
                    getActivity().runOnUiThread(()->{
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
    private void searchLandmarkAPI(String endpoint, String query, String authToken, MapView m, String action, Dashboard.OnMarkerAddedCallback callback) throws IOException, JSONException, NullPointerException{
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
                                getActivity().runOnUiThread(() -> {
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
                                getActivity().runOnUiThread(() -> {
                                    Log.i("Name: ", l.getName());
                                    // add a marker
                                    addfromMarkerToMap(m, l.getName(), l.getLatitude(), l.getLongitude(), callback);
                                });
                                break;
                            case "toLocate":
                                getActivity().runOnUiThread(() -> {
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
    private void performExternalSearchQuery (String query, MapView map, String action, Dashboard.OnMarkerAddedCallback callback) throws IOException{
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
                                getActivity().runOnUiThread(() -> {
                                    map.getOverlays().remove(searchMarker);
                                });
                                break;
                            case "fromLocate":
                            case "toLocate":
                                getActivity().runOnUiThread(() -> {
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
                                        getActivity().runOnUiThread(() -> {
                                            Log.i("Name: ", query);
                                            GeoPoint geoPoint = new GeoPoint(lat, lon);
                                            addSearchMarkerToMap(map, query, lat, lon, callback);
                                            map.getController().setCenter(geoPoint);
                                            map.getController().setZoom(18.0);

                                        });
                                        break;
                                    case "fromLocate":
                                        getActivity().runOnUiThread(() -> {
                                            Log.i("Name: ", query);
                                            // add a marker
                                            addfromMarkerToMap(map, query, lat, lon, callback);
                                        });
                                        break;
                                    case "toLocate":
                                        getActivity().runOnUiThread(() -> {
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
                        getActivity().runOnUiThread(()->{
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

    // convert JSONObject landmark to Landmark object
    private Users parseJSONObjectToUser(String jsonObjectString) {
        Gson gson = new Gson();
        Type type = new TypeToken<Users>() {}.getType();
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
    private void performSearch (String query, String token, MapView map, Dashboard.OnMarkerAddedCallback callback){
        try {
            searchLandmarkAPI("api/landmark/search", query, token, map, "search", callback);
        } catch (IOException | JSONException | NullPointerException e) {
            throw new RuntimeException(e);
        }
    }

    // Method to perform "From" location search
    private void performFromLocate (String query, String token, MapView map, Dashboard.OnMarkerAddedCallback callback){
        try {
            searchLandmarkAPI("api/landmark/search", query, token, map, "fromLocate", callback);
        } catch (IOException | JSONException | NullPointerException e) {
            throw new RuntimeException(e);
        }
    }

    // Method to perform "To" location search
    private void performToLocate (String query, String token, MapView map, Dashboard.OnMarkerAddedCallback callback){
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
    private void addSearchMarkerToMap(MapView mapView, String name, double latitude, double longitude, Dashboard.OnMarkerAddedCallback callback) {
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

    private void addfromMarkerToMap(MapView mapView, String name, double latitude, double longitude, Dashboard.OnMarkerAddedCallback callback) {
        if (fromMarker != null) {
            mapView.getOverlays().remove(fromMarker);
        }

        if (searchMarker != null) {
            mapView.getOverlays().remove(searchMarker);
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

    private void addtoMarkerToMap(MapView mapView, String name, double latitude, double longitude, Dashboard.OnMarkerAddedCallback callback) {
        if (toMarker != null) {
            mapView.getOverlays().remove(toMarker);
        }

        if (searchMarker != null) {
            mapView.getOverlays().remove(searchMarker);
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


    private void requestLocationPermission(String action) {
        if (ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
        } else {
            switch (action){
                case "fromNavigate":
                case "toNavigate":
                    getCurrentLocation(action);
                    break;
                case "fabLocateClick":
                    getCurrentLocation(action);
                    break;
                default:
                    getCurrentLocation(action);
                    startLocationUpdates();
                    break;
            }
        }
    }

    private void getCurrentLocation(String action) {
        if (ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            updateLocationOnMap(location, action);
                        } else {
                            Toast.makeText(requireContext(), "Unable to get current location", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            // Request permission if not granted
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
        }
    }


    private void startLocationUpdates() {
        // Create a LocationRequest instance using the Builder
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2000) // Minimum update interval
                .build();

        // Check location permission before requesting updates
        if (ActivityCompat.checkSelfPermission(ctx, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } else {
            // Request location permission if not already granted
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
        }
    }

    private void setupLocationCallback(String action) {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        // Handle location updates
                        updateLocationOnMap(location, action);
                        Log.d("Location Update", "Lat: " + location.getLatitude() + ", Lng: " + location.getLongitude());
                    }
                }
            }
        };
    }

    private void updateLocationOnMap(Location location, String action) {
        GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
        // Remove the old marker, if it exists
        if (currentLocationMarker != null) {
            mapView.getOverlays().remove(currentLocationMarker);
        }


        switch (action){
            case "fromNavigate":
                if (fromMarker != null) {
                    mapView.getOverlays().remove(fromMarker);
                }
                if (searchMarker != null) {
                    mapView.getOverlays().remove(searchMarker);
                }

                fromMarker = new Marker(mapView);
                fromMarker.setPosition(geoPoint);
                fromMarker.setTitle("Your Location");
                fromMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                mapView.getOverlays().add(fromMarker);
                mapView.invalidate(); // Refresh the map
                break;
            case "toNavigate":
                if (toMarker != null) {
                    mapView.getOverlays().remove(toMarker);
                }

                if (searchMarker != null) {
                    mapView.getOverlays().remove(searchMarker);
                }

                toMarker = new Marker(mapView);
                toMarker.setPosition(new GeoPoint(location.getLatitude(), location.getLongitude()));
                toMarker.setTitle("Your Location");
                toMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                mapView.getOverlays().add(toMarker);
                mapView.invalidate(); // Refresh the map
                break;
            default:
                // Add new marker for current location
                currentLocationMarker = new Marker(mapView);
                currentLocationMarker.setPosition(geoPoint);
                currentLocationMarker.setTitle("You are here");
                currentLocationMarker.setIcon(getColoredMarkerIcon(R.drawable.current_location_marker, Color.BLUE));
                currentLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                mapView.getOverlays().add(currentLocationMarker);

                switch (action){
                    case "fabLocateClick":
                        // Center the map on the current location
                        mapView.getController().setCenter(geoPoint);
                        mapView.invalidate(); // Refresh the map
                        break;
                    default:
                        // Center the map on the current location
                        mapView.invalidate(); // Refresh the map
                        break;
                }
                break;
        }
    }

    private Drawable getColoredMarkerIcon(int drawableResId, int colorValue) {
        // Retrieve the drawable resource
        Drawable drawable = ContextCompat.getDrawable(ctx, drawableResId);
        if (drawable == null) return null;

        // Apply the color filter directly with the provided color value
        drawable.setColorFilter(new PorterDuffColorFilter(
                colorValue, // This is the actual color value (e.g., 0xff0000ff)
                PorterDuff.Mode.SRC_IN
        ));

        return drawable;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                Toast.makeText(ctx, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop location updates to conserve battery
        if (fusedLocationClient != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    private void showAddEventMenu(double latitude, double longitude) {
        // Use Fragment's context for the BottomSheetDialog
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());

        // Inflate the custom layout for the bottom sheet
        View bottomSheetView = LayoutInflater.from(getContext()).inflate(
                R.layout.bottom_sheet_add_event, // Custom layout file
                requireActivity().findViewById(R.id.bottom_sheet_container) // Root container from the layout
        );

        // Initialize input fields and buttons in the layout
        EditText eventNameInput = bottomSheetView.findViewById(R.id.eventName);
        EditText eventDescriptionInput = bottomSheetView.findViewById(R.id.eventDescription);
        EditText eventLocationInput = bottomSheetView.findViewById(R.id.eventLocation);
        EditText eventTimeInput = bottomSheetView.findViewById(R.id.eventTime);
        Button saveEventButton = bottomSheetView.findViewById(R.id.saveEventButton);

        // Set Save button click listener
        saveEventButton.setOnClickListener(v -> {
            String eventName = eventNameInput.getText().toString().trim();
            String eventDescription = eventDescriptionInput.getText().toString().trim();
            String eventLocation = eventLocationInput.getText().toString().trim();
            String eventTime = eventTimeInput.getText().toString().trim();

            if (eventName.isEmpty()) eventNameInput.setError("This field is required");
            if (eventDescription.isEmpty()) eventDescriptionInput.setError("This field is required");
            if (eventLocation.isEmpty()) eventLocationInput.setError("This field is required");
            if (eventTime.isEmpty()) eventTimeInput.setError("This field is required");

            if (!eventName.isEmpty() && !eventDescription.isEmpty() && !eventLocation.isEmpty() && !eventTime.isEmpty()){
                // Send event details to the backend
                createEventAPI("/api/auth/profile", eventName, eventDescription, eventLocation, eventTime, String.valueOf(latitude), String.valueOf(longitude));

                // Dismiss the dialog after saving
                bottomSheetDialog.dismiss();
            }


        });

        // Set the content view for the dialog and show it
        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.show();
    }

    private void createEventAPI(String endpoint, String...parameters){

        EmailSharedViewModel emailSharedViewModel = new ViewModelProvider(requireActivity()).get(EmailSharedViewModel.class);
        emailSharedViewModel.getData().observe(getViewLifecycleOwner(), value -> {
            String email = value;

            FormBody.Builder requestParams = new FormBody.Builder()
                    .add("email", email);

            RequestBody requestBody = requestParams.build();
            Request request = new Request.Builder()
                    .url(baseURL + endpoint)
                    .post(requestBody)
                    .build();

            new Thread(()->{
                try(Response response = client.newCall(request).execute()){
                    int statusCode = response.code();
                    Log.i("statusCode", String.valueOf(statusCode));
                    String respsoneBody = response.body() != null ? response.body().string() : "null";
                    if (response.isSuccessful()){
                        if (respsoneBody.equals("null")){

                        }else {
                            JSONObject jsonResponse = new JSONObject(respsoneBody);
                            Users user = parseJSONObjectToUser(jsonResponse.toString());

                            JSONObject eventDetails = new JSONObject();
                            eventDetails.put("name", parameters[0]);
                            eventDetails.put("description", parameters[1]);
                            eventDetails.put("location", parameters[2]);
                            eventDetails.put("time", parameters[3]);
                            eventDetails.put("latitude", parameters[4]);
                            eventDetails.put("longitude", parameters[5]);
                            eventDetails.put("created_by", user.getUser_id());

                            saveEventToServer("api/events/create", eventDetails);
                        }
                    }

                } catch (IOException | JSONException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        });
    }

    private void saveEventToServer(String endpoint, JSONObject eventDetails) {
        RequestBody requestBody = RequestBody.create(
                eventDetails.toString(),
                MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(baseURL + endpoint)
                .post(requestBody)
                .build();

        new Thread(()->{
            try(Response response = client.newCall(request).execute()){
                int statusCode = response.code();
                Log.i("statusCode", String.valueOf(statusCode));
                String respsoneBody = response.body() != null ? response.body().string() : "null";
                if (response.isSuccessful()){
                    if (respsoneBody.equals("null")){

                    }else {
                        JSONObject jsonResponse = new JSONObject(respsoneBody);
                        String eventStatus = jsonResponse.getString("status");
                        Log.i("Event Status", eventStatus);

                        requireActivity().runOnUiThread(()->{
                            Toast.makeText(ctx, "Event created successfully", Toast.LENGTH_LONG).show();
                        });

                    }
                }
            } catch (IOException | JSONException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    private void addLongPressListener() {
        // Create a MapEventsReceiver to handle gestures
        MapEventsReceiver mapEventsReceiver = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint geoPoint) {
                // Handle single tap if needed
                return false;
            }

            @Override
            public boolean longPressHelper(GeoPoint geoPoint) {
                // Handle long press
                double latitude = geoPoint.getLatitude();
                double longitude = geoPoint.getLongitude();

                // Call your method to show the event menu
                showAddEventMenu(latitude, longitude);

                // Return true to indicate the long press event was handled
                return true;
            }
        };

        // Create an overlay to attach the gesture receiver to the map
        MapEventsOverlay eventsOverlay = new MapEventsOverlay(mapEventsReceiver);
        mapView.getOverlays().add(eventsOverlay);
    }

}