/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.lookup;

import gr.ekt.bte.core.DataLoadingSpec;
import gr.ekt.bte.core.Record;
import gr.ekt.bte.core.RecordSet;
import gr.ekt.bte.core.Value;
import gr.ekt.bte.exceptions.MalformedSourceException;

import java.io.IOException;
import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpException;
import org.dspace.core.Context;

/**
 * @author Andrea Bollini
 * @author Kostas Stamatis
 * @author Luigi Andrea Pascarelli
 * @author Panagiotis Koutsourakis
 */
public abstract class NetworkSubmissionLookupDataLoader implements
        SubmissionLookupDataLoader
{

    Map<String, Set<String>> identifiers; // Searching by identifiers (DOI ...)

    Map<String, Set<String>> searchTerms; // Searching by author, title, date

    Map<String, String> fieldMap; // mapping between service fields and local
                                  // intermediate fields

    String providerName;

    @Override
    public List<Record> getByDOIs(Context context, Set<String> doiToSearch)
            throws HttpException, IOException
    {

        Map<String, Set<String>> keys = new HashMap<String, Set<String>>();
        keys.put(DOI, doiToSearch);

        return getByIdentifier(context, keys);
    }

    // BTE Data Loader interface methods
    @Override
    public RecordSet getRecords() throws MalformedSourceException
    {

        RecordSet recordSet = new RecordSet();

        List<Record> results = null;

        try
        {
            if (getIdentifiers() != null)
            { // Search by identifiers
                results = getByIdentifier(null, getIdentifiers());
            }
            else
            {
                String title = getSearchTerms().get("title") != null ? getSearchTerms()
                        .get("title").iterator().next()
                        : null;
                String authors = getSearchTerms().get("authors") != null ? getSearchTerms()
                        .get("authors").iterator().next()
                        : null;
                String year = getSearchTerms().get("year") != null ? getSearchTerms()
                        .get("year").iterator().next()
                        : String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
                int yearInt = Integer.parseInt(year);
                results = search(null, title, authors, yearInt);
            }
        }
        catch (HttpException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        if (results != null)
        {
            for (Record record : results)
            {
                recordSet.addRecord(record);
            }
        }

        return recordSet;
    }

    @Override
    public RecordSet getRecords(DataLoadingSpec arg0)
            throws MalformedSourceException
    {

        return getRecords();
    }

    public Map<String, Set<String>> getIdentifiers()
    {
        return identifiers;
    }

    public void setIdentifiers(Map<String, Set<String>> identifiers)
    {
        this.identifiers = identifiers;
    }

    public Map<String, Set<String>> getSearchTerms()
    {
        return searchTerms;
    }

    public void setSearchTerms(Map<String, Set<String>> searchTerms)
    {
        this.searchTerms = searchTerms;
    }

    public Map<String, String> getFieldMap()
    {
        return fieldMap;
    }

    public void setFieldMap(Map<String, String> fieldMap)
    {
        this.fieldMap = fieldMap;
    }

    public void setProviderName(String providerName)
    {
        this.providerName = providerName;
    }

    public Record convertFields(Record publication)
    {
        for (String fieldName : fieldMap.keySet())
        {
            String md = null;
            if (fieldMap != null)
            {
                md = this.fieldMap.get(fieldName);
            }

            if (StringUtils.isBlank(md))
            {
                continue;
            }
            else
            {
                md = md.trim();
            }

            if (publication.isMutable())
            {
                List<Value> values = publication.getValues(fieldName);
                publication.makeMutable().removeField(fieldName);
                publication.makeMutable().addField(md, values);
            }
        }

        return publication;
    }
}
