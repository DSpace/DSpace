/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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
    private static final Message T_title =
        message("xmlui.EPerson.ResetPassword.title");
    
    private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
    
    private static final Message T_trail_forgot_password =
        message("xmlui.EPerson.trail_forgot_password");
    
    private static final Message T_head =
        message("xmlui.EPerson.ResetPassword.head");
    
    private static final Message T_para1 =
        message("xmlui.EPerson.ResetPassword.para1");
    
    private static final Message T_email_address =
        message("xmlui.EPerson.ResetPassword.email_address");
    
    private static final Message T_new_password =
        message("xmlui.EPerson.ResetPassword.new_password");
    
    private static final Message T_error_invalid_password =
        message("xmlui.EPerson.ResetPassword.error_invalid_password");
    
    private static final Message T_confirm_password =
        message("xmlui.EPerson.ResetPassword.confirm_password");
    
    private static final Message T_error_unconfirmed_password =
        message("xmlui.EPerson.ResetPassword.error_unconfirmed_password");
    
    private static final Message T_submit = 
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
        {
            this.errors = Arrays.asList(errors.split(","));
        }
        else
        {
            this.errors = new ArrayList<String>();
        }
        
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
       password.setAutofocus("autofocus");
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
