/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.oltu.oauth2.common.utils.OAuthUtils;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.jdyna.RPProperty;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.authenticate.ExtraLoggedInAction;
import org.dspace.authenticate.PostLoggedInAction;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

public class ResearcherClaimOrcidProfile implements ExtraLoggedInAction {

	private ApplicationService applicationService;

	public void loggedIn(Context context, HttpServletRequest request,
			EPerson eperson) {
		String orcid = (String) request.getAttribute("orcid");
		String scope = (String) request.getAttribute("scope");
		if(StringUtils.isNotBlank(orcid) && StringUtils.isNotBlank(scope)) {
			fromOrcidToResearcherPage(request, orcid, scope, applicationService.getResearcherByAuthorityKey(context.getCrisID()));
		}
	}
	
	public void fromOrcidToResearcherPage(HttpServletRequest request, String orcid, String scope, ResearcherPage rp) {
		if (rp == null) return;
		// delete orcid from researcher profile
		List<RPProperty> rpp = rp.getAnagrafica4view().get("orcid");
		if (rpp != null && !rpp.isEmpty()) {
			for (RPProperty rppp : rpp) {
				rp.removeProprieta(rppp);						
			}
		}

		// setup token
		String token = (String) request.getAttribute("access_token");
		String scopeMetadata = ConfigurationManager.getProperty("authentication-oauth",
				"application-client-scope");
		if (StringUtils.isNotBlank(scopeMetadata) && StringUtils.isNotBlank(token)) {
			for (String scopeConfigurated : OAuthUtils.decodeScopes(scopeMetadata)) {
				// clear all token
				List<RPProperty> rppp = rp.getAnagrafica4view()
						.get("system-orcid-token" + scopeConfigurated.replace("/", "-"));
				if (rppp != null && !rppp.isEmpty()) {
					for (RPProperty rpppp : rppp) {
						rp.removeProprieta(rpppp);
					}
				}
			}
		}

		applicationService.saveOrUpdate(ResearcherPage.class, rp);
		
		// rebuild token
		if (StringUtils.isNotBlank(scope) && StringUtils.isNotBlank(token)) {
			for (String scopeConfigurated : OAuthUtils.decodeScopes(scope)) {
				ResearcherPageUtils.buildTextValue(rp, token,
						"system-orcid-token" + scopeConfigurated.replace("/", "-"));
			}
		}

		// rebuild orcid
		ResearcherPageUtils.buildTextValue(rp, orcid, "orcid");

		applicationService.saveOrUpdate(ResearcherPage.class, rp);
	}

	public void setApplicationService(ApplicationService applicationService) {
		this.applicationService = applicationService;
	}

}
