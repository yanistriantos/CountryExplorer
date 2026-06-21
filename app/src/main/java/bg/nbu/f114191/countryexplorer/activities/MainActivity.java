package bg.nbu.f114191.countryexplorer.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import bg.nbu.f114191.countryexplorer.R;
import bg.nbu.f114191.countryexplorer.fragments.AllCountriesFragment;
import bg.nbu.f114191.countryexplorer.fragments.FavouritesFragment;
import bg.nbu.f114191.countryexplorer.services.DataSyncService;

// The first/main screen. It starts the background download, shows the two
// tabs at the bottom, and switches between the two tabs when you tap them.
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Start downloading and saving all countries in the background.
        startService(new Intent(this, DataSyncService.class));

        setUpBottomNavigation();

        // Show the All Countries tab first when the app opens.
        if (savedInstanceState == null) {
            showFragment(new AllCountriesFragment());
        }
    }

    // Makes the bottom tabs switch screens when tapped.
    private void setUpBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        bottomNavigationView.setOnItemSelectedListener(
                new NavigationBarView.OnItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull android.view.MenuItem item) {
                        int selectedItemId = item.getItemId();

                        if (selectedItemId == R.id.navigationAllCountries) {
                            showFragment(new AllCountriesFragment());
                            return true;
                        } else if (selectedItemId == R.id.navigationFavourites) {
                            showFragment(new FavouritesFragment());
                            return true;
                        }
                        return false;
                    }
                });
    }

    // Puts the chosen tab's screen into the container. No new window opens.
    private void showFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}