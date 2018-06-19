/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.lookup;

import gr.ekt.bte.core.Record;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.http.HttpException;

import org.apache.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.core.LogManager;

/**
 * @author Andrea Bollini
 * @author Kostas Stamatis
 * @author Luigi Andrea Pascarelli
 * @author Panagiotis Koutsourakis
 */
public class PubmedOnlineDataLoader extends NetworkSubmissionLookupDataLoader
{
    protected boolean searchProvider = true;

    private static final Logger log = Logger.getLogger(PubmedOnlineDataLoader.class);

    protected PubmedService pubmedService = new PubmedService();

    public void setPubmedService(PubmedService pubmedService)
    {
        this.pubmedService = pubmedService;
    }

    @Override
    public List<String> getSupportedIdentifiers()
    {
        return Arrays.asList(new String[] { PUBMED, DOI });
    }

    public void setSearchProvider(boolean searchProvider)
    {
        this.searchProvider = searchProvider;
    }

    @Override
    public boolean isSearchProvider()
    {
        return searchProvider;
    }

    @Override
    public List<Record> getByIdentifier(Context context,
            Map<String, Set<String>> keys) throws HttpException, IOException
    {
        Set<String> pmids = keys != null ? keys.get(PUBMED) : null;
        Set<String> dois = keys != null ? keys.get(DOI) : null;
        List<Record> results = new ArrayList<Record>();
        if (pmids != null && pmids.size() > 0
                && (dois == null || dois.size() == 0))
        {
            for (String pmid : pmids)
            {
                Record p = null;
                try
                {
                    p = pubmedService.getByPubmedID(pmid);
                }
                catch (Exception e)
                {
                    log.error(LogManager.getHeader(context, "getByIdentifier",
                            "pmid=" + pmid), e);
                }
                if (p != null)
                    results.add(convertFields(p));
            }
        }
        else if (dois != null && dois.size() > 0
                && (pmids == null || pmids.size() == 0))
        {
            StringBuffer query = new StringBuffer();
            for (String d : dois)
            {
                if (query.length() > 0)
                {
                    query.append(" OR ");
                }
                query.append(d).append("[AI]");
            }

            List<Record> pubmedResults = pubmedService.search(query.toString());
            for (Record p : pubmedResults)
            {
                results.add(convertFields(p));
            }
        }
        else if (dois != null && dois.size() > 0 && pmids != null
                && pmids.size() > 0)
        {
            // EKT:ToDo: support list of dois and pmids in the search method of
            // pubmedService
            List<Record> pubmedResults = pubmedService.search(dois.iterator()
                    .next(), pmids.iterator().next());
            if (pubmedResults != null)
            {
                for (Record p : pubmedResults)
                {
                    results.add(convertFields(p));
                }
            }
        }

        return results;
    }

    @Override
    public List<Record> search(Context context, String title, String author,
            int year) throws HttpException, IOException
    {
        List<Record> pubmedResults = pubmedService.search(title, author, year);
        List<Record> results = new ArrayList<Record>();
        if (pubmedResults != null)
        {
            for (Record p : pubmedResults)
            {
                results.add(convertFields(p));
            }
        }
        return results;
    }
}
