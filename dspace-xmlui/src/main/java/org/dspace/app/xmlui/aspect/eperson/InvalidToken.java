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
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Display to the user that the token provided was invalid.
 * 
 * @author Scott Phillips
 */

public class InvalidToken extends AbstractDSpaceTransformer
{
    /** language strings */
    private static final Message T_title =
        message("xmlui.EPerson.InvalidToken.title");
    
    private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
    
    private static final Message T_trail =
        message("xmlui.EPerson.InvalidToken.trail");
    
    private static final Message T_head = 
        message("xmlui.EPerson.InvalidToken.head");
    
    private static final Message T_para1 =
        message("xmlui.EPerson.InvalidToken.para1");
    
    private static final Message T_para2 = 
        message("xmlui.EPerson.InvalidToken.para2");
    
    public void addPageMeta(PageMeta pageMeta) throws WingException 
    {
        // Set the page title
        pageMeta.addMetadata("title").addContent(T_title);
       
        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
        
        pageMeta.addTrail().addContent(T_trail);
    }
    
   public void addBody(Body body) throws WingException {
        
       Division invalid = body.addDivision("invalid-token","primary");
       
       invalid.setHead(T_head);
       
       invalid.addPara(T_para1);

       Para example1 = invalid.addPara("invalid-token-example","code");
       example1.addContent(DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("dspace.url") + "/register?token=ABCDEFGHIJK");
       Para example2 = invalid.addPara("invalid-token-example","code");
       example2.addContent("LMNOP");
       
       invalid.addPara(T_para2);

       Para example3 = invalid.addPara("valid-token-example","code");
       example3.addContent(DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("dspace.url") + "/register?token=ABCDEFGHIJKLMNOP");

   }
   
    
}
