package bg.nbu.f114191.countryexplorer.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import java.util.List;

import bg.nbu.f114191.countryexplorer.database.CountryDatabaseHelper;
import bg.nbu.f114191.countryexplorer.models.Country;
import bg.nbu.f114191.countryexplorer.network.CountryApiClient;

// Runs in the background when the app starts. It downloads all the countries,
// saves them in the database, then sends a small "I'm done" message so the
// list can refresh. After that it shuts itself off.
public class DataSyncService extends Service {

    // The name of the "I'm done" message other parts of the app wait for.
    public static final String ACTION_SYNC_FINISHED =
            "bg.nbu.f114191.countryexplorer.SYNC_FINISHED";
    public static final String EXTRA_SYNC_SUCCESS = "sync_success";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        CountryApiClient apiClient = new CountryApiClient();

        apiClient.fetchAllCountries(new CountryApiClient.CountryApiCallback() {
            @Override
            public void onCountriesLoaded(List<Country> countries) {
                // This runs on the background thread, so saving to the database here is fine.
                CountryDatabaseHelper databaseHelper =
                        new CountryDatabaseHelper(getApplicationContext());
                databaseHelper.replaceAllCachedCountries(countries);

                broadcastSyncFinished(true);
                stopSelf();
            }

            @Override
            public void onError(String errorMessage) {
                broadcastSyncFinished(false);
                stopSelf();
            }
        });

        return START_NOT_STICKY;
    }

    // Sends the "done" message, but only inside our own app so nothing else can catch it.
    private void broadcastSyncFinished(boolean wasSuccessful) {
        Intent broadcastIntent = new Intent(ACTION_SYNC_FINISHED);
        broadcastIntent.setPackage(getPackageName());
        broadcastIntent.putExtra(EXTRA_SYNC_SUCCESS, wasSuccessful);
        sendBroadcast(broadcastIntent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // nothing connects directly to this service
    }
}