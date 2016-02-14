/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.xmlui.aspect.swordclient;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * User: Robin Taylor
 * Date: 21-Sep-2010
 * Time: 13:44:28
 */
public class SelectTargetTransformer extends AbstractDSpaceTransformer
{
    private static final Message T_dspace_home = message("xmlui.general.dspace_home");
    private static final Message T_title = message("xmlui.swordclient.SelectTarget.title");
    private static final Message T_SwordCopy_trail = message("xmlui.swordclient.general.SwordCopy_trail");
    private static final Message T_trail = message("xmlui.swordclient.SelectTarget.trail");
    private static final Message T_main_head = message("xmlui.swordclient.general.main_head");
    private static final Message T_submit_next = message("xmlui.general.next");
    private static final Message T_submit_cancel = message("xmlui.general.cancel");

    private static final Message T_url = message("xmlui.swordclient.SelectTarget.url");
    private static final Message T_other_url = message("xmlui.swordclient.SelectTarget.other_url");
    private static final Message T_username = message("xmlui.swordclient.SelectTarget.username");
    private static final Message T_password = message("xmlui.swordclient.SelectTarget.password");
    private static final Message T_on_behalf_of = message("xmlui.swordclient.SelectTarget.on_behalf_of");

    private static final Message T_url_error = message("xmlui.swordclient.SelectTargetAction.url_error");
    private static final Message T_username_error = message("xmlui.swordclient.SelectTargetAction.username_error");
    private static final Message T_password_error = message("xmlui.swordclient.SelectTargetAction.password_error");

    private static Logger log = Logger.getLogger(SelectTargetTransformer.class);


    /**
     * Add a page title and trail links
     */
    public void addPageMeta(PageMeta pageMeta) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);

        pageMeta.addTrail().addContent(T_SwordCopy_trail);
        pageMeta.addTrail().addContent(T_trail);
    }


    public void addBody(Body body) throws SAXException, WingException, SQLException, IOException, AuthorizeException
    {
        Request request = ObjectModelHelper.getRequest(objectModel);

        String handle = parameters.getParameter("handle", null);
        String errorString = parameters.getParameter("errors",null);
        ArrayList<String> errors = new ArrayList<String>();
        if (!StringUtils.isEmpty(errorString))
		{
            for (String error : errorString.split(","))
            {
                errors.add(error);
            }
        }

        // If this screen is being redisplayed then get the previously entered values.
        String urlValue = request.getParameter("url");
        String otherUrlValue = request.getParameter("otherUrl");
        String usernameValue = request.getParameter("username");
//        String passwordValue  = request.getParameter("password");

        Division main = body.addInteractiveDivision("service-document", contextPath + "/swordclient", Division.METHOD_POST, "");
        main.setHead(T_main_head.parameterize(handle));

        List targetDetails = main.addList("target_details",List.TYPE_FORM);

        Select source = targetDetails.addItem().addSelect("url");
        String[] targets = DSpaceServicesFactory.getInstance().getConfigurationService().getArrayProperty("sword-client.targets");
        for (String target : targets)
        {
            if ((urlValue != null) && (urlValue.length() > 0) && (urlValue.equals(target)))
            {
                source.addOption(false, target, target);
            }
            else
            {
                source.addOption(false, target, target);
            }
        }
        source.setLabel(T_url);

        Text otherUrl = targetDetails.addItem().addText("otherUrl");
        otherUrl.setLabel(T_other_url);
        otherUrl.setValue(otherUrlValue);
        if (errors.contains("otherUrl"))
        {
            otherUrl.addError(T_url_error);
        }

        Text username = targetDetails.addItem().addText("username");
        username.setRequired();
        username.setLabel(T_username);
        username.setValue(usernameValue);
        if (errors.contains("username"))
        {
            username.addError(T_username_error);
        }

        Password password = targetDetails.addItem().addPassword("password");
        password.setRequired();
        password.setLabel(T_password);
        // Comment - Element Password doesn't allow me to set a value, don't know why not.
        //password.setValue(passwordValue);
        if (errors.contains("password"))
        {
            password.addError(T_password_error);
        }

        Text onBehalfOf = targetDetails.addItem().addText("onBehalfOf");
        onBehalfOf.setLabel(T_on_behalf_of);

        Para buttonList = main.addPara();
        buttonList.addButton("submit_next").setValue(T_submit_next);
        buttonList.addButton("submit_cancel").setValue(T_submit_cancel);
                
        main.addHidden("swordclient-continue").setValue(knot.getId());
    }

}
