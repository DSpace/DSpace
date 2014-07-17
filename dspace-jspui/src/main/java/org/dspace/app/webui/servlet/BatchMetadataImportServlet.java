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
import javax.servlet.jsp.jstl.fmt.LocaleSupport;

import org.apache.commons.fileupload.FileUploadBase.FileSizeLimitExceededException;
import org.apache.commons.lang3.StringUtils;
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
    	
    	int type = -1;

    	String typeS = request.getParameter("type");
    	if (typeS != null){
    		try {
    			type = Integer.parseInt(typeS);
    		} catch (NumberFormatException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
    	}

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
		
    	if (type != 0){
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
    			List<String> inputTypes =dls.getFileDataLoaders();

    			request.setAttribute("input-types", inputTypes);

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
    	else {
    		request.setAttribute("type", type);
    		
    		String message = null;
    		
    		String zipurl = request.getParameter("zipurl");
    		if (StringUtils.isEmpty(zipurl)) {
    			request.setAttribute("has-error", "true");
    		}
    		else {
    			
    			Collection owningCollection = null;
    			if (request.getParameter("collection") != null) {
    				int colId = Integer.parseInt(request.getParameter("collection"));
    				if (colId > 0)
    					owningCollection = Collection.find(context, colId);
    			}
				
    			String[] reqCollections = request.getParameterValues("collections");
				
    			if (owningCollection==null && reqCollections.length > 0){
    				request.setAttribute("has-error", "true");
    				
    				Locale locale = request.getLocale();
    		        ResourceBundle msgs = ResourceBundle.getBundle("Messages", locale);
    		        String ms = msgs.getString("jsp.layout.navbar-admin.batchimport.owningcollection");
    		        if (ms == null){
    		        	ms = "???jsp.layout.navbar-admin.batchimport.owningcollection???";
    		        }
    				request.setAttribute("message", ms);
        		
    				JSPManager.showJSP(request, response, "/dspace-admin/batchimport.jsp");
    				
    				return;
    			}
    			
    			List<Collection> collectionList = new ArrayList<Collection>();
    			if (reqCollections != null){
    				for (String colID : reqCollections){
    					int colId = Integer.parseInt(colID);
    					if (colId != owningCollection.getID()){
    						Collection col = Collection.find(context, colId);
    						if (col != null){
    							collectionList.add(col);
    						}
    					}
    				}
    			}
    			Collection[] otherCollections = collectionList.toArray(new Collection[collectionList.size()]);
				
				try {
					
				ItemImport.processUploadableImport(zipurl, owningCollection, otherCollections, context);
					
					request.setAttribute("has-error", "false");
					
				} catch (Exception e) {
					request.setAttribute("has-error", "true");
    				message = e.getMessage();
    				e.printStackTrace();
				}
    		}
    		
    		request.setAttribute("message", message);
    		
    		JSPManager.showJSP(request, response, "/dspace-admin/batchimport.jsp");
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
    	
    	String typeS = request.getParameter("type");
    	
    	int type = -1;
    	try {
			type = Integer.parseInt(typeS);
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	request.setAttribute("type", type);
    	
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
		
    	if (type==0){
    		// Show the upload screen
    		
    		JSPManager.showJSP(request, response, "/dspace-admin/batchimport.jsp");
    	}
    	else {
    		//Get all the possible data loaders from the Spring configuration
    		BTEBatchImportService dls  = new DSpace().getSingletonService(BTEBatchImportService.class);
    		List<String> inputTypes = dls.getFileDataLoaders();
    		request.setAttribute("input-types", inputTypes);

    		// Show the upload screen
    		JSPManager.showJSP(request, response, "/dspace-admin/batchmetadataimport.jsp");
    	}
    }

}