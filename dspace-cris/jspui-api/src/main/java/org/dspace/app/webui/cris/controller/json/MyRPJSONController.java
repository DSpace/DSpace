/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.controller.json;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import flexjson.JSONSerializer;
import it.cilea.osd.jdyna.model.AccessLevelConstants;
import it.cilea.osd.jdyna.value.TextValue;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.RestrictedField;
import org.dspace.app.cris.model.VisibilityConstants;
import org.dspace.app.cris.model.jdyna.RPPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.RPProperty;
import org.dspace.app.cris.model.jdyna.VisibilityTabConstant;
import org.dspace.app.cris.model.orcid.OrcidPreferencesUtils;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.content.Metadatum;
import org.dspace.core.Context;
import org.dspace.core.ConfigurationManager;
import org.dspace.eperson.EPerson;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;

/**
 * Retrieve data on the researcher profile of the logged user to be used in a
 * "My Researcher Page"
 * 
 * @author cilea
 * 
 */
public class MyRPJSONController extends MultiActionController
{
    /**
     * the applicationService for query the RP db, injected by Spring IoC
     */
    private ApplicationService applicationService;

    public void setApplicationService(ApplicationService applicationService)
    {
        this.applicationService = applicationService;
    }

    public ModelAndView create(HttpServletRequest request,
            HttpServletResponse response) throws Exception
    {
        // authorization is checked by the getMyRP method
        ResearcherPage rp = getMyResearcherPage(request);
        if (rp == null)
        {
            rp = new ResearcherPage();
            Boolean newRpStatus = ConfigurationManager.getBooleanProperty("cris","rp.profile.new.status", false);
            rp.setStatus(newRpStatus);

            EPerson currentUser = getCurrentUser(request);
			rp.setEpersonID(currentUser.getID());
            
            Metadatum[] md = currentUser.getMetadata("eperson", "orcid", null, null);
            if (md != null && md.length > 0) {
            	Metadatum[] mdToken = currentUser.getMetadata("eperson", "orcid", "accesstoken", null);
            	String token = null;
            	if (mdToken != null && mdToken.length > 0) {
            		token = mdToken[0].value;
            	}
            	String orcid = md[0].value;
				boolean orcidPopulated = OrcidPreferencesUtils.populateRP(rp, orcid, token);
            	if (!orcidPopulated && token != null) {
            		orcidPopulated = OrcidPreferencesUtils.populateRP(rp, orcid);
            	};
            	
            	if (orcidPopulated) {
            		rp.setSourceRef("orcid");
            		rp.setSourceID(orcid);
            	}
            }
            
            RPPropertiesDefinition fN = applicationService
                    .findPropertiesDefinitionByShortName(
                            RPPropertiesDefinition.class, "fullName");
            
            List<RPProperty> proprietaDellaTipologia = rp.getProprietaDellaTipologia(fN);
			if (proprietaDellaTipologia == null || proprietaDellaTipologia.size() == 0) {
	            TextValue val = new TextValue();
	            val.setOggetto(currentUser.getFullName());
	            RPProperty prop = rp.createProprieta(fN);
	            prop.setValue(val);
	            prop.setVisibility(VisibilityConstants.PUBLIC);
			}
            
            RPPropertiesDefinition email = applicationService
                    .findPropertiesDefinitionByShortName(
                            RPPropertiesDefinition.class, "email");
            
            proprietaDellaTipologia = rp.getProprietaDellaTipologia(email);
			if (proprietaDellaTipologia == null || proprietaDellaTipologia.size() == 0) {
	            TextValue valE = new TextValue();
	            valE.setOggetto(currentUser.getEmail());
	            RPProperty propE = rp.createProprieta(email);
	            propE.setValue(valE);
	            propE.setVisibility(VisibilityConstants.HIDE);
			}
            applicationService.saveOrUpdate(ResearcherPage.class, rp);
        }
        returnStatusJSON(response, rp);
        return null;
    }

    private void returnStatusJSON(HttpServletResponse response,
            ResearcherPage rp) throws IOException
    {
        RPStatusInformation info = new RPStatusInformation();
        if (rp != null)
        {
            info.setActive(rp.getStatus() != null ? rp.getStatus() : false);
            info.setUrl("/cris/" + rp.getPublicPath() + "/"
                    + ResearcherPageUtils.getPersistentIdentifier(rp));
        }
        JSONSerializer serializer = new JSONSerializer();
        serializer.rootName("myrp");
        serializer.exclude("class");
        response.setContentType("application/json");
        serializer.deepSerialize(info, response.getWriter());
    }

    private ResearcherPage getMyResearcherPage(HttpServletRequest request)
            throws SQLException, ServletException
    {
        EPerson currUser = getCurrentUser(request);
        if (currUser == null)
        {
            throw new ServletException(
                    "Wrong data or configuration: access to the my rp servlet without a valid user: there is no user logged in");
        }

        int id = currUser.getID();
        ResearcherPage rp = applicationService.getResearcherPageByEPersonId(id);
        return rp;
    }

    private EPerson getCurrentUser(HttpServletRequest request)
            throws SQLException
    {
        Context context = UIUtil.obtainContext(request);
        EPerson currUser = context.getCurrentUser();
        return currUser;
    }

    public ModelAndView activate(HttpServletRequest request,
            HttpServletResponse response) throws Exception
    {
        ResearcherPage rp = getMyResearcherPage(request);
        if (rp.getStatus() == null || rp.getStatus() == false)
        {
            rp.setStatus(true);
            applicationService.saveOrUpdate(ResearcherPage.class, rp);
        }

        returnStatusJSON(response, rp);
        return null;
    }

    public ModelAndView hide(HttpServletRequest request,
            HttpServletResponse response) throws Exception
    {
        ResearcherPage rp = getMyResearcherPage(request);
        if (rp.getStatus() == null || rp.getStatus() == true)
        {
            rp.setStatus(false);
            applicationService.saveOrUpdate(ResearcherPage.class, rp);
        }

        returnStatusJSON(response, rp);
        return null;
    }

    public ModelAndView remove(HttpServletRequest request,
            HttpServletResponse response) throws Exception
    {
        ResearcherPage rp = getMyResearcherPage(request);
        if (rp != null)
        {
            applicationService.delete(ResearcherPage.class, rp.getId());
        }
        returnStatusJSON(response, null);
        return null;
    }

    public ModelAndView status(HttpServletRequest request,
            HttpServletResponse response) throws Exception
    {
        ResearcherPage rp = getMyResearcherPage(request);
        returnStatusJSON(response, rp);
        return null;
    }

    class RPStatusInformation
    {
        private boolean active;

        private String url;

        public boolean isActive()
        {
            return active;
        }

        public void setActive(boolean active)
        {
            this.active = active;
        }

        public String getUrl()
        {
            return url;
        }

        public void setUrl(String url)
        {
            this.url = url;
        }
    }
}
