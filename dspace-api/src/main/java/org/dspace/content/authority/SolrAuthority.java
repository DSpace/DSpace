/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CommonParams;
import org.dspace.authority.AuthoritySearchService;
import org.dspace.authority.AuthorityValue;
import org.dspace.authority.SolrAuthorityInterface;
import org.dspace.authority.factory.AuthorityServiceFactory;
import org.dspace.authority.service.AuthorityValueService;
import org.dspace.core.NameAwarePlugin;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * @author Antoine Snyers (antoine at atmire.com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class SolrAuthority implements ChoiceAuthority {
    /** the name assigned to the specific instance by the PluginService, @see {@link NameAwarePlugin} **/
    private String authorityName;

    /**
     * the metadata managed by the plugin instance, derived from its authority name
     * in the form schema_element_qualifier
     */
    private String field;
    protected SolrAuthorityInterface source =
        DSpaceServicesFactory.getInstance().getServiceManager()
                             .getServiceByName("AuthoritySource", SolrAuthorityInterface.class);

    private static final Logger log = LogManager.getLogger(SolrAuthority.class);

    protected final AuthorityValueService authorityValueService
            = AuthorityServiceFactory.getInstance().getAuthorityValueService();

    protected final ConfigurationService configurationService
            = DSpaceServicesFactory.getInstance().getConfigurationService();

    public Choices getMatches(String text, int start, int limit, String locale,
                              boolean bestMatch) {
        if (limit == 0) {
            limit = 10;
        }

        SolrQuery queryArgs = new SolrQuery();
        if (text == null || text.trim().equals("")) {
            queryArgs.setQuery("*:*");
        } else {
            String searchField = "value";
            String localSearchField = "";
            try {
                //A downside of the authors is that the locale is sometimes a number, make sure that this isn't one
                Integer.parseInt(locale);
                locale = null;
            } catch (NumberFormatException e) {
                //Everything is allright
            }
            if (locale != null && !"".equals(locale)) {
                localSearchField = searchField + "_" + locale;
            }

            String query = "(" + toQuery(searchField, text) + ") ";
            if (!localSearchField.equals("")) {
                query += " or (" + toQuery(localSearchField, text) + ")";
            }
            queryArgs.setQuery(query);
        }

        queryArgs.addFilterQuery("field:" + field);
        queryArgs.set(CommonParams.START, start);
        //We add one to our facet limit so that we know if there are more matches
        int maxNumberOfSolrResults = limit + 1;
        queryArgs.set(CommonParams.ROWS, maxNumberOfSolrResults);

        String sortField = "value";
        String localSortField = "";
        if (StringUtils.isNotBlank(locale)) {
            localSortField = sortField + "_" + locale;
            queryArgs.addSort(localSortField, SolrQuery.ORDER.asc);
        } else {
            queryArgs.addSort(sortField, SolrQuery.ORDER.asc);
        }

        Choices result;
        try {
            int max = 0;
            boolean hasMore = false;
            QueryResponse searchResponse = getSearchService().search(queryArgs);
            SolrDocumentList authDocs = searchResponse.getResults();
            ArrayList<Choice> choices = new ArrayList<>();
            if (authDocs != null) {
                max = (int) searchResponse.getResults().getNumFound();
                int maxDocs = authDocs.size();
                if (limit < maxDocs) {
                    maxDocs = limit;
                }
                List<AuthorityValue> alreadyPresent = new ArrayList<>();
                for (int i = 0; i < maxDocs; i++) {
                    SolrDocument solrDocument = authDocs.get(i);
                    if (solrDocument != null) {
                        AuthorityValue val = authorityValueService.fromSolr(solrDocument);

                        Map<String, String> extras = val.choiceSelectMap();
                        extras.put("insolr", val.getId());
                        choices.add(new Choice(val.getId(), val.getValue(), val.getValue(), extras));
                        alreadyPresent.add(val);
                    }
                }

                if (StringUtils.isNotBlank(text)) {
                    int sizeFromSolr = alreadyPresent.size();
                    int maxExternalResults = sizeFromSolr < limit ? limit + 1 : sizeFromSolr + 1;
                    // force an upper limit for external results
                    if (maxExternalResults > 10) {
                        maxExternalResults = 10;
                    }
                    addExternalResults(text, choices, alreadyPresent, maxExternalResults);
                }

                // hasMore = (authDocs.size() == (limit + 1));
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

            result = new Choices(choices.toArray(new Choice[choices.size()]), start,
                                 hasMore ? max : choices.size() + start, confidence, hasMore);
        } catch (IOException | SolrServerException e) {
            log.error("Error while retrieving authority values {field: " + field + ", prefix:" + text + "}", e);
            result = new Choices(true);
        }

        return result;  //To change body of implemented methods use File | Settings | File Templates.
    }

    protected void addExternalResults(String text, ArrayList<Choice> choices, List<AuthorityValue> alreadyPresent,
                                      int max) {
        if (source != null) {
            try {
             // max has been already adapted to consider the need to filter already found entries
                List<AuthorityValue> values = source
                    .queryAuthorities(text, max);

                // filtering loop
                Iterator<AuthorityValue> iterator = values.iterator();
                while (iterator.hasNext()) {
                    AuthorityValue next = iterator.next();
                    if (alreadyPresent.contains(next)) {
                        iterator.remove();
                    }
                }

                // adding choices loop
                int added = 0;
                iterator = values.iterator();
                while (iterator.hasNext() && added < max) {
                    AuthorityValue val = iterator.next();
                    Map<String, String> extras = val.choiceSelectMap();
                    extras.put("insolr", "false");
                    choices.add(new Choice(val.generateString(), val.getValue(), val.getValue(), extras));
                    added++;
                }
            } catch (Exception e) {
                log.error("Error", e);
            }
        } else {
            log.warn("external source for authority not configured");
        }
    }

    private String toQuery(String searchField, String text) {
        return searchField + ":(" + text.toLowerCase().replaceAll(":", "\\\\:") + "*) or " + searchField + ":(" + text
            .toLowerCase().replaceAll(":", "\\\\:") + ")";
    }

    @Override
    public Choices getMatches(String text, int start, int limit, String locale) {
        return getMatches(text, start, limit, locale, true);
    }

    @Override
    public Choices getBestMatch(String text, String locale) {
        Choices matches = getMatches(text, 0, 1, locale, false);
        if (matches.values.length != 0 && !matches.values[0].value.equalsIgnoreCase(text)) {
            matches = new Choices(false);
        }
        return matches;
    }

    @Override
    public String getLabel(String key, String locale) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("requesting label for key " + key + " using locale " + locale);
            }
            SolrQuery queryArgs = new SolrQuery();
            queryArgs.setQuery("id:" + key.replaceAll(":", "\\\\:"));
            queryArgs.setRows(1);
            QueryResponse searchResponse = getSearchService().search(queryArgs);
            SolrDocumentList docs = searchResponse.getResults();
            if (docs.getNumFound() == 1) {
                String label = null;
                try {
                    label = (String) docs.get(0).getFieldValue("value_" + locale);
                } catch (Exception e) {
                    //ok to fail here
                }
                if (label != null) {
                    if (log.isDebugEnabled()) {
                        log.debug(
                            "returning label " + label + " for key " + key + " using locale " + locale + " and " +
                                "fieldvalue " + "value_" + locale);
                    }
                    return label;
                }
                try {
                    label = (String) docs.get(0).getFieldValue("value");
                } catch (Exception e) {
                    log.error("couldn't get field value for key " + key, e);
                }
                if (label != null) {
                    if (log.isDebugEnabled()) {
                        log.debug(
                            "returning label " + label + " for key " + key + " using locale " + locale + " and " +
                                "fieldvalue " + "value");
                    }
                    return label;
                }
                try {
                    label = (String) docs.get(0).getFieldValue("value_en");
                } catch (Exception e) {
                    log.error("couldn't get field value for key " + key, e);
                }
                if (label != null) {
                    if (log.isDebugEnabled()) {
                        log.debug(
                            "returning label " + label + " for key " + key + " using locale " + locale + " and " +
                                "fieldvalue " + "value_en");
                    }
                    return label;
                }
            }
        } catch (IOException | SolrServerException e) {
            log.error("error occurred while trying to get label for key " + key, e);
        }

        return key;
    }


    public static AuthoritySearchService getSearchService() {
        org.dspace.kernel.ServiceManager manager = DSpaceServicesFactory.getInstance().getServiceManager();

        return manager.getServiceByName(AuthoritySearchService.class.getName(), AuthoritySearchService.class);
    }

    @Override
    public void setPluginInstanceName(String name) {
        authorityName = name;
        for (Entry conf : configurationService.getProperties().entrySet()) {
            if (StringUtils.startsWith((String) conf.getKey(), ChoiceAuthorityServiceImpl.CHOICES_PLUGIN_PREFIX)
                    && StringUtils.equals((String) conf.getValue(), authorityName)) {
                field = ((String) conf.getKey()).substring(ChoiceAuthorityServiceImpl.CHOICES_PLUGIN_PREFIX.length())
                        .replace(".", "_");
                // exit the look immediately as we have found it
                break;
            }
        }
    }

    @Override
    public String getPluginInstanceName() {
        return authorityName;
    }
}
