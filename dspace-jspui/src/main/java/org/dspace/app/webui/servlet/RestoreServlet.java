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
import org.dspace.core.ConfigurationManager;
import java.lang.Runtime;
import java.io.RandomAccessFile;
import java.io.File;


/**
 * Servlet to batch import metadata via the BTE
 *
 * @author Stuart Lewis
 */
public class RestoreServlet extends DSpaceServlet
{
    /** log4j category */
    private static Logger log = Logger.getLogger(BatchMetadataImportServlet.class);
    private static boolean isFileLocked(String filename) {
    	boolean isLocked=false;
    	RandomAccessFile fos=null;
    	try {
        	File file = new File(filename);
        	if(file.exists()) {
            	fos=new RandomAccessFile(file,"rw");
        }
    	} catch (FileNotFoundException e) {
        	isLocked=true;
    	}catch (Exception e) {
        	// handle exception
    	}finally {
        	try {
            	if(fos!=null) {
                	fos.close();
            	}
        	}catch(Exception e) {
            		//handle exception
        	}
    	}
    	return isLocked;
    }


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
	//Get the location for Backup folder
	String backupfolder = ConfigurationManager.getProperty("backup.dir");
	String dspacedir = ConfigurationManager.getProperty("dspace.dir");
	String lockfile = ConfigurationManager.getProperty("backuprestore.lockfile");
        if (contentType != null)
        {
                String message = null;
		String inputType = (String)request.getParameter("inputType");
		//String inputType = "Hello World";
        	if(isFileLocked(lockfile) != true){		
        		request.setAttribute("snapshotname", inputType);
	    		request.setAttribute("has-error", "false");
			Runtime runTime = Runtime.getRuntime();
			Process process = runTime.exec("sh " + dspacedir + "/scripts/restore.sh " + backupfolder + "/" + inputType);	
		} else {
        		request.setAttribute("snapshotname", null);
        		request.setAttribute("message", "A backup or restore task is still ongoing..wait for its completion.");
	    		request.setAttribute("has-error", "true");
		}
      	
        	// Show the upload screen
    		JSPManager.showJSP(request, response, "/dspace-admin/restore-msg.jsp");

        }
        else
        {
	    request.setAttribute("has-error", "true");
	    request.setAttribute("message", "Some Unknown exception has occured.");
            // Show the upload screen
            JSPManager.showJSP(request, response, "/dspace-admin/restore.jsp");
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
	//Get the location for Backup folder
	String backupfolder = ConfigurationManager.getProperty("backup.dir");

	List<String> inputTypes = new ArrayList<String>();


	File[] files = new File(backupfolder).listFiles();
	//If this pathname does not denote a directory, then listFiles() returns null. 

	for (File file : files) {
    		if (file.isDirectory()) {
        		inputTypes.add(file.getName());
    		}
	}

    	request.setAttribute("snapshots", inputTypes);
	request.setAttribute("has-error", "false");
    	
        // Show the upload screen
        JSPManager.showJSP(request, response, "/dspace-admin/restore.jsp");
    }

}
