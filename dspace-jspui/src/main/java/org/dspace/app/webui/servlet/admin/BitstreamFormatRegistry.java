/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet.admin;

import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.core.Context;

/**
 * Servlet for editing the bitstream format registry
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class BitstreamFormatRegistry extends DSpaceServlet
{
    /** User wants to edit a format */
    public static final int START_EDIT = 1;

    /** User wants to delete a format */
    public static final int START_DELETE = 2;

    /** User confirms edit of a format */
    public static final int CONFIRM_EDIT = 3;

    /** User confirms delete of a format */
    public static final int CONFIRM_DELETE = 4;

    /** User wants to create a new format */
    public static final int CREATE = 4;

    private final transient BitstreamFormatService bitstreamFormatService
             = ContentServiceFactory.getInstance().getBitstreamFormatService();
    
    @Override
    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        // GET just displays the list of formats
        showFormats(context, request, response);
    }

    @Override
    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        String button = UIUtil.getSubmitButton(request, "submit");

        if (button.equals("submit_update"))
        {
            // Update the metadata for a bitstream format
            BitstreamFormat bf = bitstreamFormatService.find(context, UIUtil
                    .getIntParameter(request, "format_id"));

            bf.setMIMEType(request.getParameter("mimetype"));
            bf.setShortDescription(context, request.getParameter("short_description"));
            bf.setDescription(request.getParameter("description"));
            bf
                    .setSupportLevel(UIUtil.getIntParameter(request,
                            "support_level"));
            bf.setInternal((request.getParameter("internal") != null)
                    && request.getParameter("internal").equals("true"));

            // Separate comma-separated extensions
            List<String> extensions = new LinkedList<>();
            String extParam = request.getParameter("extensions");

            while (extParam.length() > 0)
            {
                int c = extParam.indexOf(',');

                if (c > 0)
                {
                    extensions.add(extParam.substring(0, c).trim());
                    extParam = extParam.substring(c + 1).trim();
                }
                else
                {
                    if (extParam.trim().length() > 0)
                    {
                        extensions.add(extParam.trim());
                        extParam = "";
                    }
                }
            }

            bf.setExtensions(extensions);

            bitstreamFormatService.update(context, bf);

            showFormats(context, request, response);
            context.complete();
        }
        else if (button.equals("submit_add"))
        {
            // Add a new bitstream - simply add to the list, and let the user
            // edit with the main form
            BitstreamFormat bf = bitstreamFormatService.create(context);

            // We set the "internal" flag to true, so that the empty bitstream
            // format doesn't show up in the submission UI yet
            bf.setInternal(true);
            bitstreamFormatService.update(context, bf);

            showFormats(context, request, response);
            context.complete();
        }
        else if (button.equals("submit_delete"))
        {
            // Start delete process - go through verification step
            BitstreamFormat bf = bitstreamFormatService.find(context, UIUtil
                    .getIntParameter(request, "format_id"));
            request.setAttribute("format", bf);
            JSPManager.showJSP(request, response,
                    "/dspace-admin/confirm-delete-format.jsp");
        }
        else if (button.equals("submit_confirm_delete"))
        {
            // User confirms deletion of format
            BitstreamFormat bf = bitstreamFormatService.find(context, UIUtil
                    .getIntParameter(request, "format_id"));
            bitstreamFormatService.delete(context, bf);

            showFormats(context, request, response);
            context.complete();
        }
        else
        {
            // Cancel etc. pressed - show list again
            showFormats(context, request, response);
        }
    }

    /**
     * Show list of bitstream formats
     * 
     * @param context
     *            Current DSpace context
     * @param request
     *            Current HTTP request
     * @param response
     *            Current HTTP response
     */
    private void showFormats(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        List<BitstreamFormat> formats = bitstreamFormatService.findAll(context);

        request.setAttribute("formats", formats);
        JSPManager.showJSP(request, response, "/dspace-admin/list-formats.jsp");
    }
}
