/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.requestitem;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;

/**
 * RequestItem strategy to allow DSpace support team's help desk to receive
 * requestItem requests.  With this enabled, the Item author/submitter doesn't
 * receive the request, but the help desk instead does.
 *
 * <p>Fails over to the {@link RequestItemSubmitterStrategy}, which means the
 * submitter would get the request if there is no specified help desk email.
 *
 * @author Sam Ottenhoff
 * @author Peter Dietz
 */
public class RequestItemHelpdeskStrategy
        extends RequestItemSubmitterStrategy {
    static final String P_HELPDESK_OVERRIDE
            = "request.item.helpdesk.override";
    static final String P_MAIL_HELPDESK = "mail.helpdesk";

    @Autowired(required = true)
    protected EPersonService ePersonService;

    @Autowired(required = true)
    protected ConfigurationService configurationService;

    public RequestItemHelpdeskStrategy() {
    }

    @Override
    @NonNull
    public List<RequestItemAuthor> getRequestItemAuthor(Context context, Item item)
            throws SQLException {
        boolean helpdeskOverridesSubmitter = configurationService
            .getBooleanProperty("request.item.helpdesk.override", false);
        String helpDeskEmail = configurationService.getProperty("mail.helpdesk");

        if (helpdeskOverridesSubmitter && StringUtils.isNotBlank(helpDeskEmail)) {
            List<RequestItemAuthor> authors = new ArrayList<>(1);
            authors.add(getHelpDeskPerson(context, helpDeskEmail));
            return authors;
        } else {
            //Fallback to default logic (author of Item) if helpdesk isn't fully enabled or setup
            return super.getRequestItemAuthor(context, item);
        }
    }

    /**
     * Return a RequestItemAuthor object for the specified help desk email address.
     * It makes an attempt to find if there is a matching {@link EPerson} for
     * the help desk address, to use its name.  Otherwise it falls back to the
     * {@code helpdeskname} key in {@code Messages.properties}.
     *
     * @param context       context
     * @param helpDeskEmail email
     * @return RequestItemAuthor
     * @throws SQLException if database error
     */
    public RequestItemAuthor getHelpDeskPerson(Context context, String helpDeskEmail)
            throws SQLException {
        context.turnOffAuthorisationSystem();
        EPerson helpdeskEPerson = ePersonService.findByEmail(context, helpDeskEmail);
        context.restoreAuthSystemState();

        if (helpdeskEPerson != null) {
            return new RequestItemAuthor(helpdeskEPerson);
        } else {
            String helpdeskName = I18nUtil.getMessage(
                    "org.dspace.app.requestitem.RequestItemHelpdeskStrategy.helpdeskname",
                    context);
            return new RequestItemAuthor(helpdeskName, helpDeskEmail);
        }
    }
}
