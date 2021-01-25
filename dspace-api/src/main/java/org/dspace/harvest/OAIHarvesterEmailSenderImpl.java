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
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import javax.mail.MessagingException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.CollectionService;
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

    public static final String COMPLETED_WITH_ERRORS_TEMPLATE = "harvesting_completed_with_errors";

    public static final String ERROR_TEMPLATE = "harvesting_error";

    private static final Logger LOGGER = LogManager.getLogger(OAIHarvesterEmailSenderImpl.class);

    @Autowired
    private OAIHarvesterReportGenerator oaiHarvesterReportGenerator;

    @Autowired
    private CollectionService collectionService;

    @Override
    public void notifyCompletionWithErrors(String recipient, HarvestedCollection harvestRow,
        OAIHarvesterReport report) {

        if (StringUtils.isEmpty(recipient)) {
            return;
        }

        List<String> ccAddress = getCcAddress(harvestRow.getCollection());

        Object[] args = {
            harvestRow.getCollection().getID(),
            new Date(),
            harvestRow.getHarvestStatus()
        };

        String attachmentName = oaiHarvesterReportGenerator.getName();
        String attachmentMimeType = oaiHarvesterReportGenerator.getMimeType();
        InputStream is = oaiHarvesterReportGenerator.generate(report);

        sendEmail(recipient, ccAddress, COMPLETED_WITH_ERRORS_TEMPLATE, is, attachmentName, attachmentMimeType, args);
    }

    @Override
    public void notifyFailure(String recipient, HarvestedCollection harvestRow, Exception ex) {

        if (StringUtils.isEmpty(recipient)) {
            return;
        }

        List<String> ccAddress = getCcAddress(harvestRow.getCollection());

        Object[] arguments = {
            harvestRow.getCollection().getID(),
            new Date(),
            harvestRow.getHarvestStatus(),
            ExceptionMessageUtils.getRootMessage(ex),
            ExceptionUtils.getStackTrace(ex)
        };

        sendEmail(recipient, ccAddress, ERROR_TEMPLATE, arguments);

    }

    private void sendEmail(String recipient, List<String> ccAddresses, String template, Object... arguments) {
        sendEmail(recipient, ccAddresses, template, null, null, null, arguments);
    }

    private void sendEmail(String recipient, List<String> ccAddresses, String template, InputStream attachment,
        String attachmentName, String attachmentMimeType, Object... arguments) {

        try {

            Email email = Email.getEmail(I18nUtil.getEmailFilename(Locale.getDefault(), template));

            email.addRecipient(recipient);

            ccAddresses.forEach(address -> email.addCcAddress(address));

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

    private List<String> getCcAddress(Collection collection) {
        return collectionService.getMetadata(collection, "cris", "harvesting", "ccAddress", Item.ANY).stream()
            .map(MetadataValue::getValue)
            .collect(Collectors.toList());
    }

}
