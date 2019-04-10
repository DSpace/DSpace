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
import gr.ekt.bte.core.Value;
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

    private static final String NOT_FOUND_DOI = "NOT-FOUND-DOI";
    private static final String NOT_FOUND_WOS = "NOT-FOUND-WOS";
    private static final String NOT_FOUND_PUBMED = "NOT-FOUND-PUBMED";
    private static final String NOT_FOUND_ARXIV = "NOT-FOUND-ARXIV";
    private static final String NOT_FOUND_ADSBIBCODE = "NOT-FOUND-ADSBIBCODE";
    private static final String NOT_FOUND_SCOPUS = "NOT-FOUND-SCOPUS";
    private static final String NOT_FOUND_ORCID = "NOT-FOUND-ORCID";
    private static final String NOT_FOUND_CINII = "NOT-FOUND-CINII";

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
                    record.makeMutable().addValue(
                            "originalRecord",
                            new StringValue(identifiers.keySet().iterator().next()));
                }
            }
        }

        // Question: Do we want that in case of file data loader?
        // for each publication in the record set, if it has an identifier, try to find
        // extra pubs from the other providers
        if (searchTerms != null
                || (identifiers != null
                && (!identifiers.containsKey(SubmissionLookupDataLoader.PUBMED)
                        || !identifiers.containsKey(SubmissionLookupDataLoader.DOI)
                        || !identifiers.containsKey(SubmissionLookupDataLoader.ARXIV)
                        || !identifiers.containsKey(SubmissionLookupDataLoader.CINII)
                        || !identifiers.containsKey(SubmissionLookupDataLoader.SCOPUSEID)
                        || !identifiers.containsKey(SubmissionLookupDataLoader.WOSID)
                        || !identifiers.containsKey(SubmissionLookupDataLoader.ADSBIBCODE))))
        { // Extend
            Map<String, Set<String>> provider2foundPubmeds = new HashMap<String, Set<String>>();
            List<String> foundPubmeds = new ArrayList<String>();

            Map<String, Set<String>> provider2foundDOIs = new HashMap<String, Set<String>>();
            List<String> foundDOIs = new ArrayList<String>();

            Map<String, Set<String>> provider2foundArxivs = new HashMap<String, Set<String>>();
            List<String> foundArxivs = new ArrayList<String>();

            Map<String, Set<String>> provider2foundCiniis = new HashMap<String, Set<String>>();
            List<String> foundCiniis = new ArrayList<String>();

            Map<String, Set<String>> provider2foundScopuss = new HashMap<String, Set<String>>();
            List<String> foundScopuss = new ArrayList<String>();

            Map<String, Set<String>> provider2foundWOSs = new HashMap<String, Set<String>>();
            List<String> foundWOSs = new ArrayList<String>();

            Map<String, Set<String>> provider2foundAdsbibcodes = new HashMap<String, Set<String>>();
            List<String> foundAdsbibcodes = new ArrayList<String>();

            for (Record publication : recordSet.getRecords())
            {
                String providerName = SubmissionLookupUtils.getFirstValue(
                        publication,
                        SubmissionLookupService.PROVIDER_NAME_FIELD);
                if (identifiers != null && !identifiers.containsKey(SubmissionLookupDataLoader.PUBMED))
                {
                    retrieveIdentifier(publication, providerName,
                            provider2foundPubmeds, foundPubmeds, "pubmedID", NOT_FOUND_PUBMED);
                }
                if (identifiers != null && !identifiers.containsKey(SubmissionLookupDataLoader.DOI))
                {
                    retrieveIdentifier(publication, providerName,
                            provider2foundDOIs, foundDOIs, "doi", NOT_FOUND_DOI);
                }
                if (identifiers != null && !identifiers.containsKey(SubmissionLookupDataLoader.ARXIV))
                {
                    retrieveIdentifier(publication, providerName,
                            provider2foundArxivs, foundArxivs, "url", NOT_FOUND_ARXIV);
                }
                if (identifiers != null && !identifiers.containsKey(SubmissionLookupDataLoader.CINII))
                {
                    retrieveIdentifier(publication, providerName,
                            provider2foundCiniis, foundCiniis, "ncid", NOT_FOUND_CINII);
                }
                if (identifiers != null && !identifiers.containsKey(SubmissionLookupDataLoader.SCOPUSEID))
                {
                    retrieveIdentifier(publication, providerName,
                            provider2foundScopuss, foundScopuss, "eid", NOT_FOUND_SCOPUS);
                }
                if (identifiers != null && !identifiers.containsKey(SubmissionLookupDataLoader.WOSID))
                {
                    retrieveIdentifier(publication, providerName,
                            provider2foundWOSs, foundWOSs, "isiId", NOT_FOUND_WOS);
                }
                if (identifiers != null && !identifiers.containsKey(SubmissionLookupDataLoader.ADSBIBCODE))
                {
                    retrieveIdentifier(publication, providerName,
                            provider2foundAdsbibcodes, foundAdsbibcodes, "adsbibcode", NOT_FOUND_ADSBIBCODE);
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

                // Provider must support at least one of these identifiers
                if ((!provider.getSupportedIdentifiers().contains(SubmissionLookupDataLoader.PUBMED))
                        && (!provider.getSupportedIdentifiers().contains(SubmissionLookupDataLoader.DOI))
                        && (!provider.getSupportedIdentifiers().contains(SubmissionLookupDataLoader.ARXIV))
                        && (!provider.getSupportedIdentifiers().contains(SubmissionLookupDataLoader.CINII))
                        && (!provider.getSupportedIdentifiers().contains(SubmissionLookupDataLoader.SCOPUSEID))
                        && (!provider.getSupportedIdentifiers().contains(SubmissionLookupDataLoader.WOSID))
                        && (!provider.getSupportedIdentifiers().contains(SubmissionLookupDataLoader.ADSBIBCODE)))
                {
                    continue;
                }

                // if (evictProviders != null
                // && evictProviders.contains(provider.getShortName())) {
                // continue;
                // }

                Map<String, Set<String>> keys = new HashMap<String, Set<String>>();

                if (provider.getSupportedIdentifiers().contains(SubmissionLookupDataLoader.PUBMED))
                {
                    Set<String> pubmedToSearch = new HashSet<String>();
                    Set<String> alreadyFoundPubmeds = provider2foundPubmeds.get(providerName);
                    identifierToSearch(foundPubmeds, pubmedToSearch, alreadyFoundPubmeds);
                    addIdentifierToKey(pubmedToSearch, SubmissionLookupDataLoader.PUBMED, keys);
                }
                if (provider.getSupportedIdentifiers().contains(SubmissionLookupDataLoader.DOI))
                {
                    Set<String> doiToSearch = new HashSet<String>();
                    Set<String> alreadyFoundDOIs = provider2foundDOIs.get(providerName);
                    identifierToSearch(foundDOIs, doiToSearch, alreadyFoundDOIs);
                    addIdentifierToKey(doiToSearch, SubmissionLookupDataLoader.DOI, keys);
                }
                if (provider.getSupportedIdentifiers().contains(SubmissionLookupDataLoader.ARXIV))
                {
                    Set<String> arxivToSearch = new HashSet<String>();
                    Set<String> alreadyFoundArxivs = provider2foundArxivs.get(providerName);
                    identifierToSearch(foundArxivs, arxivToSearch, alreadyFoundArxivs);
                    addIdentifierToKey(arxivToSearch, SubmissionLookupDataLoader.ARXIV, keys);
                }
                if (provider.getSupportedIdentifiers().contains(SubmissionLookupDataLoader.CINII))
                {
                    Set<String> ciniiToSearch = new HashSet<String>();
                    Set<String> alreadyFoundCiniis = provider2foundCiniis.get(providerName);
                    identifierToSearch(foundCiniis, ciniiToSearch, alreadyFoundCiniis);
                    addIdentifierToKey(ciniiToSearch, SubmissionLookupDataLoader.CINII, keys);
                }
                if (provider.getSupportedIdentifiers().contains(SubmissionLookupDataLoader.SCOPUSEID))
                {
                    Set<String> scopusToSearch = new HashSet<String>();
                    Set<String> alreadyFoundScopuss = provider2foundScopuss.get(providerName);
                    identifierToSearch(foundScopuss, scopusToSearch, alreadyFoundScopuss);
                    addIdentifierToKey(scopusToSearch, SubmissionLookupDataLoader.SCOPUSEID, keys);
                }
                if (provider.getSupportedIdentifiers().contains(SubmissionLookupDataLoader.WOSID))
                {
                    Set<String> wosToSearch = new HashSet<String>();
                    Set<String> alreadyFoundWOSs = provider2foundWOSs.get(providerName);
                    identifierToSearch(foundWOSs, wosToSearch, alreadyFoundWOSs);
                    addIdentifierToKey(wosToSearch, SubmissionLookupDataLoader.WOSID, keys);
                }
                if (provider.getSupportedIdentifiers().contains(SubmissionLookupDataLoader.ADSBIBCODE))
                {
                    Set<String> adsbibcodeToSearch = new HashSet<String>();
                    Set<String> alreadyFoundAdsbibcodes = provider2foundAdsbibcodes.get(providerName);
                    identifierToSearch(foundAdsbibcodes, adsbibcodeToSearch, alreadyFoundAdsbibcodes);
                    addIdentifierToKey(adsbibcodeToSearch, SubmissionLookupDataLoader.ADSBIBCODE, keys);
                }

                if (!keys.isEmpty())
                {
                    List<Record> pPublications = null;
                    Context context = null;
                    try
                    {
                        context = new Context();
                        pPublications = provider.getByIdentifier(context, keys);
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
        }

        log.info("BTE DataLoader finished. Items loaded: "
                + recordSet.getRecords().size());

        // Printing debug message
        if(log.isDebugEnabled()) {
		    String totalString = "";
		    for (Record record : recordSet.getRecords())
		    {
		        totalString += SubmissionLookupUtils.getPrintableString(record)
		                + "\n";
		    }
		    log.debug("#########Records loaded##########");
		    log.debug(totalString);
        }
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

    private void retrieveIdentifier(Record publication,
            String providerName, Map<String, Set<String>> provider2foundIdentifiers,
            List<String> foundIdentifiers, String identifierName, String identifierNotFound)
    {
        String identifier = null;
        List<Value> identifierValue = publication.getValues(identifierName);
        if (identifierValue != null && !identifierValue.isEmpty())
        {
            identifier = identifierValue.iterator().next().getAsString();
        }
        if (identifier == null || (identifierName.equals("url") && !identifier.contains("arxiv")))
        {
            identifier = identifierNotFound;
        }
        else
        {
            if (identifierName.equals("doi"))
            {
                identifier = SubmissionLookupUtils.normalizeDOI(identifier);
            }

            if (!foundIdentifiers.contains(identifier))
            {
                foundIdentifiers.add(identifier);
            }
            Set<String> tmp = provider2foundIdentifiers.get(providerName);
            if (tmp == null)
            {
                tmp = new HashSet<String>();
                provider2foundIdentifiers.put(providerName, tmp);
            }
            tmp.add(identifier);
        }
    }

    private void identifierToSearch(List<String> foundIdentifiers,
            Set<String> identifierToSearch, Set<String> alreadyFoundIdentifiers)
    {
        if (foundIdentifiers != null && !foundIdentifiers.isEmpty())
        for (String identifier : foundIdentifiers)
        {
            if (alreadyFoundIdentifiers == null || !alreadyFoundIdentifiers.contains(identifier))
            {
                identifierToSearch.add(identifier);
            }
        }
    }

    private void addIdentifierToKey(Set<String> identifierToSearch,
            String identifierName, Map<String, Set<String>> keys)
    {
        if (identifierToSearch != null && !identifierToSearch.isEmpty())
        {
            keys.put(identifierName, identifierToSearch);
        }
    }
}
