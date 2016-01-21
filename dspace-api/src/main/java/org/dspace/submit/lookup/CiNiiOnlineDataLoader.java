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

import org.dspace.core.Context;

/**
 * Load metadata from CiNii RDF API
 * @author Keiji Suzuki
 */
public class CiNiiOnlineDataLoader extends NetworkSubmissionLookupDataLoader
{
    protected CiNiiService ciniiService = new CiNiiService();

    protected boolean searchProvider = true;

    /** Application id to use CiNii */
    protected String appId = null;

    /** max result number to return */
    protected int maxResults = 10;

    public void setCiNiiService(CiNiiService ciniiService)
    {
        this.ciniiService = ciniiService;
    }

    @Override
    public List<String> getSupportedIdentifiers()
    {
        return Arrays.asList(new String[] { CINII });
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
        if (appId == null)
        {
            throw new RuntimeException("No CiNii Application ID is specified!");
        }

        List<Record> results = new ArrayList<Record>();
        if (keys != null)
        {
            Set<String> ciniiids = keys.get(CINII);
            if (ciniiids != null && ciniiids.size() > 0)
            {
                for (String ciniiid : ciniiids)
                {
                    Record record = ciniiService.getByCiNiiID(ciniiid, getAppId());
                    if (record != null)
                    {
                        results.add(convertFields(record));
                    }
                }
            }
        }
        return results;
    }

    @Override
    public List<Record> search(Context context, String title, String author, int year)
        throws HttpException, IOException
    {
        if (appId == null)
        {
            throw new RuntimeException("No CiNii Application ID is specified!");
        }

        return ciniiService.searchByTerm(title, author, year, 
            getMaxResults(), getAppId());
    }

    public String getAppId()
    {
        return appId;
    }

    public void setAppId(String appId)
    {
        this.appId = appId;
    }

    public int getMaxResults()
    {
        return maxResults;
    }

    public void setMaxResults(int maxResults)
    {
        this.maxResults = maxResults;
    }
}
