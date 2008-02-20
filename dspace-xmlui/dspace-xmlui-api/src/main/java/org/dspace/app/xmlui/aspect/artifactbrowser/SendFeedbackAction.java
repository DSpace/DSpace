/*
 * SendFeedbackAction.java
 *
 * Version: $Revision: 1.1 $
 *
 * Date: $Date: 2006/05/01 22:33:39 $
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
package org.dspace.app.xmlui.aspect.artifactbrowser;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.eperson.EPerson;

/**
 * @author Scott Phillips
 */

public class SendFeedbackAction extends AbstractAction
{

    /**
     * 
     */
    public Map act(Redirector redirector, SourceResolver resolver, Map objectModel,
            String source, Parameters parameters) throws Exception
    {
        Request request = ObjectModelHelper.getRequest(objectModel);
            
        String page = request.getParameter("page");
        String address = request.getParameter("email");
        String agent = request.getHeader("User-Agent");
        String session = request.getSession().getId();
        String comments = request.getParameter("comments");
        
        // User email from context
        Context context = ContextUtil.obtainContext(objectModel);
        EPerson loggedin = context.getCurrentUser();
        String eperson = null;
        if (loggedin != null)
            eperson = loggedin.getEmail();
        
        if (page == null || page.equals(""))
        {
            page = request.getHeader("Referer");
        }
        
        // Check all data is there
        if ((address == null) || address.equals("")
                || (comments == null) || comments.equals(""))
        {
            // Either the user did not fill out the form or this is the 
            // first time they are visiting the page. 
            Map<String,String> map = new HashMap<String,String>();
            map.put("page",page);
            
            if (address == null || address.equals(""))
                map.put("email",eperson);
            else
                map.put("email",address);
            
            map.put("comments",comments);
            
            return map;
        }
        
        // All data is there, send the email
        Email email = ConfigurationManager.getEmail(I18nUtil.getEmailFilename(context.getCurrentLocale(), "feedback"));
        email.addRecipient(ConfigurationManager
                .getProperty("feedback.recipient"));

        email.addArgument(new Date()); // Date
        email.addArgument(address);    // Email
        email.addArgument(eperson);    // Logged in as
        email.addArgument(page);       // Referring page
        email.addArgument(agent);      // User agent
        email.addArgument(session);    // Session ID
        email.addArgument(comments);   // The feedback itself

        // Replying to feedback will reply to email on form
        email.setReplyTo(address);

        // May generate MessageExceptions.
        email.send();

        // Finished, allow to pass.
        return null;
    }

}
