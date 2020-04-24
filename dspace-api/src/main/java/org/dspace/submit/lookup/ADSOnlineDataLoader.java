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
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author rfazio
 */
public class ADSOnlineDataLoader extends NetworkSubmissionLookupDataLoader implements InitializingBean {

    private boolean searchProvider;

    private static final Logger log = Logger.getLogger(ADSOnlineDataLoader.class);

    private ADSService adsService = new ADSService();

    private String token;

    @Autowired
    ConfigurationService configurationService;


    @Override
    public List<String> getSupportedIdentifiers() {
        return Arrays.asList(new String[] {ADSBIBCODE, DOI, ARXIV});
    }

    public void setSearchProvider(boolean searchProvider) {
        this.searchProvider = searchProvider;
    }

    @Override
    public boolean isSearchProvider() {
        return searchProvider;
    }

    @Override
    public List<Record> getByIdentifier(Context context,
                                        Map<String, Set<String>> keys) throws HttpException, IOException {
        if (StringUtils.isBlank(this.token)) {
            return new ArrayList<>(0);
        }
        Set<String> adbibcodes = (keys != null) ? keys.get(ADSBIBCODE) : null;
        Set<String> dois = (keys != null) ? keys.get(DOI) : null;
        Set<String> arxivids = (keys != null) ? keys.get(ARXIV) : null;

        List<Record> results = new ArrayList<Record>();
        StringBuffer query = new StringBuffer();
        String strQuery = "";


        if (adbibcodes != null && adbibcodes.size() > 0) {
            String adsIDQuery = queryBuilder("bibcode", adbibcodes);
            query.append(adsIDQuery);

        }
        if (dois != null && dois.size() > 0) {
            if (query.length() > 0) {
                query.append(" OR ");
            }
            String doiQuery = queryBuilder("doi", dois);
            query.append(doiQuery);
        }
        if (arxivids != null && arxivids.size() > 0) {
            if (query.length() > 0) {
                query.append(" OR ");
            }
            String arxivQuery = queryBuilder("identifier", arxivids);
            query.append(arxivQuery);
        }

        if (query.length() > 0) {
            strQuery = query.toString();

            List<Record> adsResults = new ArrayList<Record>();
            try {
                adsResults = adsService.search(strQuery, token);
            } catch (Exception e) {
                log.warn(e.getMessage(), e);
            }
            for (Record p : adsResults) {
                results.add(convertFields(p));
            }
        }


        return results;
    }

    private String queryBuilder(String param, Set<String> ids) {

        String query = "";
        int x = 0;
        for (String d : ids) {
            if (x > 0) {
                query += " OR ";
            }
            query += param + ":" + d;
            x++;
        }
        return query;
    }

    @Override
    public List<Record> search(Context context, String title, String author,
                               int year) throws HttpException, IOException {
        List<Record> adsResults = adsService.search(title, author, year, token);
        List<Record> results = new ArrayList<Record>();
        if (adsResults != null) {
            for (Record p : adsResults) {
                results.add(convertFields(p));
            }
        }
        return results;
    }

    public List<Record> search(String query) throws HttpException, IOException {
        List<Record> results = new ArrayList<Record>();
        if (query != null) {
            List<Record> search = adsService.search(query, token);
            if (search != null) {
                for (Record ads : search) {
                    results.add(convertFields(ads));
                }
            }
        }
        return results;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.token = configurationService.getProperty("submission.lookup.ads.apikey", "");
    }
}
