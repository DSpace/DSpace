/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.subscriptions;

import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.FrequencyType;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.utils.DSpace;

/**
 * Implementation of {@link DSpaceRunnable}  to find subscribed objects and send notification mails about them
 *
 * @author alba aliu
 */
public class SubscriptionEmailNotification
        extends DSpaceRunnable<SubscriptionEmailNotificationConfiguration<SubscriptionEmailNotification>> {

    private Context context;
    private SubscriptionEmailNotificationService subscriptionEmailNotificationService;

    @Override
    @SuppressWarnings("unchecked")
    public SubscriptionEmailNotificationConfiguration<SubscriptionEmailNotification> getScriptConfiguration() {
        return new DSpace().getServiceManager().getServiceByName("subscription-send",
                SubscriptionEmailNotificationConfiguration.class);
    }

    @Override
    public void setup() throws ParseException {
        this.subscriptionEmailNotificationService = new DSpace().getServiceManager().getServiceByName(
              SubscriptionEmailNotificationServiceImpl.class.getName(), SubscriptionEmailNotificationServiceImpl.class);
    }

    @Override
    public void internalRun() throws Exception {
        assignCurrentUserInContext();
        assignSpecialGroupsInContext();
        String frequencyOption = commandLine.getOptionValue("f");
        if (StringUtils.isBlank(frequencyOption)) {
            throw new IllegalArgumentException("Option --frequency (-f) must be set");
        }

        if (!FrequencyType.isSupportedFrequencyType(frequencyOption)) {
            throw new IllegalArgumentException(
                    "Option f must be one of following values D(Day), W(Week) or M(Month)");
        }
        subscriptionEmailNotificationService.perform(getContext(), handler, "content", frequencyOption);
    }

    private void assignCurrentUserInContext() throws SQLException {
        context = new Context();
        UUID uuid = getEpersonIdentifier();
        if (Objects.nonNull(uuid)) {
            EPerson ePerson = EPersonServiceFactory.getInstance().getEPersonService().find(context, uuid);
            context.setCurrentUser(ePerson);
        }
    }

    private void assignSpecialGroupsInContext() throws SQLException {
        for (UUID uuid : handler.getSpecialGroups()) {
            context.setSpecialGroup(uuid);
        }
    }

    public SubscriptionEmailNotificationService getSubscriptionEmailNotificationService() {
        return subscriptionEmailNotificationService;
    }

    public void setSubscriptionEmailNotificationService(SubscriptionEmailNotificationService notificationService) {
        this.subscriptionEmailNotificationService = notificationService;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

}
