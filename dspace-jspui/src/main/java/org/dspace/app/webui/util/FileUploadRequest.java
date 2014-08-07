/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.util;

import java.io.File;
import java.io.IOException;
import java.util.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadBase.FileSizeLimitExceededException;
import org.apache.commons.fileupload.FileUploadBase.IOFileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.lang.StringUtils;
import org.dspace.core.ConfigurationManager;

/**
 * Based on the com.oreilly.servlet.MultipartWrapper object, this is an HTTP
 * request wrapper for multi-part (MIME) POSTs. It uses DSpace configuration
 * properties to determine the temporary directory to use and the maximum
 * allowable upload size.
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class FileUploadRequest extends HttpServletRequestWrapper
{
    public static final String FILE_UPLOAD_LISTNER = "file-upload-listner";
    
    private Map<String, String> parameters = new HashMap<String, String>();

    private Map<String, FileItem> fileitems = new HashMap<String, FileItem>();

    private List<String> filenames = new ArrayList<String>();

    private String tempDir = null;

    /** Original request */
    private HttpServletRequest original = null;

    /**
     * Parse a multipart request and extracts the files
     * 
     * @param req
     *            the original request
     */
    public FileUploadRequest(HttpServletRequest req) throws IOException, FileSizeLimitExceededException
    {
        super(req);

        original = req;

        tempDir = (ConfigurationManager.getProperty("upload.temp.dir") != null)
            ? ConfigurationManager.getProperty("upload.temp.dir") : System.getProperty("java.io.tmpdir"); 
        long maxSize = ConfigurationManager.getLongProperty("upload.max");

        // Create a factory for disk-based file items
        DiskFileItemFactory factory = new DiskFileItemFactory();
        factory.setRepository(new File(tempDir));

        // Create a new file upload handler
        ServletFileUpload upload = new ServletFileUpload(factory);

        HttpSession session = req.getSession();
        
        if (ConfigurationManager.getBooleanProperty("webui.submit.upload.progressbar", true))
        {
            // set file upload progress listener
            FileUploadListener listener = new FileUploadListener();
    
            session.setAttribute(FILE_UPLOAD_LISTNER, listener);
    
            // upload servlet allows to set upload listener
            upload.setProgressListener(listener);
        }
        
        try
        {
            upload.setSizeMax(maxSize);
            List<FileItem> items = upload.parseRequest(req);
            for (FileItem item : items)
            {
                if (item.isFormField())
                {
                    parameters.put(item.getFieldName(), item.getString("UTF-8"));
                }
                else
                {
                    if (parameters.containsKey("resumableIdentifier")) 
                    {
                        String filename = getFilename(parameters.get("resumableFilename"));
                        if (!StringUtils.isEmpty(filename)) 
                        {
                            String chunkDirPath = tempDir + File.separator + parameters.get("resumableIdentifier");
                            String chunkPath = chunkDirPath + File.separator + "part" + parameters.get("resumableChunkNumber");
                            File fileDir = new File(chunkDirPath);
                            
                            if(fileDir.exists())
                            {
                                item.write(new File(chunkPath));
                            }
                        }
                    }
                    else
                    {
                        parameters.put(item.getFieldName(), item.getName());
                        fileitems.put(item.getFieldName(), item);
                        filenames.add(item.getName());

                        String filename = getFilename(item.getName());
                        if (filename != null && !"".equals(filename))
                        {
                            item.write(new File(tempDir + File.separator
                                            + filename));
                        }
                    }
                }
            }
        }
        catch(IOFileUploadException e){
            if (!(e.getMessage().contains("Stream ended unexpectedly")))
            {
                throw new IOException(e.getMessage(), e);
            }
        }
        catch (Exception e)
        {
            if(e.getMessage().contains("exceeds the configured maximum"))
            {
                // ServletFileUpload is not throwing the correct error, so this is workaround
                // the request was rejected because its size (11302) exceeds the configured maximum (536)
                int startFirstParen = e.getMessage().indexOf("(")+1;
                int endFirstParen = e.getMessage().indexOf(")");
                String uploadedSize = e.getMessage().substring(startFirstParen, endFirstParen).trim();
                Long actualSize = Long.parseLong(uploadedSize);
                throw new FileSizeLimitExceededException(e.getMessage(), actualSize, maxSize);
            }
            throw new IOException(e.getMessage(), e);
        }
        finally
        {
            if (ConfigurationManager.getBooleanProperty("webui.submit.upload.progressbar", true))
            {
                session.removeAttribute(FILE_UPLOAD_LISTNER);
            }
        }
    }

    // Methods to replace HSR methods
    public Enumeration getParameterNames()
    {
        Collection<String> c = parameters.keySet();
        return Collections.enumeration(c);
    }

    public String getParameter(String name)
    {
        return parameters.get(name);
    }

    public String[] getParameterValues(String name)
    {
        return parameters.values().toArray(new String[parameters.values().size()]);
    }

    public Map getParameterMap()
    {
        Map<String, String[]> map = new HashMap<String, String[]>();
        Enumeration eNum = getParameterNames();

        while (eNum.hasMoreElements())
        {
            String name = (String) eNum.nextElement();
            map.put(name, getParameterValues(name));
        }

        return map;
    }

    public String getFilesystemName(String name)
    {
        String filename = getFilename((fileitems.get(name))
                .getName());
        return tempDir + File.separator + filename;
    }

    public String getContentType(String name)
    {
        return (fileitems.get(name)).getContentType();
    }

    public File getFile(String name)
    {
        FileItem temp = fileitems.get(name);
        String tempName = temp.getName();
        String filename = getFilename(tempName);
        if ("".equals(filename.trim()))
        {
            return null;
        }
        return new File(tempDir + File.separator + filename);
    }

    public Enumeration<String> getFileParameterNames()
    {
        Collection<String> c = fileitems.keySet();
        return Collections.enumeration(c);
    }
    
    public Enumeration<String> getFileNames()
    {
        return Collections.enumeration(filenames);
    }

    /**
     * Get back the original HTTP request object
     * 
     * @return the original HTTP request
     */
    public HttpServletRequest getOriginalRequest()
    {
        return original;
    }

    // Required due to the fact the contents of getName() may vary based on
    // browser
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
