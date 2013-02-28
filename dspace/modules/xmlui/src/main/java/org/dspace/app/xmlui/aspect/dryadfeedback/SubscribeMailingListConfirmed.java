/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.dryadfeedback;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;

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

/**
 * Simple page to let the user know their mailing list subscription has been
 * requested
 *
 * @author Dan Leehr
 */
public class SubscribeMailingListConfirmed extends AbstractDSpaceTransformer implements CacheableProcessingComponent
{
    public static final Message T_dspace_home =
        message("xmlui.general.dspace_home");

    /**
     * Generate the unique caching key.
     */
    public Serializable getKey() {
        return "1";
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

        pageMeta.addMetadata("title").addContent("Subscribed");
        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
        pageMeta.addTrail().addContent("trail");
    }

    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        Division feedback = body.addDivision("feedback-sent","primary");
        feedback.setHead("Subscribed");
        feedback.addPara("Thank you for subscribing to the Dryad mailing list");
    }
}
