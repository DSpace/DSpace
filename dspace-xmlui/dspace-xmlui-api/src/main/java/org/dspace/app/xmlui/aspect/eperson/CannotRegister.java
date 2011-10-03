/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.eperson;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.PageMeta;

/**
 * Inform the user that the email address they entered cannot be registered 
 * with DSpace. This is a state within the new user registration flow.
 * 
 * @author Scott Phillips
 */
public class CannotRegister extends AbstractDSpaceTransformer
{
    /** Language strings */
    private static final Message T_title =
        message("xmlui.EPerson.CannotRegister.title");
    
    private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
    
    private static final Message T_trail_new_registration =
        message("xmlui.EPerson.trail_new_registration");
    
    private static final Message T_head =
        message("xmlui.EPerson.CannotRegister.head");
    
    private static final Message T_para1 =
        message("xmlui.EPerson.CannotRegister.para1");
  
    public void addPageMeta(PageMeta pageMeta) throws WingException 
    {
        // Set the page title
        pageMeta.addMetadata("title").addContent(T_title);

        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
        pageMeta.addTrail().addContent(T_trail_new_registration);
    }
    
    public void addBody(Body body) throws WingException
    {
        Division cannot = body.addDivision("register-cannot","primary");

        cannot.setHead(T_head);

        EPersonUtils.registrationProgressList(cannot, 0);
        
        cannot.addPara(T_para1); 
    }

}
