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
import java.net.URISyntaxException;
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
 * @author rfazio
 */
public class ADSOnlineDataLoader extends NetworkSubmissionLookupDataLoader
{
    private boolean searchProvider = true;
    
    private String token;

    private static final Logger log = Logger.getLogger(ADSOnlineDataLoader.class);

    private ADSService adsService = new ADSService();


    @Override
    public List<String> getSupportedIdentifiers()
    {
        return Arrays.asList(new String[] { ADSBIBCODE, DOI,ARXIV });
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
        Set<String> adbibcodes = keys != null ? keys.get(ADSBIBCODE) : null;
        Set<String> dois = keys != null ? keys.get(DOI) : null;
        Set<String> arxivids = keys != null ? keys.get(ARXIV) : null;        
        
        List<Record> results = new ArrayList<Record>();
        StringBuffer query = new StringBuffer();
        String strQuery ="";
        
        
        if (adbibcodes != null && adbibcodes.size() > 0 )
        {
            String adsIDQuery = queryBuilder("bibcode", adbibcodes);
            query.append(adsIDQuery);
        	
        }
        if (dois != null && dois.size() > 0)
        {
            if (query.length() > 0)
            {
                query.append(" OR ");
            }
            String doiQuery = queryBuilder("doi", dois);
            query.append(doiQuery);
        }
        if (arxivids != null && arxivids.size() > 0)
        {
            if (query.length() > 0)
            {
                query.append(" OR ");
            }
            String arxivQuery = queryBuilder("identifier", arxivids);
            query.append(arxivQuery);
        }
        
        if(query.length()>0){
        	strQuery=query.toString();
        }

        List<Record> adsResults = adsService.search(strQuery,token);
        for (Record p : adsResults)
        {
            results.add(convertFields(p));
        }

        return results;
    }

	private String queryBuilder(String param,Set<String> ids){

		String query="";
    	int x=0;
    	for (String d : ids)
        {
            if(x>0){
                query+=" OR ";
            }
    		query+=param+":"+d;
    		x++;
        }
		return query;
	}

    @Override
    public List<Record> search(Context context, String title, String author,
            int year) throws HttpException, IOException
    {
        List<Record> adsResults = adsService.search(title, author, year,token);
        List<Record> results = new ArrayList<Record>();
        if (adsResults != null)
        {
            for (Record p : adsResults)
            {
                results.add(convertFields(p));
            }
        }
        return results;
    }
    
    public List<Record> search(String query) throws HttpException, IOException
    {
        List<Record> results = new ArrayList<Record>();
        if (query != null) {
            List<Record> search = adsService.search(query,token);
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
}
