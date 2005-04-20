/*
 * RequestInfo.java
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

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * Stores information about an HTTP request. This is used so that the request
 * can be replicated during a later request, once authentication has
 * successfully occurred.
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class RequestInfo
{
    /** The original parameters */
    private Map originalParameterMap;

    /** The original method */
    private String originalMethod;

    /** The original query */
    private String originalQueryString;

    /**
     * Construct a request info object storing information about the given
     * request
     * 
     * @param request
     *            the request to get information from
     */
    public RequestInfo(HttpServletRequest request)
    {
        originalParameterMap = new HashMap(request.getParameterMap());
        originalMethod = request.getMethod();
        originalQueryString = request.getQueryString();
    }

    /**
     * Wrap an incoming request to make it look like the request that the
     * constructor was called with
     * 
     * @param request
     *            the request to wrap
     * 
     * @return a wrapper around the request passed into this method, wrapped so
     *         that it looks like the request passed into the constructor
     */
    public HttpServletRequest wrapRequest(HttpServletRequest request)
    {
        return new MyWrapper(request);
    }

    /**
     * Our own flavour of HTTP request wrapper, that uses information from= this
     * RequestInfo object
     */
    class MyWrapper extends HttpServletRequestWrapper
    {
        public MyWrapper(HttpServletRequest request)
        {
            super(request);
        }

        // ====== Methods below here are the wrapped methods ======
        public String getParameter(String name)
        {
            String[] vals = (String[]) originalParameterMap.get(name);

            if (vals == null)
            {
                // Delegate to wrapped object
                // FIXME: This is possibly a bug in Tomcat
                return super.getParameter(name);
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

        /**
         * This class converts an interator into an enumerator. This is done
         * because we have the parameters as a Map (JDK 1.2 style), but for some
         * weird reason the HttpServletRequest interface returns an Enumeration
         * from getParameterNames() (JDK1.0 style.) JDK apparently offers no way
         * of simply changing between the new styles.
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
}
