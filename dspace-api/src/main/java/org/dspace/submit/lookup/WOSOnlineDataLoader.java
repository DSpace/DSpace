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

public class WOSOnlineDataLoader extends NetworkSubmissionLookupDataLoader {

    private static Logger log = Logger.getLogger(WOSOnlineDataLoader.class);

    private boolean searchProvider = true;

    private WOSService wosService = new WOSService();

    private String wosUser = "";
    private String wosPassword = "";
    private Boolean ipAuthentication;

    private ConfigurationService configurationService;

    @Override
    public List<String> getSupportedIdentifiers() {
        return Arrays.asList(new String[] { WOSID, DOI });
    }

    @Override
    public boolean isSearchProvider() {

        return searchProvider;
    }

    public void setSearchProvider(boolean searchProvider) {
        this.searchProvider = searchProvider;
    }

    public void setWosService(WOSService wosService) {
        this.wosService = wosService;
    }

    @Override
    public List<Record> search(Context context, String title, String author, int year) throws HttpException,
        IOException {
        List<Record> results = new ArrayList<Record>();
        if (title != null && year != 0) {
            List<Record> search = wosService
                .search(null, title, author, year, getWosUser(), getWosPassword(), getIpAuthentication());
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
        Set<String> wosIds = keys != null ? keys.get(WOSID) : null;
        Set<String> dois = keys != null ? keys.get(DOI) : null;
        List<Record> results = new ArrayList<Record>();

        if (wosIds != null && wosIds.size() > 0) {
            for (String wosId : wosIds) {
                Record record = wosService.retrieve(wosId, getWosUser(), getWosPassword(), getIpAuthentication());
                if (record != null) {
                    results.add(convertFields(record));
                }
            }
        } else if (dois != null && dois.size() > 0) {
            List<Record> records = wosService.search(dois, getWosUser(), getWosPassword(), getIpAuthentication());
            for (Record record : records) {
                results.add(convertFields(record));
            }
        }
        return results;
    }

    public String getWosUser() {
        if (StringUtils.isBlank(this.wosUser)) {
            this.wosUser = getConfigurationService()
                .getProperty("submission.lookup.webofknowledge.user");
        }
        return wosUser;
    }

    public void setWosUser(String wosUser) {
        this.wosUser = wosUser;
    }

    public String getWosPassword() {
        if (StringUtils.isBlank(this.wosPassword)) {
            this.wosPassword = getConfigurationService()
                .getProperty("submission.lookup.webofknowledge.password");
        }
        return wosPassword;
    }

    public void setWosPassword(String wosPassword) {
        this.wosPassword = wosPassword;
    }

    public ConfigurationService getConfigurationService() {
        return configurationService;
    }

    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    public Boolean getIpAuthentication() {
        if (this.ipAuthentication == null) {
            this.ipAuthentication = getConfigurationService()
                .getPropertyAsType("submission.lookup.webofknowledge.ip.authentication", false);
        }
        return ipAuthentication;
    }

    public void setIpAuthentication(Boolean ipAuthentication) {
        this.ipAuthentication = ipAuthentication;
    }

    public List<Record> searchByAffiliation(String userQuery,
                                            String databaseID, String symbolicTimeSpan, String start, String end)
        throws HttpException, IOException {
        List<Record> results = new ArrayList<Record>();
        if (databaseID != null) {
            List<Record> search = wosService
                .searchByAffiliation(userQuery, databaseID, symbolicTimeSpan, start, end, getWosUser(),
                                     getWosPassword(), getIpAuthentication());
            if (search != null) {
                for (Record scopus : search) {
                    results.add(convertFields(scopus));
                }
            }
        }
        return results;
    }
}
