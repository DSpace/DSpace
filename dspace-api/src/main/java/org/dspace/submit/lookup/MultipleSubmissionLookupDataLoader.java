/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.lookup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.dspace.core.Context;

import gr.ekt.bte.core.DataLoader;
import gr.ekt.bte.core.DataLoadingSpec;
import gr.ekt.bte.core.Record;
import gr.ekt.bte.core.RecordSet;
import gr.ekt.bte.core.StringValue;
import gr.ekt.bte.dataloader.FileDataLoader;
import gr.ekt.bte.exceptions.MalformedSourceException;

/**
 * @author Andrea Bollini
 * @author Kostas Stamatis
 * @author Luigi Andrea Pascarelli
 * @author Panagiotis Koutsourakis
 */
public class MultipleSubmissionLookupDataLoader implements DataLoader
{

    private static Logger log = Logger
            .getLogger(MultipleSubmissionLookupDataLoader.class);

    protected final String NOT_FOUND_DOI = "NOT-FOUND-DOI";

    Map<String, DataLoader> dataloadersMap;

    // Depending on these values, the multiple data loader loads data from the
    // appropriate providers
    Map<String, Set<String>> identifiers = null; // Searching by identifiers
                                                 // (DOI ...)

    Map<String, Set<String>> searchTerms = null; // Searching by author, title,
                                                 // date

    String filename = null; // Uploading file

    String type = null; // the type of the upload file (bibtex, etc.)

    /*
     * (non-Javadoc)
     * 
     * @see gr.ekt.bte.core.DataLoader#getRecords()
     */
    @Override
    public RecordSet getRecords() throws MalformedSourceException
    {

        RecordSet recordSet = new RecordSet();

        // KSTA:ToDo: Support timeout (problematic) providers
        // List<String> timeoutProviders = new ArrayList<String>();
        for (String providerName : filterProviders().keySet())
        {
            DataLoader provider = dataloadersMap.get(providerName);
            RecordSet subRecordSet = provider.getRecords();
            recordSet.addAll(subRecordSet);
            // Add in each record the provider name... a new provider doesn't
            // need to know about it!
            for (Record record : subRecordSet.getRecords())
            {
                if (record.isMutable())
                {
                    record.makeMutable().addValue(
                            SubmissionLookupService.PROVIDER_NAME_FIELD,
                            new StringValue(providerName));
                }
            }
        }

        // Question: Do we want that in case of file data loader?
        // for each publication in the record set, if it has a DOI, try to find
        // extra pubs from the other providers
        if (searchTerms != null
                || (identifiers != null && !identifiers
                        .containsKey(SubmissionLookupDataLoader.DOI)))
        { // Extend
            Map<String, Set<String>> provider2foundDOIs = new HashMap<String, Set<String>>();
            List<String> foundDOIs = new ArrayList<String>();

            for (Record publication : recordSet.getRecords())
            {
                String providerName = SubmissionLookupUtils.getFirstValue(
                        publication,
                        SubmissionLookupService.PROVIDER_NAME_FIELD);

                String doi = null;

                if (publication.getValues(SubmissionLookupDataLoader.DOI) != null
                        && publication
                                .getValues(SubmissionLookupDataLoader.DOI)
                                .size() > 0)
                    doi = publication.getValues(SubmissionLookupDataLoader.DOI)
                            .iterator().next().getAsString();
                if (doi == null)
                {
                    doi = NOT_FOUND_DOI;
                }
                else
                {
                    doi = SubmissionLookupUtils.normalizeDOI(doi);
                    if (!foundDOIs.contains(doi))
                    {
                        foundDOIs.add(doi);
                    }
                    Set<String> tmp = provider2foundDOIs.get(providerName);
                    if (tmp == null)
                    {
                        tmp = new HashSet<String>();
                        provider2foundDOIs.put(providerName, tmp);
                    }
                    tmp.add(doi);
                }
            }

            for (String providerName : dataloadersMap.keySet())
            {
                DataLoader genProvider = dataloadersMap.get(providerName);

                if (!(genProvider instanceof SubmissionLookupDataLoader))
                {
                    continue;
                }

                SubmissionLookupDataLoader provider = (SubmissionLookupDataLoader) genProvider;

                // Provider must support DOI
                if (!provider.getSupportedIdentifiers().contains(
                        SubmissionLookupDataLoader.DOI))
                {
                    continue;
                }

                // if (evictProviders != null
                // && evictProviders.contains(provider.getShortName())) {
                // continue;
                // }
                Set<String> doiToSearch = new HashSet<String>();
                Set<String> alreadyFoundDOIs = provider2foundDOIs
                        .get(providerName);
                for (String doi : foundDOIs)
                {
                    if (alreadyFoundDOIs == null
                            || !alreadyFoundDOIs.contains(doi))
                    {
                        doiToSearch.add(doi);
                    }
                }
                List<Record> pPublications = null;
                Context context = null;
                try
                {
                    if (doiToSearch.size() > 0)
                    {
                        context = new Context();
                        pPublications = provider.getByDOIs(context, doiToSearch);
                    }
                }
                catch (Exception e)
                {
                    log.error(e.getMessage(), e);
                }
                finally {
                    if(context!=null && context.isValid()) {
                        context.abort();
                    }
                }
                if (pPublications != null)
                {
                    for (Record rec : pPublications)
                    {
                        recordSet.addRecord(rec);
                        if (rec.isMutable())
                        {
                            rec.makeMutable().addValue(
                                    SubmissionLookupService.PROVIDER_NAME_FIELD,
                                    new StringValue(providerName));
                        }
                    }
                   
                }
            }
        }

        log.info("BTE DataLoader finished. Items loaded: "
                + recordSet.getRecords().size());

        // Printing debug message
        String totalString = "";
        for (Record record : recordSet.getRecords())
        {
            totalString += SubmissionLookupUtils.getPrintableString(record)
                    + "\n";
        }
        log.debug("Records loaded:\n" + totalString);

        return recordSet;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * gr.ekt.bte.core.DataLoader#getRecords(gr.ekt.bte.core.DataLoadingSpec)
     */
    @Override
    public RecordSet getRecords(DataLoadingSpec loadingSpec)
            throws MalformedSourceException
    {

        if (loadingSpec.getOffset() > 0) // Identify the end of loading
            return new RecordSet();

        return getRecords();
    }

    public Map<String, DataLoader> getProvidersMap()
    {
        return dataloadersMap;
    }

    public void setDataloadersMap(Map<String, DataLoader> providersMap)
    {
        this.dataloadersMap = providersMap;
    }

    public void setIdentifiers(Map<String, Set<String>> identifiers)
    {
        this.identifiers = identifiers;
        this.filename = null;
        this.searchTerms = null;

        if (dataloadersMap != null)
        {
            for (String providerName : dataloadersMap.keySet())
            {
                DataLoader provider = dataloadersMap.get(providerName);
                if (provider instanceof NetworkSubmissionLookupDataLoader)
                {
                    ((NetworkSubmissionLookupDataLoader) provider)
                            .setIdentifiers(identifiers);
                }

            }
        }
    }

    public void setSearchTerms(Map<String, Set<String>> searchTerms)
    {
        this.searchTerms = searchTerms;
        this.identifiers = null;
        this.filename = null;

        if (dataloadersMap != null)
        {
            for (String providerName : dataloadersMap.keySet())
            {
                DataLoader provider = dataloadersMap.get(providerName);
                if (provider instanceof NetworkSubmissionLookupDataLoader)
                {
                    ((NetworkSubmissionLookupDataLoader) provider)
                            .setSearchTerms(searchTerms);
                }
            }
        }
    }

    public void setFile(String filename, String type)
    {
        this.filename = filename;
        this.type = type;
        this.identifiers = null;
        this.searchTerms = null;

        if (dataloadersMap != null)
        {
            for (String providerName : dataloadersMap.keySet())
            {
                DataLoader provider = dataloadersMap.get(providerName);
                if (provider instanceof FileDataLoader)
                {
                    ((FileDataLoader) provider).setFilename(filename);
                }
            }
        }
    }

    public Map<String, DataLoader> filterProviders()
    {
        Map<String, DataLoader> result = new HashMap<String, DataLoader>();
        for (String providerName : dataloadersMap.keySet())
        {
            DataLoader dataLoader = dataloadersMap.get(providerName);
            if (searchTerms != null && identifiers == null && filename == null)
            {
                if (dataLoader instanceof SubmissionLookupDataLoader
                        && ((SubmissionLookupDataLoader) dataLoader)
                                .isSearchProvider())
                {
                    result.put(providerName, dataLoader);
                }
            }
            else if (searchTerms == null && identifiers != null
                    && filename == null)
            {
                if (dataLoader instanceof SubmissionLookupDataLoader)
                {
                    result.put(providerName, dataLoader);
                }
            }
            else if (searchTerms == null && identifiers == null
                    && filename != null)
            {
                if (dataLoader instanceof FileDataLoader)
                {
                    if (providerName.endsWith(type)) // add only the one that we
                                                     // are interested in
                        result.put(providerName, dataLoader);
                }
            }
        }

        return result;
    }
}
