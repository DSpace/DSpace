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

import org.apache.commons.httpclient.HttpException;
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
    private CrossRefService crossrefService = new CrossRefService();

    private boolean searchProvider = true;

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
            try
            {
                items = crossrefService.search(context, dois);
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
        List<Record> items = crossrefService.search(context, title, author,
                year, 10);
        return items;
    }

    @Override
    public boolean isSearchProvider()
    {
        return searchProvider;
    }
}
