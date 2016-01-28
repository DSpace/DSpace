/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.artifactbrowser;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.NOPValidity;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.authorize.AuthorizeException;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;

/**
 * Let admin/author know their email response has been sent
 */
public class ItemRequestResponseSent extends AbstractDSpaceTransformer implements CacheableProcessingComponent
{
    /** language strings */
    public static final Message T_title =
            message("xmlui.ArtifactBrowser.ItemRequestResponseSent.title");

    public static final Message T_dspace_home =
            message("xmlui.general.dspace_home");

    public static final Message T_trail =
            message("xmlui.ArtifactBrowser.ItemRequestResponseSent.trail");

    public static final Message T_head =
            message("xmlui.ArtifactBrowser.ItemRequestResponseSent.head");

    public static final Message T_para1 =
            message("xmlui.ArtifactBrowser.ItemRequestResponseSent.para1");

    /**
     * Generate the unique caching key.
     */
    public Serializable getKey() {
        return 0;
    }

    /**
     * Generate the cache validity object.
     */
    public SourceValidity getValidity()
    {
        return NOPValidity.SHARED_INSTANCE;
    }


    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {

        pageMeta.addMetadata("title").addContent(T_title);

        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
        pageMeta.addTrail().addContent(T_trail);
    }


    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        Division feedback = body.addDivision("itemRequestResponse-sent","primary");

        feedback.setHead(T_head);

        feedback.addPara(T_para1);

    }
}
