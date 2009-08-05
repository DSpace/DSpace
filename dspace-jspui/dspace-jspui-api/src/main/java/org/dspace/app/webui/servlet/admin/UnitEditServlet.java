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
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.Unit;

/**
 * Servlet for editing units
 * 
 * @author Ben Wallberg
 */
public class UnitEditServlet extends DSpaceServlet
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
        // Find out if there's a unit parameter
        int unitID = UIUtil.getIntParameter(request, "unit_id");
        Unit unit = null;

        if (unitID >= 0)
        {
            unit = Unit.find(c, unitID);
        }

        // unit is set
        if (unit != null)
        {
            boolean submit_edit = (request.getParameter("submit_edit") != null);
            boolean submit_unit_update = (request.getParameter("submit_unit_update") != null);
            boolean submit_unit_delete = (request.getParameter("submit_unit_delete") != null);
            boolean submit_confirm_delete = (request.getParameter("submit_confirm_delete") != null);
            boolean submit_cancel_delete = (request.getParameter("submit_cancel_delete") != null);


            // just chosen a unit to edit - get unit and pass it to
            // unit-edit.jsp
            if (submit_edit && !submit_unit_update && !submit_unit_delete)
            {
                request.setAttribute("unit", unit);

                JSPManager.showJSP(request, response, "/tools/unit-edit.jsp");
            }
            else if (submit_unit_update)
            {
                // first off, did we change the unit name?
                String newName = request.getParameter("unit_name");

                if (!newName.equals(unit.getName()))
                {
                    unit.setName(newName);
                    unit.update();
                }

                request.setAttribute("unit", unit);

                JSPManager.showJSP(request, response, "/tools/unit-edit.jsp");
                c.complete();
            }
            else if (submit_unit_delete)
            {
                // direct to a confirmation step
                request.setAttribute("unit", unit);
                JSPManager.showJSP(request, response, "/dspace-admin/unit-confirm-delete.jsp");
            }
            else if (submit_confirm_delete)
            {
                // delete unit, return to unit-list.jsp
                unit.delete();

                showMainPage(c, request, response);
            }
            else if (submit_cancel_delete)
            {
                // show unit list
                showMainPage(c, request, response);
            }
            else
            {
                // unknown action, show edit page
                request.setAttribute("unit", unit);

                JSPManager.showJSP(request, response, "/tools/unit-edit.jsp");
            }
        }
        else
        // no unit set
        {
            // want to add a unit - create a blank one, and pass to
            // unit_edit.jsp
            String button = UIUtil.getSubmitButton(request, "submit");

            if (button.equals("submit_add"))
            {
                unit = Unit.create(c);

                unit.setName("new unit" + unit.getID());
                unit.update();

                request.setAttribute("unit", unit);

                JSPManager.showJSP(request, response, "/tools/unit-edit.jsp");
                c.complete();
            }
            else
            {
                // show the main page (select units)
                showMainPage(c, request, response);
            }
        }
    }

    private void showMainPage(Context c, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        Unit[] units = Unit.findAll(c, Unit.NAME);

        request.setAttribute("units", units);

        JSPManager.showJSP(request, response, "/tools/unit-list.jsp");
        c.complete();
    }
}
