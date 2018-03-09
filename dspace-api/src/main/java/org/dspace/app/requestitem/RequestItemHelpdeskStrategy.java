/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.requestitem;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;

/**
 * RequestItem strategy to allow DSpace support team's helpdesk to receive requestItem request
 * With this enabled, then the Item author/submitter doesn't receive the request, but the helpdesk instead does.
 *
 * Failover to the RequestItemSubmitterStrategy, which means the submitter would get the request if there is no specified helpdesk email.
 *
 *  @author Sam Ottenhoff
 *  @author Peter Dietz
 */
public class RequestItemHelpdeskStrategy extends RequestItemSubmitterStrategy {

    private Logger log = Logger.getLogger(RequestItemHelpdeskStrategy.class);

    @Autowired(required = true)
    protected EPersonService ePersonService;

    public RequestItemHelpdeskStrategy() {}

    @Override
    public RequestItemAuthor getRequestItemAuthor(Context context, Item item) throws SQLException {
        boolean helpdeskOverridesSubmitter = ConfigurationManager.getBooleanProperty("request.item.helpdesk.override", false);
        String helpDeskEmail = ConfigurationManager.getProperty("mail.helpdesk");

        if (helpdeskOverridesSubmitter && StringUtils.isNotBlank(helpDeskEmail)) {
            return getHelpDeskPerson(context, helpDeskEmail);
        } else {
            //Fallback to default logic (author of Item) if helpdesk isn't fully enabled or setup
            return super.getRequestItemAuthor(context, item);
        }
    }

    /**
     * Return a RequestItemAuthor object for the specified helpdesk email address.
     * It makes an attempt to find if there is a matching eperson for the helpdesk address, to use the name,
     * Otherwise it falls back to a helpdeskname key in the Messages.props.
     * @param context context
     * @param helpDeskEmail email
     * @return RequestItemAuthor
     * @throws SQLException if database error
     */
    public RequestItemAuthor getHelpDeskPerson(Context context, String helpDeskEmail) throws SQLException{
        EPerson helpdeskEPerson = null;

        context.turnOffAuthorisationSystem();
        helpdeskEPerson = ePersonService.findByEmail(context, helpDeskEmail);
        context.restoreAuthSystemState();

        if(helpdeskEPerson != null) {
            return new RequestItemAuthor(helpdeskEPerson);
        } else {
            String helpdeskName = I18nUtil.getMessage(
                "org.dspace.app.requestitem.RequestItemHelpdeskStrategy.helpdeskname",
                context);
            return new RequestItemAuthor(helpdeskName, helpDeskEmail);
        }
    }
}
