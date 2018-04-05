/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileUploadBase.FileSizeLimitExceededException;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.itemimport.BTEBatchImportService;
import org.dspace.app.itemimport.factory.ItemImportServiceFactory;
import org.dspace.app.itemimport.service.ItemImportService;
import org.dspace.app.webui.util.FileUploadRequest;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Context;
import org.dspace.utils.DSpace;

/**
 * Servlet to batch import metadata via the BTE
 *
 * @author Stuart Lewis
 */
public class BatchImportServlet extends DSpaceServlet
{
    /** log4j category */
    private static final Logger log = Logger.getLogger(BatchImportServlet.class);
    
    private final transient CollectionService collectionService
             = ContentServiceFactory.getInstance().getCollectionService();

    private final transient ItemImportService itemImportService
             = ItemImportServiceFactory.getInstance().getItemImportService();
    
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
    @Override
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

    			String inputType = wrapper.getParameter("inputType");
    			List<String> reqCollectionsTmp = getRepeatedParameter(wrapper, "collections", "collections");
    			String[] reqCollections = new String[reqCollectionsTmp.size()];
    			reqCollectionsTmp.toArray(reqCollections);
    			
    			//Get all collections
    	    	List<Collection> collections = null;
    	    	String colIdS = wrapper.getParameter("colId");
    	    	if (colIdS!=null){
    	    		collections = new ArrayList<>();
    	    		collections.add(collectionService.findByIdOrLegacyId(context, colIdS));

    	    	}
    	    	else {
    	    		collections = collectionService.findAll(context);
    	    	}
    	    	request.setAttribute("collections", collections);
    	    	
    	    	
    	    	Collection owningCollection = null;
    			if (wrapper.getParameter("collection") != null) {
					owningCollection = collectionService.findByIdOrLegacyId(context,
							wrapper.getParameter("collection"));
    			}
    			
    	    	//Get all the possible data loaders from the Spring configuration
        		BTEBatchImportService dls  = new DSpace().getSingletonService(BTEBatchImportService.class);
        		List<String> inputTypes =dls.getFileDataLoaders();
        		request.setAttribute("input-types", inputTypes);
        		
        		if (reqCollectionsTmp!=null)
        			request.setAttribute("otherCollections", reqCollectionsTmp);
        		if (owningCollection!=null)
        			request.setAttribute("owningCollection", owningCollection.getID());
        		request.setAttribute("inputType", inputType);
        		
        		File f = null;
    			String zipurl = null;

    			if (inputType.equals("saf")){
    				zipurl = wrapper.getParameter("zipurl");
    				if (StringUtils.isEmpty(zipurl)) {
    					request.setAttribute("has-error", "true");
    					Locale locale = request.getLocale();
    					ResourceBundle msgs = ResourceBundle.getBundle("Messages", locale);
    					try {
    						message = msgs.getString("jsp.layout.navbar-admin.batchimport.fileurlempty");
    					} catch (Exception e) {
    						message = "???jsp.layout.navbar-admin.batchimport.fileurlempty???";
    					}
    					
    					request.setAttribute("message", message);

        				JSPManager.showJSP(request, response, "/dspace-admin/batchimport.jsp");

        				return;
    				}
    			}
    			else {
    				f = wrapper.getFile("file");
    				if (f == null) {
    					request.setAttribute("has-error", "true");
    					Locale locale = request.getLocale();
    					ResourceBundle msgs = ResourceBundle.getBundle("Messages", locale);
    					try {
    						message = msgs.getString("jsp.layout.navbar-admin.batchimport.fileempty");
    					} catch (Exception e) {
    						message = "???jsp.layout.navbar-admin.batchimport.fileempty???";
    					}
    					
    					request.setAttribute("message", message);

        				JSPManager.showJSP(request, response, "/dspace-admin/batchimport.jsp");

        				return;
    				}
    				else if (owningCollection==null && !"safupload".equals(inputType)){
    					request.setAttribute("has-error", "true");
    					Locale locale = request.getLocale();
    					ResourceBundle msgs = ResourceBundle.getBundle("Messages", locale);
    					try {
    						message = msgs.getString("jsp.layout.navbar-admin.batchimport.owningcollectionempty");
    					} catch (Exception e) {
    						message = "???jsp.layout.navbar-admin.batchimport.owningcollectionempty???";
    					}
    					
    					request.setAttribute("message", message);

        				JSPManager.showJSP(request, response, "/dspace-admin/batchimport.jsp");

        				return;
    				}
    			}

    			String uploadId = wrapper.getParameter("uploadId");
    			if (uploadId != null){
    				request.setAttribute("uploadId", uploadId);
    			}

    			if (owningCollection==null && reqCollections != null && reqCollections.length > 0){
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

    			try {
    				String finalInputType = "saf";
    				String filePath = zipurl;
    				if (f!=null){
    					finalInputType = inputType;
        				filePath = f.getAbsolutePath();
    				}
    				
    				itemImportService.processUIImport(filePath, owningCollection, reqCollections, uploadId, finalInputType, context, true);
    				
    				request.setAttribute("has-error", "false");
    				request.setAttribute("uploadId", null);

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

       		request.setAttribute("message", message);

    		// Show the upload screen
    		JSPManager.showJSP(request, response, "/dspace-admin/batchimport.jsp");

    	}
    	else
    	{
    		request.setAttribute("has-error", "true");

    		// Show the upload screen
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
    @Override
    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
    	//Get all collections
		List<Collection> collections = null;
		String colIdS = request.getParameter("colId");
		if (colIdS!=null){
			collections = new ArrayList<>();
			collections.add(collectionService.findByIdOrLegacyId(context, colIdS));

		}
		else {
			collections = collectionService.findAll(context);
		}

		request.setAttribute("collections", collections);

		//Get all the possible data loaders from the Spring configuration
		BTEBatchImportService dls  = new DSpace().getSingletonService(BTEBatchImportService.class);
		List<String> inputTypes = dls.getFileDataLoaders();
		request.setAttribute("input-types", inputTypes);

		// Show the upload screen
		JSPManager.showJSP(request, response, "/dspace-admin/batchimport.jsp");
    }
    
    /**
     * Get repeated values from a form. If "foo" is passed in as the parameter,
     * values in the form of parameters "foo", "foo_1", "foo_2", etc. are
     * returned.
     * <P>
     * This method can also handle "composite fields" (metadata fields which may
     * require multiple params, etc. a first name and last name).
     *
     * @param request
     *            the HTTP request containing the form information
     * @param metadataField
     *            the metadata field which can store repeated values
     * @param param
     *            the repeated parameter on the page (used to fill out the
     *            metadataField)
     *
     * @return a List of Strings
     */
    protected List<String> getRepeatedParameter(HttpServletRequest request,
            String metadataField, String param)
    {
        List<String> vals = new LinkedList<>();

        int i = 1;    //start index at the first of the previously entered values
        boolean foundLast = false;

        // Iterate through the values in the form.
        while (!foundLast)
        {
            String s = null;

            //First, add the previously entered values.
            // This ensures we preserve the order that these values were entered
            s = request.getParameter(param + "_" + i);

            // If there are no more previously entered values,
            // see if there's a new value entered in textbox
            if (s==null)
            {
                s = request.getParameter(param);
                //this will be the last value added
                foundLast = true;
            }

            // We're only going to add non-null values
            if (s != null)
            {
                boolean addValue = true;

                // Check to make sure that this value was not selected to be
                // removed.
                // (This is for the "remove multiple" option available in
                // Manakin)
                String[] selected = request.getParameterValues(metadataField
                        + "_selected");

                if (selected != null)
                {
                    for (int j = 0; j < selected.length; j++)
                    {
                        if (selected[j].equals(metadataField + "_" + i))
                        {
                            addValue = false;
                        }
                    }
                }

                if (addValue)
                {
                    vals.add(s.trim());
                }
            }

            i++;
        }

        log.debug("getRepeatedParameter: metadataField=" + metadataField
                + " param=" + metadataField + ", return count = "+vals.size());

        return vals;
    }
}
