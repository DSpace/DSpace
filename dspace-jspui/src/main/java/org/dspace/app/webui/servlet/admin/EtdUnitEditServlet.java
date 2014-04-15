/*
 * Copyright (c) 2009 The University of Maryland. All Rights Reserved.
 * 
 */

package org.dspace.app.webui.servlet.admin;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.content.Collection;
import org.dspace.content.EtdUnit;
import org.dspace.eperson.EPerson;

/**
 * Servlet for editing etdunits
 * 
 * @author Ben Wallberg
 */
public class EtdUnitEditServlet extends DSpaceServlet
{
    protected void doDSGet(Context c, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        doDSPost(c, request, response);
    }

    protected void doDSPost(Context c, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        // Find out if there's a etdunit parameter
        int etdunitID = UIUtil.getIntParameter(request, "etdunit_id");
        EtdUnit etdunit = null;

        if (etdunitID >= 0)
        {
            etdunit = EtdUnit.find(c, etdunitID);
        }

        // etdunit is set
        if (etdunit != null)
        {
            boolean submit_edit = (request.getParameter("submit_edit") != null);
            boolean submit_etdunit_update = (request.getParameter("submit_etdunit_update") != null);
            boolean submit_etdunit_delete = (request.getParameter("submit_etdunit_delete") != null);
            boolean submit_confirm_delete = (request.getParameter("submit_confirm_delete") != null);
            boolean submit_cancel_delete = (request.getParameter("submit_cancel_delete") != null);


            // just chosen a etdunit to edit - get etdunit and pass it to
            // etdunit-edit.jsp
            if (submit_edit && !submit_etdunit_update && !submit_etdunit_delete)
            {
                request.setAttribute("etdunit", etdunit);
                request.setAttribute("collections", etdunit.getCollections());

                JSPManager.showJSP(request, response, "/tools/etdunit-edit.jsp");
            }
            else if (submit_etdunit_update)
            {
                // first off, did we change the etdunit name?
                String newName = request.getParameter("etdunit_name");

                if (!newName.equals(etdunit.getName()))
                {
                    etdunit.setName(newName);
                    etdunit.update();
                }

                int[] collection_ids = UIUtil.getIntParameters(request, "collection_ids");

                // get set of old collections
                HashSet collectionsOld = new HashSet(Arrays.asList(etdunit.getCollections()));

                if (collection_ids != null)
                {
                  // get set of new collections
                  HashSet collectionsNew = new HashSet();

                  for (int x = 0; x < collection_ids.length; x++) {
                    collectionsNew.add(Collection.find(c, collection_ids[x]));
                  }
                    
                  // add new collections
                  HashSet collectionsNewOnly = (HashSet)collectionsNew.clone();
                  collectionsNewOnly.removeAll(collectionsOld);
                    
                  for (Iterator i = collectionsNewOnly.iterator(); i.hasNext(); ) {
                    etdunit.addCollection((Collection)i.next());
                  }

                  // remove old collections
                  HashSet collectionsOldOnly = (HashSet)collectionsOld.clone();
                  collectionsOldOnly.removeAll(collectionsNew);
                    
                  for (Iterator i = collectionsOldOnly.iterator(); i.hasNext(); ) {
                    etdunit.removeCollection((Collection)i.next());
                  }
                }
                else
                {
                  // no collections submitted, remove all collections
                  for (Iterator i = collectionsOld.iterator(); i.hasNext(); ) {
                    etdunit.removeCollection((Collection)i.next());
                  }
                }

                request.setAttribute("etdunit", etdunit);
                request.setAttribute("collections", etdunit.getCollections());

                JSPManager.showJSP(request, response, "/tools/etdunit-edit.jsp");
                c.complete();
            }
            else if (submit_etdunit_delete)
            {
                // direct to a confirmation step
                request.setAttribute("etdunit", etdunit);
                JSPManager.showJSP(request, response, "/dspace-admin/etdunit-confirm-delete.jsp");
            }
            else if (submit_confirm_delete)
            {
                // delete etdunit, return to etdunit-list.jsp
                etdunit.delete();

                showMainPage(c, request, response);
            }
            else if (submit_cancel_delete)
            {
                // show etdunit list
                showMainPage(c, request, response);
            }
            else
            {
                // unknown action, show edit page
                request.setAttribute("etdunit", etdunit);

                JSPManager.showJSP(request, response, "/tools/etdunit-edit.jsp");
            }
        }
        else
        // no etdunit set
        {
            // want to add a etdunit - create a blank one, and pass to
            // etdunit_edit.jsp
            String button = UIUtil.getSubmitButton(request, "submit");

            if (button.equals("submit_add"))
            {
                etdunit = EtdUnit.create(c);

                etdunit.setName("new etdunit" + etdunit.getID());
                etdunit.update();

                request.setAttribute("etdunit", etdunit);
                request.setAttribute("collections", etdunit.getCollections());

                JSPManager.showJSP(request, response, "/tools/etdunit-edit.jsp");
                c.complete();
            }
            else
            {
                // show the main page (select etdunits)
                showMainPage(c, request, response);
            }
        }
    }

    private void showMainPage(Context c, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        EtdUnit[] etdunits = EtdUnit.findAll(c, EtdUnit.NAME);

        request.setAttribute("etdunits", etdunits);

        JSPManager.showJSP(request, response, "/tools/etdunit-list.jsp");
        c.complete();
    }
}
