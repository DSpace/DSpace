/*
 * RequestMimic.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2001, Hewlett-Packard Company and Massachusetts
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

import java.util.Enumeration;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;


/**
 * HttpServletRequestWrapper representing information about an HTTP request.
 * This is used to store information about a request so that it can be mimiced
 * and completed during a later request.  This is used if a request couldn't
 * initially be fulfilled because the user was unauthenticated, so that after
 * the user authenticates, what they were originally doing can be completed.
 *
 * @author  Robert Tansley
 * @version $Revision$
 */
public class RequestMimic extends HttpServletRequestWrapper
{
    /** The original parameters */
    private Map originalParameterMap;
        
    /** The original method */
    private String originalMethod;
        
    /** The original query */
    private String originalQueryString;
        
    /** The original servlet path */
    private String originalServletPath;
        
    /** The original path info */
    private String originalPathInfo;
        
    /** The original request URL */
    private String originalRequestURL;
        
    /**
     * Construct a wrapper using the given request - not equivalent to
     * HttpServletWrapper constructor.  Information about the request
     * passed in is stored - this should be the original request that
     * causes authentication to occur.  At a later time, and during a
     * different request, use setRequest() to make this wrap the current
     * request, so that it mimics the request that led to the
     * authentication, and the user can carry on as they were.
     *
     * @param  request   the request from which to extract information -
     *                   NOT the request to wrap
     */
    public RequestMimic(HttpServletRequest request)
    {
        // The wrapper class constructor requires a request
        super(request);

        originalParameterMap = new HashMap(request.getParameterMap());
        originalMethod = request.getMethod();
        originalQueryString = request.getQueryString();
        originalServletPath = request.getServletPath();
        originalPathInfo = request.getPathInfo();
        originalRequestURL = request.getRequestURL().toString();
    }


    // ====== Methods below here are the wrapped methods ======

    public String getParameter(String name)
    {
        String[] vals = (String[]) originalParameterMap.get(name);

        if (vals == null)
        {
            return null;
        }
        else
        {
            return vals[0];
        }
    }

    public Map getParameterMap()
    {
        return originalParameterMap;
    }

    public Enumeration getParameterNames()
    {
        Iterator i = originalParameterMap.keySet().iterator();

        return new EnumIterator(i);
    }

    public String[] getParameterValues(String name)
    {
        return ((String[]) originalParameterMap.get(name));
    }

    public String getMethod()
    {
        return originalMethod;
    }

    public String getQueryString()
    {
        return originalQueryString;
    }

    public String getServletPath()
    {
        return originalServletPath;
    }

    public String getPathInfo()
    {
        return originalPathInfo;
    }
        
    public StringBuffer getRequestURL()
    {
        return (new StringBuffer(originalRequestURL));
    }

    /**
     * This class converts an interator into an enumerator.  This is done
     * because we have the parameters as a Map (JDK 1.2 style), but for 
     * some weird reason the HttpServletRequest interface returns an
     * Enumeration from getParameterNames() (JDK1.0 style.)  JDK apparently
     * offers no way of simply changing between the new styles.
     */
    class EnumIterator implements Enumeration
    {
        private Iterator iterator;

        public EnumIterator(Iterator i)
        {
            iterator = i;
        }
                
        public boolean hasMoreElements()
        {
            return iterator.hasNext();
        }

        public Object nextElement()
        {
            return iterator.next();
        }
    }
}
