 /**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.xmlui.aspect.swordclient;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.authorize.AuthorizeException;

import java.sql.SQLException;

/**
 * User: Robin Taylor
 * Date: 08-Apr-2010
 * Time: 09:54:52
 */
public class SwordResponseTransformer extends AbstractDSpaceTransformer
{
    private static final Message T_dspace_home = message("xmlui.general.dspace_home");

    private static final Message T_title = message("xmlui.swordclient.SwordResponse.title");
    private static final Message T_SwordCopy_trail = message("xmlui.swordclient.general.SwordCopy_trail");
    private static final Message T_trail = message("xmlui.swordclient.SwordResponse.trail");
    private static final Message T_main_head = message("xmlui.swordclient.general.main_head");


    public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);

        pageMeta.addTrail().addContent(T_SwordCopy_trail);
        pageMeta.addTrail().addContent(T_trail);
    }

      public void addBody(Body body) throws WingException, SQLException, AuthorizeException
    {
        //Division main = body.addDivision("deposit-response");
        //main.setHead(T_main_head.parameterize(handle));
               



    }

}
