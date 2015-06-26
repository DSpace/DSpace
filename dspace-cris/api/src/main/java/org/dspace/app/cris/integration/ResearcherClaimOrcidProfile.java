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

public class ResearcherClaimOrcidProfile implements PostLoggedInAction, ExtraLoggedInAction {

	private ApplicationService applicationService;

	private String netidSourceRef;

	@Override
	public void loggedIn(Context context, HttpServletRequest request, EPerson eperson) {
		try {
			String orcid = (String) request.getAttribute("orcid");
			String scope = (String) request.getAttribute("scope");
			
			if (StringUtils.isNotBlank(orcid)) {
				ResearcherPage rp = applicationService.getResearcherPageByEPersonId(eperson.getID());

				// check Researcher Profile existence
				if (rp == null && eperson.getNetid() != null) {
					rp = applicationService.getEntityBySourceId(netidSourceRef.toUpperCase(), eperson.getNetid(),
							ResearcherPage.class);
					if (rp == null) {
						// build a simple RP
						rp = new ResearcherPage();

						// NOTE: usually the netid is the orcid
						rp.setSourceID(eperson.getNetid());
						rp.setSourceRef(netidSourceRef);

						ResearcherPageUtils.buildTextValue(rp, eperson.getFullName(), "fullName");
						ResearcherPageUtils.buildTextValue(rp, eperson.getEmail(), "email");

					}
				}
				
				fromOrcidToResearcherPage(request, orcid, scope, rp);
			}
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}

	}

	public void extraLoggedIn(Context context, HttpServletRequest request,
			EPerson eperson) {
		String orcid = (String) request.getAttribute("orcid");
		String scope = (String) request.getAttribute("scope");
		if(StringUtils.isNotBlank(orcid) && StringUtils.isNotBlank(scope)) {
			fromOrcidToResearcherPage(request, orcid, scope, applicationService.getResearcherByAuthorityKey(context.getCrisID()));
		}
	}
	
	public void fromOrcidToResearcherPage(HttpServletRequest request, String orcid, String scope, ResearcherPage rp) {
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
			for (String scopeConfigurated : OAuthUtils.decodeScopes(scope)) {
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
		if (StringUtils.isNotBlank(scopeMetadata) && StringUtils.isNotBlank(token)) {
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

	public void setNetidSourceRef(String netidSourceRef) {
		this.netidSourceRef = netidSourceRef;
	}
}
