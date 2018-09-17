/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.metrics.scopus.script;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.cris.metrics.common.model.ConstantMetrics;
import org.dspace.app.cris.metrics.common.model.CrisMetrics;
import org.dspace.app.cris.metrics.common.services.MetricsPersistenceService;
import org.dspace.app.cris.metrics.scopus.dto.ScopusResponse;
import org.dspace.app.cris.metrics.scopus.services.ScopusService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.ConfigurationManager;
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

public class ScriptRetrieveCitation {

	private static final String fieldPubmedID = ConfigurationManager.getProperty("cris", "ametrics.identifier.pmid");
	private static final String fieldScopusID = ConfigurationManager.getProperty("cris", "ametrics.identifier.eid");
	private static final String fieldDoiID = ConfigurationManager.getProperty("cris", "ametrics.identifier.doi");

	/** log4j logger */
	private static Logger log = Logger.getLogger(ScriptRetrieveCitation.class);

	private static MetricsPersistenceService pService;

	private static ScopusService sService;

	private static SearchService searcher;

	private static long timeElapsed = 3600000 * 24 * 7; // 1 week

	private static int maxItemToWork = 100;

	private static String queryDefault;

	private static int MAX_QUERY_RESULTS = 50;

	private static boolean sleep = true;

	private static boolean enrichMetadataItem = false;

	public static void main(String[] args)
			throws SearchServiceException, SQLException, AuthorizeException, ParseException {

		CommandLineParser parser = new PosixParser();

		Options options = new Options();
		options.addOption("h", "help", false, "help");

		options.addOption("e", "enrich", false, "Enrich item with the response from scopus");

		options.addOption("s", "disable-sleep", false, "disable sleep timeout for each call to scopus");

		options.addOption("t", "time", true,
				"Limit to update only citation more old than <t> seconds. Use 0 to force update of all record");

		options.addOption("x", "max", true,
				"Process a max of <x> items. Only worked items matter, item not worked because up-to-date (see t option) are not counted. Use 0 to set no limits");

		options.addOption("q", "query", true,
				"Override the default query to retrieve puntual publication (used for test scope, the default query will be deleted");

		CommandLine line = parser.parse(options, args);

		if (line.hasOption('h')) {
			HelpFormatter myhelp = new HelpFormatter();
			myhelp.printHelp("RetrieveCitation \n", options);
			System.out.println("\n\nUSAGE:\n RetrieveCitation [-t 3600] [-x 100] \n");
			System.exit(0);
		}

		DSpace dspace = new DSpace();
		int citationRetrieved = 0;
		int itemWorked = 0;
		int itemForceWorked = 0;
		Date startDate = new Date();

		if (line.hasOption('t')) {
			timeElapsed = Long.valueOf(line.getOptionValue('t').trim()) * 1000; // option
																				// is
																				// in
																				// seconds
		}
		if (line.hasOption('x')) {
			maxItemToWork = Integer.valueOf(line.getOptionValue('x').trim());
			if (maxItemToWork < MAX_QUERY_RESULTS) {
				MAX_QUERY_RESULTS = maxItemToWork;
			}
		}

		queryDefault = fieldPubmedID + ":[* TO *] OR " + fieldDoiID + ":[* TO *] OR " + fieldScopusID + ":[* TO *]";
		if (line.hasOption('q')) {
			queryDefault = line.getOptionValue('q').trim();
		}

		if (line.hasOption('s')) {
			sleep = false;
		}

		if (line.hasOption('e')) {
			enrichMetadataItem = true;
		}

		ServiceManager serviceManager = dspace.getServiceManager();

		searcher = serviceManager.getServiceByName(SearchService.class.getName(), SearchService.class);

		pService = serviceManager.getServiceByName(MetricsPersistenceService.class.getName(),
				MetricsPersistenceService.class);

		sService = serviceManager.getServiceByName(ScopusService.class.getName(), ScopusService.class);

		Context context = null;
		long resultsTot = -1;
		try {
			context = new Context();
			context.turnOffAuthorisationSystem();
			all: for (int page = 0;; page++) {
				int start = page * MAX_QUERY_RESULTS;
				if (resultsTot != -1 && start >= resultsTot) {
					break all;
				}
				if (maxItemToWork != 0 && itemWorked >= maxItemToWork  && itemForceWorked > 50)
					break all;

				// get all items that contains PMID or DOI or EID
				DiscoverQuery query = new DiscoverQuery();
				query.setStart(start);
				query.setMaxResults(MAX_QUERY_RESULTS);
				query.setQuery(queryDefault);
				query.setDSpaceObjectFilter(Constants.ITEM);

				query.addSearchField(fieldPubmedID);
				query.addSearchField(fieldDoiID);
				query.addSearchField(fieldScopusID);

                query.addSearchField("search.resourceid");
                query.addSearchField("search.resourcetype");
                query.addSearchField("handle");
				DiscoverResult qresp = searcher.search(context, query);
				resultsTot = qresp.getTotalSearchResults();
				log.info(LogManager.getHeader(null, "retrieve_citation_scopus",
						"Processing " + qresp.getTotalSearchResults() + " items"));
                log.info(LogManager.getHeader(null, "retrieve_citation_scopus",
                        "Processing informations itemWorked:\""+itemWorked+"\" maxItemToWork: \"" + maxItemToWork + "\" - start:\"" + start + "\" - page:\"" + page + "\""));
				// for each item check
				for (DSpaceObject dso : qresp.getDspaceObjects()) {

					List<SearchDocument> list = qresp.getSearchDocument(dso);
					for (SearchDocument doc : list) {
						if (maxItemToWork != 0 && itemWorked >= maxItemToWork  && itemForceWorked > 50)
							break all;

						Integer itemID = dso.getID();

						if (isCheckRequired(itemID)) {
							itemWorked++;
							List<String> pmids = doc.getSearchFieldValues(fieldPubmedID);
							List<String> dois = doc.getSearchFieldValues(fieldDoiID);
							List<String> eids = doc.getSearchFieldValues(fieldScopusID);

							log.debug(LogManager.getHeader(null, "retrieve_citation_scopus",
									"lookup pmid:" + pmids + ", lookup doi:" + dois + ", lookup eid:" + eids));

							ScopusResponse response = sService.getCitations(sleep, pmids, dois, eids);

							boolean itWorks = buildCiting(dso, response);
							if(itWorks) {
							    itemForceWorked++;
							}
						}
					}
				}

				context.commit();
			}
			Date endDate = new Date();
			long processTime = (endDate.getTime() - startDate.getTime()) / 1000;
			log.info(LogManager.getHeader(null, "retrieve_citation", "Processing time " + processTime
					+ " sec. - Retrieved " + citationRetrieved + " Scopus citation for " + itemWorked + " items(" + itemForceWorked + " forced items)"));
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		} finally {

			if (context != null && context.isValid()) {
				context.abort();
			}

		}

	}

	private static boolean buildCiting(DSpaceObject dso, ScopusResponse response) throws SQLException, AuthorizeException {
        CrisMetrics citation = response.getCitation();
        if (!response.isError())
        {
            if (citation != null)
            {
                citation.setResourceId(dso.getID());
                citation.setResourceTypeId(dso.getType());
                citation.setUuid(dso.getHandle());
                pService.saveOrUpdate(CrisMetrics.class, citation);
                if (enrichMetadataItem)
                {
                    Item item = (Item) dso;
                    if (StringUtils.isNotBlank(citation.getIdentifier()))
                    {
                        Metadatum[] metadatumEid = item
                                .getMetadataByMetadataString(fieldScopusID);
                        if (metadatumEid != null && metadatumEid.length > 0)
                        {
                            item.clearMetadata(metadatumEid[0].schema,
                                    metadatumEid[0].element,
                                    metadatumEid[0].qualifier,
                                    metadatumEid[0].language);
                            item.addMetadata(metadatumEid[0].schema,
                                    metadatumEid[0].element,
                                    metadatumEid[0].qualifier,
                                    metadatumEid[0].language,
                                    citation.getIdentifier());
                        }
                    }

                    item.update();
                }
                return true;
            }
        }
        return false;
	}

	private static boolean isCheckRequired(Integer itemID) {
		if (timeElapsed != 0) {
			CrisMetrics cit = pService.getLastMetricByResourceIDAndResourceTypeAndMetricsType(itemID, Constants.ITEM, ConstantMetrics.STATS_INDICATOR_TYPE_SCOPUS);
			if (cit == null || cit.getMetricCount()==-1) {
				if(cit!=null) {
					pService.delete(CrisMetrics.class, cit.getId());
				}
				return true;
			}
			long now = new Date().getTime();

			Date lastCheck = cit.getTimeStampInfo().getCreationTime();
			long lastCheckTime = 0;

			if (lastCheck != null)
				lastCheckTime = lastCheck.getTime();

			return (now - lastCheckTime >= timeElapsed);
		} else {
			return true;
		}
	}
}
