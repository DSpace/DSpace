/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.lookup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gr.ekt.bte.core.Record;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpException;
import org.apache.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;


public class SciValOnlineDataLoader extends NetworkSubmissionLookupDataLoader {

    private static Logger log = Logger.getLogger(SciValOnlineDataLoader.class);

    private boolean searchProvider = true;

    private SciValService scopusService = new SciValService();

    private ConfigurationService configurationService;

    private String apiKey = "";


    @Override
    public List<String> getSupportedIdentifiers() {
        return Arrays.asList(new String[] { SCOPUSEID, DOI });
    }

    @Override
    public boolean isSearchProvider() {

        return searchProvider;
    }

    public void setSearchProvider(boolean searchProvider) {
        this.searchProvider = searchProvider;
    }

    public void setScopusService(SciValService scopusService) {
        this.scopusService = scopusService;
    }

    @Override
    public List<Record> search(Context context, String title, String author, int year) throws HttpException,
        IOException {
        List<Record> results = new ArrayList<Record>();
        if (title != null && year != 0) {
            List<Record> search = scopusService.search(null, title, author, year, getApiKey());
            if (search != null) {
                for (Record scopus : search) {
                    results.add(convertFields(scopus));
                }
            }
        }
        return results;
    }

    @Override
    public List<Record> getByIdentifier(Context context, Map<String, Set<String>> keys) throws HttpException,
        IOException {
        Set<String> eids = keys != null ? keys.get(SCOPUSEID) : null;
        Set<String> dois = keys != null ? keys.get(DOI) : null;
        List<Record> results = new ArrayList<Record>();

        if (eids != null && eids.size() > 0) {
            for (String eid : eids) {
                Record record = scopusService.retrieve(eid, getApiKey());
                if (record != null) {
                    results.add(convertFields(record));
                }
            }
        } else if (dois != null && dois.size() > 0) {
            List<Record> records = scopusService.search(dois, getApiKey());
            for (Record record : records) {
                results.add(convertFields(record));
            }

        }

        return results;
    }

    public String getApiKey() {
        if (StringUtils.isBlank(this.apiKey)) {
            this.apiKey = getConfigurationService().getProperty("submission.lookup.scivalcontent.apikey");
        }
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public ConfigurationService getConfigurationService() {
        return configurationService;
    }

    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

}
