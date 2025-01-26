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
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import org.osmdroid.views.overlay.Marker;

import io.kamzy.futolocate.Tools.DataManager;
import io.kamzy.futolocate.Tools.EmailSharedViewModel;
import io.kamzy.futolocate.Tools.TokenSharedViewModel;

public class Dashboard extends AppCompatActivity {
    
    Context ctx;
    BottomNavigationView bottomNav;
    private FragmentManager fragmentManager;
    String token, email;
    private Fragment exploreFragment = new ExploreFragment();
    private  Fragment landmarkFragment = new LandmarkFragment();
    private  Fragment eventFragment = new EventFragment();
    private Fragment activeFragment;


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
        bottomNav = findViewById(R.id.bottom_navigation);
        token = getIntent().getStringExtra("token");
        email = getIntent().getStringExtra("email");
        fragmentManager = getSupportFragmentManager();

        DataManager.getInstance().setToken(token);

        TokenSharedViewModel tokenSharedViewModel = new ViewModelProvider(this).get(TokenSharedViewModel.class);
        tokenSharedViewModel.setData(token);

        EmailSharedViewModel emailSharedViewModel = new ViewModelProvider(this).get(EmailSharedViewModel.class);
        emailSharedViewModel.setData(email);

        // Set default fragment
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, exploreFragment, "Explore")
                .commit();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, landmarkFragment, "Landmark").hide(landmarkFragment)
                .commit();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, eventFragment, "Event").hide(eventFragment)
                .commit();
        activeFragment = exploreFragment;

        bottomNav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                handleNavigationItemSelected(item);
                return true;
            }
        });

    }

    // Use this:
    private boolean handleNavigationItemSelected(@NonNull MenuItem item) {
        Fragment selectedFragment = getFragmentForMenuItem(item);

        if (selectedFragment != null) {
            showFragment(selectedFragment);
        }

        return true;
    }

    private Fragment getFragmentForMenuItem(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.nav_explore) {
            return exploreFragment;
        } else if (itemId == R.id.nav_landmarks) {
            return landmarkFragment;
        } else if (itemId == R.id.events) {
            return eventFragment;
        }else {
            return null;
        }
    }

    private void showFragment(Fragment fragment) {

        getSupportFragmentManager().beginTransaction().hide(activeFragment).show(fragment).commit();
        activeFragment = fragment;
    }

    // Method to programmatically switch tabs and pass data
    public void navigateToExplore(double latitude, double longitude, String eventName, String eventLocation) {
        // Select the Explore tab
        bottomNav.setSelectedItemId(R.id.nav_explore);

        // Pass data to ExploreFragment
        Bundle bundle = new Bundle();
        bundle.putDouble("latitude", latitude);
        bundle.putDouble("longitude", longitude);
        bundle.putString("eventName", eventName);
        bundle.putString("eventLocation", eventLocation);

        // Replace ExploreFragment with arguments
        exploreFragment = new ExploreFragment();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, exploreFragment, "Explore")
                .commit();
        exploreFragment.setArguments(bundle);

        showFragment(exploreFragment);
    }

    // Method to programmatically switch tabs and pass data
    public void navigateToExploreForSearch(String Location) {
        // Select the Explore tab
        bottomNav.setSelectedItemId(R.id.nav_explore);

        // Pass data to ExploreFragment
        Bundle bundle = new Bundle();
        bundle.putString("landmark",Location);

        // Replace ExploreFragment with arguments
        exploreFragment = new ExploreFragment();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, exploreFragment, "Explore")
                .commit();
        exploreFragment.setArguments(bundle);

        showFragment(exploreFragment);
    }


    public interface OnMarkerAddedCallback {
        void onMarkerAdded(Marker marker);
    }

}