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
 * @author Philipp Rumpf
 */
public class ISBNOnlineDataLoader extends NetworkSubmissionLookupDataLoader
{
    private ISBNService ISBNService = new ISBNService();

    private boolean searchProvider = true;
    private String isbnURL;
    private String queryFieldName;

    public void setISBNService(ISBNService ISBNService)
    {
	this.ISBNService = ISBNService;
    }

    @Override
    public List<String> getSupportedIdentifiers()
    {
	return Arrays.asList(new String[] { ISBN });
    }

    public void setSearchProvider(boolean searchProvider)
    {
	this.searchProvider = searchProvider;
    }

    public void setIsbnURL(String url)
    {
	isbnURL = url;
    }

    public void setQueryFieldName(String name)
    {
	queryFieldName = name;
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
	    Set<String> isbns = keys.get(ISBN);
	    List<Record> items = new ArrayList<Record>();
	    if (isbns != null && isbns.size() > 0)
	    {
		for (String isbn : isbns)
		{
			Record record = ISBNService.getByISBN(isbn, isbnURL, queryFieldName);

			if (record != null)
				items.add(record);
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
	List<Record> items = ISBNService.searchByTerm(title, author, year);
	for (Record item : items)
	{
	    results.add(convertFields(item));
	}
	return results;
    }
}
