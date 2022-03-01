/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.action;

import static java.lang.String.format;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.ldn.model.Notification;
import org.dspace.app.ldn.utility.LDNUtils;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.services.ConfigurationService;
import org.dspace.web.ContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;

public class LDNEmailAction implements LDNAction {

    private static final Logger log = LogManager.getLogger(LDNEmailAction.class);

    private final static String DATE_PATTERN = "dd-MM-yyyy HH:mm:ss";

    @Autowired
    private ItemService itemService;

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

    @Override
    public ActionStatus execute(Notification notification) throws Exception {
        Context context = ContextUtil.obtainCurrentRequestContext();

        try {
            UUID uuid = LDNUtils.getUUIDFromURL(notification.getContext().getId());

            Item item = itemService.find(context, uuid);

            if (Objects.isNull(item)) {
                throw new ResourceNotFoundException(format("Item with uuid %s not found", uuid));
            }

            Locale supportedLocale = I18nUtil.getEPersonLocale(context.getCurrentUser());
            Email email = Email.getEmail(I18nUtil.getEmailFilename(supportedLocale, actionSendEmailTextFile));

            // Setting recipients email
            for (String recipient : retrieveRecipientsEmail(item)) {
                email.addRecipient(recipient);
            }

            String date = new SimpleDateFormat(DATE_PATTERN).format(Calendar.getInstance().getTime());

            // # Parameters: {0} Service Name
            // #             {1} Item Name
            // #             {2} Service URL
            // #             {3} Item URL
            // #             {4} Submitter's Name
            // #             {5} Date of the received LDN notification
            // #             {6} LDN notification
            // #             {7} Item

            email.addArgument(notification.getActor().getName());
            email.addArgument(item.getName());
            email.addArgument(notification.getActor().getId());
            email.addArgument(notification.getContext().getId());
            email.addArgument(item.getSubmitter().getFullName());
            email.addArgument(date);
            email.addArgument(notification);
            email.addArgument(item);

            email.send();
        } catch (Exception e) {
            log.error("An Error Occurred while sending a notification email", e);
        }

        return ActionStatus.CONTINUE;
    }

    public String getActionSendFilter() {
        return actionSendFilter;
    }

    public void setActionSendFilter(String actionSendFilter) {
        this.actionSendFilter = actionSendFilter;
    }

    public String getActionSendEmailTextFile() {
        return actionSendEmailTextFile;
    }

    public void setActionSendEmailTextFile(String actionSendEmailTextFile) {
        this.actionSendEmailTextFile = actionSendEmailTextFile;
    }

    private List<String> retrieveRecipientsEmail(Item item) throws SQLException {
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
