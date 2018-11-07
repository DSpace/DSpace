/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.batch;

import java.util.Date;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.orcid.OrcidPreferencesUtils;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.util.Researcher;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.authority.orcid.OrcidService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.discovery.SearchService;
import org.dspace.utils.DSpace;
import org.orcid.ns.record.Record;

public class ScriptCleanInvalidOrcidTokens {

	/** log4j logger */
	private static Logger log = Logger.getLogger(ScriptCleanInvalidOrcidTokens.class);

	private static boolean singleMode = false;
	
	/**
	 * Batch script to register ORCID WebHooks. Try with -h to see more helps.
	 */
	public static void main(String[] args) throws ParseException {

		log.info("#### START Script clean unvalid ORCID token: -----" + new Date() + " ----- ####");
		Context context = null;
		try {
			DSpace dspace = new DSpace();
			context = new Context();
			context.turnOffAuthorisationSystem();
			SearchService searchService = dspace.getSingletonService(SearchService.class);
			ApplicationService applicationService = dspace.getServiceManager().getServiceByName("applicationService",
					ApplicationService.class);

			CommandLineParser parser = new PosixParser();

			Options options = new Options();
			options.addOption("h", "help", false, "help");
			options.addOption("s", "single_researcher", true, "It works on single researcher. By default will check all the rp with an access token");
			
			CommandLine line = parser.parse(options, args);

			if (line.hasOption('h')) {
				HelpFormatter myhelp = new HelpFormatter();
				myhelp.printHelp("ScriptCleanUnvalidOrcidToken \n", options);
				System.out.println(
						"\n\nUSAGE:\n ScriptCleanUnvalidOrcidToken [-s <researcher_identifier>]- with no options it will check all the RP with an ORCID token \n");

				System.exit(0);
			}
			
			String crisID = null;
			
            if (line.hasOption('s')) {
				singleMode = true;
				crisID = line.getOptionValue('s');
			}
            
			if (singleMode) {
				ResearcherPage rp = null;
				if (StringUtils.isNotBlank(crisID)) {
					rp = applicationService.uniqueByCrisID(crisID);
				}
				if (rp == null) {
					System.out.println("RP not found. CRIS ID " + crisID);
	                System.exit(1);
				}
				else {
					cleanInvalidToken(context, crisID, rp);
					context.commit();
					System.exit(0);
				}
			}
			else {
				// work on multiple researchers
				SolrQuery sQuery = new SolrQuery("crisrp.orcid:[* TO *]");
				sQuery.addFilterQuery("search.resourcetype:"+CrisConstants.RP_TYPE_ID);
				sQuery.addFilterQuery("crisrp." + OrcidService.SYSTEM_ORCID_TOKEN_ACCESS+ ":[* TO *]");	
				sQuery.setFields("cris-id");
				QueryResponse qResp = searchService.search(sQuery);
				if (qResp.getResults() != null && qResp.getResults().getNumFound() > 0) {
					for (SolrDocument sd : qResp.getResults()) {
						crisID = (String) qResp.getResults().get(0).getFirstValue("cris-id");
						ResearcherPage rp = applicationService.uniqueByCrisID(crisID);
						cleanInvalidToken(context, crisID, rp);
						context.commit();
					}
				}
			}
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		} finally {
			if (context != null && context.isValid()) {
				context.abort();
			}
		}
		log.info("#### END: -----" + new Date() + " ----- ####");
		System.exit(0);
	}

	public static void cleanInvalidToken(Context context, String crisID, ResearcherPage rp) {
		String token = OrcidPreferencesUtils.getTokenReleasedForSync(rp, OrcidService.SYSTEM_ORCID_TOKEN_ACCESS);
		if (token == null) {
			// nothing to do, we don't have an access token yet
			System.out.println("nothing to do, we don't have an access token yet" + crisID);
		    System.exit(1);
		}
		
		String orcid = ResearcherPageUtils.getStringValue(rp, "orcid");
		Record record = null;
		try {
			record = OrcidService.getOrcid().getRecord(orcid, token);
		}
		catch (Exception e) {
			log.info("Invalid token for orcid " + orcid + " got " + e.getMessage());
			log.debug(e.getMessage(), e);
		}
		
		if (record == null) {
			context.turnOffAuthorisationSystem();
			OrcidPreferencesUtils.setTokens(rp, null);
			log.info(LogManager.getHeader(context, "revoke_orcid_permission",
					"Removing orcid tokens from " + rp.getCrisID()));
			if (!"all".equalsIgnoreCase(ConfigurationManager.getProperty("authentication-oauth", "orcid-webhook")) && 
					!"none".equalsIgnoreCase(ConfigurationManager.getProperty("authentication-oauth", "orcid-webhook"))) {
				OrcidPreferencesUtils.unregisterOrcidWebHook(rp);
			}
			new Researcher().getApplicationService().saveOrUpdate(ResearcherPage.class, rp);
			context.restoreAuthSystemState();
			System.out.println("Access tokens are not longer valid, removed from " + crisID);
		}
		else {
			System.out.println("Access tokens are STILL VALID for " + crisID);
		}
	}

}
