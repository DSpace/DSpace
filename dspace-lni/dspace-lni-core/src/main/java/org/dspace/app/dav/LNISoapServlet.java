/*
 * LNISoapServlet.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2007, Hewlett-Packard Company and Massachusetts
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
package org.dspace.app.dav;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.GenericServlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.axis.MessageContext;
import org.apache.axis.transport.http.AxisServlet;
import org.apache.log4j.Logger;
import org.dspace.authenticate.AuthenticationManager;
import org.dspace.authenticate.AuthenticationMethod;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.jdom.Document;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;


/**
 * Servlet implementing SOAP services of DSpace Lightweight Network Interface
 * <P>
 * This is implemented as a subclass of the AxisServlet that processes SOAP
 * requests, so it can pick out the requests it handles and pass on the rest to
 * the Axis Engine.
 * <p>
 * Note that it also handles WebDAV GET and PUT requests, so the SOAP client can
 * use the same URL as a SOAP endpoint and WebDAV resource root.
 * 
 * @author Larry Stone
 * @version $Revision$
 */
public class LNISoapServlet extends AxisServlet

{
    
    /** log4j category. */
    private static Logger log = Logger.getLogger(LNISoapServlet.class);

    /** The output pretty. */
    private static XMLOutputter outputPretty = new XMLOutputter(Format
            .getPrettyFormat());

    // state of this transaction
    /** The request. */
    private HttpServletRequest request = null;

    /** The response. */
    private HttpServletResponse response = null;

    // last servlet instance when put into service, set by init()
    /** The servlet instance. */
    private static GenericServlet servletInstance = null;

    /* (non-Javadoc)
     * @see javax.servlet.GenericServlet#init(javax.servlet.ServletConfig)
     */
    @Override
    public void init(ServletConfig sc) throws ServletException
    {
        super.init(sc);
        servletInstance = this;
    }

    /**
     * Gets the servlet instance.
     * 
     * @return the servlet instance
     */
    public static GenericServlet getServletInstance()
    {
        return servletInstance;
    }

    /**
     * Pass a GET request directly to the WebDAV implementation. It handles
     * authentication.
     * 
     * @param request the request
     * @param response the response
     * 
     * @throws ServletException the servlet exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        DAVServlet.serviceInternal("GET", request, response);
    }

    /**
     * Pass a PUT request directly to the WebDAV implementation. It handles
     * authentication.
     * 
     * @param request the request
     * @param response the response
     * 
     * @throws ServletException the servlet exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        DAVServlet.serviceInternal("PUT", request, response);
    }

    /**
     * Authenticate and return the filled-in DSpace context This is the prologue
     * to all calls.
     * 
     * @return the context
     * 
     * @throws SQLException the SQL exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private Context prologue() throws SQLException, IOException
    {
        MessageContext mc = MessageContext.getCurrentContext();
        String username = null, password = null;

        if (mc.getUsername() != null)
        {
            username = DAVServlet.URLDecode(mc.getUsername());
        }
        if (mc.getPassword() != null)
        {
            password = DAVServlet.URLDecode(mc.getPassword());
        }

        /***********************************************************************
         * ** XXX TEMPORARY *** Instrumentation to explore the guts of Axis at
         * runtime, *** leave this commented-out. java.util.Iterator pi =
         * mc.getPropertyNames(); while (pi.hasNext()) log.debug("SOAP: request
         * has property named \""+((String)pi.next())+"\""); log.debug("SOAP:
         * getSOAPActionURI = \""+mc.getSOAPActionURI()+"\""); log.debug("SOAP:
         * property(servletEndpointContext) = =
         * \""+mc.getProperty("servletEndpointContext").toString()+"\"");
         * log.debug("SOAP: property(realpath) = =
         * \""+mc.getProperty("realpath").toString()+"\""); log.debug("SOAP:
         * property(transport.http.servletLocation) = =
         * \""+mc.getProperty("transport.http.servletLocation").toString()+"\"");
         * *** end TEMPORARY INSTRUMENTATION
         **********************************************************************/

        this.request = (HttpServletRequest) mc
                .getProperty("transport.http.servletRequest");
        this.response = (HttpServletResponse) mc
                .getProperty("transport.http.servletResponse");
        Context context = new Context();

        // try cookie shortcut
        if (DAVServlet.getAuthFromCookie(context, this.request))
        {
            DAVServlet.putAuthCookie(context, this.request, this.response, false);
            log.debug("SOAP service " + this.getClass().getName()
                    + " authenticated with cookie.");
            return context;
        }

        int status = AuthenticationManager.authenticate(context, username,
                password, null, this.request);
        if (status == AuthenticationMethod.SUCCESS)
        {
            EPerson cu = context.getCurrentUser();
            log.debug("SOAP service " + this.getClass().getName()
                    + " authenticated as " + cu.getEmail() + " ("
                    + cu.getFirstName() + " " + cu.getLastName() + ")");
            DAVServlet.putAuthCookie(context, this.request, this.response, true);
            return context;
        }
        else if (status == AuthenticationMethod.BAD_CREDENTIALS)
        {
            throw new LNIRemoteException(
                    "Authentication failed: Bad Credentials.");
        }
        else if (status == AuthenticationMethod.CERT_REQUIRED)
        {
            throw new LNIRemoteException(
                    "Authentication failed: This user may only login with X.509 certificate.");
        }
        else if (status == AuthenticationMethod.NO_SUCH_USER)
        {
            throw new LNIRemoteException("Authentication failed: No such user.");
        }
        else
        {
            /** AuthenticationMethod.BAD_ARGS and etc * */
            throw new LNIRemoteException(
                    "Authentication failed: Cannot authenticate.");
        }
    }

    /**
     * Propfind.
     * 
     * @param uri the uri
     * @param doc the doc
     * @param depth the depth
     * @param types the types
     * 
     * @return the string
     * 
     * @throws LNIRemoteException the LNI remote exception
     */
    public String propfind(String uri, String doc, int depth, String types)
            throws LNIRemoteException
    {
        // break up path into elements.
        if (uri.startsWith("/"))
        {
            uri = uri.substring(1);
        }
        String pathElt[] = uri.split("/");
        Context context = null;
        try
        {
            context = prologue();

            // return properties only for resources of these types, comma-sep
            // list
            String aTypes[] = (types == null) ? null : types.split(",");
            int typeMask = DAVResource.typesToMask(aTypes);

            DAVResource resource = DAVResource.findResource(context, null,
                    null, pathElt);
            if (resource == null)
            {
                throw new LNIRemoteException("Resource not found.");
            }
            else
            {
                Document outdoc = resource.propfindDriver(depth,
                        new ByteArrayInputStream(doc.getBytes()), typeMask);
                if (outdoc != null)
                {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    outputPretty.output(outdoc, baos);
                    context.complete();
                    context = null;
                    return baos.toString();
                }
                else
                {
                    // this should never happen, it should throw an error
                    // before returning null
                    throw new LNIRemoteException(
                            "propfind failed, no document returned.");
                }
            }
        }
        catch (IOException ie)
        {
            throw new LNIRemoteException("Exception executing PROPFIND", ie);
        }
        catch (SQLException e)
        {
            throw new LNIRemoteException("Failure accessing database", e);
        }
        catch (DAVStatusException e)
        {
            throw new LNIRemoteException("PROPFIND request failed: "
                    + e.getStatusLine());
        }
        catch (AuthorizeException e)
        {
            throw new LNIRemoteException(
                    "You are not authorized for the requested operation.", e);
        }
        finally
        {
            if (context != null)
            {
                context.abort();
            }
        }
    }

    /**
     * Proppatch.
     * 
     * @param uri the uri
     * @param doc the doc
     * 
     * @return the string
     * 
     * @throws LNIRemoteException the LNI remote exception
     */
    public String proppatch(String uri, String doc) throws LNIRemoteException
    {
        // break up path into elements.
        if (uri.startsWith("/"))
        {
            uri = uri.substring(1);
        }
        String pathElt[] = uri.split("/");

        Context context = null;
        try
        {
            context = prologue();
            DAVResource resource = DAVResource.findResource(context, null,
                    null, pathElt);
            if (resource == null)
            {
                throw new LNIRemoteException("Resource not found.");
            }
            else
            {
                Document outdoc = resource
                        .proppatchDriver(new ByteArrayInputStream(doc
                                .getBytes()));
                if (outdoc != null)
                {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    outputPretty.output(outdoc, baos);
                    context.complete();
                    context = null;
                    return baos.toString();
                }
                else
                {
                    // this should never happen, it should throw an error
                    // before returning null
                    throw new LNIRemoteException(
                            "proppatch failed, no document returned.");
                }
            }
        }
        catch (IOException ie)
        {
            throw new LNIRemoteException("Exception executing PROPPATCH", ie);
        }
        catch (SQLException e)
        {
            throw new LNIRemoteException("Failure accessing database", e);
        }
        catch (DAVStatusException e)
        {
            throw new LNIRemoteException("PROPPATCH request failed: "
                    + e.getStatusLine());
        }
        catch (AuthorizeException e)
        {
            throw new LNIRemoteException(
                    "You are not authorized for the requested operation.", e);
        }
        finally
        {
            if (context != null)
            {
                context.abort();
            }
        }
    }

    /** The lookup path elt. */
    private static String lookupPathElt[] = { "lookup", "handle" };

    /**
     * Return "absolute" DAV URI for the given handle (and optional bitstream
     * persistent identifier). Always returns a valid URI; if resource is not
     * found it throws an exception.
     * 
     * @param handle the handle
     * @param bitstreamPid the bitstream pid
     * 
     * @return the string
     * 
     * @throws LNIRemoteException the LNI remote exception
     */
    public String lookup(String handle, String bitstreamPid)
            throws LNIRemoteException
    {
        Context context = null;
        try
        {
            context = prologue();

            // trim leading scheme if any:
            if (handle.startsWith("hdl:"))
            {
                handle = handle.substring(4);
            }

            DAVLookup resource = new DAVLookup(context, this.request, this.response,
                    lookupPathElt);
            String result = resource.makeURI(handle, bitstreamPid);
            if (result == null)
            {
                throw new LNIRemoteException("Resource not found.");
            }
            context.complete();
            context = null;
            return result;
        }
        catch (IOException ie)
        {
            throw new LNIRemoteException("Exception executing LOOKUP", ie);
        }
        catch (SQLException e)
        {
            throw new LNIRemoteException("Failure accessing database", e);
        }
        finally
        {
            if (context != null)
            {
                context.abort();
            }
        }
    }

    /**
     * Copy.
     * 
     * @param source the source
     * @param destination the destination
     * @param depth the depth
     * @param overwrite the overwrite
     * @param keepProperties the keep properties
     * 
     * @return the int
     * 
     * @throws LNIRemoteException the LNI remote exception
     */
    public int copy(String source, String destination, int depth,
            boolean overwrite, boolean keepProperties)
            throws LNIRemoteException
    {
        // break up path into elements.
        if (source.startsWith("/"))
        {
            source = source.substring(1);
        }
        String pathElt[] = source.split("/");

        Context context = null;
        try
        {
            context = prologue();
            DAVResource resource = DAVResource.findResource(context, null,
                    null, pathElt);
            if (resource == null)
            {
                throw new LNIRemoteException("Resource not found.");
            }
            context.complete();
            context = null;
            return resource.copyDriver(destination, depth, overwrite,
                    keepProperties);
        }
        catch (IOException ie)
        {
            throw new LNIRemoteException("IOException while executing COPY", ie);
        }
        catch (SQLException e)
        {
            throw new LNIRemoteException("Failure accessing database", e);
        }
        catch (DAVStatusException e)
        {
            throw new LNIRemoteException("COPY request failed: "
                    + e.getStatusLine());
        }
        catch (AuthorizeException e)
        {
            throw new LNIRemoteException(
                    "You are not authorized for the requested operation.", e);
        }
        finally
        {
            if (context != null)
            {
                context.abort();
            }
        }
    }
}
