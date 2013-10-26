/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet;

import java.io.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileUploadBase.FileSizeLimitExceededException;
import org.apache.log4j.Logger;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.FileUploadRequest;
import org.dspace.app.itemimport.BTEBatchImportService;
import org.dspace.app.itemimport.ItemImport;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.core.*;
import org.dspace.utils.DSpace;
import org.elasticsearch.common.collect.Lists;

/**
 * Servlet to batch import metadata via the BTE
 *
 * @author Stuart Lewis
 */
public class BatchMetadataImportServlet extends DSpaceServlet
{
    /** log4j category */
    private static Logger log = Logger.getLogger(BatchMetadataImportServlet.class);

    /**
     * Respond to a post request for metadata bulk importing via csv
     *
     * @param context a DSpace Context object
     * @param request the HTTP request
     * @param response the HTTP response
     *
     * @throws ServletException
     * @throws IOException
     * @throws SQLException
     * @throws AuthorizeException
     */
    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        // First, see if we have a multipart request (uploading a metadata file)
        String contentType = request.getContentType();     
        if ((contentType != null) && (contentType.indexOf("multipart/form-data") != -1))
        {
        	String message = null;
        	
        	// Process the file uploaded
        	try {
        		// Wrap multipart request to get the submission info
        		FileUploadRequest wrapper = new FileUploadRequest(request);
        		File f = wrapper.getFile("file");

        		int colId = Integer.parseInt(wrapper.getParameter("collection"));
        		Collection collection = Collection.find(context, colId);
        		
        		String inputType = wrapper.getParameter("inputType");
        		
        		try {
					ItemImport.processUploadableImport(f, new Collection[]{collection}, inputType, context);
					
					request.setAttribute("has-error", "false");
					
				} catch (Exception e) {
					request.setAttribute("has-error", "true");
					message = e.getMessage();
					e.printStackTrace();
				}
        	} catch (FileSizeLimitExceededException e) {
        		request.setAttribute("has-error", "true");
        		message = e.getMessage();
        		e.printStackTrace();
        	} catch (Exception e) {
        		request.setAttribute("has-error", "true");
        		message = e.getMessage();
        		e.printStackTrace();
        	}
        	
        	//Get all the possible data loaders from the Spring configuration
        	BTEBatchImportService dls  = new DSpace().getSingletonService(BTEBatchImportService.class);
        	List<String> inputTypes = Lists.newArrayList(dls.getDataLoaders().keySet());
        	
        	request.setAttribute("input-types", inputTypes);
        	
        	//Get all collections
        	List<Collection> collections = null;
        	String colIdS = request.getParameter("colId");
        	if (colIdS!=null){
        		collections = new ArrayList<Collection>();
        		collections.add(Collection.find(context, Integer.parseInt(colIdS)));
        		
        	}
        	else {
        		collections = Arrays.asList(Collection.findAll(context));
        	}
        	
        	request.setAttribute("collections", collections);
        	
        	request.setAttribute("message", message);
        	
        	// Show the upload screen
    		JSPManager.showJSP(request, response, "/dspace-admin/batchmetadataimport.jsp");

        }
        else
        {
        	request.setAttribute("has-error", "true");
        	
            // Show the upload screen
            JSPManager.showJSP(request, response, "/dspace-admin/batchmetadataimport.jsp");
        }
    }
    
    /**
     * GET request is only ever used to show the upload form
     * 
     * @param context
     *            a DSpace Context object
     * @param request
     *            the HTTP request
     * @param response
     *            the HTTP response
     *
     * @throws ServletException
     * @throws IOException
     * @throws SQLException
     * @throws AuthorizeException
     */
    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
    	//Get all the possible data loaders from the Spring configuration
    	BTEBatchImportService dls  = new DSpace().getSingletonService(BTEBatchImportService.class);
    	List<String> inputTypes = Lists.newArrayList(dls.getDataLoaders().keySet());
    	
    	request.setAttribute("input-types", inputTypes);
    	
    	//Get all collections
    	List<Collection> collections = null;
    	String colIdS = request.getParameter("colId");
    	if (colIdS!=null){
    		collections = new ArrayList<Collection>();
    		collections.add(Collection.find(context, Integer.parseInt(colIdS)));
    		
    	}
    	else {
    		collections = Arrays.asList(Collection.findAll(context));
    	}
    	
    	request.setAttribute("collections", collections);
    	
        // Show the upload screen
        JSPManager.showJSP(request, response, "/dspace-admin/batchmetadataimport.jsp");
    }

}