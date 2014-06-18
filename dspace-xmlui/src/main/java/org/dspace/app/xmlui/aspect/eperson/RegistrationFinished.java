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
 * Display to the user that they have successfully registered.
 * 
 * @author Scott Phillips
 */

public class RegistrationFinished extends AbstractDSpaceTransformer
{
    /** Language strings */
    private static final Message T_title =
        message("xmlui.EPerson.RegistrationFinished.title");
    
    private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
    
    private static final Message T_trail_new_registration =
        message("xmlui.EPerson.trail_new_registration");
    
    private static final Message T_head =
        message("xmlui.EPerson.RegistrationFinished.head");
    
    private static final Message T_para1 =
        message("xmlui.EPerson.RegistrationFinished.para1");
    
    private static final Message T_go_home =
        message("xmlui.general.go_home");
    
    
  public void addPageMeta(PageMeta pageMeta) throws WingException
  {
    // Set the page title
    pageMeta.addMetadata("title").addContent(T_title);

    pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
    pageMeta.addTrail().addContent(T_trail_new_registration);
  }

  public void addBody(Body body) throws WingException
  {
    Division finished = body.addDivision("registration-finished", "primary");

    finished.setHead(T_head);

    EPersonUtils.registrationProgressList(finished, 3);

    finished.addPara(T_para1);

    finished.addPara().addXref(contextPath + "/", T_go_home);
  }

}
