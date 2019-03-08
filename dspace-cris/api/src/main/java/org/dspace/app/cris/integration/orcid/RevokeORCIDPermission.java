/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.cris.integration.orcid;

import javax.servlet.http.HttpServletRequest;

import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.orcid.OrcidPreferencesUtils;
import org.dspace.app.cris.util.Researcher;
import org.dspace.authority.orcid.OrcidService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.orcid.jaxb.model.record_v2.Record;

public class RevokeORCIDPermission extends AORCIDWebHookCallbackProcessor {

	@Override
	public boolean processChange(Context context, ResearcherPage rp, String orcid, HttpServletRequest req) {
		OrcidService orcidService = OrcidService.getOrcid();
		String token = OrcidPreferencesUtils.getTokenReleasedForSync(rp, OrcidService.SYSTEM_ORCID_TOKEN_ACCESS);
		if (token == null) {
			// nothing to do, we don't have an access token yet
			return true;
		}
		
		Record record = getORCIDInfo(Record.class, req);
		if (record == null) {
			try {
				record = orcidService.getRecord(orcid, token);
			}
			catch (Exception e) {
				log.info(LogManager.getHeader(context, "revoke_orcid_permission",
						"Invalid token for orcid " + orcid + " got " + e.getMessage()));
				log.debug(e.getMessage(), e);
			}
		}
		
		if (record == null) {
			context.turnOffAuthorisationSystem();
			OrcidPreferencesUtils.setTokens(rp, null);
			log.info(LogManager.getHeader(context, "revoke_orcid_permission",
					"Removing orcid tokens from " + rp.getCrisID()));
			if (!"all".equalsIgnoreCase(ConfigurationManager.getProperty("authentication-oauth", "orcid-webhook"))) {
				OrcidPreferencesUtils.unregisterOrcidWebHook(rp);
			}
			new Researcher().getApplicationService().saveOrUpdate(ResearcherPage.class, rp);
			context.restoreAuthSystemState();
		}
		else {
			setORCIDInfo(record, req);
		}
		return true;
	}

}
