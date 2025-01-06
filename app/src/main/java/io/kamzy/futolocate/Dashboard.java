package io.kamzy.futolocate;

import android.content.Context;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

public class Dashboard extends AppCompatActivity {

    MapView mapView;
    Context ctx;
    BottomNavigationView bottomNav;

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
        
        Configuration.getInstance().setUserAgentValue(getPackageName());

        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(17.0);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        GeoPoint futoLocation = new GeoPoint(5.3792, 6.9974);
        mapView.getController().setCenter(futoLocation);

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
                switch (item.getItemId()) {
                    case R.id.nav_explore:
                        // Handle Explore tab
                        return true;
                    case R.id.nav_landmarks:
                        // Handle Landmark tab
                        return true;
                    case R.id.history:
                        // Handle History tab
                        return true;
                    case R.id.profile:
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
}