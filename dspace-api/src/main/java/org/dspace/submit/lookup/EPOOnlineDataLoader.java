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

public class EPOOnlineDataLoader extends NetworkSubmissionLookupDataLoader {

    private static Logger log = Logger.getLogger(EPOOnlineDataLoader.class);

    private boolean searchProvider = true;

    private EPOService epoService = new EPOService();

    private String consumerKey = "";
    private String consumerSecretKey = "";

    private ConfigurationService configurationService;

    @Override
    public List<String> getSupportedIdentifiers() {
        return Arrays.asList(new String[] { EPOPUBLICATIONNUMBER, EPOAPPLICANTNUMBER });
    }

    @Override
    public boolean isSearchProvider() {

        return searchProvider;
    }

    public void setSearchProvider(boolean searchProvider) {
        this.searchProvider = searchProvider;
    }

    public void setWosService(EPOService wosService) {
        this.epoService = wosService;
    }

    @Override
    public List<Record> search(Context context, String title, String author, int year)
            throws HttpException, IOException {
        List<Record> results = new ArrayList<Record>();
//        if (title != null && year != 0) {
//            List<Record> search = epoService.search(null, title, author, year, getConsumerKey(),
//                    getConsumerSecretKey());
//            if (search != null) {
//                for (Record scopus : search) {
//                    results.add(convertFields(scopus));
//                }
//            }
//        }
        return results;
    }

    @Override
    public List<Record> getByIdentifier(Context context, Map<String, Set<String>> keys)
            throws HttpException, IOException {
        String query = "";
        List<String> ids = getSupportedIdentifiers();
        List<Record> results = new ArrayList<Record>();

        for (String id : ids) {
            Set<String> values = keys.get(id);

            for (String value : values) {
                if (query.length() > 0) {
                    query += " OR ";
                }
                query += id + "=" + value;
            }
        }

        if (query.length() > 0) {
            List<Record> records = epoService.search(query, getConsumerKey(), getConsumerSecretKey());
            
            for (Record record : records) {
                results.add(convertFields(record));
            }
        }
        return results;
    }

    public String getConsumerKey() {
        if (StringUtils.isBlank(this.consumerKey)) {
            this.consumerKey = getConfigurationService().getProperty("submission.lookup.epo.consumerKey");
        }
        return consumerKey;
    }

    public void setConsumerKey(String consumerKey) {
        this.consumerKey = consumerKey;
    }

    public String getConsumerSecretKey() {
        if (StringUtils.isBlank(this.consumerSecretKey)) {
            this.consumerSecretKey = getConfigurationService().getProperty("submission.lookup.epo.consumerSecretKey");
        }
        return consumerSecretKey;
    }

    public void setConsumerSecretKey(String consumerSecretKey) {
        this.consumerSecretKey = consumerSecretKey;
    }

    public ConfigurationService getConfigurationService() {
        return configurationService;
    }

    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }
}
