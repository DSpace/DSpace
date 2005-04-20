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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.dspace.core.ConfigurationManager;

import com.oreilly.servlet.MultipartRequest;

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
    private MultipartRequest mreq = null;

    /** Original request */
    private HttpServletRequest original = null;

    /**
     * Wraps a multipart request and extracts the files
     * 
     * @param req
     *            the original request
     */
    public FileUploadRequest(HttpServletRequest req) throws IOException
    {
        super(req);

        original = req;

        String tempDir = ConfigurationManager.getProperty("upload.temp.dir");
        int maxSize = ConfigurationManager.getIntProperty("upload.max");

        mreq = new MultipartRequest(req, tempDir, maxSize, "UTF-8");
    }

    // Methods to replace HSR methods
    public Enumeration getParameterNames()
    {
        return mreq.getParameterNames();
    }

    public String getParameter(String name)
    {
        return mreq.getParameter(name);
    }

    public String[] getParameterValues(String name)
    {
        return mreq.getParameterValues(name);
    }

    public Map getParameterMap()
    {
        Map map = new HashMap();
        Enumeration eNum = getParameterNames();

        while (eNum.hasMoreElements())
        {
            String name = (String) eNum.nextElement();
            map.put(name, mreq.getParameterValues(name));
        }

        return map;
    }

    // Methods only in MultipartRequest
    public Enumeration getFileNames()
    {
        return mreq.getFileNames();
    }

    public String getFilesystemName(String name)
    {
        return mreq.getFilesystemName(name);
    }

    public String getContentType(String name)
    {
        return mreq.getContentType(name);
    }

    public File getFile(String name)
    {
        return mreq.getFile(name);
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
}
