/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.sherpa.SHERPAJournal;
import org.dspace.app.sherpa.SHERPAPublisher;
import org.dspace.app.sherpa.SHERPAResponse;
import org.dspace.app.sherpa.submit.SHERPASubmitService;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * This servlet uses the SHERPASubmitService to build an html page with the
 * publisher policy for the journal referred in the specified Item
 * 
 * @author Andrea Bollini
 * 
 */
public class SHERPAPublisherPolicyServlet extends DSpaceServlet
{
    private final transient SHERPASubmitService sherpaSubmitService 
            = DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName(
                    SHERPASubmitService.class.getCanonicalName(),
                    SHERPASubmitService.class);

    private final transient ItemService itemService
             = ContentServiceFactory.getInstance().getItemService();
    
    /** log4j logger */
    private static final Logger log = Logger
            .getLogger(SHERPAPublisherPolicyServlet.class);
    
    @Override
    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        UUID itemID = UIUtil.getUUIDParameter(request, "item_id");
        Item item = itemService.find(context, itemID);
        if (item == null)
        {
            return;
        }
        SHERPAResponse shresp = sherpaSubmitService.searchRelatedJournals(
                context, item);
        if (shresp.isError())
        {
            request.setAttribute("error", Boolean.TRUE);
        }
        else
        {
            List<SHERPAJournal> journals = shresp.getJournals();
            if (journals != null)
            {
                Object[][] results = new Object[journals.size()][];
                if (journals.size() > 0)
                {
                    Iterator<SHERPAJournal> ijourn = journals.iterator();
                    int idx = 0;
                    while (ijourn.hasNext())
                    {
                        SHERPAJournal journ = ijourn.next();
                        List<SHERPAPublisher> publishers = shresp
                                .getPublishers();
                        results[idx] = new Object[] {
                                journ,
                                publishers != null && publishers.size() > 0 ? publishers
                                        .get(0) : null };
                        idx++;
                    }
                }

                request.setAttribute("result", results);
            }
        }
        // Simply forward to the plain form
        JSPManager.showJSP(request, response, "/sherpa/sherpa-policy.jsp");
    }

    @Override
    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }
}
