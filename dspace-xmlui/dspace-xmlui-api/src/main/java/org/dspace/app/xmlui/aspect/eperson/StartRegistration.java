/*
 * StartRegistration.java
 *
 * Version: $Revision: 1.7 $
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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.NOPValidity;
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
 * Display the new user registration form, allowing the user to enter 
 * in an email address and have the system verify the email address
 * before allowing the user create an account
 * 
 * There are two parameters that may be given to the form:
 * 
 * email - The email of the new account account
 * 
 * retry - A boolean value indicating that the previously entered email was invalid.
 * 
 * accountExists - A boolean value indicating the email previously entered allready
 *   belongs to a user.
 *   
 * @author Scott Phillips
 */

public class StartRegistration extends AbstractDSpaceTransformer implements CacheableProcessingComponent
{
    /** language strings */
    private static final Message T_title =
        message("xmlui.EPerson.StartRegistration.title");
    
    private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
    
    private static final Message T_trail_new_registration =
        message("xmlui.EPerson.trail_new_registration");
    
    private static final Message T_head1 =
        message("xmlui.EPerson.StartRegistration.head1");
    
    private static final Message T_para1 = 
        message("xmlui.EPerson.StartRegistration.para1");
    
    private static final Message T_reset_password_for =
        message("xmlui.EPerson.StartRegistration.reset_password_for");
    
    private static final Message T_submit_reset = 
        message("xmlui.EPerson.StartRegistration.submit_reset");
    
    private static final Message T_head2 = 
        message("xmlui.EPerson.StartRegistration.head2");
    
    private static final Message T_para2 = 
        message("xmlui.EPerson.StartRegistration.para2");
    
    private static final Message T_email_address =
        message("xmlui.EPerson.StartRegistration.email_address");

    private static final Message T_email_address_help =
        message("xmlui.EPerson.StartRegistration.email_address_help");
    
    private static final Message T_error_bad_email =
        message("xmlui.EPerson.StartRegistration.error_bad_email");
    
    private static final Message T_submit_register = 
        message("xmlui.EPerson.StartRegistration.submit_register");
    

    /** The email address previously entered */
    private String email;
    
    /** Determine if the user failed on their last attempt to enter an email address */
    private java.util.List<String> errors;
    
    /** 
     * Determine if the last failed attempt was because an account allready 
     * existed for the given email address 
     */
    private boolean accountExists;
    
    public void setup(SourceResolver resolver, Map objectModel, String src,
            Parameters parameters) throws ProcessingException, SAXException,
            IOException
    { 
        super.setup(resolver,objectModel,src,parameters);
        
        this.email = parameters.getParameter("email","");
        this.accountExists = parameters.getParameterAsBoolean("accountExists",false);
        String errors = parameters.getParameter("errors","");
        if (errors.length() > 0)
            this.errors = Arrays.asList(errors.split(","));
        else
            this.errors = new ArrayList<String>();
    }
     
    
    /**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     */
    public Serializable getKey()
    {
        // Only cache on the first attempt.
        if (email == null && accountExists == false && errors != null && errors.size() == 0)
            // cacheable
            return "1";
        else
            // Uncachable
            return "0";
    }

    /**
     * Generate the cache validity object.
     */
    public SourceValidity getValidity()
    {
        if (email == null && accountExists == false && errors != null && errors.size() == 0)
            // Always valid
            return NOPValidity.SHARED_INSTANCE;
        else
            // invalid
            return null;
    }
    
    
    public void addPageMeta(PageMeta pageMeta) throws WingException 
    {
        // Set the page title
        pageMeta.addMetadata("title").addContent(T_title);

        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
        pageMeta.addTrail().addContent(T_trail_new_registration);
    }
    
   public void addBody(Body body) throws WingException {
        
       if (accountExists) {
           Division exists = body.addInteractiveDivision("register-account-exists",contextPath+"/register",Division.METHOD_POST,"primary");

           exists.setHead(T_head1);
           
           exists.addPara(T_para1);

           List form = exists.addList("form");
           
           form.addLabel(T_reset_password_for);
           form.addItem(this.email);
           
           form.addLabel();
           Item submit = form.addItem();
           submit.addButton("submit_forgot").setValue(T_submit_reset);
           
           exists.addHidden("email").setValue(this.email);
           exists.addHidden("eperson-continue").setValue(knot.getId()); 
       }
       
       
       Division register = body.addInteractiveDivision("register",
               contextPath+"/register",Division.METHOD_POST,"primary");
       
       register.setHead(T_head2);
       
       EPersonUtils.registrationProgressList(register,1);
       
       register.addPara(T_para2);
       
       List form = register.addList("form",List.TYPE_FORM);
       
       Text email = form.addItem().addText("email");
       email.setRequired();
       email.setLabel(T_email_address);
       email.setHelp(T_email_address_help);
       email.setValue(this.email);
       if (errors.contains("email"))
           email.addError(T_error_bad_email);
       
       Item submit = form.addItem();
       submit.addButton("submit").setValue(T_submit_register);
       
       register.addHidden("eperson-continue").setValue(knot.getId()); 
   }
   
   /**
    * Recycle
    */
   public void recycle() 
   {
       this.email = null;
       this.errors = null;
       super.recycle();
   }
}
