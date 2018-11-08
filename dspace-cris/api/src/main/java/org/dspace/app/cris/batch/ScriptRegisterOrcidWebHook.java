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
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.authority.orcid.OrcidService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.discovery.SearchService;
import org.dspace.utils.DSpace;

public class ScriptRegisterOrcidWebHook {

	/** log4j logger */
	private static Logger log = Logger.getLogger(ScriptRegisterOrcidWebHook.class);

	private static boolean singleMode = false;
	
	/**
	 * Batch script to register ORCID WebHooks. Try with -h to see more helps.
	 */
	public static void main(String[] args) throws ParseException {

		log.info("#### START Script register webhooks from ORCID: -----" + new Date() + " ----- ####");
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
			options.addOption("s", "single_researcher", true, "It works on single researcher using the ORCID in the RP or the one specified with the -o param");
			options.addOption("o", "orcid", true,
					"It register a webhook for the specified ORCID if a local RP exists. If the -s option is also present the script will fail in case of conflict between the specified orcid and the one in the rp");
			options.addOption("f", "force", false, "force the creation of the webhooks also if they look already registered");
			options.addOption("x", "unregister", true, "switch the script to the unregister mode");
			
			CommandLine line = parser.parse(options, args);

			if (line.hasOption('h')) {
				HelpFormatter myhelp = new HelpFormatter();
				myhelp.printHelp("ScriptRegisterOrcidWebHooks \n", options);
				System.out.println(
						"\n\nUSAGE:\n ScriptRegisterOrcidWebHook [-s <researcher_identifier>] [-o <ORCID>] [-f][-x] - with no options it will register all the required webhooks for all the RP with an ORCID \n");

				System.exit(0);
			}
			
			String crisID = null;
			String orcidParam = null;
			boolean unregisterMode = false;
			boolean force = false;
			
            if (line.hasOption('s') || line.hasOption('o')) {
				singleMode = true;
				crisID = line.getOptionValue('s');
				orcidParam = line.getOptionValue('o');
			}
            
			if (line.hasOption('x')) {
				unregisterMode = true;
			}
            
			if (line.hasOption('f')) {
				force = true;
			}
            
			if (singleMode) {
				ResearcherPage rp = null;
				if (StringUtils.isNotBlank(crisID)) {
					rp = applicationService.uniqueByCrisID(crisID);
				}
				if (StringUtils.isNotBlank(orcidParam)) {
					if (rp != null) {
						String rpORCID = ResearcherPageUtils.getStringValue(rp, "orcid");
						if (!StringUtils.equals(rpORCID, orcidParam)) {
							System.out.println("ORCID mismatch the RP: " + crisID + " has " + rpORCID + " that differ from the o param " + orcidParam);
			                System.exit(1);
						}
					}
					else {
						SolrQuery sQuery = new SolrQuery("crisrp.orcid:\""+orcidParam+"\"");
						QueryResponse qResp = searchService.search(sQuery);
						if (qResp.getResults() != null && qResp.getResults().getNumFound() == 1) {
							crisID = (String) qResp.getResults().get(0).getFirstValue("cris-id");
							rp = applicationService.uniqueByCrisID(crisID);		
						}
					}
				}
				if (rp == null) {
					System.out.println("RP not found. CRIS ID " + crisID + " ORCID " + orcidParam);
	                System.exit(1);
				}
				else {
					Boolean flag = ResearcherPageUtils.getBooleanValue(rp, OrcidPreferencesUtils.RPPDEF_ORCID_WEBHOOK);
					
					if (force || flag == null || (flag && unregisterMode) || (!flag && !unregisterMode)) {
						if (unregisterMode) {
							OrcidPreferencesUtils.unregisterOrcidWebHook(rp);
						}
						else {
							OrcidPreferencesUtils.registerOrcidWebHook(rp);
						}
						applicationService.saveOrUpdate(ResearcherPage.class, rp);
					}
					else {
						System.out.println("RP webhook property is " + flag + " "
								+ (unregisterMode ? "to unregister " : "to register ") + " use the force flag");
		                System.exit(1);
					}
				}
			}
			else {
				if (!"all".equalsIgnoreCase(ConfigurationManager.getProperty("authentication-oauth", "orcid-webhook"))
						&& !"connected".equalsIgnoreCase(
								ConfigurationManager.getProperty("authentication-oauth", "orcid-webhook"))) {
					System.out.println("ORCID webhook are inactive, please enable them in the authentication-oauth.cfg for all or connected profiles");
	                System.exit(1);
				}
				// work on multiple researchers
				SolrQuery sQuery = new SolrQuery("crisrp.orcid:[* TO *]");
				sQuery.addFilterQuery("search.resourcetype:"+CrisConstants.RP_TYPE_ID);
				if (!force) {
					if (unregisterMode) {
						sQuery.addFilterQuery("crisrp." + OrcidPreferencesUtils.RPPDEF_ORCID_WEBHOOK + ":true");	
					}
					else {
						sQuery.addFilterQuery("-crisrp." + OrcidPreferencesUtils.RPPDEF_ORCID_WEBHOOK + ":true");
					}
				}
				
				if (!"all".equalsIgnoreCase(ConfigurationManager.getProperty("authentication-oauth", "orcid-webhook"))) {
					// if we don't want webhook for imported profiles, limit the search to the one with an access token (= connected)
					sQuery.addFilterQuery("crisrp." + OrcidService.SYSTEM_ORCID_TOKEN_ACCESS+ ":[* TO *]");
				}
				
				sQuery.setFields("cris-id");
				QueryResponse qResp = searchService.search(sQuery);
				if (qResp.getResults() != null && qResp.getResults().getNumFound() > 0) {
					for (SolrDocument sd : qResp.getResults()) {
						crisID = (String) qResp.getResults().get(0).getFirstValue("cris-id");
						ResearcherPage rp = applicationService.uniqueByCrisID(crisID);
						if (unregisterMode) {
							OrcidPreferencesUtils.unregisterOrcidWebHook(rp);
						}
						else {
							OrcidPreferencesUtils.registerOrcidWebHook(rp);
						}
						applicationService.saveOrUpdate(ResearcherPage.class, rp);
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

}
