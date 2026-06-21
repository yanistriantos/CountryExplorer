package bg.nbu.f114191.countryexplorer.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import bg.nbu.f114191.countryexplorer.R;
import bg.nbu.f114191.countryexplorer.models.Country;

/**
 * The adapter does three jobs:
 *   - creates a new row view when one is needed (onCreateViewHolder)
 *   - fills a row with one country's data (onBindViewHolder)
 *   - reports how many rows there are (getItemCount)
 */
public class CountryAdapter extends RecyclerView.Adapter<CountryAdapter.CountryViewHolder> {

    /**
     * Lets the screen react when a row is tapped. The Activity/Fragment that
     * owns the list implements this and decides what happens (open details).
     */
    public interface OnCountryClickListener {
        void onCountryClicked(Country country);
    }

    private final Context context;
    private final List<Country> countries;
    private final OnCountryClickListener clickListener;

    // Used to jump back to the main thread when a flag image finishes loading.
    private final Handler mainThreadHandler;

    public CountryAdapter(Context context, OnCountryClickListener clickListener) {
        this.context = context;
        this.countries = new ArrayList<>();
        this.clickListener = clickListener;
        this.mainThreadHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Swaps in a fresh list of countries and tells the RecyclerView to redraw.
     * We use this for the first load, for search filtering, and for refreshing
     * favourites.
     */
    public void updateCountries(List<Country> newCountries) {
        countries.clear();
        countries.addAll(newCountries);
        notifyDataSetChanged();
    }

    /**
     * Called when the RecyclerView needs a brand-new empty row. We inflate
     * (build) our item_country.xml layout and wrap it in a ViewHolder.
     */
    @NonNull
    @Override
    public CountryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View rowView = LayoutInflater.from(context)
                .inflate(R.layout.item_country, parent, false);
        return new CountryViewHolder(rowView);
    }

    /**
     * Called to put ONE country's data into an existing row at a given position.
     * This runs constantly as the user scrolls and rows get recycled.
     */
    @Override
    public void onBindViewHolder(@NonNull CountryViewHolder holder, int position) {
        final Country country = countries.get(position);

        holder.countryNameTextView.setText(country.getCommonName());
        holder.countryRegionTextView.setText(country.getRegion());

        // When this row is tapped, tell the listener which country was chosen.
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clickListener.onCountryClicked(country);
            }
        });

        loadFlagImage(holder.countryFlagImageView, country.getFlagImageUrl());
    }

    @Override
    public int getItemCount() {
        return countries.size();
    }

    /**
     * Downloads a flag image from its URL on a worker thread (no image library
     * is allowed, so we do it by hand) and shows it on the main thread.
     *
     * Because rows are recycled, by the time an image finishes downloading the
     * row may already show a different country. To prevent the wrong flag from
     * appearing, we tag the ImageView with its URL and only set the bitmap if
     * the tag still matches when the download completes.
     */
    private void loadFlagImage(final ImageView imageView, final String flagUrl) {
        imageView.setTag(flagUrl);
        imageView.setImageDrawable(null); // clear the old flag while loading

        if (flagUrl == null || flagUrl.isEmpty()) {
            return;
        }

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

                    mainThreadHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            // Only show it if this row still wants THIS flag.
                            if (flagUrl.equals(imageView.getTag())) {
                                imageView.setImageBitmap(flagBitmap);
                            }
                        }
                    });
                } catch (Exception exception) {
                    // If a flag fails to load we simply leave it blank.
                }
            }
        }).start();
    }

    /**
     * CountryViewHolder
     * -----------------
     * Holds the views inside one row so we don't have to look them up with
     * findViewById every single time a row is bound. This is the standard
     * RecyclerView performance pattern.
     */
    static class CountryViewHolder extends RecyclerView.ViewHolder {

        final ImageView countryFlagImageView;
        final TextView countryNameTextView;
        final TextView countryRegionTextView;

        CountryViewHolder(View rowView) {
            super(rowView);
            countryFlagImageView = rowView.findViewById(R.id.countryFlagImageView);
            countryNameTextView = rowView.findViewById(R.id.countryNameTextView);
            countryRegionTextView = rowView.findViewById(R.id.countryRegionTextView);
        }
    }
}