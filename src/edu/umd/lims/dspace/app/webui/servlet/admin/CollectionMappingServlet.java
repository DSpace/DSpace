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
      showMainPage(c, request, response, comm);
    }

    else if (action.equals("map") ||
	     action.equals("unmap") ||
	     action.equals("confirm_map") ||
	     action.equals("confirm_unmap")) {

      // Get the collection
      if (collID == -1) {
	// Instead of an error message, just show the page again
	showMainPage(c, request, response, comm);
      }
      Collection coll = Collection.find(c, collID);
      if (coll == null) {
	throw new ServletException("invalid collection_id");
      }
      request.setAttribute("collection", coll);

      // Get cancellation 
      String cancel = request.getParameter("submit_cancel");
      if (cancel != null && cancel.equals("")) {
	cancel = null;
      }

      // Take action
      if (action.equals("map")) {
	JSPManager.showJSP(request, response, "/tools/confirm-mapcollection.jsp");
      }
      else if (action.equals("unmap")) {
	JSPManager.showJSP(request, response, "/tools/confirm-unmapcollection.jsp");
      }	
      else if (action.equals("confirm_map") && cancel == null) {
	//comm.addCollection(coll);
	//c.commit();
	JSPManager.showJSP(request, response, "/tools/mapcollections_display.jsp");
      }	
      else if (action.equals("confirm_unmap") && cancel == null) {
	//comm.removeCollection(coll);
	//c.commit();
	JSPManager.showJSP(request, response, "/tools/mapcollections_display.jsp");
      }	

      showMainPage(c, request, response, comm);

    }

    else {
      throw new ServletException("Invalid action: " + action);
    }
  }

  
  /**
   *
   */

  private void showMainPage(Context c, HttpServletRequest request, HttpServletResponse response, Community comm) throws ServletException, IOException, SQLException, AuthorizeException {

    request.setAttribute("mapped", comm.getCollections());
    request.setAttribute("unmapped", comm.getCollectionsUnmapped());

    JSPManager.showJSP(request, response, "/tools/mapcollections.jsp");
  }

}

