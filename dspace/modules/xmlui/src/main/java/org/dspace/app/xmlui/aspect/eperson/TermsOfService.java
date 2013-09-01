/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.eperson;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogManager;
import org.xml.sax.SAXException;

/**
 */
public class TermsOfService extends AbstractDSpaceTransformer
{
    private static Logger log = Logger.getLogger(EditProfile.class);

    private static final Message T_head =
            message("xmlui.EPerson.TermsOfService.head");

    private static final Message T_telephone =
            message("xmlui.EPerson.TermsOfService.telephone");

    private static final Message T_terms =
            message("xmlui.EPerson.TermsOfService.terms");

    private static final Message T_help =
            message("xmlui.EPerson.TermsOfService.help");

    private static final Message T_button =
            message("xmlui.EPerson.TermsOfService.button");

    public void addBody(Body body) throws WingException, SQLException
    {
        Request request = ObjectModelHelper.getRequest(objectModel);


        if (eperson != null && !request.getRequestURI().startsWith("/profile"))
        {
            // Log that we are viewing a profile
            log.info(LogManager.getHeader(context, "terms_and_condition", ""));

            if((eperson.getPhone() == null || eperson.getPhone().trim().length() == 0) ||
                    !eperson.getTerms())
            {
                //needed for modal
                body.addDivision("background");

                Division popup = body.addDivision("modal-content");

                Division profile = popup.addInteractiveDivision("content", contextPath + "/profile", Division.METHOD_POST, "primary");;

                profile.setHead(T_head);

                if((eperson.getPhone() == null || eperson.getPhone().trim().length() == 0))
                    profile.addPara(T_telephone);

                if(!eperson.getTerms())
                    profile.addPara(T_terms);

                profile.addPara(T_help);

                List form = profile.addList("form", List.TYPE_FORM);

                Button submit = form.addItem().addButton("submit_required");
                submit.setValue(T_button);

            }
        }



    }

    /**
     * Recycle
     */
    public void recycle()
    {
        super.recycle();
    }

}
