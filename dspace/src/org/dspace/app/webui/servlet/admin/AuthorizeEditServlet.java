/*
 * AuthorizeEdit.java
 */

package org.dspace.app.webui.servlet.admin;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.SQLException;

import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;


/**
 * Servlet for editing permissions
 * @author dstuve
 * @version $Revision$
 */
public class AuthorizeEditServlet extends DSpaceServlet
{
    protected void doDSGet(Context c,
                    HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        // show the main page (select communities, collections, etc)
        showMainPage(c, request, response);
    }
    
    protected void doDSPost(Context c,
                    HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        String button = UIUtil.getSubmitButton(request, "submit");
        
        if( button.equals("submit_collection") )
        {
            // select a collection to work on
            Collection [] collections = Collection.findAll(c);
        
            request.setAttribute("collections", collections);
            JSPManager.showJSP(request, response, "/admin/collection_select.jsp" );
        }
        else if( button.equals("submit_collection_select") )
        {
            // edit the collection's permissions
            Collection collection      = Collection.find(c, UIUtil.getIntParameter(request, "collection_id"));
            ResourcePolicy [] policies = AuthorizeManager.getPolicies(c, collection);
            
            request.setAttribute("collection", collection );
            request.setAttribute("policies", policies     );
            JSPManager.showJSP(request, response, "/admin/authorize_collection_edit.jsp" );
        }
        else if( button.equals("submit_collection_delete_policy") )
        {
            // delete a permission from a collection
            Collection collection = Collection.find(c, UIUtil.getIntParameter(request, "collection_id"));
            ResourcePolicy policy = ResourcePolicy.find(c, UIUtil.getIntParameter(request, "policy_id"));
            
            // do the remove
            policy.delete();
            
            // return to collection permission page
            request.setAttribute("collection", collection );

            ResourcePolicy [] policies = AuthorizeManager.getPolicies(c, collection);
            request.setAttribute("policies", policies);

            JSPManager.showJSP(request, response, "/admin/authorize_collection_edit.jsp" );
        }
        else if( button.equals("submit_collection_edit_policy") )
        {
            // edit a collection's policy - set up and call policy editor
            Collection collection = Collection.find(c, UIUtil.getIntParameter(request, "collection_id"));
            
            int policy_id = UIUtil.getIntParameter(request, "policy_id");
            ResourcePolicy policy = null;
            
            if( policy_id == -1 )
            {
                // create new policy
                policy = ResourcePolicy.create(c);
                policy.update();
            }
            else
            {
                policy = ResourcePolicy.find(c, policy_id);
            }
            
            Group   [] groups  = Group.findAll(c, Group.NAME);
            EPerson [] epeople = EPerson.findAll(c, EPerson.EMAIL);
            
            // return to collection permission page
            request.setAttribute( "edit_title", "Collection " + collection.getID() );
            request.setAttribute( "collection", collection );
            request.setAttribute( "policy",     policy     );
            request.setAttribute( "groups",     groups     );
            request.setAttribute( "epeople",    epeople    );
            
            JSPManager.showJSP(request, response, "/admin/authorize_policy_edit.jsp" );
        }
        else if( button.equals( "submit_collection_add_policy") )
        {
            // want to add a policy, create an empty one and invoke editor
            ResourcePolicy policy = ResourcePolicy.create(c);
            policy.update();

            Collection collection = Collection.find(c,
                            UIUtil.getIntParameter(request, "collection_id"));
            
            Group   [] groups  = Group.findAll  (c, Group.NAME   );
            EPerson [] epeople = EPerson.findAll(c, EPerson.EMAIL);
            
            // return to collection permission page
            request.setAttribute( "collection", collection );
            request.setAttribute( "policy",     policy     );
            request.setAttribute( "groups",     groups     );
            request.setAttribute( "epeople",    epeople    );
            
            JSPManager.showJSP(request, response,
                "/admin/authorize_policy_edit.jsp" );
        }
        else if( button.equals( "submit_save_policy" ) )
        {
            int policy_id     = UIUtil.getIntParameter(request, "policy_id"    );
            int action_id     = UIUtil.getIntParameter(request, "action_id"    );
            int group_id      = UIUtil.getIntParameter(request, "group_id"     );
            int collection_id = UIUtil.getIntParameter (request, "collection_id");
            boolean is_public = (request.getParameter("is_public")==null ? false: true );
  
            Collection collection = Collection.find(c, collection_id );
            
            // error handling goes here if action_id or group_id aren't set
            if( true )
            {
                ResourcePolicy policy = ResourcePolicy.find(c, policy_id);
                Group group = Group.find(c, group_id);
                
                policy.setResource( collection );
                policy.setAction  ( action_id  );
                policy.setPublic  ( is_public  );
                policy.setGroup( group );
                
                policy.update();
            }

            // now return to previous state
            if( collection_id != -1 )
            {
                // return to collection edit page
                request.setAttribute("collection", collection );

                ResourcePolicy [] policies = AuthorizeManager.getPolicies(c, collection);
                request.setAttribute("policies", policies);

                JSPManager.showJSP(request, response, "/admin/authorize_collection_edit.jsp" );
            }
        }
        else if( button.equals("submit_cancel_policy") )
        {
            // return to the collection page
            int collection_id =UIUtil.getIntParameter (request, "collection_id");
            Collection collection = Collection.find(c, collection_id );
     
            request.setAttribute("collection", collection );

            ResourcePolicy [] policies =
                AuthorizeManager.getPolicies(c, collection);
            request.setAttribute("policies", policies);

            JSPManager.showJSP(request, response, "/admin/authorize_collection_edit.jsp" );
        }
        else
        {
            // return to the main page
            showMainPage(c, request, response);
        }
        
        c.complete();
    }
    
    void showMainPage(Context c,
                    HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        JSPManager.showJSP(request, response, "/admin/authorize_edit_main.jsp" );
    }
       
}

