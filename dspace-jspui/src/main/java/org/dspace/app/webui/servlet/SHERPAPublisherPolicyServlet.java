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
import org.dspace.core.Context;
import org.dspace.utils.DSpace;

/**
 * This servlet use the SHERPASubmitService to build an html page with the
 * publisher policy for the journal referred in the specified Item
 * 
 * @author Andrea Bollini
 * 
 */
public class SHERPAPublisherPolicyServlet extends DSpaceServlet
{
    private SHERPASubmitService sherpaSubmitService = new DSpace()
            .getServiceManager().getServiceByName(
                    SHERPASubmitService.class.getCanonicalName(),
                    SHERPASubmitService.class);

    /** log4j logger */
    private static Logger log = Logger
            .getLogger(SHERPAPublisherPolicyServlet.class);

    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        int itemID = UIUtil.getIntParameter(request, "item_id");
        Item item = Item.find(context, itemID);
        if (item == null)
        {
            return;
        }
        SHERPAResponse shresp = sherpaSubmitService.searchRelatedJournals(
                context, item);
        if (shresp.isError())
        {
            request.setAttribute("error", new Boolean(true));
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

    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }
}
