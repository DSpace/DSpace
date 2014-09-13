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
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Servlet to batch import metadata via the BTE
 *
 * @author Stuart Lewis
 */
public class BackupServlet extends DSpaceServlet
{
    /** log4j category */
    private static Logger log = Logger.getLogger(BatchMetadataImportServlet.class);

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
	DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
	//get current date time with Date()
	Date date = new Date();
	String snapshot = "snapshot_";
	String inputTypes = snapshot.concat(dateFormat.format(date));	

    	request.setAttribute("snapshotname", inputTypes);
    	request.setAttribute("message", backupfolder);
    	
        // Show the upload screen
        JSPManager.showJSP(request, response, "/dspace-admin/backup.jsp");
    }

}
