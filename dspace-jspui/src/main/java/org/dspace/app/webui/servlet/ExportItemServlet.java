/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.export.api.ExportItemException;
import org.dspace.export.api.ExportItemProvider;
import org.dspace.export.api.ExportItemService;
import org.dspace.export.impl.ExportItemManager;
import org.dspace.handle.HandleManager;

/**
 * Servlet for exporting items.
 * 
 * @author João Melo <jmelo@lyncode.com>
 * @version $Revision$
 */
public class ExportItemServlet extends DSpaceServlet
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/** log4j category */
    private static Logger log = Logger.getLogger(ExportItemServlet.class);
    
    private ExportItemService exportService;

    @Override
	public void init(ServletConfig arg0) throws ServletException {
		super.init(arg0);
		
		exportService = new ExportItemManager();
	}

    @Override
	protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException
    {
    	Item item = null;
    	
        // Get the ID from the URL
        String idString = request.getPathInfo();
        String handle = "";
        String exportProviderId = "";
        

        if (idString == null)
        {
            idString = "";
        }
        
        // Parse 'handle' and 'providerId' which is typically of the format:
        // {handle}/{providerId}
        
        // Remove leading slash if any:
        if (idString.startsWith("/"))
        {
            idString = idString.substring(1);
        }

        // skip first slash within handle
        int slashIndex = idString.indexOf('/');
        if (slashIndex != -1)
        {
            slashIndex = idString.indexOf('/', slashIndex + 1);
            if (slashIndex != -1)
            {
                handle = idString.substring(0, slashIndex);
                exportProviderId = idString.substring(slashIndex + 1);
            }
        }
        
        // Now try and retrieve the item
        DSpaceObject dso;
		try {
			dso = HandleManager.resolveToObject(context, handle);
	        // Make sure we have valid item and export provider number
	        if (dso != null && dso.getType() == Constants.ITEM && exportProviderId != null)
	        {
	            item = (Item) dso;
	            ExportItemProvider provider = exportService.getProvider(exportProviderId);
	            if (provider != null) {
	            	response.setHeader("Content-Disposition", "attachment; filename=" + handle.replace('/', '_') + "." + provider.getFileExtension());
	                response.setContentType(provider.getContentType());
		            ByteArrayOutputStream out = new ByteArrayOutputStream();
		            provider.export(item, out);
		            response.setContentLength(out.size());
		            OutputStream output = response.getOutputStream();
		            output.write(out.toByteArray());
		            output.flush();
		            output.close();
	            } else throw new ExportItemException("Unknown Export Provider");
	        } else throw new ExportItemException("Invalid Item or undefined Export Provider");
		} catch (IllegalStateException e) {
			throw new ServletException(e);
		} catch (SQLException e) {
			throw new ServletException(e);
		} catch (ExportItemException e) {
			log.error(e.getMessage(), e);
			throw new ServletException(e);
		}
    }
}
