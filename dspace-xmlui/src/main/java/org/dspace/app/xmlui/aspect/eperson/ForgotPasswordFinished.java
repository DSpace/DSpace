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
 * Display to the user that their password has been successfully reset.
 * 
 * @author Scott Phillips
 */

public class ForgotPasswordFinished extends AbstractDSpaceTransformer
{
    private static final Message T_title =
        message("xmlui.EPerson.ForgotPasswordFinished.title");
    
    private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
    
    private static final Message T_trail_forgot_password =
        message("xmlui.EPerson.trail_forgot_password");
    
    private static final Message T_head =
        message("xmlui.EPerson.ForgotPasswordFinished.head");
    
    private static final Message T_para1 =
        message("xmlui.EPerson.ForgotPasswordFinished.para1");
    
    private static final Message T_go_home =
        message("xmlui.general.go_home");
    
  public void addPageMeta(PageMeta pageMeta) throws WingException 
  {
      // Set the page title
      pageMeta.addMetadata("title").addContent(T_title);
    
      pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
      pageMeta.addTrail().addContent(T_trail_forgot_password);
  }
  
    public void addBody(Body body) throws WingException
    {
        Division reset = body.addDivision("password-reset", "primary");
        
        reset.setHead(T_head);
        
        EPersonUtils.forgottProgressList(reset, 3);
        
        reset.addPara(T_para1);
        
        reset.addPara().addXref(contextPath + "/", T_go_home);
    }

}
