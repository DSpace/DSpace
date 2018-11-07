/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.batch;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

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
import org.dspace.app.cris.model.jdyna.RPPropertiesDefinition;
import org.dspace.app.cris.model.orcid.OrcidPreferencesUtils;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.service.RelationPreferenceService;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.authority.orcid.OrcidService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.discovery.SearchService;
import org.dspace.utils.DSpace;

public class ScriptImportUpdateFromOrcid {

	private static final DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");

	/** log4j logger */
	private static Logger log = Logger.getLogger(ScriptImportUpdateFromOrcid.class);

	private static boolean limitAuthz = false;
	
	private static int olderThan = 0;
	
	private static String orcidParam;
	
	private static String crisID;
	
	private static List<String> propsToSkip;
	
	private static List<String> propsToReplace;
	
	private static boolean singleMode = false;
	
	/**
	 * Batch script to push data to Orcid. Try with -h to see more helps.
	 */
	public static void main(String[] args) throws ParseException {

		log.info("#### START Script import/update from ORCID to researcher page: -----" + new Date() + " ----- ####");
		Context context = null;
		try {
			DSpace dspace = new DSpace();
			context = new Context();
			context.turnOffAuthorisationSystem();
			SearchService searchService = dspace.getSingletonService(SearchService.class);
			RelationPreferenceService relationPreferenceService = dspace.getServiceManager().getServiceByName(
					"org.dspace.app.cris.service.RelationPreferenceService", RelationPreferenceService.class);
			ApplicationService applicationService = dspace.getServiceManager().getServiceByName("applicationService",
					ApplicationService.class);

			CommandLineParser parser = new PosixParser();

			Options options = new Options();
			options.addOption("h", "help", false, "help");
			options.addOption("c", "check_credentials", false, "Check client credentials");
			options.addOption("z", "only_authz", false, "It works only on researchers pages with a valid ORCID token");
			options.addOption("s", "single_researcher", true, "It works on single researcher using the ORCID in the RP or the one specified with the -o param");
			options.addOption("o", "orcid", true,
					"It imports the specified ORCID in a new RP or update the existent RP (if any) linked with the specified ORCID. If the -s option is present the script will fail in case of conflict");
			options.addOption("d", "older_days", true, "It works on researchers not updated in the latest X days");
			options.addOption("p", "overwrite", true, "list of rp properties to override with values from ORCID (default ignore properties already filled in DSpace-CRIS)");
			options.addOption("x", "skip", true, "list of rp properties to exclude from the import (default import everything)");
			
			CommandLine line = parser.parse(options, args);

			if (line.hasOption('h')) {
				HelpFormatter myhelp = new HelpFormatter();
				myhelp.printHelp("ScriptImportUpdateFromOrcid \n", options);
				System.out.println(
						"\n\nUSAGE:\n ScriptImportUpdateFromOrcid [-z] [-d <days>] -s <researcher_identifier> [-o <ORCID>] [-p <prop1> -p <prop2> ... -p <propN>] [-x <prop1> -x <prop2> ... -x <propN>] - with no options it works on all the RP with an ORCID \n");

				System.exit(0);
			}
			if(line.hasOption('c')) {
			    OrcidService orcidService = OrcidService.getOrcid();
			    try {
	                orcidService.search("test", 1, 1);
	                System.out.println("OK!");
	            }
	            catch(Exception ex) {                
	                System.out.println("ERROR MESSAGE:" + ex.getMessage());                
	                System.out.println("FAILED!");
	                System.exit(1);
	            }
			    System.exit(0);
			}
			
			if (line.hasOption('z')) {
				limitAuthz = true;
			}

            if (line.hasOption('d')) {
                olderThan = Integer.parseInt(line.getOptionValue('d'));
            }
            
            if (line.hasOption('s') || line.hasOption('o')) {
				singleMode = true;
				crisID = line.getOptionValue('s');
				orcidParam = line.getOptionValue('o');
			}
            
            if (line.getOptionValues('p') != null) {
            	propsToReplace = new LinkedList<String>();
            	for (String pp : line.getOptionValues('p')) {
					if (!"*".equals(pp) && applicationService
							.findPropertiesDefinitionByShortName(RPPropertiesDefinition.class, pp) == null) {
            			System.out.println("The property "+pp+ " doesn't exist");
    	                System.exit(1);	
            		}
            		propsToReplace.add(pp);
            	}
            }
            
			if (line.getOptionValues('x') != null) {
				propsToSkip = new LinkedList<String>();
				for (String xx : line.getOptionValues('x')) {
					if (applicationService.findPropertiesDefinitionByShortName(RPPropertiesDefinition.class, xx) == null) {
            			System.out.println("The property "+xx+ " doesn't exist");
    	                System.exit(1);	
            		}
					propsToSkip.add(xx);
				}
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
							String crisID = (String) qResp.getResults().get(0).getFirstValue("cris-id");
							rp = applicationService.uniqueByCrisID(crisID);		
						}
					}
				}
				if (rp == null) {
		            rp = new ResearcherPage();
		            Boolean newRpStatus = ConfigurationManager.getBooleanProperty("cris","rp.profile.new.status", false);
		            rp.setStatus(newRpStatus);
		            boolean orcidPopulated = OrcidPreferencesUtils.populateRP(rp, orcidParam, null, propsToSkip, propsToReplace);
            	
	            	if (orcidPopulated) {
	            		rp.setSourceRef("orcid");
	            		rp.setSourceID(orcidParam);
	            		applicationService.saveOrUpdate(ResearcherPage.class, rp);
	            		System.out.println("RP " + rp.getCrisID() + " successful created from the ORCID: " + orcidParam);
	            	}
	            	else {
	            		System.out.println("Creation of an RP from the ORCID: " + orcidParam + " Failed!");
		                System.exit(1);
	            	}
				}
				else {
					String token = OrcidPreferencesUtils.getTokenReleasedForSync(rp, OrcidService.SYSTEM_ORCID_TOKEN_READ_LIMITED_SCOPE);
					orcidParam = ResearcherPageUtils.getStringValue(rp, "orcid");
					boolean orcidPopulated = OrcidPreferencesUtils.populateRP(rp, orcidParam, token, propsToSkip, propsToReplace);
	            	if (orcidPopulated) {
	            		applicationService.saveOrUpdate(ResearcherPage.class, rp);
	            		System.out.println("RP " + rp.getCrisID() + " successful updated using the ORCID: " + orcidParam);
	            	}
	            	else {
	            		System.out.println("Update of the RP " + rp.getCrisID() + " from the ORCID: " + orcidParam + " Failed!");
		                System.exit(1);
	            	}
				}
			}
			else {
				// work on multiple researchers
				SolrQuery sQuery = new SolrQuery("crisrp.orcid:[* TO *]");
				sQuery.addFilterQuery("search.resourcetype:"+CrisConstants.RP_TYPE_ID);
				if (limitAuthz) {
					sQuery.addFilterQuery("crisrp." + OrcidService.SYSTEM_ORCID_TOKEN_READ_LIMITED_SCOPE + ":[* TO *]");
				}
				if (olderThan > 0) {
					sQuery.addFilterQuery("time_lastmodified_dt:[* TO NOW-"+olderThan+"DAYS/DAY]");
				}
				sQuery.setFields("cris-id");
				QueryResponse qResp = searchService.search(sQuery);
				if (qResp.getResults() != null && qResp.getResults().getNumFound() > 0) {
					boolean errors = false;
					int success = 0;
					int fail = 0;
					for (SolrDocument sd : qResp.getResults()) {
						String crisID = (String) qResp.getResults().get(0).getFirstValue("cris-id");
						ResearcherPage rp = applicationService.uniqueByCrisID(crisID);
						String token = OrcidPreferencesUtils.getTokenReleasedForSync(rp, OrcidService.SYSTEM_ORCID_TOKEN_READ_LIMITED_SCOPE);
						String orcidRP = ResearcherPageUtils.getStringValue(rp, "orcid");
						boolean orcidPopulated = OrcidPreferencesUtils.populateRP(rp,
								orcidRP, token, propsToSkip, propsToReplace);
						if (orcidPopulated) {
							applicationService.saveOrUpdate(ResearcherPage.class, rp);
							System.out
									.println("RP " + rp.getCrisID() + " successful updated using the ORCID: " + orcidRP);
		            		success++;
		            	}
		            	else {
		            		System.err.println("Update of the RP " + rp.getCrisID() + " from the ORCID: " + orcidRP + " Failed!");
			                errors = true;
			                fail++;
		            	}
						applicationService.evict(rp);
					}
					System.out.println("Successes: "+ success);
					System.out.println("Failures: "+ fail);
					if (errors) {
						System.err.println("Some update fails!");
						System.exit(1);
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
