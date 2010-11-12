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
 * Display to the user that their profile has been successfully updated.
 * 
 * @author Scott Phillips
 */

public class ProfileUpdated extends AbstractDSpaceTransformer
{
    /** Language string */
    private static final Message T_title =
        message("xmlui.EPerson.ProfileUpdated.title");
    
    private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
    
    private static final Message T_trail =
        message("xmlui.EPerson.ProfileUpdated.trail");
    
    private static final Message T_head =
        message("xmlui.EPerson.ProfileUpdated.head");
    
    private static final Message T_para1 =
        message("xmlui.EPerson.ProfileUpdated.para1");
    
    private static final Message T_go_home =
        message("xmlui.general.go_home");
    
    
    
  public void addPageMeta(PageMeta pageMeta) throws WingException
  {
    // Set the page title
    pageMeta.addMetadata("title").addContent(T_title);

    pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
    pageMeta.addTrail().addContent(T_trail);
  }

  public void addBody(Body body) throws WingException
  {
    Division updated = body.addDivision("profile-updated", "primary");

    updated.setHead(T_head);

    updated.addPara(T_para1);

    updated.addPara().addXref(contextPath + "/", T_go_home);
  }

}
