/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import static org.dspace.content.authority.ChoiceAuthorityServiceImpl.CHOICES_INDEX_PREFIX;
import static org.dspace.content.authority.ChoiceAuthorityServiceImpl.CHOICES_ORDER_PREFIX;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.FacetParams;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.SolrSearchCore;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;


/**
 * This class provides a mechanism to retrieve metadata values
 * using Solr for auto-completion.
 * It implements the {@link ChoiceAuthority} interface to offer choices
 * for metadata values based on a prefix text.
 *
 * <p>The implementation leverages Solr facets to find matching metadata
 * values and supports configuration through the {@link ConfigurationService}.
 * It can be used as a plugin to enable dynamic metadata lookups
 * for specific fields.</p>
 */
public class LocalMetadataLookup implements ChoiceAuthority {

    private static final Logger log = LogManager.getLogger(LocalMetadataLookup.class);

    private String pluginInstanceName;

    private String field;

    private String order = "index";

    String separator = SearchUtils.FILTER_SEPARATOR;

    protected final ConfigurationService configurationService
            = DSpaceServicesFactory.getInstance().getConfigurationService();

    /**
     * Retrieves matching metadata values from Solr based on the given prefix text.
     *
     * @param text   The prefix text to match.
     * @param start  The starting index for the results.
     * @param limit  The maximum number of results to retrieve.
     * @param locale The locale for the metadata lookup.
     * @return A {@link Choices} object containing the matching values.
     */
    @Override
    public Choices getMatches(String text, int start, int limit, String locale) {
        SolrQuery solrQuery = new SolrQuery();

        // Retrieve all documents with "*:*" query as we are only interested in facet values.
        solrQuery.setQuery("*:*");
        // Add a facet field for the specified field to group and count its distinct values.
        solrQuery.addFacetField(field);
        // Limit the number of facet values returned to (limit + 1) to check if there are more results.
        solrQuery.add("f." + field + "." + FacetParams.FACET_LIMIT, String.valueOf(limit + 1));
        // Apply a facet prefix to filter values that start with the given text (case-insensitive).
        solrQuery.setFacetPrefix(field, text.toLowerCase());
        // Start facet value retrieval from index 0 (first result).
        solrQuery.set(CommonParams.START, 0);
        // Set rows to 0, as we only need facet data, not actual documents.
        solrQuery.set(CommonParams.ROWS, 0);
        // Sort facet values based on the configured "order" (e.g., "index" for alphabetical or "count").
        solrQuery.add("f." + field + "." + FacetParams.FACET_SORT, order);
        // Include only facet values with at least one occurrence (minimum count = 1).
        solrQuery.add("f." + field + "." + FacetParams.FACET_MINCOUNT, String.valueOf(1));
        // Apply a filter query to restrict results to latest version items.
        solrQuery.set(CommonParams.FQ, "search.resourcetype:Item AND latestVersion:true");

        Choices result;

        try {
            int max = 0;
            boolean hasMore = false;
            SolrSearchCore solrSearchCore = getSearchCore();
            QueryResponse searchResponse = solrSearchCore.getSolr().query(solrQuery, solrSearchCore.REQUEST_METHOD);
            List<FacetField.Count> facetValues = searchResponse.getFacetFields().get(0).getValues();
            ArrayList<Choice> choices = new ArrayList<>();
            if (facetValues != null && !facetValues.isEmpty()) {
                max = facetValues.size();

                for (FacetField.Count facet : facetValues) {
                    String value = getDisplayValue(facet.getName());
                    // in this case, the authority is not taken into account
                    choices.add(new Choice(null, value, value));
                }

                hasMore = true;
            }

            int confidence;
            if (choices.isEmpty()) {
                confidence = Choices.CF_NOTFOUND;
            } else if (choices.size() == 1) {
                confidence = Choices.CF_UNCERTAIN;
            } else {
                confidence = Choices.CF_AMBIGUOUS;
            }

            result = new Choices(
                    choices.toArray(new Choice[choices.size()]),
                    start,
                    hasMore ? max : choices.size() + start,
                    confidence,
                    hasMore
            );
        } catch (IOException | SolrServerException e) {
            log.error("Error while retrieving values {field: " + field + ", prefix:" + text + "}", e);
            result = new Choices(true);
        }

        return result;
    }

    /**
     * Retrieves the best match from Solr for a given input text.
     *
     * @param text   The input text to match.
     * @param locale The locale for the metadata lookup.
     * @return A {@link Choices} object containing the best match.
     */
    @Override
    public Choices getBestMatch(String text, String locale) {
        Choices matches = getMatches(text, 0, 1, locale);
        if (matches.values.length != 0 && !matches.values[0].value.equalsIgnoreCase(text)) {
            matches = new Choices(false);
        }
        return matches;
    }

    /**
     * Retrieves the label for a given metadata key.
     *
     * @param key    The metadata key for which the label is required.
     * @param locale The locale for the metadata lookup.
     * @return The label corresponding to the metadata key.
     */
    @Override
    public String getLabel(String key, String locale) {
        Choice match = getMatches(key, 0, 1, locale).values[0];
        return match.label;
    }

    /**
     * Gets the name of the plugin instance.
     *
     * @return The plugin instance name.
     */
    @Override
    public String getPluginInstanceName() {
        return pluginInstanceName;
    }

    /**
     * Sets the name of the plugin instance and initializes
     * related configuration properties.
     *
     * @param name The name of the plugin instance.
     */
    @Override
    public void setPluginInstanceName(String name) {
        this.pluginInstanceName = name;
        String fieldName = "";

        String separatorFromConfig = configurationService.getProperty("discovery.solr.facets.split.char");

        if (separatorFromConfig != null) {
            separator = separatorFromConfig;
        }

        for (Map.Entry conf : configurationService.getProperties().entrySet()) {
            if (StringUtils.startsWith((String) conf.getKey(), ChoiceAuthorityServiceImpl.CHOICES_PLUGIN_PREFIX)
                    && StringUtils.equals((String) conf.getValue(), name)) {
                fieldName = ((String) conf.getKey())
                        .substring(ChoiceAuthorityServiceImpl.CHOICES_PLUGIN_PREFIX.length());
                field = fieldName;
                // exit the look immediately as we have found it
                break;
            }
        }

        for (Map.Entry conf : configurationService.getProperties().entrySet()) {
            if (conf.getKey().equals(CHOICES_ORDER_PREFIX + fieldName)) {
                order = ((String) conf.getValue());
            }

            if (conf.getKey().equals(CHOICES_INDEX_PREFIX + fieldName)) {
                field = ((String) conf.getValue());
            }
        }
    }

    private SolrSearchCore getSearchCore() {
        return DSpaceServicesFactory
                .getInstance()
                .getServiceManager()
                .getServicesByType(SolrSearchCore.class)
                .get(0);
    }

    private String getDisplayValue(String value) {
        //Escape any regex chars
        String currentSeparator = java.util.regex.Pattern.quote(separator);
        String[] fqParts = value.split(currentSeparator);
        StringBuilder valueBuffer = new StringBuilder();
        int start = fqParts.length / 2;
        for (int i = start; i < fqParts.length; i++) {
            String[] split = fqParts[i].split(SearchUtils.AUTHORITY_SEPARATOR, 2);
            valueBuffer.append(split[0]);
        }
        return valueBuffer.toString();
    }
}
