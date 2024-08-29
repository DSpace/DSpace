/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.action;

import static java.lang.String.format;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.ldn.model.Notification;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Action to send email to recipients provided in actionSendFilter. The email
 * body will be result of templating actionSendFilter.
 */
public class LDNEmailAction implements LDNAction {

    private static final Logger log = LogManager.getLogger(LDNEmailAction.class);

    private final static String DATE_PATTERN = "dd-MM-yyyy HH:mm:ss";

    @Autowired
    private ConfigurationService configurationService;

    /*
     * Supported for actionSendFilter are:
     * - <single email>
     * - GROUP:<group_name>
     * - SUBMITTER
     */
    private String actionSendFilter;

    // The file name for the requested email
    private String actionSendEmailTextFile;

    /**
     * Execute sending an email.
     *
     * Template context parameters:
     *
     * {0} Service Name
     * {1} Item Name
     * {2} Service URL
     * {3} Item URL
     * {4} Submitter's Name
     * {5} Date of the received LDN notification
     * {6} LDN notification
     * {7} Item
     *
     * @param notification
     * @param item
     * @return ActionStatus
     * @throws Exception
     */
    @Override
    public LDNActionStatus execute(Context context, Notification notification, Item item) throws Exception {
        try {
            Locale supportedLocale = I18nUtil.getEPersonLocale(context.getCurrentUser());
            Email email = Email.getEmail(I18nUtil.getEmailFilename(supportedLocale, actionSendEmailTextFile));

            // Setting recipients email
            for (String recipient : retrieveRecipientsEmail(item)) {
                email.addRecipient(recipient);
            }

            String date = new SimpleDateFormat(DATE_PATTERN).format(Calendar.getInstance().getTime());

            email.addArgument(notification.getActor().getName());
            email.addArgument(item.getName());
            email.addArgument(notification.getActor().getId());
            email.addArgument(notification.getContext() != null ?
                notification.getContext().getId() : notification.getObject().getId());
            email.addArgument(item.getSubmitter().getFullName());
            email.addArgument(date);
            email.addArgument(notification);
            email.addArgument(item);

            email.send();
        } catch (Exception e) {
            log.error("An Error Occurred while sending a notification email", e);
        }

        return LDNActionStatus.CONTINUE;
    }

    /**
     * @return String
     */
    public String getActionSendFilter() {
        return actionSendFilter;
    }

    /**
     * @param actionSendFilter
     */
    public void setActionSendFilter(String actionSendFilter) {
        this.actionSendFilter = actionSendFilter;
    }

    /**
     * @return String
     */
    public String getActionSendEmailTextFile() {
        return actionSendEmailTextFile;
    }

    /**
     * @param actionSendEmailTextFile
     */
    public void setActionSendEmailTextFile(String actionSendEmailTextFile) {
        this.actionSendEmailTextFile = actionSendEmailTextFile;
    }

    /**
     * Parses actionSendFilter for reserved tokens and returns list of email
     * recipients.
     *
     * @param item the item which to get submitter email
     * @return List<String> list of email recipients
     */
    private List<String> retrieveRecipientsEmail(Item item) {
        List<String> recipients = new LinkedList<String>();

        if (actionSendFilter.startsWith("SUBMITTER")) {
            recipients.add(item.getSubmitter().getEmail());
        } else if (actionSendFilter.startsWith("GROUP:")) {
            String groupName = actionSendFilter.replace("GROUP:", "");
            String property = format("email.%s.list", groupName);
            String[] groupEmails = configurationService.getArrayProperty(property);
            recipients = Arrays.asList(groupEmails);
        } else {
            recipients.add(actionSendFilter);
        }

        return recipients;
    }

}