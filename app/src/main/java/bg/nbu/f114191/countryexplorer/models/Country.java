package bg.nbu.f114191.countryexplorer.models;

// Holds the info for one country. We use this everywhere we move a country
// around - from the internet, into the database, and over to the detail screen.
public class Country {

    private final String countryCode;   // 3 letters, e.g. "BGR". We use it as the id in the database.
    private final String commonName;
    private final String officialName;
    private final String capital;
    private final String region;
    private final long population;
    private final String flagImageUrl;
    private final String currency;       // already made into text, e.g. "Bulgarian lev (BGN)"
    private final String languages;      // already joined, e.g. "Bulgarian, Turkish"

    // We pass in everything at once, so a country is always complete.
    public Country(String countryCode,
                   String commonName,
                   String officialName,
                   String capital,
                   String region,
                   long population,
                   String flagImageUrl,
                   String currency,
                   String languages) {
        this.countryCode = countryCode;
        this.commonName = commonName;
        this.officialName = officialName;
        this.capital = capital;
        this.region = region;
        this.population = population;
        this.flagImageUrl = flagImageUrl;
        this.currency = currency;
        this.languages = languages;
    }

    // These let other parts of the app read the values above.
    public String getCountryCode() {
        return countryCode;
    }

    public String getCommonName() {
        return commonName;
    }

    public String getOfficialName() {
        return officialName;
    }

    public String getCapital() {
        return capital;
    }

    public String getRegion() {
        return region;
    }

    public long getPopulation() {
        return population;
    }

    public String getFlagImageUrl() {
        return flagImageUrl;
    }

    public String getCurrency() {
        return currency;
    }

    public String getLanguages() {
        return languages;
    }
}