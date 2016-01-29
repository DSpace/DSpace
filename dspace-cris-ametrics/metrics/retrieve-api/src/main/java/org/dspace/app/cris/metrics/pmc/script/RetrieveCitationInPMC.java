/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.metrics.pmc.script;


import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.dspace.app.cris.metrics.common.model.ConstantMetrics;
import org.dspace.app.cris.metrics.common.model.CrisMetrics;
import org.dspace.app.cris.metrics.common.services.MetricsPersistenceService;
import org.dspace.app.cris.metrics.pmc.model.PMCCitation;
import org.dspace.app.cris.metrics.pmc.model.PMCRecord;
import org.dspace.app.cris.metrics.pmc.services.PMCEntrezException;
import org.dspace.app.cris.metrics.pmc.services.PMCEntrezLocalSOLRServices;
import org.dspace.app.cris.metrics.pmc.services.PMCEntrezServices;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
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

public class RetrieveCitationInPMC
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(RetrieveCitationInPMC.class);

    private static MetricsPersistenceService pservice;

    private static PMCEntrezServices entrez = new PMCEntrezServices();

    private static PMCEntrezLocalSOLRServices entrezLocal = new PMCEntrezLocalSOLRServices();

    private static SearchService searcher;

    private static long timeElapsed = 3600000 * 24 * 7; // 1 week

	private static int maxItemToWork = 100;
    
    private static String queryDefault = "+dc.identifier.pmid:[* TO *]";

	private static int MAX_QUERY_RESULTS = 50;

    public static void main(String[] args) throws SearchServiceException,
            SQLException, AuthorizeException, ParseException
    {
        CommandLineParser parser = new PosixParser();

        Options options = new Options();
        options.addOption("h", "help", false, "help");

        options.addOption(
                "t",
                "time",
                true,
                "Limit to update only citation more old than <t> seconds. Use 0 to force update of all record");

        options.addOption(
                "x",
                "max",
                true,
                "Process a max of <x> items. Only worked items matter, item not worked because up-to-date (see t option) are not counted. Use 0 to set no limits");

        options.addOption(
                "q",
                "query",
                true,
                "Override the default query to retrieve puntual publication (used for test scope, the default query will be deleted");
        
        CommandLine line = parser.parse(options, args);

        if (line.hasOption('h'))
        {
            HelpFormatter myhelp = new HelpFormatter();
            myhelp.printHelp("RetrieveCitationInPMC \n", options);
            System.out
                    .println("\n\nUSAGE:\n RetrieveCitationInPMC [-t 3600] [-x 100] \n");
            System.exit(0);
        }

        DSpace dspace = new DSpace();
        int citationRetrieved = 0;
        int itemWorked = 0;
        Date startDate = new Date();

        if (line.hasOption('t'))
        {
            timeElapsed = Long.valueOf(line.getOptionValue('t').trim()) * 1000; // option
                                                                                // is
                                                                                // in
                                                                                // seconds
        }
        if (line.hasOption('x'))
        {
			maxItemToWork = Integer.valueOf(line.getOptionValue('x').trim());
			if (maxItemToWork < MAX_QUERY_RESULTS) {
				MAX_QUERY_RESULTS = maxItemToWork;
			}
        }
        
        if (line.hasOption('q'))
        {
            queryDefault = line.getOptionValue('q').trim(); 
        }
        ServiceManager serviceManager = dspace.getServiceManager();
        // System.out.println(serviceManager.getServicesNames());
        searcher = serviceManager.getServiceByName(
                SearchService.class.getName(), SearchService.class);

        pservice = serviceManager.getServiceByName(
                MetricsPersistenceService.class.getName(),
                MetricsPersistenceService.class);

        Context context = null;
		long resultsTot = -1;
        try
        {
            context = new Context();

			all: for (int page = 0;; page++) {
				int start = page * MAX_QUERY_RESULTS;
				if (resultsTot != -1 && start >= resultsTot) {
					break all;
				}
				if (maxItemToWork != 0 && itemWorked == maxItemToWork)
					break all;

				DiscoverQuery query = new DiscoverQuery();
				query.setStart(start);
				query.setMaxResults(MAX_QUERY_RESULTS==0?Integer.MAX_VALUE:MAX_QUERY_RESULTS);
				query.setQuery(queryDefault);

				query.setDSpaceObjectFilter(Constants.ITEM);
				query.addSearchField("dc.identifier.pmid");
                query.addSearchField("search.resourceid");
                query.addSearchField("search.resourcetype");
                query.addSearchField("handle");
				DiscoverResult qresp = searcher.search(context, query);
				resultsTot = qresp.getTotalSearchResults();
				log.info(LogManager.getHeader(null, "retrieve_citation_pubmed", "Processing start = " + start + " | max_query_result = "+ MAX_QUERY_RESULTS + " | total = " + qresp.getTotalSearchResults()
						+ " items"));
				log.info(LogManager.getHeader(null, "retrieve_citation_pubmed",
                        "Processing informations itemWorked:\""+itemWorked+"\" maxItemToWork: \"" + maxItemToWork + "\" - start:\"" + start + "\" - page:\"" + page + "\""));
				int docWorked = 0;
				internal: for (DSpaceObject dso : qresp.getDspaceObjects()) {					
					List<SearchDocument> list = qresp.getSearchDocument(dso);
					for (SearchDocument doc : list) {
						if (maxItemToWork != 0 && itemWorked == maxItemToWork) {
							break all;
						}
						if (resultsTot != -1 && docWorked >= resultsTot) {
							break all;
						}
						Integer itemID = dso.getID();

						if (isCheckRequired(itemID)) {
							itemWorked++;
							List<String> pmids = doc.getSearchFieldValues("dc.identifier.pmid");

							for (String pmid : pmids) {
								log.debug(LogManager.getHeader(null, "retrieve_citation", "lookup pmid:" + pmid));
								try {
									Integer ipmid = Integer.valueOf((String) pmid);
									Set<Integer> citingPMCIDs = entrez.getCitedByPMEDID(ipmid);
									log.debug(LogManager.getHeader(null, "retrieve_citation_pubmed",
											"found " + citingPMCIDs.size() + " citing PMC records"));
									// if (citingPMCIDs != null &&
									// citingPMCIDs.size() >
									// 0)
									// {
									citationRetrieved += citingPMCIDs.size();
									updatePMCCiting(itemID, ipmid, citingPMCIDs);
									// }
								} catch (NumberFormatException nfe) {
									log.error(LogManager.getHeader(null, "retrieve_citation",
											"Found an invalid PID value! ItemID: " + itemID + " - PMID: " + pmid));
								} catch (PMCEntrezException pe) {
									log.error(
											LogManager.getHeader(null, "retrieve_citation", "Error in EntrezService"),
											pe);
								}
							}
						}
						docWorked++;
					}
				}
			}
			Date endDate = new Date();
			long processTime = (endDate.getTime() - startDate.getTime()) / 1000;
			log.info(LogManager.getHeader(null, "retrieve_citation", "Processing time " + processTime
					+ " sec. - Retrieved " + citationRetrieved + " PMC citation for " + itemWorked + " items"));
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		} finally {

			if (context != null && context.isValid()) {
				context.abort();
			}

		}

	}

	private static void updatePMCCiting(Integer itemID, Integer pmid,
            Set<Integer> pmcIDs) throws SearchServiceException
    {
        Integer[] arrPMCIDs = new Integer[pmcIDs.size()];
        arrPMCIDs = pmcIDs.toArray(arrPMCIDs);
        List<PMCRecord> pmcRecords = null;
        Context context = null;
        try
        {
            context = new Context();

            try
            {
                pmcRecords = entrez.getMultiPMCRecord(arrPMCIDs);
                for (PMCRecord pmcRecord : pmcRecords)
                {
                    List<Integer> pubmedIDs = entrezLocal
                            .getPubmedIDs(pmcRecord.getId());
                    if (pubmedIDs != null && pubmedIDs.size() > 0)
                    {
                        pmcRecord.setPubmedIDs(pubmedIDs);
						SolrQuery query = new SolrQuery();
                        query.setQuery("dc.identifier.pmid:("
                                + StringUtils.collectionToDelimitedString(
                                        pubmedIDs, " OR ") + ")");
						query.setFields("handle");
						query.addFilterQuery("search.resourcetype:" + Constants.ITEM);
						QueryResponse qresp = searcher.search(query);
                        List<String> handles = new ArrayList<String>();
						for (SolrDocument doc : qresp.getResults())
                        {
							handles.add((String) doc.getFirstValue("handle"));
                        }
                        pmcRecord.setHandles(handles);
                    }
                    pservice.saveOrUpdate(PMCRecord.class, pmcRecord);
                }
            }
            catch (PMCEntrezException e)
            {
                log.error(LogManager.getHeader(null, "updatePMCRecord",
                        "Error in EntrezService"), e);
            }
            
            PMCCitation citation = new PMCCitation();
            citation.setId(pmid);
            citation.setPmcRecords(pmcRecords);            
            
			SolrQuery query = new SolrQuery();
            query.setQuery("dc.identifier.pmid:" + pmid);
			query.setRows(100);
			query.setFields("search.resourceid");
			query.setFields("handle");
			query.addFilterQuery("search.resourcetype:" + Constants.ITEM);
			QueryResponse qresp = searcher.search(query);
            List<Integer> itemIDs = new ArrayList<Integer>();
			for (SolrDocument doc : qresp.getResults())
            {
				itemIDs.add((Integer) doc.getFirstValue("search.resourceid"));
            }
            citation.setItemIDs(itemIDs);
            pservice.saveOrUpdate(PMCCitation.class, citation);
            
            
            CrisMetrics crisMetrics = new CrisMetrics();
            crisMetrics.setMetricCount(pmcIDs.size());
            crisMetrics.setEndDate(new Date());
            crisMetrics.setResourceId(itemID);
            crisMetrics.setResourceTypeId(Constants.ITEM);
            query = new SolrQuery();
            query.setQuery("search.unique:" + Constants.ITEM + "-" + itemID);
            query.setRows(1);
            query.setFields("handle");
            query.addFilterQuery("search.resourcetype:" + Constants.ITEM);
            qresp = searcher.search(query);
            for (SolrDocument doc : qresp.getResults())
            {
                crisMetrics.setUuid((String)doc.getFirstValue("handle"));
                break;
            }
            
            String identifierPMID = ""+citation.getId();
            String resultPMCID = "http://www.ncbi.nlm.nih.gov/pubmed?linkname=pubmed_pubmed_citedin&from_uid="+identifierPMID;
            crisMetrics.getTmpRemark().put("link", resultPMCID);
            crisMetrics.getTmpRemark().put("identifier", identifierPMID);                        
            crisMetrics.setRemark(crisMetrics.buildMetricsRemark());
            crisMetrics.setMetricType(ConstantMetrics.STATS_INDICATOR_TYPE_PUBMED);
            pservice.saveOrUpdate(CrisMetrics.class, crisMetrics);
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

    private static boolean isCheckRequired(Integer itemID)
    {
        if (timeElapsed != 0)
        {
            PMCCitation cit = pservice.getPMCCitationByItemID(itemID);
            if (cit == null)
            {
                return true;
            }
            long now = new Date().getTime();

            Date lastCheck = cit.getTimeStampInfo().getLastModificationTime();
            long lastCheckTime = 0;

            if (lastCheck != null)
                lastCheckTime = lastCheck.getTime();

            return (now - lastCheckTime >= timeElapsed);
        }
        else
        {
            return true;
        }
    }
}
