/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.eperson;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import org.apache.avalon.framework.parameters.ParameterException;
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

public class AlreadyRegister extends AbstractDSpaceTransformer implements CacheableProcessingComponent

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
    
    public void setup(SourceResolver resolver, Map objectModel, String src,
            Parameters parameters) throws ProcessingException, SAXException,
            IOException
    { 
        super.setup(resolver,objectModel,src,parameters);
        try
        {
            this.email = parameters.getParameter("email");
        }
        catch (ParameterException pe)
        {
            throw new ProcessingException(pe);
        } 
    }
   
   public Serializable getKey()
    {
        // Only cache on the first attempt.
        if (email == null)
        {
            // cacheable
            return "1";
        }
        else
        {
            // Uncachable
            return "0";
        }
    }

    /**
     * Generate the cache validity object.
     */
    public SourceValidity getValidity()
    {
        if (email == null)
        {
            // Always valid
            return NOPValidity.SHARED_INSTANCE;
        }
        else
        {
            // invalid
            return null;
        }
    }  
    
    
    public void addPageMeta(PageMeta pageMeta) throws WingException 
    {
        // Set the page title
        pageMeta.addMetadata("title").addContent(T_title);

        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
        pageMeta.addTrail().addContent(T_trail_new_registration);
    }
    
   public void addBody(Body body) throws WingException {
        
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
