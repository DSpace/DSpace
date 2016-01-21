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

import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpException;
import org.dspace.core.Context;
import org.jdom.JDOMException;
import org.xml.sax.SAXException;

/**
 * @author Andrea Bollini
 * @author Kostas Stamatis
 * @author Luigi Andrea Pascarelli
 * @author Panagiotis Koutsourakis
 */
public class CrossRefOnlineDataLoader extends NetworkSubmissionLookupDataLoader
{
    protected CrossRefService crossrefService = new CrossRefService();

    protected boolean searchProvider = true;

    protected String apiKey = null;
    protected int maxResults = 10;
    
    public void setSearchProvider(boolean searchProvider)
    {
        this.searchProvider = searchProvider;
    }

    public void setCrossrefService(CrossRefService crossrefService)
    {
        this.crossrefService = crossrefService;
    }

    @Override
    public List<String> getSupportedIdentifiers()
    {
        return Arrays.asList(new String[] { DOI });
    }

    @Override
    public List<Record> getByIdentifier(Context context,
            Map<String, Set<String>> keys) throws HttpException, IOException
    {
        if (keys != null && keys.containsKey(DOI))
        {
            Set<String> dois = keys.get(DOI);
            List<Record> items = null;
            List<Record> results = new ArrayList<Record>();
            
            if (getApiKey() == null){
            	throw new RuntimeException("No CrossRef API key is specified!");
            }
            
            try
            {
                items = crossrefService.search(context, dois, getApiKey());
            }
            catch (JDOMException e)
            {
                throw new RuntimeException(e.getMessage(), e);
            }
            catch (ParserConfigurationException e)
            {
                throw new RuntimeException(e.getMessage(), e);
            }
            catch (SAXException e)
            {
                throw new RuntimeException(e.getMessage(), e);
            }
            for (Record record : items)
            {
                results.add(convertFields(record));
            }
            return results;
        }
        return null;
    }

    @Override
    public List<Record> search(Context context, String title, String author,
            int year) throws HttpException, IOException
    {
    	if (getApiKey() == null){
        	throw new RuntimeException("No CrossRef API key is specified!");
        }
    	
        List<Record> items = crossrefService.search(context, title, author,
                year, getMaxResults(), getApiKey());
        return items;
    }

    @Override
    public boolean isSearchProvider()
    {
        return searchProvider;
    }

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public int getMaxResults() {
		return maxResults;
	}

	public void setMaxResults(int maxResults) {
		this.maxResults = maxResults;
	}
}
