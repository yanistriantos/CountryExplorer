package bg.nbu.f114191.countryexplorer.network;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import bg.nbu.f114191.countryexplorer.models.Country;

// Gets the country data from the internet and turns it into Country objects.
// It uses two free sources (no API key needed): one with the main country info,
// and one with population numbers that we match on by name. The flag pictures
// are built from each country's 2-letter code. All of this runs on a background
// thread so the app never freezes.
public class CountryApiClient {

    private static final String LOG_TAG = "CountryApiClient";

    // Main data: a big list of countries.
    private static final String COUNTRIES_API_URL =
            "https://raw.githubusercontent.com/mledoze/countries/master/countries.json";

    // Population data: a list of { country name, population }.
    private static final String POPULATION_API_URL =
            "https://raw.githubusercontent.com/samayo/country-json/master/src/country-by-population.json";

    // Flag picture link is this + the 2-letter code (small letters) + ".png".
    private static final String FLAG_IMAGE_URL_PREFIX = "https://flagcdn.com/w320/";

    private static final int CONNECTION_TIMEOUT_MILLIS = 30000;

    // The list screen gives us one of these so we can hand the result back
    // when the download is done (or tell it something went wrong).
    public interface CountryApiCallback {
        void onCountriesLoaded(List<Country> countries);
        void onError(String errorMessage);
    }

    // Starts a background thread, downloads everything, and reports back.
    public void fetchAllCountries(final CountryApiCallback callback) {
        Thread workerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(LOG_TAG, "Downloading countries...");
                    String countriesJson = downloadJsonFromUrl(COUNTRIES_API_URL);

                    // Population is a bonus. If it fails, we still show everything else.
                    Map<String, Long> populationByName = new HashMap<>();
                    try {
                        String populationJson = downloadJsonFromUrl(POPULATION_API_URL);
                        populationByName = buildPopulationMap(populationJson);
                    } catch (Exception populationException) {
                        Log.w(LOG_TAG, "No population data, continuing without it.");
                    }

                    List<Country> countries = parseCountriesJson(countriesJson, populationByName);
                    Log.d(LOG_TAG, "Built " + countries.size() + " countries.");

                    callback.onCountriesLoaded(countries);
                } catch (Exception exception) {
                    Log.e(LOG_TAG, "Download failed: " + exception, exception);
                    callback.onError(exception.getMessage());
                }
            }
        });
        workerThread.start();
    }

    // Downloads the text at a web address and returns it as one big String.
    private String downloadJsonFromUrl(String targetUrl) throws Exception {
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        try {
            URL url = new URL(targetUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECTION_TIMEOUT_MILLIS);
            connection.setReadTimeout(CONNECTION_TIMEOUT_MILLIS);

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new Exception("Server returned error code: " + responseCode);
            }

            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder responseBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                responseBuilder.append(line);
            }
            return responseBuilder.toString();
        } finally {
            if (reader != null) {
                reader.close();
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    // Makes a quick lookup of country name -> population so we can fill it in fast.
    private Map<String, Long> buildPopulationMap(String populationJson) throws Exception {
        Map<String, Long> populationByName = new HashMap<>();
        JSONArray populationArray = new JSONArray(populationJson);
        for (int i = 0; i < populationArray.length(); i++) {
            JSONObject entry = populationArray.getJSONObject(i);
            String name = entry.optString("country", "").trim().toLowerCase(Locale.ROOT);
            long population = entry.optLong("population", 0);
            if (!name.isEmpty()) {
                populationByName.put(name, population);
            }
        }
        return populationByName;
    }

    // Goes through the main list and builds a Country for each one.
    private List<Country> parseCountriesJson(String rawJson,
                                             Map<String, Long> populationByName) throws Exception {
        List<Country> countries = new ArrayList<>();
        JSONArray countriesArray = new JSONArray(rawJson);

        for (int i = 0; i < countriesArray.length(); i++) {
            JSONObject countryObject = countriesArray.getJSONObject(i);

            String countryCode = countryObject.optString("cca3");
            if (countryCode.isEmpty()) {
                continue; // skip anything without a code
            }

            String commonName = parseCommonName(countryObject);
            String officialName = parseOfficialName(countryObject);
            String capital = parseCapital(countryObject);
            String region = countryObject.optString("region", "Unknown");
            long population = lookUpPopulation(commonName, officialName, populationByName);
            String flagUrl = buildFlagUrl(countryObject);
            String currency = parseCurrency(countryObject);
            String languages = parseLanguages(countryObject);

            countries.add(new Country(countryCode, commonName, officialName,
                    capital, region, population, flagUrl, currency, languages));
        }
        return countries;
    }

    // Finds the population by matching the name. Returns 0 if there's no match.
    private long lookUpPopulation(String commonName, String officialName,
                                  Map<String, Long> populationByName) {
        Long byCommon = populationByName.get(commonName.toLowerCase(Locale.ROOT));
        if (byCommon != null) {
            return byCommon;
        }
        Long byOfficial = populationByName.get(officialName.toLowerCase(Locale.ROOT));
        if (byOfficial != null) {
            return byOfficial;
        }
        return 0;
    }

    // Builds the flag link, e.g. "BG" turns into ".../bg.png".
    private String buildFlagUrl(JSONObject countryObject) {
        String twoLetterCode = countryObject.optString("cca2", "");
        if (twoLetterCode.isEmpty()) {
            return "";
        }
        return FLAG_IMAGE_URL_PREFIX + twoLetterCode.toLowerCase(Locale.ROOT) + ".png";
    }

    // The name is inside a "name" object with "common" and "official".
    private String parseCommonName(JSONObject countryObject) {
        JSONObject nameObject = countryObject.optJSONObject("name");
        return (nameObject != null) ? nameObject.optString("common", "Unknown") : "Unknown";
    }

    private String parseOfficialName(JSONObject countryObject) {
        JSONObject nameObject = countryObject.optJSONObject("name");
        return (nameObject != null) ? nameObject.optString("official", "Unknown") : "Unknown";
    }

    // "capital" is a list; we take the first one.
    private String parseCapital(JSONObject countryObject) {
        JSONArray capitalArray = countryObject.optJSONArray("capital");
        if (capitalArray != null && capitalArray.length() > 0) {
            return capitalArray.optString(0, "N/A");
        }
        return "N/A";
    }

    // "currencies" looks like { "BGN": { "name": "Bulgarian lev" } }.
    // We take the first one and make it read "Bulgarian lev (BGN)".
    private String parseCurrency(JSONObject countryObject) {
        JSONObject currenciesObject = countryObject.optJSONObject("currencies");
        if (currenciesObject != null && currenciesObject.length() > 0) {
            Iterator<String> currencyCodes = currenciesObject.keys();
            if (currencyCodes.hasNext()) {
                String currencyCode = currencyCodes.next();
                JSONObject currencyData = currenciesObject.optJSONObject(currencyCode);
                if (currencyData != null) {
                    String currencyName = currencyData.optString("name", "");
                    if (!currencyName.isEmpty()) {
                        return currencyName + " (" + currencyCode + ")";
                    }
                    return currencyCode;
                }
            }
        }
        return "N/A";
    }

    // "languages" looks like { "bul": "Bulgarian" }. We join them with commas.
    private String parseLanguages(JSONObject countryObject) {
        JSONObject languagesObject = countryObject.optJSONObject("languages");
        if (languagesObject != null && languagesObject.length() > 0) {
            StringBuilder languagesBuilder = new StringBuilder();
            Iterator<String> languageKeys = languagesObject.keys();
            while (languageKeys.hasNext()) {
                String key = languageKeys.next();
                if (languagesBuilder.length() > 0) {
                    languagesBuilder.append(", ");
                }
                languagesBuilder.append(languagesObject.optString(key));
            }
            return languagesBuilder.toString();
        }
        return "N/A";
    }
}