package bg.nbu.f114191.countryexplorer.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

import bg.nbu.f114191.countryexplorer.models.Country;

// Does all the saving and loading with the phone's SQLite database.
// There are two tables with the same columns: one keeps all the countries
// (so the app works without internet), the other keeps the user's favourites.
// They're kept apart so refreshing the country list never wipes the favourites.
public class CountryDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "country_explorer.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_CACHED_COUNTRIES = "cached_countries";
    private static final String TABLE_FAVOURITE_COUNTRIES = "favourite_countries";

    private static final String COLUMN_COUNTRY_CODE = "country_code";   // the id of each row
    private static final String COLUMN_COMMON_NAME = "common_name";
    private static final String COLUMN_OFFICIAL_NAME = "official_name";
    private static final String COLUMN_CAPITAL = "capital";
    private static final String COLUMN_REGION = "region";
    private static final String COLUMN_POPULATION = "population";
    private static final String COLUMN_FLAG_URL = "flag_url";
    private static final String COLUMN_CURRENCY = "currency";
    private static final String COLUMN_LANGUAGES = "languages";

    public CountryDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Both tables look the same, so we write the "make table" text once and reuse it.
    private String buildCreateTableStatement(String tableName) {
        return "CREATE TABLE " + tableName + " (" +
                COLUMN_COUNTRY_CODE + " TEXT PRIMARY KEY, " +
                COLUMN_COMMON_NAME + " TEXT, " +
                COLUMN_OFFICIAL_NAME + " TEXT, " +
                COLUMN_CAPITAL + " TEXT, " +
                COLUMN_REGION + " TEXT, " +
                COLUMN_POPULATION + " INTEGER, " +
                COLUMN_FLAG_URL + " TEXT, " +
                COLUMN_CURRENCY + " TEXT, " +
                COLUMN_LANGUAGES + " TEXT" +
                ")";
    }

    // Runs once, the first time the app opens the database. Makes the two tables.
    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(buildCreateTableStatement(TABLE_CACHED_COUNTRIES));
        database.execSQL(buildCreateTableStatement(TABLE_FAVOURITE_COUNTRIES));
    }

    // Runs if we ever change the database version. We just rebuild the tables.
    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        database.execSQL("DROP TABLE IF EXISTS " + TABLE_CACHED_COUNTRIES);
        database.execSQL("DROP TABLE IF EXISTS " + TABLE_FAVOURITE_COUNTRIES);
        onCreate(database);
    }

    // Turns a Country into the "column = value" form the database wants for one row.
    private ContentValues countryToContentValues(Country country) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_COUNTRY_CODE, country.getCountryCode());
        values.put(COLUMN_COMMON_NAME, country.getCommonName());
        values.put(COLUMN_OFFICIAL_NAME, country.getOfficialName());
        values.put(COLUMN_CAPITAL, country.getCapital());
        values.put(COLUMN_REGION, country.getRegion());
        values.put(COLUMN_POPULATION, country.getPopulation());
        values.put(COLUMN_FLAG_URL, country.getFlagImageUrl());
        values.put(COLUMN_CURRENCY, country.getCurrency());
        values.put(COLUMN_LANGUAGES, country.getLanguages());
        return values;
    }

    // Reads one row and turns it back into a Country.
    // (A Cursor is like a pointer that goes through the rows one by one.)
    private Country cursorToCountry(Cursor cursor) {
        String code = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COUNTRY_CODE));
        String commonName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COMMON_NAME));
        String officialName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_OFFICIAL_NAME));
        String capital = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CAPITAL));
        String region = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REGION));
        long population = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_POPULATION));
        String flagUrl = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FLAG_URL));
        String currency = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CURRENCY));
        String languages = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LANGUAGES));
        return new Country(code, commonName, officialName, capital,
                region, population, flagUrl, currency, languages);
    }

    // Clears the old country list and saves the new one. Doing it all at once is faster.
    public void replaceAllCachedCountries(List<Country> countries) {
        SQLiteDatabase database = getWritableDatabase();
        database.beginTransaction();
        try {
            database.delete(TABLE_CACHED_COUNTRIES, null, null);
            for (Country country : countries) {
                database.insert(TABLE_CACHED_COUNTRIES, null, countryToContentValues(country));
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    // Reads back every saved country, sorted A to Z.
    public List<Country> getAllCachedCountries() {
        List<Country> countries = new ArrayList<>();
        SQLiteDatabase database = getReadableDatabase();
        Cursor cursor = database.query(TABLE_CACHED_COUNTRIES, null, null, null,
                null, null, COLUMN_COMMON_NAME + " ASC");
        while (cursor.moveToNext()) {
            countries.add(cursorToCountry(cursor));
        }
        cursor.close();
        return countries;
    }

    // Saves a favourite. If it's already there it just overwrites it, so no doubles.
    public void addFavourite(Country country) {
        SQLiteDatabase database = getWritableDatabase();
        database.insertWithOnConflict(TABLE_FAVOURITE_COUNTRIES, null,
                countryToContentValues(country), SQLiteDatabase.CONFLICT_REPLACE);
    }

    // Removes a favourite by its code. The "?" is the safe way to put a value into a query.
    public void removeFavourite(String countryCode) {
        SQLiteDatabase database = getWritableDatabase();
        database.delete(TABLE_FAVOURITE_COUNTRIES,
                COLUMN_COUNTRY_CODE + " = ?", new String[]{countryCode});
    }

    // Checks if a country is in favourites (true/false).
    public boolean isFavourite(String countryCode) {
        SQLiteDatabase database = getReadableDatabase();
        Cursor cursor = database.query(TABLE_FAVOURITE_COUNTRIES,
                new String[]{COLUMN_COUNTRY_CODE},
                COLUMN_COUNTRY_CODE + " = ?", new String[]{countryCode},
                null, null, null);
        boolean isFavourite = cursor.getCount() > 0;
        cursor.close();
        return isFavourite;
    }

    // Reads back all favourites, sorted A to Z.
    public List<Country> getAllFavourites() {
        List<Country> favourites = new ArrayList<>();
        SQLiteDatabase database = getReadableDatabase();
        Cursor cursor = database.query(TABLE_FAVOURITE_COUNTRIES, null, null, null,
                null, null, COLUMN_COMMON_NAME + " ASC");
        while (cursor.moveToNext()) {
            favourites.add(cursorToCountry(cursor));
        }
        cursor.close();
        return favourites;
    }
}