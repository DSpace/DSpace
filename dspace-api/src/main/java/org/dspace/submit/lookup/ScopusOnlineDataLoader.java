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
import org.apache.http.HttpException;
import org.apache.log4j.Logger;
import org.dspace.core.Context;

/**
 * @author Andrea Bollini
 * @author Kostas Stamatis
 * @author Luigi Andrea Pascarelli
 * @author Panagiotis Koutsourakis
 */
public class ScopusOnlineDataLoader extends NetworkSubmissionLookupDataLoader {
    private boolean searchProvider = true;

    private static final Logger log = Logger.getLogger(ScopusOnlineDataLoader.class);

    private ScopusService scopusService = new ScopusService();

    public void setScopusService(ScopusService scopusService) {
        this.scopusService = scopusService;
    }


    @Override
    public List<String> getSupportedIdentifiers() {
        return Arrays.asList(new String[] { PUBMED, DOI, SCOPUSEID });
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
        Set<String> eids = keys != null ? keys.get(SCOPUSEID) : null;
        Set<String> dois = keys != null ? keys.get(DOI) : null;
        Set<String> pmids = keys != null ? keys.get(PUBMED) : null;
        Set<String> orcids = keys != null ? keys.get(ORCID) : null;
        List<Record> results = new ArrayList<Record>();
        StringBuffer query = new StringBuffer();

        if (eids != null && eids.size() > 0) {
            String eidQuery = queryBuilder("EID", eids);
            query.append(eidQuery);

        }
        if (dois != null && dois.size() > 0) {
            if (query.length() > 0) {
                query.append(" OR ");
            }
            String doiQuery = queryBuilder("DOI", dois);
            query.append(doiQuery);
        }
        if (pmids != null && pmids.size() > 0) {
            if (query.length() > 0) {
                query.append(" OR ");
            }
            String pmidQuery = queryBuilder("PMID", pmids);
            query.append(pmidQuery);
        }

        if (orcids != null && orcids.size() > 0) {
            if (query.length() > 0) {
                query.append(" OR ");
            }
            String orcidQuery = queryBuilder("ORCID", orcids);
            query.append(orcidQuery);
        }

        List<Record> scopusResults = scopusService.search(query.toString());
        for (Record p : scopusResults) {
            results.add(convertFields(p));
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
            query += param + "(" + d + ")";
            x++;
        }
        return query;
    }

    @Override
    public List<Record> search(Context context, String title, String author,
                               int year) throws HttpException, IOException {
        List<Record> scopusResults = scopusService.search(title, author, year);
        List<Record> results = new ArrayList<Record>();
        if (scopusResults != null) {
            for (Record p : scopusResults) {
                results.add(convertFields(p));
            }
        }
        return results;
    }

    public List<Record> search(String query) throws HttpException, IOException {
        List<Record> results = new ArrayList<Record>();
        if (query != null) {
            List<Record> search = scopusService.search(query);
            if (search != null) {
                for (Record scopus : search) {
                    results.add(convertFields(scopus));
                }
            }
        }
        return results;
    }
}
