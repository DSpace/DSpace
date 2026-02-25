/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.customurl.service;

import static java.util.Optional.ofNullable;
import static org.dspace.content.Item.ANY;

import java.sql.SQLException;
import java.text.Normalizer;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.customurl.CustomUrlService;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.indexobject.IndexableItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link CustomUrlService}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class CustomUrlServiceImpl implements CustomUrlService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomUrlServiceImpl.class);

    // Pre-compiled regex pattern for performance
    private static final String NUMERIC_SUFFIX_PATTERN = "-(\\d+)$";


    @Autowired
    private ItemService itemService;

    @Autowired
    private SearchService searchService;

    @Override
    public Optional<String> getCustomUrl(Item item) {
        return ofNullable(itemService.getMetadataFirstValue(item, "dspace", "customurl", null, Item.ANY));
    }

    @Override
    public List<String> getOldCustomUrls(Item item) {
        return itemService.getMetadataByMetadataString(item, "dspace.customurl.old").stream()
                          .map(MetadataValue::getValue)
                          .collect(Collectors.toList());
    }

    @Override
    public List<String> getAllCustomUrls(Item item) {
        return Stream.concat(getCustomUrl(item).stream(), getOldCustomUrls(item).stream())
                     .collect(Collectors.toList());
    }

    @Override
    public void replaceCustomUrl(Context context, Item item, String newUrl) {
        try {
            itemService.clearMetadata(context, item, "dspace", "customurl", null, ANY);
            itemService.addMetadata(context, item, "dspace", "customurl", null, null, newUrl);
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    @Override
    public void deleteAnyOldCustomUrlEqualsTo(Context context, Item item, String customUrl) {

        List<MetadataValue> redirectedUrls = getRedirectedUrlMetadataValuesWithValue(item, customUrl);
        if (CollectionUtils.isNotEmpty(redirectedUrls)) {
            deleteMetadataValues(context, item, redirectedUrls);
        }

    }

    @Override
    public void addOldCustomUrl(Context context, Item item, String url) {
        try {
            itemService.addMetadata(context, item, "dspace", "customurl", "old", null, url);
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    @Override
    public void deleteCustomUrl(Context context, Item item) {
        try {
            itemService.clearMetadata(context, item, "dspace", "customurl", null, ANY);
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    @Override
    public void deleteAllOldCustomUrls(Context context, Item item) {
        try {
            itemService.clearMetadata(context, item, "dspace", "customurl", "old", ANY);
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    @Override
    public void deleteOldCustomUrlByIndex(Context context, Item item, int index) {

        List<MetadataValue> redirectedUrls = itemService.getMetadata(item, "dspace", "customurl", "old", ANY);

        if (index >= redirectedUrls.size()) {
            throw new IllegalArgumentException(
                "The provided index is not consistent with the cardinality of the old custom urls");
        }

        deleteMetadataValues(context, item, List.of(redirectedUrls.get(index)));
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Optional<Item> findItemByCustomUrl(Context context, String customUrl) {
        if (StringUtils.isBlank(customUrl)) {
            return Optional.empty();
        }
        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.addDSpaceObjectFilter(IndexableItem.TYPE);
        discoverQuery.addFilterQueries("customurl:" + searchService.escapeQueryChars(customUrl));
        discoverQuery.addFilterQueries("latestVersion:true");
        discoverQuery.setIncludeNotDiscoverableOrWithdrawn(true);

        List<IndexableObject> indexableObjects = findIndexableObjects(context, discoverQuery);

        if (CollectionUtils.isEmpty(indexableObjects)) {
            return Optional.empty();
        }

        if (indexableObjects.size() > 1) {
            LOGGER.error("Found multiple item with the same custom URL {} - Ids: {}", customUrl,
                         getIds(indexableObjects));
            throw new IllegalStateException("Found multiple item with the same custom URL: " + customUrl);
        }

        return Optional.of(indexableObjects.get(0))
                       .map(indexableObject -> (Item) indexableObject.getIndexedObject());

    }

    @Override
    public Optional<String> findLatestCustomUrlByPattern(Context context, String basePattern) {
        if (StringUtils.isBlank(basePattern)) {
            return Optional.empty();
        }

        List<String> matchingUrls = findCustomUrlsWithPattern(context, basePattern);
        if (matchingUrls.isEmpty()) {
            return Optional.empty();
        }

        return findUrlWithHighestNumericSuffix(matchingUrls, basePattern);
    }

    @Override
    public String generateUniqueCustomUrl(Context context, String rawInput) {

        if (StringUtils.isBlank(rawInput)) {
            throw new IllegalArgumentException("Input for custom URL generation cannot be null or empty");
        }

        String cleanBase = normalizeUrl(rawInput);
        if (StringUtils.isBlank(cleanBase)) {
            throw new IllegalArgumentException("Input '" + rawInput + "' does not contain any valid " +
                                                   "alphanumeric characters to generate a URL");
        }

        if (!customUrlExists(context, cleanBase)) {
            return cleanBase;
        }

        return findNextAvailableUrl(context, cleanBase);
    }

    /**
     * Normalizes a given string into a URL-friendly "slug".
     * <p>
     * The normalization process follows these steps:
     * <ul>
     * <li>Decomposes Unicode characters to remove accents (e.g., "é" becomes "e").</li>
     * <li>Converts the entire string to lowercase.</li>
     * <li>Replaces all sequences of non-alphanumeric characters with a single hyphen.</li>
     * <li>Trims leading and trailing hyphens.</li>
     * </ul>
     * </p>
     * * @param text the raw string to be normalized (e.g., an Item title or metadata value).
     *
     * @return a sanitized, lowercase, hyphenated string suitable for use in a URL,
     * or an empty string if the input is {@code null} or blank.
     */
    private String normalizeUrl(String text) {
        if (StringUtils.isBlank(text)) {
            return "";
        }

        // 1. Remove accents (Normalize Unicode to ASCII)
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}", "");

        // 2. Lowercase and replace anything NOT alphanumeric with a dash
        normalized = normalized.toLowerCase()
                               .replaceAll("[^a-z0-9]+", "-");

        // 3. Remove leading/trailing dashes that might result from step 2
        normalized = normalized.replaceAll("^-|-$", "");

        return normalized;
    }

    /**
     * Searches for all custom URLs that start with the given base pattern.
     * This method is optimized to fetch only the URLs from Solr without retrieving full Item objects.
     * Only considers the latest versions of items.
     *
     * @param context     DSpace context
     * @param basePattern the base pattern to search for
     * @return list of matching custom URLs from latest versions
     */
    private List<String> findCustomUrlsWithPattern(Context context, String basePattern) {
        try {
            DiscoverQuery discoverQuery = new DiscoverQuery();
            discoverQuery.addDSpaceObjectFilter(IndexableItem.TYPE);
            discoverQuery.addFilterQueries("customurl:" + searchService.escapeQueryChars(basePattern) + "*");
            discoverQuery.addFilterQueries("latestVersion:true");
            discoverQuery.setIncludeNotDiscoverableOrWithdrawn(true);

            List<IndexableObject> indexableObjects = searchService.search(context, discoverQuery)
                                                                  .getIndexableObjects();

            return indexableObjects.stream()
                                   .map(indexableObject -> (Item) indexableObject.getIndexedObject())
                                   .flatMap(item -> getAllCustomUrls(item).stream())
                                   .filter(url -> url.startsWith(basePattern))
                                   .distinct()
                                   .collect(Collectors.toList());

        } catch (SearchServiceException e) {
            LOGGER.error("Error searching for custom URLs with pattern: {}", basePattern, e);
            throw new RuntimeException("Failed to search for custom URLs", e);
        }
    }

    /**
     * Finds the URL with the highest numeric suffix from a list of URLs.
     * Efficiently processes URLs to identify numeric suffixes and return the latest.
     *
     * @param matchingUrls list of URLs that match the base pattern
     * @param basePattern  the base pattern
     * @return the URL with the highest numeric suffix, or empty if none found
     */
    private Optional<String> findUrlWithHighestNumericSuffix(List<String> matchingUrls, String basePattern) {
        Pattern pattern = Pattern.compile(Pattern.quote(basePattern) + NUMERIC_SUFFIX_PATTERN);

        return matchingUrls.stream()
                           .filter(url -> pattern.matcher(url).matches())
                           .max((url1, url2) -> {
                               int num1 = extractNumericSuffix(url1, pattern);
                               int num2 = extractNumericSuffix(url2, pattern);
                               return Integer.compare(num1, num2);
                           })
                           .or(() -> matchingUrls.contains(basePattern)
                               ? Optional.of(basePattern)
                               : Optional.empty());
    }

    /**
     * Extracts numeric suffix from a URL using the provided pattern.
     *
     * @param url     the URL to extract from
     * @param pattern the compiled pattern
     * @return the numeric suffix, or 0 if extraction fails
     */
    private int extractNumericSuffix(String url, Pattern pattern) {
        Matcher matcher = pattern.matcher(url);
        if (matcher.matches()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                LOGGER.debug("Invalid numeric suffix in URL: {}", url);
            }
        }
        return 0;
    }

    /**
     * Checks if a custom URL already exists without retrieving the full item.
     * Only checks against the latest versions of items.
     *
     * @param context   DSpace context
     * @param customUrl the custom URL to check
     * @return true if the URL exists on a latest version, false otherwise
     */
    private boolean customUrlExists(Context context, String customUrl) {
        try {
            DiscoverQuery discoverQuery = new DiscoverQuery();
            discoverQuery.addDSpaceObjectFilter(IndexableItem.TYPE);
            discoverQuery.addFilterQueries("customurl:" + searchService.escapeQueryChars(customUrl));
            discoverQuery.addFilterQueries("latestVersion:true");
            discoverQuery.setIncludeNotDiscoverableOrWithdrawn(true);
            discoverQuery.setMaxResults(1); // We only need to know if any exist

            return !searchService.search(context, discoverQuery).getIndexableObjects().isEmpty();

        } catch (SearchServiceException e) {
            LOGGER.error("Error checking if custom URL exists: {}", customUrl, e);
            // Conservative approach: assume it exists to avoid conflicts
            return true;
        }
    }

    /**
     * Finds the next available URL with a numeric suffix.
     * Uses the existing findLatestCustomUrlByPattern method to get the current highest,
     * then increments to find the next available.
     *
     * @param context DSpace context
     * @param baseUrl the base URL pattern
     * @return the next available URL with numeric suffix
     */
    private String findNextAvailableUrl(Context context, String baseUrl) {
        Optional<String> latest = findLatestCustomUrlByPattern(context, baseUrl);

        // If no latest URL found, or the match is exactly the base, start at -1
        if (latest.isEmpty() || latest.get().equals(baseUrl)) {
            return baseUrl + "-1";
        }

        // Extract and increment the numeric suffix
        Pattern pattern = Pattern.compile(Pattern.quote(baseUrl) + NUMERIC_SUFFIX_PATTERN);
        Matcher matcher = pattern.matcher(latest.get());

        if (matcher.matches()) {
            try {
                int currentNumber = Integer.parseInt(matcher.group(1));
                if (currentNumber == Integer.MAX_VALUE) {
                    throw new IllegalStateException("No more custom URLs available for base: " + baseUrl);
                }
                return baseUrl + "-" + (currentNumber + 1);
            } catch (NumberFormatException e) {
                LOGGER.warn("Invalid numeric suffix in URL: {}", latest);
            }
        }

        // Fallback: append -1 to base URL
        return baseUrl + "-1";
    }

    @SuppressWarnings("rawtypes")
    private List<IndexableObject> findIndexableObjects(Context context, DiscoverQuery discoverQuery) {
        try {
            return searchService.search(context, discoverQuery).getIndexableObjects();
        } catch (SearchServiceException e) {
            throw new RuntimeException(e);
        }
    }

    private void deleteMetadataValues(Context context, Item item, List<MetadataValue> metadataValues) {
        try {
            itemService.removeMetadataValues(context, item, metadataValues);
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    private List<MetadataValue> getRedirectedUrlMetadataValuesWithValue(Item item, String value) {
        return itemService.getMetadataByMetadataString(item, "dspace.customurl.old").stream()
                          .filter(metadataValue -> value.equals(metadataValue.getValue()))
                          .collect(Collectors.toList());
    }

    @SuppressWarnings("rawtypes")
    private List<Object> getIds(List<IndexableObject> indexableObjects) {
        return indexableObjects.stream()
                               .map(IndexableObject::getID)
                               .collect(Collectors.toList());
    }

}
