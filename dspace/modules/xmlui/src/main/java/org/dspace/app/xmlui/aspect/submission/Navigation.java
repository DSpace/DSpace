/*
 * Navigation.java
 *
 * Version: $Revision: 3705 $
 *
 * Date: $Date: 2009-04-11 19:02:24 +0200 (za, 11 apr 2009) $
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
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
package org.dspace.app.xmlui.aspect.submission;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.util.HashUtil;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.NOPValidity;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.AuthenticationUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.utils.DSpaceValidity;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Options;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.handle.HandleManager;
import org.dspace.content.Collection;
import org.dspace.core.ConfigurationManager;
import org.dspace.eperson.Group;
import org.dspace.workflow.*;
import org.xml.sax.SAXException;

/**
 * Simple navigation class to add the top level link to
 * the main submissions page.
 *
 * @author Scott Phillips
 */
public class Navigation extends AbstractDSpaceTransformer implements CacheableProcessingComponent
{
    private static final Message T_my_account =
	message("xmlui.EPerson.Navigation.my_account");         

	/** Language Strings **/
    protected static final Message T_submissions =
        message("xmlui.Submission.Navigation.submissions");

    protected static final Message T_my_tasks =
        message("xmlui.Submission.Navigation.my_tasks");

    /** Cached validity object */
    private SourceValidity validity;

    private static final Logger log = Logger.getLogger(Navigation.class);

	 /**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     */
    public Serializable getKey() {

         Request request = ObjectModelHelper.getRequest(objectModel);

         // Special case, don't cache anything if the user is logging
         // in. The problem occures because of timming, this cache key
         // is generated before we know whether the operation has
         // succeded or failed. So we don't know whether to cache this
         // under the user's specific cache or under the anonymous user.
         if (request.getParameter("login_email")    != null ||
             request.getParameter("login_password") != null ||
             request.getParameter("login_realm")    != null )
         {
             return null;
         }

         // FIXME:
         // Do not cache the home page. There is a bug that is causing the
         // homepage to be cached with user's data after a logout. This
         // polutes the cache. As a work-around this problem we just won't
         // cache this page.
         if (request.getSitemapURI().length() == 0)
         {
             return null;
         }

         String key;
         if (context.getCurrentUser() != null)
             key = context.getCurrentUser().getEmail();
         else
             key = "anonymous";

         // Add the user's language
         Enumeration locales = request.getLocales();
         while (locales.hasMoreElements())
         {
             Locale locale = (Locale) locales.nextElement();
             key += "-" + locale.toString();
         }

         return HashUtil.hash(key);
    }

    /**
     * Generate the cache validity object.
     */
    public SourceValidity getValidity()
    {
        // Check if our user changed !
        if (this.validity == null)
        {
            // Only use the DSpaceValidity object is someone is logged in.
            if (context.getCurrentUser() != null)
            {
                try {
                    DSpaceValidity validity = new DSpaceValidity();

                    validity.add(eperson);

                    Group[] groups = Group.allMemberGroups(context, eperson);
                    for (Group group : groups)
                    {
                        validity.add(group);
                    }

                    this.validity = validity.complete();
                }
                catch (SQLException sqle)
                {
                    // Just ignore it and return invalid.
                }
            }
            else
            {
                this.validity = NOPValidity.SHARED_INSTANCE;
            }
        }
        return this.validity;
    }


    public void addOptions(Options options) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        List test = options.addList("submitNow");

        Collection coll = (Collection) HandleManager.resolveToObject(context, ConfigurationManager.getProperty("submit.publications.collection"));

        if (coll != null) {
        	Item item = test.addItem("submitnowitem", "submitnowli");
        	item.addXref(contextPath + "/handle/" + coll.getHandle() + "/submit", "Submit Data Now!", "submitnowbutton");
        }

        //Only logged in users can view the submissions link
        if(context.getCurrentUser() != null){
	    options.addList("browse");
            List account = options.addList("account");
	    options.addList("context");
	    options.addList("administrative");         

	    account.setHead(T_my_account);
        account.addItemXref(contextPath+"/submissions",T_submissions);

            if(AuthorizeManager.isCuratorOrAdmin(context))
            {
                account.addItemXref(contextPath+"/my-tasks",T_my_tasks);
            }
        }


        // Basic navigation skeleton
//        options.addList("browse");
//        options.addList("account");
//        options.addList("context");
//        options.addList("administrative");

//      This dosn't flow very well, lets remove it and see if anyone misses it.
//    	DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
//    	if (dso != null && dso instanceof Collection)
//    	{
//    		Collection collection = (Collection) dso;
//    		if (AuthorizeManager.authorizeActionBoolean(context, collection, Constants.ADD))
//    		{
//    	        String submitURL = contextPath + "/handle/" + collection.getHandle() + "/submit";
//		        account.addItemXref(submitURL,"Submit to this collection");
//    		}
//    	}
    }
}
