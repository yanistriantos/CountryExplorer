package bg.nbu.f114191.countryexplorer.activities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.NumberFormat;
import java.util.Locale;

import bg.nbu.f114191.countryexplorer.R;
import bg.nbu.f114191.countryexplorer.database.CountryDatabaseHelper;
import bg.nbu.f114191.countryexplorer.models.Country;

// The second screen. Opens when you tap a country in the list.
// It receives the country's info through the Intent, shows it, and lets
// you add or remove the country from your favourites.
public class CountryDetailActivity extends AppCompatActivity {

    // These are the labels used to pass each piece of data through the Intent.
    // The list screen puts data in with these names, and here we read it back
    // using the same names.
    public static final String EXTRA_COUNTRY_CODE = "extra_country_code";
    public static final String EXTRA_COMMON_NAME = "extra_common_name";
    public static final String EXTRA_OFFICIAL_NAME = "extra_official_name";
    public static final String EXTRA_CAPITAL = "extra_capital";
    public static final String EXTRA_REGION = "extra_region";
    public static final String EXTRA_POPULATION = "extra_population";
    public static final String EXTRA_FLAG_URL = "extra_flag_url";
    public static final String EXTRA_CURRENCY = "extra_currency";
    public static final String EXTRA_LANGUAGES = "extra_languages";

    private CountryDatabaseHelper databaseHelper;

    // The country we're showing right now.
    private Country currentCountry;

    // True if this country is already saved as a favourite.
    private boolean isCurrentlyFavourite;

    private Button favouriteToggleButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_country_detail);

        databaseHelper = new CountryDatabaseHelper(this);

        // Build the country object from the data the list screen sent us.
        currentCountry = readCountryFromIntent();

        showCountryDetails(currentCountry);
        setUpFavouriteButton();
    }

    // Reads each value out of the Intent and makes a Country from it.
    private Country readCountryFromIntent() {
        String countryCode = getIntent().getStringExtra(EXTRA_COUNTRY_CODE);
        String commonName = getIntent().getStringExtra(EXTRA_COMMON_NAME);
        String officialName = getIntent().getStringExtra(EXTRA_OFFICIAL_NAME);
        String capital = getIntent().getStringExtra(EXTRA_CAPITAL);
        String region = getIntent().getStringExtra(EXTRA_REGION);
        long population = getIntent().getLongExtra(EXTRA_POPULATION, 0);
        String flagUrl = getIntent().getStringExtra(EXTRA_FLAG_URL);
        String currency = getIntent().getStringExtra(EXTRA_CURRENCY);
        String languages = getIntent().getStringExtra(EXTRA_LANGUAGES);

        return new Country(countryCode, commonName, officialName, capital,
                region, population, flagUrl, currency, languages);
    }

    // Puts all the country info into the text fields on screen.
    private void showCountryDetails(Country country) {
        TextView commonNameTextView = findViewById(R.id.detailCommonNameTextView);
        TextView officialNameTextView = findViewById(R.id.detailOfficialNameTextView);
        TextView capitalTextView = findViewById(R.id.detailCapitalTextView);
        TextView regionTextView = findViewById(R.id.detailRegionTextView);
        TextView populationTextView = findViewById(R.id.detailPopulationTextView);
        TextView currencyTextView = findViewById(R.id.detailCurrencyTextView);
        TextView languagesTextView = findViewById(R.id.detailLanguagesTextView);

        commonNameTextView.setText(country.getCommonName());
        officialNameTextView.setText(country.getOfficialName());
        capitalTextView.setText("Capital: " + country.getCapital());
        regionTextView.setText("Region: " + country.getRegion());

        // Some small countries have no population number, so we only show
        // the population line if we actually have one (otherwise hide it).
        if (country.getPopulation() > 0) {
            // Adds the commas, e.g. 6,500,000
            String formattedPopulation = NumberFormat.getInstance(Locale.getDefault())
                    .format(country.getPopulation());
            populationTextView.setText("Population: " + formattedPopulation);
            populationTextView.setVisibility(View.VISIBLE);
        } else {
            populationTextView.setVisibility(View.GONE);
        }

        currencyTextView.setText("Currency: " + country.getCurrency());
        languagesTextView.setText("Languages: " + country.getLanguages());

        loadFlagImage(country.getFlagImageUrl());
    }

    // Sets the button text (Add or Remove) and handles taps on it.
    private void setUpFavouriteButton() {
        favouriteToggleButton = findViewById(R.id.favouriteToggleButton);

        // Check if it's already saved, so the button shows the right text.
        isCurrentlyFavourite = databaseHelper.isFavourite(currentCountry.getCountryCode());
        refreshFavouriteButtonLabel();

        favouriteToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isCurrentlyFavourite) {
                    databaseHelper.removeFavourite(currentCountry.getCountryCode());
                    isCurrentlyFavourite = false;
                    Toast.makeText(CountryDetailActivity.this,
                            "Removed from favourites", Toast.LENGTH_SHORT).show();
                } else {
                    databaseHelper.addFavourite(currentCountry);
                    isCurrentlyFavourite = true;
                    Toast.makeText(CountryDetailActivity.this,
                            "Added to favourites", Toast.LENGTH_SHORT).show();
                }
                refreshFavouriteButtonLabel();
            }
        });
    }

    // Changes the button text depending on whether it's a favourite or not.
    private void refreshFavouriteButtonLabel() {
        if (isCurrentlyFavourite) {
            favouriteToggleButton.setText("Remove from favourites");
        } else {
            favouriteToggleButton.setText("Add to favourites");
        }
    }

    // Downloads the flag picture on a background thread, then shows it.
    // We don't use any image library, so we download it ourselves.
    private void loadFlagImage(final String flagUrl) {
        final ImageView flagImageView = findViewById(R.id.detailFlagImageView);
        if (flagUrl == null || flagUrl.isEmpty()) {
            return;
        }

        final Handler mainThreadHandler = new Handler(Looper.getMainLooper());
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(flagUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setConnectTimeout(10000);
                    InputStream inputStream = connection.getInputStream();
                    final Bitmap flagBitmap = BitmapFactory.decodeStream(inputStream);
                    inputStream.close();
                    connection.disconnect();

                    // Showing a picture has to happen on the main thread.
                    mainThreadHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            flagImageView.setImageBitmap(flagBitmap);
                        }
                    });
                } catch (Exception exception) {
                    // If the flag won't load, just leave it empty.
                }
            }
        }).start();
    }
}