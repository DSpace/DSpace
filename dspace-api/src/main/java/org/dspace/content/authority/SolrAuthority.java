/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import org.dspace.authority.AuthoritySearchService;
import org.dspace.authority.AuthorityValue;
import org.dspace.authority.factory.AuthorityServiceFactory;
import org.dspace.authority.rest.RestSource;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CommonParams;
import org.dspace.authority.service.AuthorityValueService;
import org.dspace.content.Collection;
import org.dspace.core.ConfigurationManager;
import org.dspace.services.factory.DSpaceServicesFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Antoine Snyers (antoine at atmire.com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class SolrAuthority implements ChoiceAuthority {

    private static final Logger log = Logger.getLogger(SolrAuthority.class);
    protected RestSource source = DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName("AuthoritySource", RestSource.class);
    protected boolean externalResults = false;
    protected final AuthorityValueService authorityValueService = AuthorityServiceFactory.getInstance().getAuthorityValueService();

    public Choices getMatches(String field, String text, Collection collection, int start, int limit, String locale, boolean bestMatch) {
        if(limit == 0)
            limit = 10;

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
        if(externalResults){
            maxNumberOfSolrResults = ConfigurationManager.getIntProperty("xmlui.lookup.select.size", 12);
        }
        queryArgs.set(CommonParams.ROWS, maxNumberOfSolrResults);

        String sortField = "value";
        String localSortField = "";
        if (StringUtils.isNotBlank(locale)) {
            localSortField = sortField + "_" + locale;
            queryArgs.setSortField(localSortField, SolrQuery.ORDER.asc);
        } else {
            queryArgs.setSortField(sortField, SolrQuery.ORDER.asc);
        }

        Choices result;
        try {
            int max = 0;
            boolean hasMore = false;
            QueryResponse searchResponse = getSearchService().search(queryArgs);
            SolrDocumentList authDocs = searchResponse.getResults();
            ArrayList<Choice> choices = new ArrayList<Choice>();
            if (authDocs != null) {
                max = (int) searchResponse.getResults().getNumFound();
                int maxDocs = authDocs.size();
                if (limit < maxDocs)
                    maxDocs = limit;
                List<AuthorityValue> alreadyPresent = new ArrayList<AuthorityValue>();
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

                if (externalResults && StringUtils.isNotBlank(text)) {
                    int sizeFromSolr = alreadyPresent.size();
                    int maxExternalResults = limit <= 10 ? Math.max(limit - sizeFromSolr, 2) : Math.max(limit - 10 - sizeFromSolr, 2) + limit - 10;
                    addExternalResults(text, choices, alreadyPresent, maxExternalResults);
                }


                // hasMore = (authDocs.size() == (limit + 1));
                hasMore = true;
            }


            int confidence;
            if (choices.size() == 0)
                confidence = Choices.CF_NOTFOUND;
            else if (choices.size() == 1)
                confidence = Choices.CF_UNCERTAIN;
            else
                confidence = Choices.CF_AMBIGUOUS;

            result = new Choices(choices.toArray(new Choice[choices.size()]), start, hasMore ? max : choices.size() + start, confidence, hasMore);
        } catch (Exception e) {
            log.error("Error while retrieving authority values {field: " + field + ", prefix:" + text + "}", e);
            result = new Choices(true);
        }

        return result;  //To change body of implemented methods use File | Settings | File Templates.
    }

    protected void addExternalResults(String text, ArrayList<Choice> choices, List<AuthorityValue> alreadyPresent, int max) {
        if(source != null){
            try {
                List<AuthorityValue> values = source.queryAuthorities(text, max * 2); // max*2 because results get filtered

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
            this.externalResults = false;
        } else {
            log.warn("external source for authority not configured");
        }
    }

    private String toQuery(String searchField, String text) {
        return searchField + ":\"" + text.toLowerCase().replaceAll(":", "\\:") + "*\" or " + searchField + ":\"" + text.toLowerCase().replaceAll(":", "\\:")+"\"";
    }

    @Override
    public Choices getMatches(String field, String text, Collection collection, int start, int limit, String locale) {
        return getMatches(field, text, collection, start, limit, locale, true);
    }

    @Override
    public Choices getBestMatch(String field, String text, Collection collection, String locale) {
        Choices matches = getMatches(field, text, collection, 0, 1, locale, false);
        if (matches.values.length !=0 && !matches.values[0].value.equalsIgnoreCase(text)) {
            matches = new Choices(false);
        }
        return matches;
    }

    @Override
    public String getLabel(String field, String key, String locale) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("requesting label for key " + key + " using locale " + locale);
            }
            SolrQuery queryArgs = new SolrQuery();
            queryArgs.setQuery("id:" + key);
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
                        log.debug("returning label " + label + " for key " + key + " using locale " + locale + " and fieldvalue " + "value_" + locale);
                    }
                    return label;
                }
                try {
                    label = (String) docs.get(0).getFieldValue("value");
                } catch (Exception e) {
                    log.error("couldn't get field value for key " + key,e);
                }
                if (label != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("returning label " + label + " for key " + key + " using locale " + locale + " and fieldvalue " + "value");
                    }
                    return label;
                }
                try {
                    label = (String) docs.get(0).getFieldValue("value_en");
                } catch (Exception e) {
                    log.error("couldn't get field value for key " + key,e);
                }
                if (label != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("returning label " + label + " for key " + key + " using locale " + locale + " and fieldvalue " + "value_en");
                    }
                    return label;
                }
            }
        } catch (Exception e) {
            log.error("error occurred while trying to get label for key " + key,e);
        }

        return key;
    }


    public static AuthoritySearchService getSearchService() {
        org.dspace.kernel.ServiceManager manager = DSpaceServicesFactory.getInstance().getServiceManager();

        return manager.getServiceByName(AuthoritySearchService.class.getName(), AuthoritySearchService.class);
    }

    public void addExternalResultsInNextMatches() {
        this.externalResults = true;
    }
}
