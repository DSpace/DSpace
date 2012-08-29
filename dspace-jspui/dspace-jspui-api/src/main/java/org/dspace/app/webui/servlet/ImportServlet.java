package org.dspace.app.webui.servlet;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;
import org.dspace.app.importer.ImportResultBean;
import org.dspace.app.importer.Importer;
import org.dspace.app.importer.ImporterException;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.PluginManager;

/**
 * Servlet to create items in workspace using an import plugin
 * (DOI, PubmedID, ArXiv, etc.)
 * 
 * @version $Revision: 1.1 $
 */
public class ImportServlet extends DSpaceServlet
{
    /** Logger */
    private static Logger log = Logger.getLogger(ImportServlet.class);

    @Override
    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        String[] importPlugins = PluginManager.getAllPluginNames(Importer.class);
        request.setAttribute("plugins", importPlugins);
        JSPManager.showJSP(request, response, "/tools/import-form.jsp");
    }

    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
    	String importdatafile = "";
    	String importdata = "";
    	String format = "";

    	
    	List items;
    	FileItem item = null;
    	HashMap parameters = new HashMap();
    	String tempDir = ConfigurationManager.getProperty("upload.temp.dir");
    	
    	int maxSize = ConfigurationManager.getIntProperty("upload.max");
    	
    	// Create a factory for disk-based file items
    	DiskFileItemFactory factory = new DiskFileItemFactory();
    	factory.setRepository(new File(tempDir));
    	
    	// Create a new file upload handler
    	ServletFileUpload upload = new ServletFileUpload(factory);
    	
    	try {
    		upload.setSizeMax(maxSize);
			items = upload.parseRequest(request);
    		for (Iterator i = items.iterator(); i.hasNext();)
    		{	
    			item = (FileItem) i.next();
				//System.out.println(item.getFieldName());
				if(item.getFieldName().equals("importdata")) {
    				parameters.put(item.getFieldName(), item.getString());
    			}
				if(item.getFieldName().equals("format")) {
    				parameters.put(item.getFieldName(), item.getString());
    			}
    			if (item.getFieldName().equals("importdatafile")) {
    				parameters.put(getFilename(item.getFieldName()), getFilename(item.getName()));
    				if (item.getName() != null && !"".equals(getFilename(item.getName())))
	                {
	                    item.write(new File(tempDir + File.separator + getFilename(item.getName())));
	                }
    			}
    		}
    	}catch (Exception e){
    		 IOException t = new IOException(e.getMessage());
             t.initCause(e);
             throw t;
    	}
        
    	format = (String) parameters.get("format");
    	importdata = (String) parameters.get("importdata");
    	
        String filename = (String) parameters.get("importdatafile");
        
        
        if (filename != null && !"".equals(filename)) {
	        File file = new File(tempDir + File.separator + filename);
	        FileInputStream is = null;        
	        
			try {
				is = new FileInputStream(file);
				
	            StringBuilder sb = new StringBuilder();
	            String line;          
	            BufferedReader reader = new BufferedReader(
	                        new InputStreamReader(is, "UTF-8"));
	            while ((line = reader.readLine()) != null) {
	            	sb.append(line).append("\n");
	            }
	            
	            importdatafile = sb.toString();
				is.close();
				
				importdata += importdatafile;
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
	            is.close();
	        }
        }

        Importer importer = (Importer) PluginManager.getNamedPlugin(Importer.class, format);
               
        if (importer == null)
        {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        ImportResultBean result = null;
        try {
        	result = importer.ingest(importdata, context.getCurrentUser());
        }
        catch(ImporterException exc) {
        	request.setAttribute("error", exc);
        	JSPManager.showJSP(request, response, "/tools/import-error.jsp");	
        	return;
        }
        
        request.setAttribute("result", result);
        JSPManager.showJSP(request, response, "/tools/import-result.jsp");
    }
    
    private String getFilename(String filepath)
    {
        String filename = filepath.trim();

        int index = filepath.lastIndexOf(File.separator);
        if (index > -1)
        {
            filename = filepath.substring(index);
        }
        return filename;
    }
}
