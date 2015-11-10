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
 * @author Andrea Bollini
 * @author Kostas Stamatis
 * @author Luigi Andrea Pascarelli
 * @author Panagiotis Koutsourakis
 */
public class ArXivOnlineDataLoader extends NetworkSubmissionLookupDataLoader
{
    protected ArXivService arXivService = new ArXivService();

    protected boolean searchProvider = true;

    public void setArXivService(ArXivService arXivService)
    {
        this.arXivService = arXivService;
    }

    @Override
    public List<String> getSupportedIdentifiers()
    {
        return Arrays.asList(new String[] { ARXIV, DOI });
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
        List<Record> results = new ArrayList<Record>();
        if (keys != null)
        {
            Set<String> dois = keys.get(DOI);
            Set<String> arxivids = keys.get(ARXIV);
            List<Record> items = new ArrayList<Record>();
            if (dois != null && dois.size() > 0)
            {
                items.addAll(arXivService.getByDOIs(dois));
            }
            if (arxivids != null && arxivids.size() > 0)
            {
                for (String arxivid : arxivids)
                {
                    items.add(arXivService.getByArXivIDs(arxivid));
                }
            }

            for (Record item : items)
            {
                results.add(convertFields(item));
            }
        }
        return results;
    }

    @Override
    public List<Record> search(Context context, String title, String author,
            int year) throws HttpException, IOException
    {
        List<Record> results = new ArrayList<Record>();
        List<Record> items = arXivService.searchByTerm(title, author, year);
        for (Record item : items)
        {
            results.add(convertFields(item));
        }
        return results;
    }
}
