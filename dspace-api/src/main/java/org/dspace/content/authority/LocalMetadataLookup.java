/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.dspace.content.authority.ChoiceAuthorityServiceImpl.CHOICES_INDEX_PREFIX;
import static org.dspace.content.authority.ChoiceAuthorityServiceImpl.CHOICES_ORDER_PREFIX;

public class LocalMetadataLookup implements ChoiceAuthority {

    private static final Logger log = LogManager.getLogger(LocalMetadataLookup.class);

    private String pluginInstanceName;

    private String field;

    private String order = "index";

    String separator = SearchUtils.FILTER_SEPARATOR;

    protected final ConfigurationService configurationService
            = DSpaceServicesFactory.getInstance().getConfigurationService();

    @Override
    public Choices getMatches(String text, int start, int limit, String locale) {
        SolrQuery solrQuery = new SolrQuery();

        solrQuery.setQuery("*:*");
        solrQuery.addFacetField(field);
        solrQuery.add("f." + field + "." + FacetParams.FACET_LIMIT, String.valueOf(limit + 1));
        solrQuery.setFacetPrefix(field, text.toLowerCase());
        solrQuery.set(CommonParams.START, 0);
        solrQuery.set(CommonParams.ROWS, 0);
        solrQuery.add("f." + field + "." + FacetParams.FACET_SORT, order);
        solrQuery.add("f." + field + "." + FacetParams.FACET_MINCOUNT, String.valueOf(1));
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

    @Override
    public Choices getBestMatch(String text, String locale) {
        Choices matches = getMatches(text, 0, 1, locale);
        if (matches.values.length != 0 && !matches.values[0].value.equalsIgnoreCase(text)) {
            matches = new Choices(false);
        }
        return matches;
    }

    @Override
    public String getLabel(String key, String locale) {
        Choice match = getMatches(key, 0, 1, locale).values[0];
        return match.label;
    }

    @Override
    public String getPluginInstanceName() {
        return pluginInstanceName;
    }

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
                fieldName = ((String) conf.getKey()).substring(ChoiceAuthorityServiceImpl.CHOICES_PLUGIN_PREFIX.length());
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
