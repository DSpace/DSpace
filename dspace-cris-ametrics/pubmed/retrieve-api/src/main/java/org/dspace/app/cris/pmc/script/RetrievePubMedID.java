/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.pmc.script;


import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.app.cris.pmc.services.PMCEntrezException;
import org.dspace.app.cris.pmc.services.PMCEntrezLocalSOLRServices;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.DiscoverResult.SearchDocument;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.kernel.ServiceManager;
import org.dspace.utils.DSpace;
import org.springframework.util.StringUtils;

public class RetrievePubMedID
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(RetrievePubMedID.class);

    /** The DSpace context to initialize ONLY if needed (some pubmedID found) */
    private static Context context = null;

    public static void main(String[] args) throws SearchServiceException,
            SQLException, AuthorizeException, PMCEntrezException
    {
        DSpace dspace = new DSpace();
        int pmidRetrieved = 0;
        Date startDate = new Date();

        ServiceManager serviceManager = dspace.getServiceManager();
        SearchService searcher = serviceManager.getServiceByName(
                SearchService.class.getName(), SearchService.class);
        PMCEntrezLocalSOLRServices entrez = new PMCEntrezLocalSOLRServices();
        Context context = null;
        try
        {
            context = new Context();
            DiscoverQuery query = new DiscoverQuery();
            query.setQuery("+(dc.identifier.doi:[* TO *] OR dc.identifier.pmcid:[* TO *]) -dc.identifier.pmid:[* TO *]");
            query.setMaxResults(Integer.MAX_VALUE);
            query.setDSpaceObjectFilter(Constants.ITEM);
            query.addSearchField("dc.identifier.doi");
            query.addSearchField("dc.identifier.pmcid");           
            DiscoverResult qresp = searcher.search(context, query);
            for (DSpaceObject dso : qresp.getDspaceObjects())
            {

                List<SearchDocument> list = qresp.getSearchDocument(dso);
                log.info(LogManager.getHeader(null, "retrieve_pubmedID",
                        "Processing " + qresp.getTotalSearchResults()
                                + " items"));
                for (SearchDocument doc : list)
                {
                    Integer itemID = dso.getID();
                    if (isCheckRequired(itemID))
                    {
                        List<String> dois = doc
                                .getSearchFieldValues("dc.identifier.doi");
                        List<String> pmcids = doc
                                .getSearchFieldValues("dc.identifier.pmcid");
                        if (dois != null)
                        {
                            for (Object doi : dois)
                            {
                                log.debug(LogManager.getHeader(null,
                                        "retrieve_pubmedID", "search doi:"
                                                + doi));
                                List<Integer> pubmedIDs = entrez
                                        .getPubmedIDs((String) doi);
                                log.debug(LogManager.getHeader(null,
                                        "retrieve_pubmedID", "pubmedIDs="
                                                + pubmedIDs));
                                if (pubmedIDs != null && pubmedIDs.size() > 0)
                                {
                                    pmidRetrieved++;
                                    recordPubmedID(itemID, pubmedIDs);
                                }
                            }
                        }
                        if (pmcids != null)
                        {
                            for (Object pmcid : pmcids)
                            {
                                String spmcid = (String) pmcid;
                                if (spmcid.toLowerCase().startsWith("pmc"))
                                {
                                    spmcid = spmcid.substring(3);
                                }
                                log.debug(LogManager.getHeader(null,
                                        "retrieve_pubmedID", "search PMCID:"
                                                + pmcid));
                                try
                                {
                                    Integer ipmcid = Integer.valueOf(spmcid);
                                    List<Integer> pubmedIDs = entrez
                                            .getPubmedIDs(ipmcid);
                                    log.debug(LogManager.getHeader(
                                            null,
                                            "retrieve_pubmedID",
                                            "pubmedIDs="
                                                    + StringUtils
                                                            .collectionToCommaDelimitedString(pubmedIDs)));
                                    if (pubmedIDs != null
                                            && pubmedIDs.size() > 0)
                                    {
                                        pmidRetrieved++;
                                        recordPubmedID(itemID, pubmedIDs);
                                    }
                                }
                                catch (NumberFormatException nfe)
                                {
                                    log.error(LogManager.getHeader(null,
                                            "retrieve_pubmedID",
                                            "Found an invalid PMCID value! ItemID: "
                                                    + itemID + " - PMCID: "
                                                    + pmcid));
                                }
                            }
                        }
                    }
                }
            }
            Date endDate = new Date();
            long processTime = (endDate.getTime() - startDate.getTime()) / 1000;
            log.info(LogManager.getHeader(null, "retrieve_pubmedID",
                    "Processing time " + processTime + " sec. - Retrieved "
                            + pmidRetrieved + " pubmedID"));
        }
        catch (Exception ex)
        {
            log.error(ex.getMessage(), ex);
        }
        finally
        {
            if (context != null && context.isValid())
            {
                context.abort();
            }
        }

    }

    private static void recordPubmedID(Integer itemID, List<Integer> pubmedIDs)
            throws SQLException, AuthorizeException
    {
        Context context = getContext();
        Item item = Item.find(context, itemID);
        for (Integer pid : pubmedIDs)
        {
            item.clearMetadata("dc", "identifier", "pmid", null);
            item.addMetadata("dc", "identifier", "pmid", null, pid.toString());
            item.update();
        }
        context.commit();
        context.removeCached(item, itemID);
    }

    private static Context getContext() throws SQLException
    {
        if (context != null)
        {
            return context;
        }
        context = new Context();
        context.turnOffAuthorisationSystem();
        return context;
    }

    private static boolean isCheckRequired(Integer fieldValue)
    {
        return true;
    }
}
