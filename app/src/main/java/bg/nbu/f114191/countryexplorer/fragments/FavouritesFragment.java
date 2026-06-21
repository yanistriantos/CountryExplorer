package bg.nbu.f114191.countryexplorer.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import bg.nbu.f114191.countryexplorer.R;
import bg.nbu.f114191.countryexplorer.activities.CountryDetailActivity;
import bg.nbu.f114191.countryexplorer.adapters.CountryAdapter;
import bg.nbu.f114191.countryexplorer.database.CountryDatabaseHelper;
import bg.nbu.f114191.countryexplorer.models.Country;

// The "Favourites" tab. It shows only the countries the user has saved.
// It uses the same list adapter as the main tab, and reloads every time you
// come back to it, so countries you just added or removed show up right away.
public class FavouritesFragment extends Fragment
        implements CountryAdapter.OnCountryClickListener {

    private RecyclerView favouritesRecyclerView;
    private TextView emptyFavouritesTextView;

    private CountryAdapter countryAdapter;
    private CountryDatabaseHelper databaseHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favourites, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        databaseHelper = new CountryDatabaseHelper(requireContext());

        favouritesRecyclerView = view.findViewById(R.id.favouritesRecyclerView);
        emptyFavouritesTextView = view.findViewById(R.id.emptyFavouritesTextView);

        countryAdapter = new CountryAdapter(requireContext(), this);
        favouritesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        favouritesRecyclerView.setAdapter(countryAdapter);
    }

    // Reload every time this tab comes back into view.
    @Override
    public void onResume() {
        super.onResume();
        loadFavouritesFromDatabase();
    }

    // Shows the favourites, or a small message if there aren't any yet.
    private void loadFavouritesFromDatabase() {
        List<Country> favouriteCountries = databaseHelper.getAllFavourites();
        countryAdapter.updateCountries(favouriteCountries);

        if (favouriteCountries.isEmpty()) {
            emptyFavouritesTextView.setVisibility(View.VISIBLE);
            favouritesRecyclerView.setVisibility(View.GONE);
        } else {
            emptyFavouritesTextView.setVisibility(View.GONE);
            favouritesRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    // Same as the main tab: open the detail screen with this country's info.
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