/*
 * Copyright (c) 2007 The University of Maryland. All Rights Reserved.
 */

package edu.umd.lims.dspace.app.webui.servlet.admin;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;

import org.dspace.core.Constants;
import org.dspace.core.Context;

import org.dspace.content.Collection;
import org.dspace.content.Community;

import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

/**
 * Servlet for mapping Collections to multiple Communities.  Editing
 * happens at the Community.
 * 
 * @author Ben Wallberg
 */

public class CollectionMappingServlet extends DSpaceServlet {


  protected void doDSGet(Context c, HttpServletRequest request,
			 HttpServletResponse response) throws ServletException, IOException,
    SQLException, AuthorizeException {
    doDSPost(c, request, response);
  }


  protected void doDSPost(Context c, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SQLException, AuthorizeException {

    // Get parameters
    String action = request.getParameter("action");
    int commID = UIUtil.getIntParameter(request, "community_id");
    int collID = UIUtil.getIntParameter(request, "collection_id");

    //request.setAttribute("group", group);
    //request.setAttribute("members", group.getMembers());
    //JSPManager.showJSP(request, response, "/tools/group-edit.jsp");

    // Get the community
    if (commID == -1) {
      throw new ServletException("community_id is a required parameter");
    }
    Community comm = Community.find(c, commID);
    if (comm == null) {
      throw new ServletException("invalid community_id");
    }
    request.setAttribute("community", comm);

    // Take action
    if (action == null || action.equals("")) {

      request.setAttribute("mapped", comm.getCollections());
      request.setAttribute("unmapped", comm.getCollectionsUnmapped());

      JSPManager.showJSP(request, response, "/tools/mapcollections.jsp");
      
    }

    else {
      throw new ServletException("Invalid action: " + action);
    }
  }
}
