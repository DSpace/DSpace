/*
 * DAVResource.java
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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.JDOMParseException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;


/**
 * Superclass for all DSpace "resources" in the WebDAV interface. Maps DAV
 * operations onto DSpace objects. An instance is created for one HTTP request
 * and discarded thereafter.
 * <p>
 * This class has the high-level driver for each DAV (or SOAP) method. It calls
 * on subclasses for implementation details, e.g. how to get properties in and
 * out of each type of resource, PUT, COPY, etc.
 * <p>
 * All of the interpretation (and generation) of DAV Resource URIs should be in
 * this class, in methods such as <code>findResource()</code>,
 * <code>hrefURL()</code> etc, calling on the <code>matchResource()</code>
 * methods in some subclasses for help. NOTE: <code>matchResource</code>
 * should be an abstract method but it cannot be since it is necessarily static.
 * <p>
 * DAVResource instances are generally only created by the
 * <code>findResource()</code> method and the <code>children()</code>
 * methods in resource subclasses.
 */
abstract class DAVResource
{
    
    /** log4j category. */
    private static Logger log = Logger.getLogger(DAVResource.class);

    /** The path elt. */
    protected String pathElt[] = null;

    /** The request. */
    protected HttpServletRequest request = null;

    /** The response. */
    protected HttpServletResponse response = null;

    /** The context. */
    protected Context context = null;

    /** Optional limit of resources traversed in a PROPFIND - initialized from PROPFIND_LIMIT_CONFIG in config properties. Names maximum number of HREF's in PROPFIND result or 0 for unlimited. */
    private static final String PROPFIND_LIMIT_CONFIG = "dav.propfind.limit";

    /** The propfind resource limit. */
    private static int propfindResourceLimit = ConfigurationManager
            .getIntProperty(PROPFIND_LIMIT_CONFIG);

    /** Resource type (i.e. DSpace object type) masks to implement type filter in PROPFIND. */
    protected static final int TYPE_OTHER = 0x20; // 100000

    /** The Constant TYPE_SITE. */
    protected static final int TYPE_SITE = 0x10; // 10000

    /** The Constant TYPE_COMMUNITY. */
    protected static final int TYPE_COMMUNITY = 0x08; // 01000

    /** The Constant TYPE_COLLECTION. */
    protected static final int TYPE_COLLECTION = 0x04; // 00100

    /** The Constant TYPE_ITEM. */
    protected static final int TYPE_ITEM = 0x02; // 00010

    /** The Constant TYPE_BITSTREAM. */
    protected static final int TYPE_BITSTREAM = 0x01; // 00001

    /** The Constant TYPE_ALL. */
    protected static final int TYPE_ALL = 0x1F; // 11111

    /** Type code, set by constructor in each subclass. */
    protected int type = 0;

    /** The output raw. */
    private static XMLOutputter outputRaw = new XMLOutputter();

    /** The output pretty. */
    private static XMLOutputter outputPretty = new XMLOutputter(Format
            .getPrettyFormat());

    // enable dumping XML for last PROPFIND or PROPPATCH transaction.
    /** The Constant debugXML. */
    private static final boolean debugXML = ConfigurationManager
            .getBooleanProperty("dav.debug.xml", false);

    /** DAV Properties common to all resources:. */
    protected static final Element displaynameProperty = new Element(
            "displayname", DAV.NS_DAV);

    /** The Constant resourcetypeProperty. */
    protected static final Element resourcetypeProperty = new Element(
            "resourcetype", DAV.NS_DAV);

    /** The Constant typeProperty. */
    protected static final Element typeProperty = new Element("type",
            DAV.NS_DSPACE);

    /** The Constant current_user_privilege_setProperty. */
    protected static final Element current_user_privilege_setProperty = new Element(
            "current-user-privilege-set", DAV.NS_DAV);

    /** The common props. */
    protected static List commonProps = new Vector();
    static
    {
        commonProps.add(displaynameProperty);
        commonProps.add(resourcetypeProperty);
        commonProps.add(typeProperty);
        commonProps.add(current_user_privilege_setProperty);
    }

    /**
     * Instantiates a new DAV resource.
     * 
     * @param context the context
     * @param request the request
     * @param response the response
     * @param pathElt the path elt
     */
    protected DAVResource(Context context, HttpServletRequest request,
            HttpServletResponse response, String pathElt[])
    {
        this.pathElt = pathElt;
        this.request = request;
        this.response = response;
        this.context = context;
    }

    /*----------------- Abstracts -----------------------*/

    /**
     * Returns an array of <code>DAVResource</code>'s considered the direct
     * children of this resource. Array can be empty but MUST NOT be null.
     * 
     * @return array of immediate children, or empty array if none.
     * 
     * @throws SQLException the SQL exception
     */
    abstract protected DAVResource[] children() throws SQLException;

    /**
     * Execute a PROPFIND method request on this Resource, and insert the
     * results into the <code>multistatus</code> XML element. This may be one
     * of many <code>propfind()</code> calls that build up the result of a
     * PROPFIND on a whole hierarchy. The depth option of the PROPFIND method is
     * managed by the driver that calls this, so it is only responsible for its
     * own resource.
     * 
     * @param property the property
     * 
     * @return the element
     * 
     * @throws SQLException the SQL exception
     * @throws AuthorizeException the authorize exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws DAVStatusException the DAV status exception
     */
    abstract protected Element propfindInternal(Element property)
            throws SQLException, AuthorizeException, IOException,
            DAVStatusException;

    /**
     * Gets the all properties.
     * 
     * @return list of all properties that resource wants known to a "propname"
     * request.
     */
    abstract protected List getAllProperties();

    /**
     * Execute a PROPPATCH method request on this Resource, and insert the
     * results into the <code>multistatus</code> XML element.
     * 
     * @param action either SET or REMOVE, taken from PROPERTYUPDATE element.
     * @param prop the PROP element from the request structure.
     * 
     * @return HTTP extended status code, e.g. 200 for success.
     * 
     * @throws SQLException the SQL exception
     * @throws AuthorizeException the authorize exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws DAVStatusException the DAV status exception
     */
    abstract protected int proppatchInternal(int action, Element prop)
            throws SQLException, AuthorizeException, IOException,
            DAVStatusException;

    /**
     * Output the GET method results for the resource to the response, should
     * include content-type and length headers.
     * 
     * @throws SQLException the SQL exception
     * @throws AuthorizeException the authorize exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws ServletException the servlet exception
     * @throws DAVStatusException the DAV status exception
     */
    abstract protected void get() throws SQLException, AuthorizeException,
            ServletException, IOException, DAVStatusException;

    /**
     * Create a new resource out of the contents of this request. For example,
     * add a new Item under an existing Collection. Might also replace a
     * Bitstream (or install a new version).
     * 
     * @throws SQLException the SQL exception
     * @throws AuthorizeException the authorize exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws ServletException the servlet exception
     * @throws DAVStatusException the DAV status exception
     */
    abstract protected void put() throws SQLException, AuthorizeException,
            ServletException, IOException, DAVStatusException;

    /**
     * Create a copy of a DAV resource under a different DAV "collection"
     * resource, within the limits of DSpace semantics. Use this to install an
     * Item in an additional Collection, for example. NOTE: copyInternal SHOULD
     * only return success status codes; throw a DAVStatusException to indicate
     * an error so the descriptive text can also be included.
     * 
     * @param destination the destination
     * @param depth the depth
     * @param overwrite the overwrite
     * @param keepProperties the keep properties
     * 
     * @return HTTP status code (201 or 204 for success)
     * 
     * @throws SQLException the SQL exception
     * @throws AuthorizeException the authorize exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws DAVStatusException the DAV status exception
     */
    abstract protected int copyInternal(DAVResource destination, int depth,
            boolean overwrite, boolean keepProperties)
            throws DAVStatusException, SQLException, AuthorizeException,
            IOException;

    /**
     * Return value of dspace:type property, e.g. an empty element with the tag
     * "dspace:item".
     * 
     * @return JDOM Element to put in PROPFIND result as value of "dspace:type"
     */
    protected abstract Element typeValue();

    /**
     * Execute a DELETE method request on this Resource. Inserts nothing into
     * the <code>multistatus</code> XML element.
     * 
     * @return HTTP extended status code, e.g. 200 for success.
     * 
     * @throws SQLException the SQL exception
     * @throws AuthorizeException the authorize exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws DAVStatusException the DAV status exception
     */
    abstract protected int deleteInternal() throws SQLException,
            AuthorizeException, IOException, DAVStatusException;

    /**
     * Execute a MKCOL method request on this Resource. Inserts nothing into the
     * <code>multistatus</code> XML element. Only makes sense to do a MKCOL
     * operation on Communities and Collections.
     * 
     * @param name the name
     * 
     * @return HTTP extended status code, e.g. 200 for success.
     * 
     * @throws SQLException the SQL exception
     * @throws AuthorizeException the authorize exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws DAVStatusException the DAV status exception
     */
    abstract protected int mkcolInternal(String name) throws SQLException,
            AuthorizeException, IOException, DAVStatusException;

    /*----------------- Interpreting Resource URIs -----------------------*/

    /**
     * Get resource named by a DAV URI. The URI can be relative (from the Site)
     * or absolute if it starts with the WebDAV server's prefix.
     * 
     * @param uri the uri
     * 
     * @return resource object or null upon error.
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws SQLException the SQL exception
     * @throws DAVStatusException the DAV status exception
     * @throws AuthorizeException the authorize exception
     */
    protected DAVResource URIToResource(String uri) throws IOException,
            SQLException, DAVStatusException, AuthorizeException
    {
        String dest = uri;
        String prefix = hrefPrefix();
        if (dest.startsWith(prefix))
        {
            dest = dest.substring(prefix.length());
        }
        if (dest.startsWith("/"))
        {
            dest = dest.substring(1);
        }
        return findResource(this.context, this.request, this.response, dest.split("/"));
    }

    /**
     * Creates a resource object corresponding to the path.
     * 
     * @param context the context
     * @param request the request
     * @param response the response
     * @param pathElt the path elt
     * 
     * @return resource, or null if path cannot be interpreted. Sends HTTP
     * status in the event of failure.
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws SQLException the SQL exception
     * @throws DAVStatusException the DAV status exception
     * @throws AuthorizeException the authorize exception
     */
    protected static DAVResource findResource(Context context,
            HttpServletRequest request, HttpServletResponse response,
            String pathElt[]) throws IOException, SQLException,
            DAVStatusException, AuthorizeException
    {
        DAVResource result = null;

        if ((result = DAVSite.matchResourceURI(context, request, response,
                pathElt)) == null
                && (result = DAVLookup.matchResourceURI(context, request,
                        response, pathElt)) == null
                && (result = DAVWorkspace.matchResourceURI(context, request,
                        response, pathElt)) == null
                && (result = DAVWorkflow.matchResourceURI(context, request,
                        response, pathElt)) == null
                && (result = DAVEPerson.matchResourceURI(context, request,
                        response, pathElt)) == null
                && (result = DAVItem.matchResourceURI(context, request,
                        response, pathElt)) == null
                && (result = DAVBitstream.matchResourceURI(context, request,
                        response, pathElt)) == null
                && (result = DAVDSpaceObject.matchResourceURI(context, request,
                        response, pathElt)) == null)
        {
            throw new DAVStatusException(HttpServletResponse.SC_BAD_REQUEST,
                    "Unrecognized DSpace resource URI");
        }
        return result;
    }

    /*----------------- Generating Resource URIs -----------------------*/

    /**
     * Construct URL prefix up to the resource path, for making up "href" values
     * in e.g. PROPFIND responses.
     * 
     * @return start of resource URI ending with a '/'.
     */
    protected String hrefPrefix()
    {
        // Note: contextPath and servletPath include leading slashes but no
        // trailing..
        if (this.request == null)
        {
            return "/";
        }
        else
        {
            return this.request.getScheme() + "://" + this.request.getServerName() + ":"
                    + String.valueOf(this.request.getServerPort())
                    + this.request.getContextPath() + this.request.getServletPath() + "/";
        }
    }

    /**
     * Construct the whole absolute URL to resource - prefix plus path elements.
     * 
     * @return the string
     */
    protected String hrefURL()
    {
        StringBuffer result = new StringBuffer(hrefPrefix());
        for (int i = 0; i < this.pathElt.length; ++i)
        {
            if (i + 1 < this.pathElt.length)
            {
                result.append(this.pathElt[i] + "/");
            }
            else
            {
                result.append(this.pathElt[i]);
            }
        }

        return result.toString();
    }

    /**
     * Href to E person.
     * 
     * @param ep the ep
     * 
     * @return fully-qualfied URL to resource of given EPerson.
     */
    protected String hrefToEPerson(EPerson ep)
    {
        return hrefPrefix() + DAVEPerson.getPath(ep);
    }

    /**
     * Construct path to child DSpaceObject.
     * 
     * @param child the child
     * 
     * @return the string[]
     */
    protected String[] makeChildPath(DSpaceObject child)
    {
        String bpath[] = makeChildPathInternal();
        bpath[this.pathElt.length] = DAVDSpaceObject.getPathElt(child);
        return bpath;
    }

    /**
     * Construct path to child with last element predetermined.
     * 
     * @param lastElt the last elt
     * 
     * @return the string[]
     */
    protected String[] makeChildPath(String lastElt)
    {
        String bpath[] = makeChildPathInternal();
        bpath[this.pathElt.length] = lastElt;
        return bpath;
    }

    // actaul work of building a child path.
    /**
     * Make child path internal.
     * 
     * @return the string[]
     */
    private String[] makeChildPathInternal()
    {
        String bpath[] = new String[this.pathElt.length + 1];
        for (int k = 0; k < this.pathElt.length; ++k)
        {
            bpath[k] = this.pathElt[k];
        }
        return bpath;
    }

    /*----------------- Handling DAV Requests ------------------*/

    /**
     * PROPFIND method service: Collect parameters and launch the recursive
     * generic propfind driver which in turn calls resource methods.
     * 
     * @throws ServletException the servlet exception
     * @throws SQLException the SQL exception
     * @throws AuthorizeException the authorize exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws DAVStatusException the DAV status exception
     */
    protected void propfind() throws ServletException, SQLException,
            AuthorizeException, IOException, DAVStatusException
    {
        // set all incoming encoding to UTF-8
        this.request.setCharacterEncoding("UTF-8");

        /*
         * FIXME:(?) this is technically wrong, wrt. WebDAV protocol, but it's
         * more efficient; default depth should be DAV.DAV_INFINITY; we cheat
         * and make it 0.
         */
        int depth = 0;

        String sdepth = this.request.getHeader("Depth");
        if (sdepth != null)
        {
            sdepth = sdepth.trim();
            try
            {
                if (sdepth.equalsIgnoreCase("infinity"))
                {
                    depth = DAV.DAV_INFINITY;
                }
                else
                {
                    depth = Integer.parseInt(sdepth);
                }
            }
            catch (NumberFormatException nfe)
            {
                throw new DAVStatusException(
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Bad Depth header: " + sdepth);
            }
        }

        // get object-type mask from request query args, e.g.
        // type=ITEM,type=BITSTREAM ...
        String types[] = this.request.getParameterValues("type");
        int typeMask = typesToMask(types);

        Document outdoc = propfindDriver(depth, this.request.getInputStream(),
                typeMask);
        if (outdoc != null)
        {
            this.response.setStatus(DAV.SC_MULTISTATUS);
            this.response.setContentType("text/xml");

            outputRaw.output(outdoc, this.response.getOutputStream());

            if (debugXML)
            {
                log.debug("PROPFIND response = "
                        + outputPretty.outputString(outdoc));
            }
        }
    }

    /**
     * Inner logic for propfind. Shared with SOAP servlet
     * 
     * @param depth the depth
     * @param pfDoc the pf doc
     * @param typeMask the type mask
     * 
     * @return the document
     * 
     * @throws SQLException the SQL exception
     * @throws AuthorizeException the authorize exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws DAVStatusException the DAV status exception
     */
    protected Document propfindDriver(int depth, InputStream pfDoc, int typeMask)
            throws SQLException, AuthorizeException, IOException,
            DAVStatusException
    {
        // When there is no document, type defaults to <allprop>.
        int pfType = DAV.PROPFIND_ALLPROP;
        List pfProps = null;

        try
        {
            SAXBuilder builder = new SAXBuilder();
            Document reqdoc = builder.build(pfDoc);
            Element propfind = reqdoc.getRootElement();
            if (!propfind.getName().equals("propfind")
                    || !propfind.getNamespace().equals(DAV.NS_DAV))
            {
                throw new DAVStatusException(
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Bad Root Element, must be propfind in DAV:");
            }

            // Propfind child is be ONE of: allprop | propname | prop
            // if "prop", also get list of type names.
            List pfChild = propfind.getChildren();
            if (pfChild.size() > 0)
            {
                Element child0 = (Element) pfChild.get(0);
                String rawType = child0.getName();
                if (rawType.equalsIgnoreCase("prop"))
                {
                    pfType = DAV.PROPFIND_PROP;
                    pfProps = child0.getChildren();
                }
                else if (rawType.equalsIgnoreCase("allprop"))
                {
                    pfType = DAV.PROPFIND_ALLPROP;
                }
                else if (rawType.equalsIgnoreCase("propname"))
                {
                    pfType = DAV.PROPFIND_PROPNAME;
                }
                else
                {
                    log.warn(LogManager.getHeader(this.context, "propfind",
                            "Unknown TYPE child of <propfind>, named: \""
                                    + rawType + "\""));
                }
            }

            if (debugXML)
            {
                log.debug("PROPFIND request = "
                        + outputPretty.outputString(reqdoc));
            }
        }
        catch (JDOMParseException je)
        {
            // if there is no document we get error at line -1, so let it pass.
            if (je.getLineNumber() >= 0)
            {
                log.error(LogManager.getHeader(this.context, "propfind", je
                        .toString()));
                throw new DAVStatusException(
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Could not parse request document: " + je.toString());
            }
        }
        catch (JDOMException je)
        {
            log.error(LogManager.getHeader(this.context, "propfind", je.toString()));
            throw new DAVStatusException(HttpServletResponse.SC_BAD_REQUEST,
                    "Could not parse request document: " + je.toString());
        }

        // At this point, pfProps, pfType and URI define the whole request.

        // Construct response XML
        Element multistatus = new Element("multistatus", DAV.NS_DAV);
        Document outdoc = new Document(multistatus);

        propfindCrawler(multistatus, pfType, pfProps, depth, typeMask, 0);
        return outdoc;
    }

    // Recursive propfind driver: each call to
    // resource.propfindInternal accumulates <response>s in multistatus.
    /**
     * Propfind crawler.
     * 
     * @param multistatus the multistatus
     * @param pfType the pf type
     * @param pfProps the pf props
     * @param depth the depth
     * @param typeMask the type mask
     * @param count the count
     * 
     * @return the int
     * 
     * @throws DAVStatusException the DAV status exception
     * @throws SQLException the SQL exception
     * @throws AuthorizeException the authorize exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private int propfindCrawler(Element multistatus, int pfType, List pfProps,
            int depth, int typeMask, int count) throws DAVStatusException,
            SQLException, AuthorizeException, IOException
    {
        if ((this.type & typeMask) != 0 || this.type == TYPE_OTHER)
        {
            // check the count of resources visited
            ++count;
            if (propfindResourceLimit > 0 && count > propfindResourceLimit)
            {
                throw new DAVStatusException(HttpServletResponse.SC_CONFLICT,
                        "PROPFIND request exceeded server's limit on number of resources to query.");
            }

            String uri = hrefURL();
            log.debug("PROPFIND returning (count=" + String.valueOf(count)
                    + ") href=" + uri);
            Vector notFound = new Vector();
            Vector forbidden = new Vector();
            Element responseElt = new Element("response", DAV.NS_DAV);
            multistatus.addContent(responseElt);

            Element href = new Element("href", DAV.NS_DAV);
            href.setText(uri);
            responseElt.addContent(href);

            // just get the names
            if (pfType == DAV.PROPFIND_PROPNAME)
            {
                responseElt.addContent(makePropstat(
                        copyElementList(getAllProperties()),
                        HttpServletResponse.SC_OK, "OK"));
            }
            else
            {
                List success = new LinkedList();
                List props = (pfType == DAV.PROPFIND_ALLPROP) ? getAllProperties()
                        : pfProps;
                ListIterator pi = props.listIterator();
                while (pi.hasNext())
                {
                    Element property = (Element) pi.next();
                    try
                    {
                        Element value = propfindInternal(property);
                        if (value != null)
                        {
                            success.add(value);
                        }
                        else
                        {
                            notFound.add((Element) property.clone());
                        }
                    }
                    catch (DAVStatusException se)
                    {
                        if (se.getStatus() == HttpServletResponse.SC_NOT_FOUND)
                        {
                            notFound.add((Element) property.clone());
                        }
                        else
                        {
                            responseElt.addContent(makePropstat(
                                    (Element) property.clone(), se.getStatus(),
                                    se.getMessage()));
                        }
                    }
                    catch (AuthorizeException ae)
                    {
                        forbidden.add((Element) property.clone());
                    }
                }

                if (success.size() > 0)
                {
                    responseElt.addContent(makePropstat(success,
                            HttpServletResponse.SC_OK, "OK"));
                }
                if (notFound.size() > 0)
                {
                    responseElt.addContent(makePropstat(notFound,
                            HttpServletResponse.SC_NOT_FOUND, "Not found"));
                }
                if (forbidden.size() > 0)
                {
                    responseElt
                            .addContent(makePropstat(forbidden,
                                    HttpServletResponse.SC_FORBIDDEN,
                                    "Not authorized."));
                }
            }
        }

        // recurse on children; filter on types first.
        // child types are all lower bits than this type, so (type - 1) is
        // bit-mask of all the possible child types. Skip recursion if
        // all child types would be masked out anyway.
        if (depth != 0 && ((this.type - 1) & typeMask) != 0)
        {
            DAVResource[] kids = children();
            for (DAVResource element : kids)
            {
                count = element.propfindCrawler(multistatus, pfType, pfProps,
                        depth == DAV.DAV_INFINITY ? depth : depth - 1,
                        typeMask, count);
            }
        }

        return count;
    }

    /**
     * Service routine for PROPPATCH method on a resource: Take apart the
     * PROPERTYUPDATE request document, call resource's proppatchInternal() for
     * each property, and accumulate the response document.
     * 
     * @throws ServletException the servlet exception
     * @throws SQLException the SQL exception
     * @throws AuthorizeException the authorize exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws DAVStatusException the DAV status exception
     */
    protected void proppatch() throws ServletException, SQLException,
            AuthorizeException, IOException, DAVStatusException
    {
        // set all incoming encoding to UTF-8
        this.request.setCharacterEncoding("UTF-8");

        Document outdoc = proppatchDriver(this.request.getInputStream());

        this.response.setStatus(DAV.SC_MULTISTATUS);
        this.response.setContentType("text/xml");
        outputRaw.output(outdoc, this.response.getOutputStream());

        if (debugXML)
        {
            log.debug("PROPPATCH response = "
                    + outputPretty.outputString(outdoc));
        }
    }

    /**
     * Inner logic for proppatch. Shared with SOAP servlet
     * 
     * @param docStream the doc stream
     * 
     * @return the document
     * 
     * @throws SQLException the SQL exception
     * @throws AuthorizeException the authorize exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws DAVStatusException the DAV status exception
     */
    protected Document proppatchDriver(InputStream docStream)
            throws SQLException, AuthorizeException, IOException,
            DAVStatusException
    {

        Document reqdoc = null;
        try
        {
            SAXBuilder builder = new SAXBuilder();
            reqdoc = builder.build(docStream);
        }
        catch (JDOMParseException je)
        {
            // if there is no document we get error at line -1, so let it pass.
            if (je.getLineNumber() >= 0)
            {
                log.error(LogManager.getHeader(this.context, "proppatch", je
                        .toString()));
                throw new DAVStatusException(
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Could not parse PROPERTYUPDATE request document: "
                                + je.toString());
            }
        }
        catch (JDOMException je)
        {
            log
                    .error(LogManager.getHeader(this.context, "proppatch", je
                            .toString()));
            throw new DAVStatusException(HttpServletResponse.SC_BAD_REQUEST,
                    "Could not parse PROPERTYUPDATE request document: "
                            + je.toString());
        }
        if (reqdoc == null)
        {
            throw new DAVStatusException(HttpServletResponse.SC_BAD_REQUEST,
                    "Failed to parse any valid PROPERTYUPDATE document in request.");
        }

        Element pupdate = reqdoc.getRootElement();
        if (!pupdate.getName().equals("propertyupdate")
                || !pupdate.getNamespace().equals(DAV.NS_DAV))
        {
            log.warn(LogManager.getHeader(this.context, "proppatch",
                    "Got bad root element, XML=" + pupdate.toString()));
            throw new DAVStatusException(HttpServletResponse.SC_BAD_REQUEST,
                    "Bad Root Element, must be propertyupdate in DAV:");
        }

        Element multistatus = new Element("multistatus", DAV.NS_DAV);
        Element msResponse = new Element("response", DAV.NS_DAV);
        multistatus.addContent(msResponse);
        Element href = new Element("href", DAV.NS_DAV);
        msResponse.addContent(href);
        href.addContent(hrefURL());

        // result status and accumulation:
        boolean failing = false;
        List failedDep = new LinkedList();
        List success = new LinkedList();

        // process the SET and REMOVE elements under PROPERTYUPDATE:
        ListIterator ci = pupdate.getChildren().listIterator();
        while (ci.hasNext())
        {
            int action = -1;
            Element e = (Element) ci.next();
            if (e.getName().equals("set")
                    && e.getNamespace().equals(DAV.NS_DAV))
            {
                action = DAV.PROPPATCH_SET;
            }
            else if (e.getName().equals("remove")
                    && e.getNamespace().equals(DAV.NS_DAV))
            {
                action = DAV.PROPPATCH_REMOVE;
            }
            else
            {
                log.warn(LogManager.getHeader(this.context, "proppatch",
                        "Got unrecognized PROPERTYUPDATE element:"
                                + e.toString()));
                continue;
            }
            // PROP elements under SET or REMOVE
            ListIterator pi = e.getChildren().listIterator();
            while (pi.hasNext())
            {
                Element p = (Element) pi.next();
                if (p.getName().equals("prop")
                        && p.getNamespace().equals(DAV.NS_DAV))
                {
                    ListIterator propi = p.getChildren().listIterator();
                    while (propi.hasNext())
                    {
                        Element thisprop = (Element) propi.next();

                        // if this PROPPATCH request is failing, just
                        // accumulate properties for Failed Dependency status
                        if (failing)
                        {
                            failedDep.add(new Element(thisprop.getName(),
                                    thisprop.getNamespace()));
                        }
                        else
                        {
                            int status = 0;
                            String statusMsg = null;
                            try
                            {
                                status = proppatchCommonInternal(action,
                                        thisprop);
                            }
                            catch (DAVStatusException se)
                            {
                                status = se.getStatus();
                                statusMsg = se.getMessage();
                            }
                            catch (AuthorizeException ae)
                            {
                                status = HttpServletResponse.SC_FORBIDDEN;
                                statusMsg = "Permission denied.";
                            }
                            if (status == HttpServletResponse.SC_OK)
                            {
                                success.add(new Element(thisprop.getName(),
                                        thisprop.getNamespace()));
                                log.debug("proppatch SET/REMOVE OK, action="
                                        + String.valueOf(action) + ", prop="
                                        + thisprop.toString());
                            }
                            else
                            {
                                failing = true;
                                msResponse.addContent(makePropstat(thisprop,
                                        status, statusMsg));
                                log
                                        .debug("proppatch SET/REMOVE FAILED with status="
                                                + String.valueOf(status)
                                                + ", on prop="
                                                + thisprop.toString());
                            }
                        }
                    }
                }
                else
                {
                    log.warn(LogManager.getHeader(this.context, "proppatch",
                            "No PROP element where expected, found:"
                                    + p.toString()));
                }
            }
        }

        // add success and failure propstat elements to response
        if (failing)
        {
            failedDep.addAll(success);
            if (failedDep.size() > 0)
            {
                msResponse.addContent(makePropstat(failedDep,
                        DAV.SC_FAILED_DEPENDENCY,
                        "Failed because another property failed."));
            }
            this.context.abort();
        }
        else
        {
            if (success.size() > 0)
            {
                msResponse.addContent(makePropstat(success,
                        HttpServletResponse.SC_OK, "OK"));
            }
            this.context.complete();
        }
        return new Document(multistatus);

    }

    // call proppatchInternal after taking care of "common" properties
    /**
     * Proppatch common internal.
     * 
     * @param action the action
     * @param prop the prop
     * 
     * @return the int
     * 
     * @throws SQLException the SQL exception
     * @throws AuthorizeException the authorize exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws DAVStatusException the DAV status exception
     */
    private int proppatchCommonInternal(int action, Element prop)
            throws SQLException, AuthorizeException, IOException,
            DAVStatusException
    {
        if (elementsEqualIsh(prop, resourcetypeProperty)
                || elementsEqualIsh(prop, typeProperty)
                || elementsEqualIsh(prop, current_user_privilege_setProperty))
        {
            throw new DAVStatusException(DAV.SC_CONFLICT, "The "
                    + prop.getName() + " property cannot be changed.");
        }
        else
        {
            return proppatchInternal(action, prop);
        }
    }

    /**
     * Returns value of one of the common properties. Returns null when property
     * isn't one of the common ones. Throws exception if property isn't
     * available (i.e. 404). <br>
     * NOTE: This MUST be called from the subclass's propfindInternal() method
     * so it be passed the content object as a DSpaceObject.
     * <p>
     * Although the displayname is common to all, it is computed differently by
     * each subclass so it's implemented there.
     * 
     * @param property the property
     * @param isCollection the is collection
     * 
     * @return the element
     * 
     * @throws DAVStatusException the DAV status exception
     * @throws SQLException the SQL exception
     */
    protected Element commonPropfindInternal(Element property,
            boolean isCollection) throws DAVStatusException, SQLException
    {
        // DSpace object type -- also a special case, see typeValue method.
        if (elementsEqualIsh(property, typeProperty))
        {
            Element p = (Element) typeProperty.clone();
            p.addContent(typeValue());
            return p;
        }
        // resourcetype -- special case, sub-element: collection or nothing
        else if (elementsEqualIsh(property, resourcetypeProperty))
        {
            Element p = (Element) resourcetypeProperty.clone();
            if (isCollection)
            {
                p.addContent(new Element("collection", DAV.NS_DAV));
            }
            return p;
        }

        // value is dspace:action element with allowable actions.
        else if (elementsEqualIsh(property, current_user_privilege_setProperty))
        {
            Element c = (Element) current_user_privilege_setProperty.clone();

            // if we're an admin we have all privs everywhere.
            if (AuthorizeManager.isAdmin(this.context))
            {
                addPrivilege(c, new Element("all", DAV.NS_DAV));
            }
            else
            {
                addPrivilege(c, new Element("read", DAV.NS_DAV));
            }

            return c;
        }
        else
        {
            return null;
        }
    }

    /**
     * Service routine for COPY HTTP request.
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws SQLException the SQL exception
     * @throws AuthorizeException the authorize exception
     * @throws DAVStatusException the DAV status exception
     */
    protected void copy() throws IOException, SQLException, AuthorizeException,
            DAVStatusException
    {
        // Destination arg from header
        String destination = this.request.getHeader("Destination");
        if (destination == null)
        {
            throw new DAVStatusException(HttpServletResponse.SC_BAD_REQUEST,
                    "Missing the required request header \"Destination\"");
        }

        // Fix a common misfeature in clients: they will append
        // the final pathname element of the "source" URI to the
        // "destination" URI, which is misleading in our URI scheme so
        // we have to strip it off:
        try
        {
            String srcPath = (new URI(this.request.getRequestURI())).getPath();
            String destPath = (new URI(destination)).getPath();
            int slash = srcPath.lastIndexOf("/");
            if (slash > -1)
            {
                String lastElt = srcPath.substring(slash);
                if (destPath.endsWith(lastElt))
                {
                    destination = destination.substring(0, destination.length()
                            - lastElt.length() + 1);
                }
            }
            log.debug("Copy dest. URI repair: srcPath=" + srcPath
                    + ", destPath=" + destPath + ", final dest=" + destination);
        }
        catch (URISyntaxException e)
        {
            throw new DAVStatusException(HttpServletResponse.SC_BAD_REQUEST,
                    "Illegal URI syntax in value of \"Destination\" header: "
                            + destination);
        }

        // Depth arg from header
        int depth = DAV.DAV_INFINITY;
        String sdepth = this.request.getHeader("Depth");
        if (sdepth != null)
        {
            sdepth = sdepth.trim();
            try
            {
                if (sdepth.equalsIgnoreCase("infinity"))
                {
                    depth = DAV.DAV_INFINITY;
                }
                else
                {
                    depth = Integer.parseInt(sdepth);
                }
            }
            catch (NumberFormatException nfe)
            {
                throw new DAVStatusException(
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Illegal value in Depth request header: " + sdepth);
            }
        }

        // overwrite header logic
        boolean overwrite = false;
        String soverwrite = this.request.getHeader("Overwrite");
        if (soverwrite != null && soverwrite.trim().equalsIgnoreCase("T"))
        {
            overwrite = true;
        }

        // keepProperties - extract from XML doc in request, if any..
        boolean keepProperties = false;
        Document reqdoc = null;
        try
        {
            SAXBuilder builder = new SAXBuilder();
            reqdoc = builder.build(this.request.getInputStream());
        }
        catch (JDOMParseException je)
        {
            // if there is no document we get error at line -1, so let it pass.
            if (je.getLineNumber() >= 0)
            {
                throw new DAVStatusException(
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Error parsing XML document in COPY request.");
            }
        }
        catch (JDOMException je)
        {
            throw new DAVStatusException(HttpServletResponse.SC_BAD_REQUEST,
                    "Error parsing XML document in COPY request: "
                            + je.toString());
        }
        if (reqdoc != null)
        {
            Element propertybehavior = reqdoc.getRootElement();
            Namespace ns = propertybehavior.getNamespace();
            if (!(ns != null && ns.equals(DAV.NS_DAV) && propertybehavior
                    .getName().equals("propertybehavior")))
            {
                throw new DAVStatusException(
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Illegal XML document in COPY request, root= "
                                + propertybehavior.toString());
            }

            // FIXME: (?) Punt on parsing exact list of properties to
            // "keepalive" since we don't implement it anyway.
            if (propertybehavior.getChild("keepalive", DAV.NS_DAV) != null)
            {
                keepProperties = true;
            }
            else if (propertybehavior.getChild("omit", DAV.NS_DAV) == null)
            {
                throw new DAVStatusException(
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Illegal propertybehavior document in COPY request, no omit or keepalive child.");
            }
        }

        int result = copyDriver(destination, depth, overwrite, keepProperties);
        if (result >= 200 && result < 300)
        {
            this.response.setStatus(result);
        }
        else
        {
            throw new DAVStatusException(result, "COPY Failed.");
        }
    }

    /**
     * "copy" driver gets parameters from request and calls resource's method.
     * This is shared with SOAP servelet.
     * 
     * @param destination the destination
     * @param depth the depth
     * @param overwrite the overwrite
     * @param keepProperties the keep properties
     * 
     * @return HTTP success status code (201 or 204 for success), Does not
     * return errors, but throws exception.
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws SQLException the SQL exception
     * @throws AuthorizeException the authorize exception
     * @throws DAVStatusException the DAV status exception
     */
    protected int copyDriver(String destination, int depth, boolean overwrite,
            boolean keepProperties) throws IOException, SQLException,
            AuthorizeException, DAVStatusException
    {
        DAVResource destResource = URIToResource(destination);
        if (destResource == null)
        {
            throw new DAVStatusException(HttpServletResponse.SC_BAD_REQUEST,
                    "Destination is not a legal DAV resource: " + destination);
        }

        log.debug("Executing COPY method, depth=" + String.valueOf(depth)
                + ", overwrite=" + String.valueOf(overwrite)
                + ", keepProperties=" + String.valueOf(keepProperties)
                + ", destination=\"" + destination + "\"");

        int result = copyInternal(destResource, depth, overwrite,
                keepProperties);
        this.context.commit();
        return result;
    }

    /**
     * Make bitmask out resource-type keywords. Used by DAV and SOAP interfaces.
     * 
     * @param types the types
     * 
     * @return binary mask of types to allow
     * 
     * @throws DAVStatusException if an unrecognized type keyword is given.
     */
    protected static int typesToMask(String types[]) throws DAVStatusException
    {
        int typeMask = 0;
        if (types == null || types.length == 0)
        {
            typeMask = TYPE_ALL;
        }
        else
            for (String element : types)
            {
                String key = element.trim();
                if (key.equalsIgnoreCase("SITE"))
                {
                    typeMask |= TYPE_SITE;
                }
                else if (key.equalsIgnoreCase("COMMUNITY"))
                {
                    typeMask |= TYPE_COMMUNITY;
                }
                else if (key.equalsIgnoreCase("COLLECTION"))
                {
                    typeMask |= TYPE_COLLECTION;
                }
                else if (key.equalsIgnoreCase("ITEM"))
                {
                    typeMask |= TYPE_ITEM;
                }
                else if (key.equalsIgnoreCase("BITSTREAM"))
                {
                    typeMask |= TYPE_BITSTREAM;
                }
                else
                {
                    throw new DAVStatusException(
                            HttpServletResponse.SC_BAD_REQUEST,
                            "Unrecognized type keyword: " + key);
                }
            }
        return typeMask;
    }

    /**
     * Predicate, test if name and namespace of Elements match.
     * 
     * @param a the a
     * @param b the b
     * 
     * @return true if elements have same name and namespace.
     */
    protected boolean elementsEqualIsh(Element a, Element b)
    {
        Namespace nsa = a.getNamespace();
        Namespace nsb = b.getNamespace();
        return a.getName().equals(b.getName())
                && (nsa == nsb || (nsa != null && nsb != null && nsa
                        .equals(nsb)));
    }

    // assemble a PROPSTAT element for response document.
    /**
     * Make propstat.
     * 
     * @param property the property
     * @param status the status
     * @param message the message
     * 
     * @return the element
     */
    private Element makePropstat(Element property, int status, String message)
    {
        Element ps = makePropstatInternal(status, message);
        Element p = new Element("prop", DAV.NS_DAV);
        Element pp = new Element(property.getName(), property.getNamespace());
        p.addContent(pp);
        ps.addContent(0, p);
        return ps;
    }

    /**
     * Make propstat.
     * 
     * @param properties the properties
     * @param status the status
     * @param message the message
     * 
     * @return the element
     */
    private static Element makePropstat(List properties, int status,
            String message)
    {
        Element ps = makePropstatInternal(status, message);
        Element p = new Element("prop", DAV.NS_DAV);
        p.addContent(properties);
        ps.addContent(0, p);
        return ps;
    }

    /**
     * Make propstat internal.
     * 
     * @param status the status
     * @param message the message
     * 
     * @return the element
     */
    private static Element makePropstatInternal(int status, String message)
    {
        Element ps = new Element("propstat", DAV.NS_DAV);
        Element s = new Element("status", DAV.NS_DAV);
        s.addContent("HTTP/1.1 " + String.valueOf(status) + " " + message);
        ps.addContent(s);
        return ps;
    }

    /*----------------- Utility Functions -----------------------*/

    /**
     * Translate DSpace authorization "action" code into a WebDAV ACL privilege
     * element; make the most obvious mappings for ones that can be represented
     * accurately and allocate DSpace-namespace elements for the rest.
     * 
     * @param action the action
     * 
     * @return DAV privilege element, or null when there is no mapping.
     */
    protected static Element actionToPrivilege(int action)
    {
        if (action == Constants.ADD)
        {
            return new Element("bind", DAV.NS_DAV);
        }
        else if (action == Constants.COLLECTION_ADMIN)
        {
            return new Element("collection_admin", DAV.NS_DSPACE);
        }
        else if (action == Constants.DEFAULT_BITSTREAM_READ)
        {
            return new Element("default_bitstream_read", DAV.NS_DSPACE);
        }
        else if (action == Constants.DEFAULT_ITEM_READ)
        {
            return new Element("default_item_read", DAV.NS_DSPACE);
        }
        else if (action == Constants.DELETE || action == Constants.REMOVE)
        {
            return new Element("unbind", DAV.NS_DAV);
        }
        else if (action == Constants.READ)
        {
            return new Element("read", DAV.NS_DAV);
        }
        else if (action == Constants.WORKFLOW_ABORT)
        {
            return new Element("workflow_abort", DAV.NS_DSPACE);
        }
        else if (action == Constants.WORKFLOW_STEP_1)
        {
            return new Element("workflow_step_1", DAV.NS_DSPACE);
        }
        else if (action == Constants.WORKFLOW_STEP_2)
        {
            return new Element("workflow_step_2", DAV.NS_DSPACE);
        }
        else if (action == Constants.WORKFLOW_STEP_3)
        {
            return new Element("workflow_step_3", DAV.NS_DSPACE);
        }
        else if (action == Constants.WRITE)
        {
            return new Element("write", DAV.NS_DAV);
        }
        else
        {
            return null;
        }
    }

    /**
     * Add a privilege element to property like current-user-privilege-set,
     * wrapped in &lt;privilege&gt; first.
     * 
     * @param prop the prop
     * @param thePriv the the priv
     */
    protected static void addPrivilege(Element prop, Element thePriv)
    {
        Element priv = new Element("privilege", DAV.NS_DAV);
        priv.addContent(thePriv);
        prop.addContent(priv);
    }

    // make a "deep" copy of list of empty elements so we can
    // add "all props" list to a propstat with impunity..
    /**
     * Copy element list.
     * 
     * @param el the el
     * 
     * @return the list
     */
    private static List copyElementList(List el)
    {
        Vector result = new Vector(el.size());
        ListIterator li = el.listIterator();
        while (li.hasNext())
        {
            result.add(((Element) li.next()).clone());
        }
        return result;
    }

    /**
     * Get canonical form of persistent identifier (Handle), but allow it to be
     * null. Canonical form is an URN or perhaps URL. For CNRI Handle System
     * handles, it is "hdl:" followed by handle.
     * 
     * @param handle object handle in bare form, e.g. "12345/xyz".
     * 
     * @return canonical form of handle, or null if arg was null.
     */
    protected static String canonicalizeHandle(String handle)
    {
        if (handle != null)
        {
            if (handle.startsWith("hdl:"))
            {
                return handle;
            }
            else
            {
                return "hdl:" + handle;
            }
        }
        else
        {
            return null;
        }
    }

    /**
     * Utility to filter out characters illegal XML characters when putting
     * something of random provenance into TEXT element.
     * <p>
     * See <a href="http://www.w3.org/TR/2004/REC-xml-20040204/#charsets">
     * http://www.w3.org/TR/2004/REC-xml-20040204/#charsets</a> for rules,
     * essentially, anything above 0x20 and 0x09 (\t, HT), 0x0a (\n, NL), 0x0d
     * (\r, CR).
     * <p>
     * FIXME: for now, just replace all control chars with '?' Maybe someday
     * attempt to do something more meaningful, once it's clear what that would
     * be.
     * 
     * @param in the in
     * 
     * @return the string
     */
    protected static String filterForXML(String in)
    {
        final Pattern illegals = Pattern
                .compile("[\\x00-\\x08\\x0b\\x0c\\x0e-\\x1f]");
        Matcher m = illegals.matcher(in);
        if (m.find())
        {
            return m.replaceAll("?");
        }
        else
        {
            return in;
        }
    }

    /**
     * Mostly sugar around catching the UnsupportedEncodingException.
     * 
     * @param pathFrag the path frag
     * 
     * @return URL-decoded version of an encoded handle
     */
    protected static String decodeHandle(String pathFrag)
    {
        try
        {
            String handle = URLDecoder.decode(pathFrag, "UTF-8");

            // XXX KLUDGE: WebDAV client cadaver double-encodes the %2f,
            // into "%252f" so look for a leftover %25 (== '%').
            if (pathFrag.indexOf("%25") >= 0)
            {
                handle = URLDecoder.decode(handle, "UTF-8");
            }

            return handle;
        }
        catch (java.io.UnsupportedEncodingException e)
        {
            return "";
        }
    }

    /**
     * Mostly sugar around catching the UnsupportedEncodingException.
     * 
     * @param handle the handle
     * 
     * @return URL-encoded version of handle
     */
    protected static String encodeHandle(String handle)
    {
        try
        {
            return java.net.URLEncoder.encode(handle, "UTF-8");
        }
        catch (java.io.UnsupportedEncodingException ue)
        {
            // highly unlikely.
        }
        return "";
    }

    /**
     * Service routine for DELETE method on a resource:.
     * 
     * @throws ServletException the servlet exception
     * @throws SQLException the SQL exception
     * @throws AuthorizeException the authorize exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws DAVStatusException the DAV status exception
     */
    protected void delete() throws ServletException, SQLException,
            AuthorizeException, IOException, DAVStatusException
    {
        // set all incoming encoding to UTF-8
        this.request.setCharacterEncoding("UTF-8");

        Document outdoc = deleteDriver(this.request.getInputStream());

        this.response.setStatus(DAV.SC_MULTISTATUS);
        this.response.setContentType("text/xml");
        outputRaw.output(outdoc, this.response.getOutputStream());

        if (debugXML)
        {
            log.debug("DELETE response = " + outputPretty.outputString(outdoc));
        }
    }

    /**
     * Inner logic for delete. Shared with SOAP servlet(??)
     * 
     * @param docStream the doc stream
     * 
     * @return the document
     * 
     * @throws SQLException the SQL exception
     * @throws AuthorizeException the authorize exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws DAVStatusException the DAV status exception
     */
    protected Document deleteDriver(InputStream docStream) throws SQLException,
            AuthorizeException, IOException, DAVStatusException
    {
        Document reqdoc = null;
        try
        {
            SAXBuilder builder = new SAXBuilder();
            reqdoc = builder.build(docStream);
        }
        catch (JDOMParseException je)
        {
            // if there is no document we get error at line -1, so let it pass.
            if (je.getLineNumber() >= 0)
            {
                log.error(LogManager
                        .getHeader(this.context, "delete", je.toString()));
                throw new DAVStatusException(
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Could not parse DELETE request document: "
                                + je.toString());
            }
        }
        catch (JDOMException je)
        {
            log.error(LogManager.getHeader(this.context, "delete", je.toString()));
            throw new DAVStatusException(HttpServletResponse.SC_BAD_REQUEST,
                    "Could not parse DELETE request document: " + je.toString());
        }
        if (reqdoc == null)
        {
            throw new DAVStatusException(HttpServletResponse.SC_BAD_REQUEST,
                    "Failed to parse any valid DELETE document in request.");
        }

        Element pupdate = reqdoc.getRootElement();
        if (!pupdate.getName().equals("delete"))
        {
            log.warn(LogManager.getHeader(this.context, "delete",
                    "Got bad root element, XML=" + pupdate.toString()));
            throw new DAVStatusException(HttpServletResponse.SC_BAD_REQUEST,
                    "Bad Root Element, must be delete");
        }

        deleteInternal();

        Element multistatus = new Element("multistatus", DAV.NS_DAV);
        Element msResponse = new Element("response", DAV.NS_DAV);
        multistatus.addContent(msResponse);
        Element href = new Element("href", DAV.NS_DAV);
        msResponse.addContent(href);
        href.addContent(hrefURL());
        return new Document(multistatus);
    }

    /**
     * Service routine for MKCOL method on a resource:.
     * 
     * @throws ServletException the servlet exception
     * @throws SQLException the SQL exception
     * @throws AuthorizeException the authorize exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws DAVStatusException the DAV status exception
     */
    protected void mkcol() throws ServletException, SQLException,
            AuthorizeException, IOException, DAVStatusException
    {
        // set all incoming encoding to UTF-8
        this.request.setCharacterEncoding("UTF-8");

        Document outdoc = mkcolDriver(this.request.getInputStream());

        this.response.setStatus(DAV.SC_MULTISTATUS);
        this.response.setContentType("text/xml");
        outputRaw.output(outdoc, this.response.getOutputStream());

        if (debugXML)
        {
            log.debug("MKCOL response = " + outputPretty.outputString(outdoc));
        }
    }

    /**
     * Inner logic for mkcol. Shared with SOAP servlet(??)
     * 
     * @param docStream the doc stream
     * 
     * @return the document
     * 
     * @throws SQLException the SQL exception
     * @throws AuthorizeException the authorize exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws DAVStatusException the DAV status exception
     */
    protected Document mkcolDriver(InputStream docStream) throws SQLException,
            AuthorizeException, IOException, DAVStatusException
    {
        Document reqdoc = null;
        try
        {
            SAXBuilder builder = new SAXBuilder();
            reqdoc = builder.build(docStream);
        }
        catch (JDOMParseException je)
        {
            // if there is no document we get error at line -1, so let it pass.
            if (je.getLineNumber() >= 0)
            {
                log
                        .error(LogManager.getHeader(this.context, "mkcol", je
                                .toString()));
                throw new DAVStatusException(
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Could not parse MKCOL request document: "
                                + je.toString());
            }
        }
        catch (JDOMException je)
        {
            log.error(LogManager.getHeader(this.context, "mkcol", je.toString()));
            throw new DAVStatusException(HttpServletResponse.SC_BAD_REQUEST,
                    "Could not parse MKCOL request document: " + je.toString());
        }
        if (reqdoc == null)
        {
            throw new DAVStatusException(HttpServletResponse.SC_BAD_REQUEST,
                    "Failed to parse any valid MKCOL document in request.");
        }

        Element pupdate = reqdoc.getRootElement();
        String newNodeName = null;
        if (!pupdate.getName().equals("mkcol")
                || (newNodeName = pupdate.getValue()) == null)
        {
            log.warn(LogManager.getHeader(this.context, "mkcol",
                    "Got bad root element, XML=" + pupdate.toString()));
            throw new DAVStatusException(HttpServletResponse.SC_BAD_REQUEST,
                    "Bad Root Element, must be mkcol");
        }
        mkcolInternal(newNodeName);

        Element multistatus = new Element("multistatus", DAV.NS_DAV);
        Element msResponse = new Element("response", DAV.NS_DAV);
        multistatus.addContent(msResponse);
        Element href = new Element("href", DAV.NS_DAV);
        msResponse.addContent(href);
        href.addContent(hrefURL());
        return new Document(multistatus);
    }
}
