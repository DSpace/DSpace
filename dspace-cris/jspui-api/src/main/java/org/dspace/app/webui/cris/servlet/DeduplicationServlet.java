/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.servlet;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.cris.deduplication.utils.DedupUtils;
import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.utils.DSpace;

public class DeduplicationServlet extends DSpaceServlet
{
    private static Logger log = Logger.getLogger(DeduplicationServlet.class);
    
    DSpace dspace = new DSpace();
    @Override
    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        DedupUtils dedupUtils = dspace.getServiceManager().getServiceByName("dedupUtils", DedupUtils.class);
        
        String action = request.getPathInfo();

        if (action == null)
        {
            action = "";
        }

        if (action.startsWith("/"))
        {
            action = action.substring(1);
        }

        int typeId = UIUtil.getIntParameter(request, "type");        
        Boolean check = UIUtil.getBoolParameter(request, "dcheck");
        
        if (action.equals("reject"))
        {
            int firstId = UIUtil.getIntParameter(request, "itemID");
            int secondId = UIUtil.getIntParameter(request, "duplicateID");
            boolean fake = UIUtil.getBoolParameter(request, "fake");
            String note = null;
            if (!fake)
            {
                note = request.getParameter("note");
            }
            dedupUtils.rejectDups(context, firstId, secondId, typeId, fake, note, check);
            context.complete();
            return;
        }

        if (action.equals("verify"))
        {
            int dedupId = UIUtil.getIntParameter(request, "dedupID");
            int firstId = UIUtil.getIntParameter(request, "itemID");
            int secondId = UIUtil.getIntParameter(request, "duplicateID");
            boolean toFix = UIUtil.getBoolParameter(request, "toFix");
            String note = request.getParameter("note");
            dedupUtils.verify(context, dedupId, firstId, secondId, typeId, toFix, note, check);
            context.complete();
            return;
        }
    }
}
