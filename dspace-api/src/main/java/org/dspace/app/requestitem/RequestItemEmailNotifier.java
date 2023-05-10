/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.requestitem;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import javax.mail.MessagingException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.requestitem.factory.RequestItemServiceFactory;
import org.dspace.app.requestitem.service.RequestItemService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogHelper;
import org.dspace.eperson.EPerson;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Send item requests and responses by email.
 *
 * @author Mark H. Wood <mwood@iupui.edu>
 */
public class RequestItemEmailNotifier {
    private static final Logger LOG = LogManager.getLogger();

    private static final BitstreamService bitstreamService
            = ContentServiceFactory.getInstance().getBitstreamService();

    private static final ConfigurationService configurationService
            = DSpaceServicesFactory.getInstance().getConfigurationService();

    private static final HandleService handleService
            = HandleServiceFactory.getInstance().getHandleService();

    private static final RequestItemService requestItemService
            = RequestItemServiceFactory.getInstance().getRequestItemService();

    private static final RequestItemAuthorExtractor requestItemAuthorExtractor
            = DSpaceServicesFactory.getInstance()
                    .getServiceManager()
                    .getServiceByName("requestItemAuthorExtractor",
                            RequestItemAuthorExtractor.class);

    private RequestItemEmailNotifier() {}

    /**
     * Send the request to the approver(s).
     *
     * @param context current DSpace session.
     * @param ri the request.
     * @param responseLink link back to DSpace to send the response.
     * @throws IOException passed through.
     * @throws SQLException if the message was not sent.
     */
    static public void sendRequest(Context context, RequestItem ri, String responseLink)
            throws IOException, SQLException {
        // Who is making this request?
        List<RequestItemAuthor> authors = requestItemAuthorExtractor
                .getRequestItemAuthor(context, ri.getItem());

        // Build an email to the approver.
        Email email = Email.getEmail(I18nUtil.getEmailFilename(context.getCurrentLocale(),
                "request_item.author"));
        for (RequestItemAuthor author : authors) {
            email.addRecipient(author.getEmail());
        }
        email.setReplyTo(ri.getReqEmail()); // Requester's address

        email.addArgument(ri.getReqName()); // {0} Requester's name

        email.addArgument(ri.getReqEmail()); // {1} Requester's address

        email.addArgument(ri.isAllfiles() // {2} All bitstreams or just one?
            ? I18nUtil.getMessage("itemRequest.all") : ri.getBitstream().getName());

        email.addArgument(handleService.getCanonicalForm(ri.getItem().getHandle())); // {3}

        email.addArgument(ri.getItem().getName()); // {4} requested item's title

        email.addArgument(ri.getReqMessage()); // {5} message from requester

        email.addArgument(responseLink); // {6} Link back to DSpace for action

        StringBuilder names = new StringBuilder();
        StringBuilder addresses = new StringBuilder();
        for (RequestItemAuthor author : authors) {
            if (names.length() > 0) {
                names.append("; ");
                addresses.append("; ");
            }
            names.append(author.getFullName());
            addresses.append(author.getEmail());
        }
        email.addArgument(names.toString()); // {7} corresponding author name
        email.addArgument(addresses.toString()); // {8} corresponding author email

        email.addArgument(configurationService.getProperty("dspace.name")); // {9}

        email.addArgument(configurationService.getProperty("mail.helpdesk")); // {10}

        // Send the email.
        try {
            email.send();
            Bitstream bitstream = ri.getBitstream();
            String bitstreamID;
            if (null == bitstream) {
                bitstreamID = "null";
            } else {
                bitstreamID = ri.getBitstream().getID().toString();
            }
            LOG.info(LogHelper.getHeader(context,
                    "sent_email_requestItem",
                    "submitter_id={},bitstream_id={},requestEmail={}"),
                    ri.getReqEmail(), bitstreamID, ri.getReqEmail());
        } catch (MessagingException e) {
            LOG.warn(LogHelper.getHeader(context,
                    "error_mailing_requestItem", e.getMessage()));
            throw new IOException("Request not sent:  " + e.getMessage());
        }
    }

    /**
     * Send the approver's response back to the requester, with files attached
     * if approved.
     *
     * @param context current DSpace session.
     * @param ri the request.
     * @param subject email subject header value.
     * @param message email body (may be empty).
     * @throws IOException if sending failed.
     */
    static public void sendResponse(Context context, RequestItem ri, String subject,
            String message)
            throws IOException {
        // Build an email back to the requester.
        Email email = new Email();
        email.setContent("body", message);
        email.setSubject(subject);
        email.addRecipient(ri.getReqEmail());
        // Attach bitstreams.
        try {
            if (ri.isAccept_request()) {
                if (ri.isAllfiles()) {
                    Item item = ri.getItem();
                    List<Bundle> bundles = item.getBundles("ORIGINAL");
                    for (Bundle bundle : bundles) {
                        List<Bitstream> bitstreams = bundle.getBitstreams();
                        for (Bitstream bitstream : bitstreams) {
                            if (!bitstream.getFormat(context).isInternal() &&
                                    requestItemService.isRestricted(context,
                                    bitstream)) {
                                email.addAttachment(bitstreamService.retrieve(context,
                                        bitstream), bitstream.getName(),
                                        bitstream.getFormat(context).getMIMEType());
                            }
                        }
                    }
                } else {
                    Bitstream bitstream = ri.getBitstream();
                    email.addAttachment(bitstreamService.retrieve(context, bitstream),
                            bitstream.getName(),
                            bitstream.getFormat(context).getMIMEType());
                }
                email.send();
            } else {
                boolean sendRejectEmail = configurationService
                    .getBooleanProperty("request.item.reject.email", true);
                // Not all sites want the "refusal" to be sent back to the requester via
                // email. However, by default, the rejection email is sent back.
                if (sendRejectEmail) {
                    email.send();
                }
            }
        } catch (MessagingException | IOException | SQLException | AuthorizeException e) {
            LOG.warn(LogHelper.getHeader(context,
                    "error_mailing_requestItem", e.getMessage()));
            throw new IOException("Reply not sent:  " + e.getMessage());
        }
        LOG.info(LogHelper.getHeader(context,
                "sent_attach_requestItem", "token={}"), ri.getToken());
    }

    /**
     * Send, to a repository administrator, a request to open access to a
     * requested object.
     *
     * @param context current DSpace session
     * @param ri the item request that the approver is handling
     * @throws IOException if the message body cannot be loaded or the message
     *          cannot be sent.
     */
    static public void requestOpenAccess(Context context, RequestItem ri)
            throws IOException {
        Email message = Email.getEmail(I18nUtil.getEmailFilename(context.getCurrentLocale(),
                "request_item.admin"));

        // Which Bitstream(s) requested?
        Bitstream bitstream = ri.getBitstream();
        String bitstreamName;
        if (bitstream != null) {
            bitstreamName = bitstream.getName();
        } else {
            bitstreamName = "all"; // TODO localize
        }

        // Which Item?
        Item item = ri.getItem();

        // Fill the message's placeholders.
        EPerson approver = context.getCurrentUser();
        message.addArgument(bitstreamName);          // {0} bitstream name or "all"
        message.addArgument(item.getHandle());       // {1} Item handle
        message.addArgument(ri.getToken());          // {2} Request token
        if (approver != null) {
            message.addArgument(approver.getFullName()); // {3} Approver's name
            message.addArgument(approver.getEmail());    // {4} Approver's address
        } else {
            message.addArgument("anonymous approver");                           // [3] Approver's name
            message.addArgument(configurationService.getProperty("mail.admin")); // [4] Approver's address
        }

        // Who gets this message?
        String recipient;
        EPerson submitter = item.getSubmitter();
        if (submitter != null) {
            recipient = submitter.getEmail();
        } else {
            recipient = configurationService.getProperty("mail.helpdesk");
        }
        if (null == recipient) {
            recipient = configurationService.getProperty("mail.admin");
        }
        message.addRecipient(recipient);

        // Send the message.
        try {
            message.send();
        } catch (MessagingException ex) {
            LOG.warn(LogHelper.getHeader(context, "error_mailing_requestItem",
                    ex.getMessage()));
            throw new IOException("Open Access request not sent:  " + ex.getMessage());
        }
    }
}
