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
import org.apache.cocoon.util.HashUtil;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.NOPValidity;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.app.xmlui.wing.element.TextArea;
import org.dspace.authorize.AuthorizeException;
import org.xml.sax.SAXException;

/**
 * Display to the user a simple form letting the user sign up for the
 * Dryad Mailing List
 *
 * @author Dan Leehr
 */
public class SubscribeMailingListForm extends AbstractDSpaceTransformer implements CacheableProcessingComponent
{
    /** Language Strings */
    private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
    /**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     */
    public Serializable getKey() {
        String email = parameters.getParameter("email","");
        return HashUtil.hash(email);
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
        pageMeta.addMetadata("title").addContent("Subscribe to the Dryad Mailing List");
        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
        pageMeta.addTrail().addContent("Subscribe");
    }

    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        // Build the item viewer division.
        Division feedback = body.addInteractiveDivision("feedback-form",
                contextPath+"/subscribe",Division.METHOD_POST,"primary");

        feedback.setHead("Subscribe to the Dryad Mailing List");

        List form = feedback.addList("form",List.TYPE_FORM);

        Text email = form.addItem().addText("email");
        email.setLabel("Your Email");
        email.setHelp("This address will receive periodic announcements from Dryad");
        email.setValue(parameters.getParameter("email",""));

        form.addItem().addButton("submit").setValue("Subscribe");
    }
}
