/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.util;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.io.Serializable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * Stores information about an HTTP request. This is used so that the request
 * can be replicated during a later request, once authentication has
 * successfully occurred.
 *
 * Note: Implements Serializable as it will be saved to the current session during submission.
 * Please ensure that nothing is added to this class that isn't also serializable
 *
 * @author Robert Tansley
 * @version $Revision$
 */
public class RequestInfo implements Serializable
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
