/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.core.I18nUtil;

/**
 * Mapping between Country codes, English Country names,
 * Continent Codes, and English Continent names
 *
 * @author kevinvandevelde at atmire.com
 * @author ben at atmire.com
 */
public class LocationUtils {
    private static final Logger logger = LogManager.getLogger(LocationUtils.class);

    private static final Properties countryToContinent = new Properties();

    private static final String CONTINENT_NAMES_BUNDLE
        = LocationUtils.class.getPackage().getName() + ".continent-names";

    /**
     * Default constructor
     */
    private LocationUtils() {
    }

    /**
     * Map DSpace continent codes onto ISO country codes.
     *
     * @param countryCode ISO 3166-1 alpha-2 country code.
     * @return DSpace 2-character code for continent containing that country, or
     * an error message string.
     */
    static public String getContinentCode(String countryCode) {
        if (null == countryCode) {
            logger.info("Null country code");
            return I18nUtil
                .getMessage("org.dspace.statistics.util.LocationUtils.unknown-continent");
        }

        if (countryToContinent.isEmpty()) {
            try {
                countryToContinent.load(LocationUtils.class
                    .getResourceAsStream("country-continent-codes.properties"));
            } catch (IOException e) {
                logger.error("Could not load country/continent map file", e);
            }
        }

        String continent = countryToContinent.getProperty(countryCode);
        if (null == continent) {
            logger.info("Unknown country code " + countryCode);
            return I18nUtil
                .getMessage("org.dspace.statistics.util.LocationUtils.unknown-continent");
        } else {
            return continent;
        }
    }

    /**
     * Map DSpace continent codes onto localized continent names.
     *
     * @param continentCode DSpace 2-character code for a continent.
     * @param locale        The desired localization.
     * @return Localized name of the continent, or an error message string.
     */
    static public String getContinentName(String continentCode, Locale locale) {
        ResourceBundle names;

        if (null == locale) {
            locale = Locale.US;
        }

        if (null == continentCode) {
            logger.info("Null continentCode");
            return I18nUtil
                .getMessage("org.dspace.statistics.util.LocationUtils.unknown-continent");
        }

        try {
            names = ResourceBundle.getBundle(CONTINENT_NAMES_BUNDLE, locale);
        } catch (MissingResourceException e) {
            logger.error("Could not load continent code/name resource bundle",
                e);
            return I18nUtil
                .getMessage("org.dspace.statistics.util.LocationUtils.unknown-continent");
        }

        String name;
        try {
            name = names.getString(continentCode);
        } catch (MissingResourceException e) {
            logger.info("No continent code " + continentCode + " in bundle "
                        + names.getLocale().getDisplayName());
            return I18nUtil
                .getMessage("org.dspace.statistics.util.LocationUtils.unknown-continent");
        }
        return name;
    }

    /**
     * Revert a country name back into a country code (iso2)
     * Source: https://stackoverflow.com/a/38588988
     *
     * @param countryName Name of country (according to Locale)
     * @return Corresponding iso2 country code
     */
    static public String getCountryCode(String countryName) {
        // Get all country codes in a string array.
        String[] isoCountryCodes = Locale.getISOCountries();
        Map<String, String> countryMap = new HashMap<>();
        Locale locale;
        String name;

        // Iterate through all country codes:
        for (String code : isoCountryCodes) {
            // Create a locale using each country code
            locale = new Locale("", code);
            // Get country name for each code.
            name = locale.getDisplayCountry();
            // Map all country names and codes in key - value pairs.
            countryMap.put(name, code);
        }

        // Return the country code for the given country name using the map.
        // Here you will need some validation or better yet
        // a list of countries to give to user to choose from.
        return countryMap.get(countryName); // "NL" for Netherlands.
    }

    /**
     * Map ISO country codes onto localized country names.
     *
     * @param countryCode ISO 3166-1 alpha-2 country code.
     * @param locale      Desired localization.
     * @return Localized name of the country, or an error message string.
     */
    static public String getCountryName(String countryCode, Locale locale) {
        if (null == countryCode) {
            return I18nUtil
                .getMessage("org.dspace.statistics.util.LocationUtils.unknown-country");
        }

        Locale country = new Locale("EN", countryCode);
        String name = country.getDisplayCountry(locale);
        if (name.isEmpty()) {
            return I18nUtil
                .getMessage("org.dspace.statistics.util.LocationUtils.unknown-country");
        } else {
            return name;
        }
    }
}
