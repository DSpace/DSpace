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
import org.dspace.subscriptions.service.DSpaceObjectUpdates;
import org.dspace.subscriptions.service.SubscriptionGenerator;
import org.dspace.utils.DSpace;

import javax.annotation.Resource;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


/**
 * Implementation of {@link DSpaceRunnable}  to find subscribed objects and send notification mails about them
 *
 * @author alba aliu
 */
public class SubscriptionEmailNotification extends DSpaceRunnable<SubscriptionEmailNotificationConfiguration<SubscriptionEmailNotification>> {
    private Context context;
    private SubscriptionEmailNotificationService subscriptionEmailNotificationService;
    @Resource(name = "generators")
    private final Map<String, SubscriptionGenerator> generators = new HashMap<>();
    @Resource(name = "contentUpdates")
    private final Map<String, DSpaceObjectUpdates> contentUpdates = new HashMap<>();

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
        assignSpecialGroupsInContext();

        if ((commandLine.getOptionValue("t") == null || !generators.keySet().contains(commandLine.getOptionValue("t")))
                || (commandLine.getOptionValue("f") == null || !contentUpdates.keySet().contains(commandLine.getOptionValue("f")))) {
            throw new IllegalArgumentException("Options type t and frequency f must be set");
        }
        subscriptionEmailNotificationService.perform(context, handler, commandLine.getOptionValue("t"), commandLine.getOptionValue("f"));
    }

    protected void assignCurrentUserInContext() throws SQLException {
        context = new Context();
        UUID uuid = getEpersonIdentifier();
        if (uuid != null) {
            EPerson ePerson = EPersonServiceFactory.getInstance().getEPersonService().find(context, uuid);
            context.setCurrentUser(ePerson);
        }
    }

    private void assignSpecialGroupsInContext() throws SQLException {
        for (UUID uuid : handler.getSpecialGroups()) {
            context.setSpecialGroup(uuid);
        }
    }
}
