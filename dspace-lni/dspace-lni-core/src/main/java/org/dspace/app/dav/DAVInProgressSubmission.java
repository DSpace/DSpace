/*
 * DAVInProgressSubmission.java
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
import java.sql.SQLException;
import java.util.List;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.jdom.Element;


/**
 * Superclass of the two kinds of resources that implement DSpace
 * InProgressSubmission objects, namely WorkspaceItem and WorkflowItem. It
 * coalesces their common code.
 * <p>
 * 
 * @author Larry Stone
 * @see DAVWorkspaceItem
 * @see DAVWorkflowItem
 */
abstract class DAVInProgressSubmission extends DAVResource
{
    
    /** DSpace object which this resource represents. */
    protected InProgressSubmission inProgressItem = null;

    /** The Constant collectionProperty. */
    protected static final Element collectionProperty = new Element(
            "collection", DAV.NS_DSPACE);

    /** The Constant submitterProperty. */
    protected static final Element submitterProperty = new Element("submitter",
            DAV.NS_DSPACE);

    /** The Constant has_multiple_filesProperty. */
    protected static final Element has_multiple_filesProperty = new Element(
            "has_multiple_files", DAV.NS_DSPACE);

    /** The Constant has_multiple_titlesProperty. */
    protected static final Element has_multiple_titlesProperty = new Element(
            "has_multiple_titles", DAV.NS_DSPACE);

    /** The Constant is_published_beforeProperty. */
    protected static final Element is_published_beforeProperty = new Element(
            "is_published_before", DAV.NS_DSPACE);

    /** State of in-progress item. Defined here so workflow items and workspace items can both get at it for PROPPATCH; but don't allow it in the list of common properties. since it's write-only for DAVWorkspaceItem. */
    protected static final Element stateProperty = new Element("state",
            DAV.NS_DSPACE);

    /** Commonly visible properties. */
    protected static List inProgressProps = new Vector(9);
    static
    {
        inProgressProps.add(current_user_privilege_setProperty);
        inProgressProps.add(displaynameProperty);
        inProgressProps.add(resourcetypeProperty);
        inProgressProps.add(typeProperty);
        inProgressProps.add(collectionProperty);
        inProgressProps.add(submitterProperty);
        inProgressProps.add(has_multiple_filesProperty);
        inProgressProps.add(has_multiple_titlesProperty);
        inProgressProps.add(is_published_beforeProperty);
    }

    /**
     * Instantiates a new DAV in progress submission.
     * 
     * @param context the context
     * @param request the request
     * @param response the response
     * @param pathElt the path elt
     * @param wi the wi
     */
    protected DAVInProgressSubmission(Context context,
            HttpServletRequest request, HttpServletResponse response,
            String pathElt[], InProgressSubmission wi)
    {
        super(context, request, response, pathElt);
        this.inProgressItem = wi;
        this.type = TYPE_OTHER;
    }

    /**
     * The only child is the Item this wraps:.
     * 
     * @return the DAV resource[]
     * 
     * @throws SQLException the SQL exception
     */
    @Override
    protected DAVResource[] children() throws SQLException
    {
        DAVResource result[] = new DAVResource[1];
        Item item = this.inProgressItem.getItem();
        result[0] = new DAVItem(this.context, this.request, this.response,
                makeChildPath(DAVItem.getPathElt(item.getID())), item);
        return result;
    }

    /**
     * Typically overridden by subclass which then calls back via "super";.
     * 
     * @param property the property
     * 
     * @return null if nothing matched so subclass has a chance.
     * 
     * @throws SQLException the SQL exception
     * @throws AuthorizeException the authorize exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws DAVStatusException the DAV status exception
     */
    @Override
    protected Element propfindInternal(Element property) throws SQLException,
            AuthorizeException, IOException, DAVStatusException
    {
        String value = null;

        if (elementsEqualIsh(property, collectionProperty))
        {
            value = canonicalizeHandle(this.inProgressItem.getCollection()
                    .getHandle());
        }
        else if (elementsEqualIsh(property, submitterProperty))
        {
            EPerson ep = this.inProgressItem.getSubmitter();
            if (ep != null)
            {
                value = hrefToEPerson(ep);
            }
        }
        else if (elementsEqualIsh(property, has_multiple_filesProperty))
        {
            value = String.valueOf(this.inProgressItem.hasMultipleFiles());
        }
        else if (elementsEqualIsh(property, has_multiple_titlesProperty))
        {
            value = String.valueOf(this.inProgressItem.hasMultipleTitles());
        }
        else if (elementsEqualIsh(property, is_published_beforeProperty))
        {
            value = String.valueOf(this.inProgressItem.isPublishedBefore());
        }
        else if (elementsEqualIsh(property, current_user_privilege_setProperty))
        {
            // if we see a WFI/WSI, we are the owner and have all privs:
            Element c = (Element) current_user_privilege_setProperty.clone();
            addPrivilege(c, new Element("all", DAV.NS_DAV));
            return c;
        }
        else
        {
            return commonPropfindInternal(property, true);
        }

        // value was set up by "if" clause:
        if (value == null)
        {
            throw new DAVStatusException(HttpServletResponse.SC_NOT_FOUND,
                    "Not found.");
        }
        Element p = new Element(property.getName(), property.getNamespace());
        p.setText(filterForXML(value));
        return p;
    }

    /**
     * Parses the boolean.
     * 
     * @param in the in
     * 
     * @return true, if successful
     * 
     * @throws DAVStatusException the DAV status exception
     */
    static boolean parseBoolean(String in) throws DAVStatusException
    {
        in = in.trim();
        if (in.equalsIgnoreCase("true"))
        {
            return true;
        }
        else if (in.equalsIgnoreCase("false"))
        {
            return false;
        }
        throw new DAVStatusException(DAV.SC_CONFLICT,
                "Unacceptable value for boolean: " + in);
    }

    /**
     * Since this is in a superclass, subclass must call it first and return if
     * it answers SC_OK. Otherwise, subclass gets a chance to set a property.
     * 
     * @param action the action
     * @param prop the prop
     * 
     * @return HTTP status - SC_OK means it set something, SC_NOT_FOUND if no
     * property was matched.
     * 
     * @throws DAVStatusException when property cannot be changed.
     * @throws SQLException the SQL exception
     * @throws AuthorizeException the authorize exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    protected int proppatchInternal(int action, Element prop)
            throws SQLException, AuthorizeException, IOException,
            DAVStatusException
    {
        if (elementsEqualIsh(prop, displaynameProperty)
                || elementsEqualIsh(prop, resourcetypeProperty)
                || elementsEqualIsh(prop, typeProperty)
                || elementsEqualIsh(prop, collectionProperty)
                || elementsEqualIsh(prop, submitterProperty))
        {
            throw new DAVStatusException(DAV.SC_CONFLICT, "The "
                    + prop.getName() + " property cannot be changed.");
        }

        String newValue = (action == DAV.PROPPATCH_REMOVE) ? null : prop
                .getText();
        if (elementsEqualIsh(prop, has_multiple_filesProperty))
        {
            this.inProgressItem.setMultipleFiles(parseBoolean(newValue));
        }
        else if (elementsEqualIsh(prop, has_multiple_titlesProperty))
        {
            this.inProgressItem.setMultipleTitles(parseBoolean(newValue));
        }
        else if (elementsEqualIsh(prop, is_published_beforeProperty))
        {
            this.inProgressItem.setPublishedBefore(parseBoolean(newValue));
        }
        else
        {
            return HttpServletResponse.SC_NOT_FOUND;
        }

        this.inProgressItem.update();
        return HttpServletResponse.SC_OK;
    }

    /* (non-Javadoc)
     * @see org.dspace.app.dav.DAVResource#get()
     */
    @Override
    protected void get() throws SQLException, AuthorizeException,
            ServletException, IOException, DAVStatusException
    {
        throw new DAVStatusException(HttpServletResponse.SC_NOT_IMPLEMENTED,
                "GET method not implemented for workspace.");
    }

    /* (non-Javadoc)
     * @see org.dspace.app.dav.DAVResource#put()
     */
    @Override
    protected void put() throws SQLException, AuthorizeException,
            ServletException, IOException, DAVStatusException
    {
        throw new DAVStatusException(HttpServletResponse.SC_NOT_IMPLEMENTED,
                "PUT method not implemented for workspace.");
    }

    /**
     * Implement a "copy" into a collection by adding the child Item to the
     * Collection -- this isn't strictly what they asked for, but it *is* what
     * they want, so DWIM here.
     * 
     * @param destination the destination
     * @param depth the depth
     * @param overwrite the overwrite
     * @param keepProperties the keep properties
     * 
     * @return the int
     * 
     * @throws DAVStatusException the DAV status exception
     * @throws SQLException the SQL exception
     * @throws AuthorizeException the authorize exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    protected int copyInternal(DAVResource destination, int depth,
            boolean overwrite, boolean keepProperties)
            throws DAVStatusException, SQLException, AuthorizeException,
            IOException
    {
        return DAVItem.addItemToCollection(this.context, this.inProgressItem.getItem(),
                destination, overwrite);
    }

    /* (non-Javadoc)
     * @see org.dspace.app.dav.DAVResource#deleteInternal()
     */
    @Override
    protected int deleteInternal() throws DAVStatusException, SQLException,
            AuthorizeException, IOException
    {
        throw new DAVStatusException(HttpServletResponse.SC_NOT_IMPLEMENTED,
                "DELETE method not implemented for InProgressSubmission.");
    }

    /* (non-Javadoc)
     * @see org.dspace.app.dav.DAVResource#mkcolInternal(java.lang.String)
     */
    @Override
    protected int mkcolInternal(String waste) throws DAVStatusException,
            SQLException, AuthorizeException, IOException
    {
        throw new DAVStatusException(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                "MKCOL method not allowed for InProgressSubmission.");
    }
}
