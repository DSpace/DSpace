/*
 * FileUploadRequest.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.app.webui.util;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.fileupload.DiskFileUpload;
import org.apache.commons.fileupload.FileItem;
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

    /** Multipart request */
    private List items = null;

    private HashMap parameters = new HashMap();

    private HashMap fileitems = new HashMap();

    private Vector filenames = new Vector();

    private String tempDir = null;

    /** Original request */
    private HttpServletRequest original = null;

    /**
     * Parse a multipart request and extracts the files
     * 
     * @param req
     *            the original request
     */
    public FileUploadRequest(HttpServletRequest req) throws IOException
    {
        super(req);

        original = req;

        tempDir = ConfigurationManager.getProperty("upload.temp.dir");
        int maxSize = ConfigurationManager.getIntProperty("upload.max");

        DiskFileUpload upload = new DiskFileUpload();

        try
        {
            upload.setRepositoryPath(tempDir);
            upload.setSizeMax(maxSize);
            items = upload.parseRequest(req);
            for (Iterator i = items.iterator(); i.hasNext();)
            {
                FileItem item = (FileItem) i.next();
                if (item.isFormField())
                {
                    parameters
                            .put(item.getFieldName(), item.getString("UTF-8"));
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
        catch (Exception e)
        {
            IOException t = new IOException(e.getMessage());
            t.initCause(e);
            throw t;
        }
    }

    // Methods to replace HSR methods
    public Enumeration getParameterNames()
    {
        Collection c = parameters.keySet();
        return Collections.enumeration(c);
    }

    public String getParameter(String name)
    {
        return (String) parameters.get(name);
    }

    public String[] getParameterValues(String name)
    {
        return (String[]) parameters.values().toArray();
    }

    public Map getParameterMap()
    {
        Map map = new HashMap();
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
        String filename = getFilename(((FileItem) fileitems.get(name))
                .getName());
        return tempDir + File.separator + filename;
    }

    public String getContentType(String name)
    {
        return ((FileItem) fileitems.get(name)).getContentType();
    }

    public File getFile(String name)
    {
        FileItem temp = (FileItem) fileitems.get(name);
        String tempName = temp.getName();
        String filename = getFilename(tempName);
        if ("".equals(filename.trim()))
        {
            return null;
        }
        return new File(tempDir + File.separator + filename);
    }

    public Enumeration getFileParameterNames()
    {
        Collection c = fileitems.keySet();
        return Collections.enumeration(c);
    }
    
    public Enumeration getFileNames()
    {
        return filenames.elements();
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