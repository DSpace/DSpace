/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;
import java.io.IOException;
import java.util.Date;
import java.util.Objects;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.dspace.content.service.FeedbackService;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;

/**
 * Implementation of {@link FeedbackService} interface.
 * It is responsible for sendint a feedback email with content a DSpace user
 * fills from feedback section of DSpace.
 */
public class FeedbackServiceImpl implements FeedbackService {

    @Override
    public void sendEmail(Context context, HttpServletRequest request, String recipientEmail, String senderEmail,
            String message, String page) throws IOException, MessagingException {
        String session = request.getHeader("x-correlation-id");
        String agent = request.getHeader("User-Agent");
        String currentUserEmail = StringUtils.EMPTY;

        if (Objects.nonNull(context.getCurrentUser())) {
            currentUserEmail = context.getCurrentUser().getEmail();
        }
        Email email = Email.getEmail(I18nUtil.getEmailFilename(context.getCurrentLocale(), "feedback"));
        email.addRecipient(recipientEmail);
        email.addArgument(new Date());         // Date
        email.addArgument(senderEmail);       // Email
        email.addArgument(currentUserEmail); // Logged in as
        email.addArgument(page);            // Referring page
        email.addArgument(agent);          // User agent
        email.addArgument(session);       // Session ID
        email.addArgument(message);      // The feedback itself
        email.send();
    }

}