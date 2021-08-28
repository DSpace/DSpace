/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.subscriptions;

import org.apache.commons.cli.ParseException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.utils.DSpace;

import java.sql.SQLException;
import java.util.UUID;


/**
 * Implementation of {@link DSpaceRunnable}  to find subscribed objects and send notification mails about them
 *
 * @author alba aliu
 */
public class SubscriptionEmailNotification extends DSpaceRunnable<SubscriptionEmailNotificationConfiguration<SubscriptionEmailNotification>> {
    private Context context;
    private SubscriptionEmailNotificationService subscriptionEmailNotificationService;

    @Override
    public SubscriptionEmailNotificationConfiguration<SubscriptionEmailNotification> getScriptConfiguration() {
        return new DSpace().getServiceManager().getServiceByName("subscription-send",
                SubscriptionEmailNotificationConfiguration.class);
    }

    @Override
    public void setup() throws ParseException {
        this.subscriptionEmailNotificationService = new DSpace().getServiceManager().getServiceByName(
                SubscriptionEmailNotificationService.class.getName(), SubscriptionEmailNotificationService.class);
    }

    @Override
    public void internalRun() throws Exception {
        assignCurrentUserInContext();
        subscriptionEmailNotificationService.perform(context, handler, commandLine.getArgList().get(1), commandLine.getArgList().get(2));
    }

    protected void assignCurrentUserInContext() throws SQLException {
        context = new Context();
        UUID uuid = getEpersonIdentifier();
        if (uuid != null) {
            EPerson ePerson = EPersonServiceFactory.getInstance().getEPersonService().find(context, uuid);
            context.setCurrentUser(ePerson);
        }
    }
}
