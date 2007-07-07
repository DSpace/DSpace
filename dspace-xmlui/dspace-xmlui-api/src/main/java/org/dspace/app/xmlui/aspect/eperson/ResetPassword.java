/*
 * ResetPassword.java
 *
 * Version: $Revision: 1.8 $
 *
 * Date: $Date: 2006/07/28 20:08:18 $
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

package org.dspace.app.xmlui.aspect.eperson;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.SourceResolver;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Field;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.xml.sax.SAXException;

/**
 * Display a reset password form allowing the user to select a new password.
 * 
 * @author Scott Phillips
 */

public class ResetPassword extends AbstractDSpaceTransformer
{
    /** Language strings */
    private final static Message T_title =
        message("xmlui.EPerson.ResetPassword.title");
    
    private final static Message T_dspace_home =
        message("xmlui.general.dspace_home");
    
    private final static Message T_trail_forgot_password =
        message("xmlui.EPerson.trail_forgot_password");
    
    private final static Message T_head = 
        message("xmlui.EPerson.ResetPassword.head");
    
    private final static Message T_para1 =
        message("xmlui.EPerson.ResetPassword.para1");
    
    private final static Message T_email_address =
        message("xmlui.EPerson.ResetPassword.email_address");
    
    private final static Message T_new_password =
        message("xmlui.EPerson.ResetPassword.new_password");
    
    private final static Message T_error_invalid_password =
        message("xmlui.EPerson.ResetPassword.error_invalid_password");
    
    private final static Message T_confirm_password =
        message("xmlui.EPerson.ResetPassword.confirm_password");
    
    private final static Message T_error_unconfirmed_password =
        message("xmlui.EPerson.ResetPassword.error_unconfirmed_password");
    
    private final static Message T_submit = 
        message("xmlui.EPerson.ResetPassword.submit");
    
    
	private String email;
    private java.util.List<String> errors;
    
    public void setup(SourceResolver resolver, Map objectModel, String src,
            Parameters parameters) throws ProcessingException, SAXException,
            IOException
    { 
        super.setup(resolver,objectModel,src,parameters);

        this.email = parameters.getParameter("email","unknown");
        
        String errors = parameters.getParameter("errors","");
        if (errors.length() > 0)
            this.errors = Arrays.asList(errors.split(","));
        else
            this.errors = new ArrayList<String>();
        
    }
       
    
    public void addPageMeta(PageMeta pageMeta) throws WingException 
    {
        // Set the page title
        pageMeta.addMetadata("title").addContent(T_title);
      
        
        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
        pageMeta.addTrail().addContent(T_trail_forgot_password);
    }
    
   public void addBody(Body body) throws WingException {
        
       Division register = body.addInteractiveDivision("reset-password",
               contextPath+"/register",Division.METHOD_POST,"primary");
       
       register.setHead(T_head);
       
       EPersonUtils.forgottProgressList(register,2);
       
       register.addPara(T_para1);
       
       List form = register.addList("form",List.TYPE_FORM);
       
       form.addLabel(T_email_address);
       form.addItem(email);
       
       Field password = form.addItem().addPassword("password");
       password.setRequired();
       password.setLabel(T_new_password);
       if (errors.contains("password"))
       {
           password.addError(T_error_invalid_password);
       }
       
       Field passwordConfirm = form.addItem().addPassword("password_confirm");
       passwordConfirm.setRequired();
       passwordConfirm.setLabel(T_confirm_password);
       if (errors.contains("password_confirm"))
       {
           passwordConfirm.addError(T_error_unconfirmed_password);
       }
       
       form.addItem().addButton("submit").setValue(T_submit);
       
       register.addHidden("eperson-continue").setValue(knot.getId()); 
   }
   
    
}
