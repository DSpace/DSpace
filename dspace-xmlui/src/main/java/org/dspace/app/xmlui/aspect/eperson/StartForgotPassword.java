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
    private static final Message T_title =
        message("xmlui.EPerson.StartForgotPassword.title");
    
    private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
    
    private static final Message T_trail_forgot_password =
        message("xmlui.EPerson.trail_forgot_password");
    
    private static final Message T_head =
        message("xmlui.EPerson.StartForgotPassword.head");
    
    private static final Message T_para1 =
        message("xmlui.EPerson.StartForgotPassword.para1");
    
    private static final Message T_email_address =
        message("xmlui.EPerson.StartForgotPassword.email_address");

    private static final Message T_email_address_help =
        message("xmlui.EPerson.StartForgotPassword.email_address_help");
    
    private static final Message T_error_not_found =
        message("xmlui.EPerson.StartForgotPassword.error_not_found");
    
    private static final Message T_submit = 
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
        
       Division forgot = body.addInteractiveDivision("start-forgot-password",
               contextPath+"/forgot",Division.METHOD_POST,"primary");
       
       forgot.setHead(T_head);
       
       EPersonUtils.forgottProgressList(forgot,1);
       
       forgot.addPara(T_para1);
       
       List form = forgot.addList("form",List.TYPE_FORM);
       
       Text email = form.addItem().addText("email");
       email.setRequired();
       email.setAutofocus("autofocus");
       email.setLabel(T_email_address);
       email.setHelp(T_email_address_help);
       
       // Prefill with invalid email if this is a retry attempt.
       if (email != null)
       {
           email.setValue(this.email);
       }
       if (errors.contains("email"))
       {
           email.addError(T_error_not_found);
       }
       
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
