/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.servlet;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.jdyna.EditTabResearcherPage;
import org.dspace.app.cris.model.orcid.OrcidPreferencesUtils;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.webui.cris.util.CrisAuthorizeManager;
import org.dspace.app.webui.json.JSONRequest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.utils.DSpace;

import com.google.gson.JsonObject;

public class JSONOrcidServlet extends JSONRequest
{

    private Logger log = Logger.getLogger(JSONOrcidServlet.class);

    @Override
    public void doJSONRequest(Context context, HttpServletRequest request,
            HttpServletResponse response) throws AuthorizeException, IOException
    {

        int status = 0;
        String message = "";
        
        String crisID = request.getParameter("crisid");
        EPerson currUser = context.getCurrentUser();

        if (request.getPathInfo().contains("disconnect"))
        {
            try
            {
                if (currUser != null)
                {
                    ApplicationService applicationservice = new DSpace()
                            .getServiceManager()
                            .getServiceByName("applicationService",
                                    ApplicationService.class);

                    ResearcherPage r = applicationservice
                            .getEntityByCrisId(crisID, ResearcherPage.class);
                    if (CrisAuthorizeManager.isAdmin(context, r) || CrisAuthorizeManager.canEdit(context, applicationservice, EditTabResearcherPage.class, r)
                            || (r.getEpersonID() != null && r.getEpersonID()
                                    .equals(currUser.getID())))
                    {
                        OrcidPreferencesUtils
                                .disconnectOrcidbyResearcherPage(context, r);
                        applicationservice.saveOrUpdate(ResearcherPage.class, r);
                    }
                    else
                    {
                        status = -1;
                    }
                }
            }
            catch (Exception e)
            {
                status = -1;
                log.error(e.getMessage(), e);
            }
        }
        JsonObject jo = new JsonObject();
        jo.addProperty("result", status);
        jo.addProperty("message", message);
        response.getWriter().write(jo.toString());
    }
    
}
