/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Locale;
import javax.mail.MessagingException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.harvest.model.OAIHarvesterReport;
import org.dspace.harvest.service.OAIHarvesterEmailSender;
import org.dspace.harvest.service.OAIHarvesterReportGenerator;
import org.dspace.util.ExceptionMessageUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link OAIHarvesterEmailSender}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OAIHarvesterEmailSenderImpl implements OAIHarvesterEmailSender {

    private static final Logger LOGGER = LogManager.getLogger(OAIHarvesterEmailSenderImpl.class);

    @Autowired
    private OAIHarvesterReportGenerator oaiHarvesterReportGenerator;

    @Override
    public void notifyCompletionWithErrors(String recipient, HarvestedCollection harvestRow,
        OAIHarvesterReport report) {

        if (StringUtils.isEmpty(recipient)) {
            return;
        }

        Object[] args = {
            harvestRow.getCollection().getID(),
            new Date(),
            harvestRow.getHarvestStatus()
        };

        String attachmentName = oaiHarvesterReportGenerator.getName();
        String attachmentMimeType = oaiHarvesterReportGenerator.getMimeType();
        InputStream reportIs = oaiHarvesterReportGenerator.generate(report);

        sendEmail(recipient, "harvesting_completed_with_errors", reportIs, attachmentName, attachmentMimeType, args);
    }

    @Override
    public void notifyFailure(String recipient, HarvestedCollection harvestRow, Exception ex) {

        if (StringUtils.isEmpty(recipient)) {
            return;
        }

        Object[] arguments = {
            harvestRow.getCollection().getID(),
            new Date(),
            harvestRow.getHarvestStatus(),
            ExceptionMessageUtils.getRootMessage(ex),
            ExceptionUtils.getStackTrace(ex)
        };

        sendEmail(recipient, "harvesting_error", arguments);

    }

    private void sendEmail(String recipient, String emailFile, Object... arguments) {
        sendEmail(recipient, emailFile, null, null, null, arguments);
    }

    private void sendEmail(String recipient, String emailFile, InputStream attachment,
        String attachmentName, String attachmentMimeType, Object... arguments) {

        try {

            Email email = Email.getEmail(I18nUtil.getEmailFilename(Locale.getDefault(), emailFile));

            email.addRecipient(recipient);

            for (Object argument : arguments) {
                email.addArgument(argument);
            }

            if (attachment != null) {
                email.addAttachment(attachment, attachmentName, attachmentMimeType);
            }

            email.send();

        } catch (IOException | MessagingException e) {
            LOGGER.error(String.format("Unable to send email alert to %s", recipient), e);
        }
    }

}
