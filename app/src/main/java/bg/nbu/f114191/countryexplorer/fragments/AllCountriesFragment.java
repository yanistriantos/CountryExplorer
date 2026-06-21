package bg.nbu.f114191.countryexplorer.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import bg.nbu.f114191.countryexplorer.R;
import bg.nbu.f114191.countryexplorer.activities.CountryDetailActivity;
import bg.nbu.f114191.countryexplorer.adapters.CountryAdapter;
import bg.nbu.f114191.countryexplorer.database.CountryDatabaseHelper;
import bg.nbu.f114191.countryexplorer.models.Country;
import bg.nbu.f114191.countryexplorer.services.DataSyncService;

// The "All Countries" tab. It loads the countries from the database, filters
// them as you type in the search box, and opens the detail screen when you tap
// one. It also waits for the "download finished" message so the list fills in
// as soon as the data is ready.
public class AllCountriesFragment extends Fragment
        implements CountryAdapter.OnCountryClickListener {

    private RecyclerView countriesRecyclerView;
    private ProgressBar loadingProgressBar;
    private EditText searchEditText;

    private CountryAdapter countryAdapter;
    private CountryDatabaseHelper databaseHelper;

    // The full list from the database. The search filters a copy of this.
    private final List<Country> allLoadedCountries = new ArrayList<>();

    // Waits for the "download finished" message from the background service.
    private final BroadcastReceiver syncFinishedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            loadCountriesFromDatabase();
        }
    };

    // Builds this tab's view from its layout file.
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_all_countries, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        databaseHelper = new CountryDatabaseHelper(requireContext());

        countriesRecyclerView = view.findViewById(R.id.countriesRecyclerView);
        loadingProgressBar = view.findViewById(R.id.loadingProgressBar);
        searchEditText = view.findViewById(R.id.searchEditText);

        // Set up the list and connect our adapter to it.
        countryAdapter = new CountryAdapter(requireContext(), this);
        countriesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        countriesRecyclerView.setAdapter(countryAdapter);

        setUpSearchFiltering();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Start listening for the "download finished" message while this tab is open.
        // NOT_EXPORTED means only our own app can send it to us.
        IntentFilter filter = new IntentFilter(DataSyncService.ACTION_SYNC_FINISHED);
        ContextCompat.registerReceiver(requireContext(), syncFinishedReceiver, filter,
                ContextCompat.RECEIVER_NOT_EXPORTED);

        loadCountriesFromDatabase();
    }

    @Override
    public void onStop() {
        super.onStop();
        // Stop listening when the tab isn't shown anymore.
        requireContext().unregisterReceiver(syncFinishedReceiver);
    }

    // Loads the saved countries. If there are none yet (first launch), keep the spinner.
    private void loadCountriesFromDatabase() {
        List<Country> countriesFromDatabase = databaseHelper.getAllCachedCountries();
        allLoadedCountries.clear();
        allLoadedCountries.addAll(countriesFromDatabase);

        if (allLoadedCountries.isEmpty()) {
            loadingProgressBar.setVisibility(View.VISIBLE);
        } else {
            loadingProgressBar.setVisibility(View.GONE);
            applySearchFilter(searchEditText.getText().toString());
        }
    }

    // Makes the list filter every time the search text changes.
    private void setUpSearchFiltering() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                applySearchFilter(text.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
    }

    // Keeps only the countries whose name contains what was typed.
    private void applySearchFilter(String query) {
        String loweredQuery = query.toLowerCase(Locale.getDefault()).trim();
        List<Country> filteredCountries = new ArrayList<>();
        for (Country country : allLoadedCountries) {
            String loweredName = country.getCommonName().toLowerCase(Locale.getDefault());
            if (loweredName.contains(loweredQuery)) {
                filteredCountries.add(country);
            }
        }
        countryAdapter.updateCountries(filteredCountries);
    }

    // Opens the detail screen and sends the tapped country's info with the Intent.
    @Override
    public void onCountryClicked(Country country) {
        Intent detailIntent = new Intent(requireContext(), CountryDetailActivity.class);
        detailIntent.putExtra(CountryDetailActivity.EXTRA_COUNTRY_CODE, country.getCountryCode());
        detailIntent.putExtra(CountryDetailActivity.EXTRA_COMMON_NAME, country.getCommonName());
        detailIntent.putExtra(CountryDetailActivity.EXTRA_OFFICIAL_NAME, country.getOfficialName());
        detailIntent.putExtra(CountryDetailActivity.EXTRA_CAPITAL, country.getCapital());
        detailIntent.putExtra(CountryDetailActivity.EXTRA_REGION, country.getRegion());
        detailIntent.putExtra(CountryDetailActivity.EXTRA_POPULATION, country.getPopulation());
        detailIntent.putExtra(CountryDetailActivity.EXTRA_FLAG_URL, country.getFlagImageUrl());
        detailIntent.putExtra(CountryDetailActivity.EXTRA_CURRENCY, country.getCurrency());
        detailIntent.putExtra(CountryDetailActivity.EXTRA_LANGUAGES, country.getLanguages());
        startActivity(detailIntent);
    }
}