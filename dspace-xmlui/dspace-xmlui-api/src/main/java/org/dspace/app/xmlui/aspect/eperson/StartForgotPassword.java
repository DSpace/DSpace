/*
 * StartForgotPassword.java
 *
 * Version: $Revision: 1.8 $
 *
 * Date: $Date: 2006/08/08 20:57:24 $
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
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Text;
import org.xml.sax.SAXException;

/**
 * Display the forgot password form, allowing the user to enter 
 * in an email address and have the system verify the email address
 * before allowing the user to reset their password.
 * 
 * There are two parameters that may be given to the form:
 * 
 * email - The email of the forgotten account
 * 
 * retry - A boolean value indicating that the previously entered email was invalid.
 * 
 * @author Scott Phillips
 */
public class StartForgotPassword extends AbstractDSpaceTransformer
{
    /** Language strings */
    private final static Message T_title =
        message("xmlui.EPerson.StartForgotPassword.title");
    
    private final static Message T_dspace_home =
        message("xmlui.general.dspace_home");
    
    private final static Message T_trail_forgot_password =
        message("xmlui.EPerson.trail_forgot_password");
    
    private final static Message T_head = 
        message("xmlui.EPerson.StartForgotPassword.head");
    
    private final static Message T_para1 =
        message("xmlui.EPerson.StartForgotPassword.para1");
    
    private final static Message T_email_address =
        message("xmlui.EPerson.StartForgotPassword.email_address");

    private final static Message T_email_address_help =
        message("xmlui.EPerson.StartForgotPassword.email_address_help");
    
    private final static Message T_error_not_found =
        message("xmlui.EPerson.StartForgotPassword.error_not_found");
    
    private final static Message T_submit = 
        message("xmlui.EPerson.StartForgotPassword.submit");
    
    
    
    /** The email of the forgotten account */
    private String email;
    
    /** A list of fields in error */
    private java.util.List<String> errors;
    
    public void setup(SourceResolver resolver, Map objectModel, String src,
            Parameters parameters) throws ProcessingException, SAXException,
            IOException
    { 
        super.setup(resolver,objectModel,src,parameters);
        
        this.email = parameters.getParameter("email","");
        
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
        
       Division forgot = body.addInteractiveDivision("start-forgot-password",
               contextPath+"/forgot",Division.METHOD_POST,"primary");
       
       forgot.setHead(T_head);
       
       EPersonUtils.forgottProgressList(forgot,1);
       
       forgot.addPara(T_para1);
       
       List form = forgot.addList("form",List.TYPE_FORM);
       
       Text email = form.addItem().addText("email");
       email.setRequired();
       email.setLabel(T_email_address);
       email.setHelp(T_email_address_help);
       
       // Prefill with invalid email if this is a retry attempt.
       if (email != null)
           email.setValue(this.email);
       if (errors.contains("email"))
           email.addError(T_error_not_found);
       
       Item submit = form.addItem();
       submit.addButton("submit").setValue(T_submit);
       
       forgot.addHidden("eperson-continue").setValue(knot.getId()); 
   }
   
   /**
    * Recycle
    */
   public void recycle() 
   {
       this.email = null;
       super.recycle();
   } 
}
