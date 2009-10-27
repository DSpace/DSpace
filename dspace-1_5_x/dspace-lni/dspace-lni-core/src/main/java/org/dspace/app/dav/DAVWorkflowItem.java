/*
 * DAVWorkflowItem.java
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.InProgressSubmission;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowManager;
import org.jdom.Element;


/**
 * Reflect the contents of a WorkflowItem object, which is mainly an in-progress
 * submission wrapper around an Item. Allow the client to read and set the slots
 * in the workflow item and explore its child Item.
 * <p>
 * WorkflowItem resources are reached through the Workflow resource, typically
 * with a path of /workflow_pool/wfi_db_{id}
 * <p>
 * 
 * @author Larry Stone
 * @see DAVInProgressSubmission
 * @see DAVWorkflow
 */
class DAVWorkflowItem extends DAVInProgressSubmission
{
    
    /** log4j category. */
    private static Logger log = Logger.getLogger(DAVWorkflowItem.class);

    /** The Constant ownerProperty. */
    private static final Element ownerProperty = new Element("owner",
            DAV.NS_DSPACE);

    /** The all props. */
    private static List allProps = new Vector(inProgressProps);
    static
    {
        allProps.add(ownerProperty);
        allProps.add(stateProperty);
    }

    /**
     * Instantiates a new DAV workflow item.
     * 
     * @param context the context
     * @param request the request
     * @param response the response
     * @param pathElt the path elt
     * @param wi the wi
     */
    protected DAVWorkflowItem(Context context, HttpServletRequest request,
            HttpServletResponse response, String pathElt[],
            InProgressSubmission wi)
    {
        super(context, request, response, pathElt, wi);
    }

    /* (non-Javadoc)
     * @see org.dspace.app.dav.DAVResource#getAllProperties()
     */
    @Override
    protected List getAllProperties()
    {
        return allProps;
    }

    /* (non-Javadoc)
     * @see org.dspace.app.dav.DAVResource#typeValue()
     */
    @Override
    protected Element typeValue()
    {
        return new Element("workflow-item", DAV.NS_DSPACE);
    }

    // format the final path element for one of these
    /**
     * Gets the path elt.
     * 
     * @param dbid the dbid
     * 
     * @return the path elt
     */
    protected static String getPathElt(int dbid)
    {
        return "wfi_db_" + String.valueOf(dbid);
    }

    /**
     * Gets the path elt.
     * 
     * @param wfi the wfi
     * 
     * @return the path elt
     */
    protected static String getPathElt(WorkflowItem wfi)
    {
        return getPathElt(wfi.getID());
    }

    /**
     * Match resource URI.
     * 
     * @param context the context
     * @param request the request
     * @param response the response
     * @param pathElt the path elt
     * 
     * @return the DAV resource
     * 
     * @throws DAVStatusException the DAV status exception
     * @throws SQLException the SQL exception
     */
    protected static DAVResource matchResourceURI(Context context,
            HttpServletRequest request, HttpServletResponse response,
            String pathElt[]) throws DAVStatusException, SQLException
    {
        try
        {
            // Match "/workflow/wfi_db_<id>" URI
            if (pathElt.length >= 2 && pathElt[0].startsWith("workflow_")
                    && pathElt[1].startsWith("wfi_db_"))
            {
                // match /workflow/wfi_db_<id>/item_db_<id> ...
                // should be an Item (or Bitstream) URI, child of this
                // WorkflowItem.
                if (pathElt.length >= 3)
                {
                    DAVResource result = DAVItem.matchResourceURI(context,
                            request, response, pathElt);
                    if (result == null)
                    {
                        throw new DAVStatusException(
                                HttpServletResponse.SC_NOT_FOUND,
                                "Invalid resource path.");
                    }
                    else
                    {
                        return result;
                    }
                }

                // get this WFI
                int id = Integer.parseInt(pathElt[1].substring(7));
                InProgressSubmission ips = WorkflowItem.find(context, id);
                if (ips == null)
                {
                    log.warn("invalid WorkflowItem DB ID in DAV URI, " + "id="
                            + pathElt[1]);
                    throw new DAVStatusException(
                            HttpServletResponse.SC_NOT_FOUND, "Not found: "
                                    + pathElt[1] + " does not exist.");
                }
                else
                {
                    return new DAVWorkflowItem(context, request, response,
                            pathElt, ips);
                }
            }
            return null;
        }
        catch (NumberFormatException ne)
        {
            throw new DAVStatusException(HttpServletResponse.SC_BAD_REQUEST,
                    "Error parsing number in request URI.");
        }
    }

    /* (non-Javadoc)
     * @see org.dspace.app.dav.DAVInProgressSubmission#propfindInternal(org.jdom.Element)
     */
    @Override
    protected Element propfindInternal(Element property) throws SQLException,
            AuthorizeException, IOException, DAVStatusException
    {
        Element result = super.propfindInternal(property);
        if (result != null)
        {
            return result;
        }

        String value = null;

        // displayname - title or handle.
        if (elementsEqualIsh(property, displaynameProperty))
        {
            value = getPathElt(this.inProgressItem.getID());
        }
        else if (elementsEqualIsh(property, ownerProperty))
        {
            EPerson ep = ((WorkflowItem) this.inProgressItem).getOwner();
            if (ep != null)
            {
                value = hrefToEPerson(ep);
            }
        }

        else if (elementsEqualIsh(property, stateProperty))
        {
            value = WorkflowManager.workflowText[((WorkflowItem) this.inProgressItem)
                    .getState()];
        }
        else
        {
            return super.propfindInternal(property);
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

    /* (non-Javadoc)
     * @see org.dspace.app.dav.DAVInProgressSubmission#proppatchInternal(int, org.jdom.Element)
     */
    @Override
    protected int proppatchInternal(int action, Element prop)
            throws SQLException, AuthorizeException, IOException,
            DAVStatusException
    {
        if (super.proppatchInternal(action, prop) == HttpServletResponse.SC_OK)
        {
            return HttpServletResponse.SC_OK;
        }
        else if (elementsEqualIsh(prop, stateProperty))
        {
            if (action == DAV.PROPPATCH_REMOVE)
            {
                throw new DAVStatusException(DAV.SC_CONFLICT,
                        "The state property cannot be removed.");
            }
            String key = prop.getTextTrim();
            int newState;

            if (key.equalsIgnoreCase("abort"))
            {
                WorkflowManager.abort(this.context, (WorkflowItem) this.inProgressItem,
                        this.inProgressItem.getSubmitter());
            }
            else if (key.equalsIgnoreCase("reject"))
            {
                EPerson cu = this.context.getCurrentUser();
                String who = cu == null ? "nobody" : cu.getFullName();
                WorkflowManager.reject(this.context, (WorkflowItem) this.inProgressItem,
                        this.inProgressItem.getSubmitter(), "Rejected by " + who
                                + ", via WebDAV Network Interface");

            }
            else if (key.equalsIgnoreCase("advance"))
            {
                WorkflowManager.advance(this.context, (WorkflowItem) this.inProgressItem,
                        this.context.getCurrentUser());
            }
            else if (key.equalsIgnoreCase("claim"))
            {
                WorkflowManager.claim(this.context, (WorkflowItem) this.inProgressItem,
                        this.context.getCurrentUser());
            }
            else if (key.equalsIgnoreCase("unclaim"))
            {
                WorkflowManager.unclaim(this.context, (WorkflowItem) this.inProgressItem,
                        this.context.getCurrentUser());
            }
            else if ((newState = WorkflowManager.getWorkflowID(key)) >= 0)
            {
                ((WorkflowItem) this.inProgressItem).setState(newState);
            }
            else
            {
                throw new DAVStatusException(DAV.SC_CONFLICT,
                        "Unrecognized verb or state-name in value for state property.");
            }

            this.inProgressItem.update();
            return HttpServletResponse.SC_OK;
        }
        throw new DAVStatusException(DAV.SC_CONFLICT, "The " + prop.getName()
                + " property cannot be changed.");
    }
}
