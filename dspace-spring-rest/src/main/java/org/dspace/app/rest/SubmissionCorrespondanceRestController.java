/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.UUID;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.DCDate;
import org.dspace.content.Item;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * This is a specialized controller to support correspondance between the
 * submitter and the repository staff (controllers and administrators)
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@RestController
@RequestMapping("/api/messages")
public class SubmissionCorrespondanceRestController implements InitializingBean {
    @Autowired
    DiscoverableEndpointsService discoverableEndpointsService;

    @Autowired
    ItemService itemService;

    @Autowired
    BundleService bundleService;

    @Autowired
    BitstreamService bitstreamService;

    @Autowired
    BitstreamFormatService bitstreamFormatService;


    @Autowired
    GroupService groupService;

    @Autowired
    AuthorizeService authorizeService;

    private static final Logger log = Logger.getLogger(SubmissionCorrespondanceRestController.class);

    @Override
    public void afterPropertiesSet() {
        discoverableEndpointsService.register(this, Arrays
            .asList(new Link("/api/messages", "messages"), new Link("/api/messages/read", "messages-read"),
                new Link("/api/messages/unread", "messages-unread")));
    }

    @RequestMapping(method = { RequestMethod.POST })
    public void create(@RequestParam(required = true) UUID uuid, @RequestParam(required = true) String subject,
                       @RequestParam(required = true) String description, HttpServletResponse response,
                       HttpServletRequest request) throws SQLException, AuthorizeException, IOException {

        Context context = ContextUtil.obtainContext(request);
        Item item = itemService.find(context, uuid);

        checkIfSubmitterOrController(context, item);

        context.turnOffAuthorisationSystem();
        Bundle message = null;
        for (Bundle bnd : item.getBundles()) {
            if ("MESSAGE".equals(bnd.getName())) {
                message = bnd;
                break;
            }
        }
        if (message == null) {
            message = bundleService.create(context, item, "MESSAGE");
        }

        BitstreamFormat bitstreamFormat = bitstreamFormatService.findByMIMEType(context, "text/plain");

        InputStream is = new ByteArrayInputStream(description.getBytes(StandardCharsets.UTF_8));
        Bitstream bitMessage = bitstreamService.create(context, message, is);
        bitMessage.setFormat(context, bitstreamFormat);
        bitstreamService.addMetadata(context, bitMessage, "dc", "title", null, null, subject);
        bitstreamService.addMetadata(context, bitMessage, "dc", "creator", null, null,
            context.getCurrentUser().getFullName() + " <" + context.getCurrentUser().getEmail() + ">");
        bitstreamService
            .addMetadata(context, bitMessage, "dc", "date", "issued", null, new DCDate(new Date()).toString());

        Set<EPerson> toSet = new HashSet<EPerson>();
        Group controllers = groupService.findByName(context, Group.CONTROLLERS);
        Group administrators = groupService.findByName(context, Group.ADMIN);
        if (isSubmitter(context, item)) {
            bitstreamService.addMetadata(context, bitMessage, "dc", "type", null, null, "inbound");
            toSet.add(item.getSubmitter());
        } else {
            bitstreamService.addMetadata(context, bitMessage, "dc", "type", null, null, "outbound");
            toSet.addAll(controllers.getMembers());
            toSet.addAll(administrators.getMembers());
        }
        bitstreamService.update(context, bitMessage);
        authorizeService.addPolicy(context, bitMessage, Constants.READ, context.getCurrentUser());

        if (controllers != null) {
            authorizeService.addPolicy(context, bitMessage, Constants.READ, controllers);
        }
        notify(context, item, toSet, subject, description);
        context.commit();
        context.restoreAuthSystemState();
    }

    @RequestMapping(method = { RequestMethod.POST }, value = "/read")
    public void read(@RequestParam(required = true) UUID uuid, HttpServletResponse response, HttpServletRequest request)
        throws SQLException, AuthorizeException {
        Context context = ContextUtil.obtainContext(request);
        Bitstream bitMessage = bitstreamService.find(context, uuid);

        System.out.println(context.getCurrentUser());
        boolean inbound = "inbound".contentEquals(bitMessage.getMetadata("dc.type"));

        Item item = (Item) bitMessage.getParentObject();
        if (inbound) {
            checkIfController(context, item);

        } else {
            checkIfSubmitter(context, item);
        }
        context.turnOffAuthorisationSystem();
        bitstreamService
            .addMetadata(context, bitMessage, "dc", "date", "accessioned", null, new DCDate(new Date()).toString());
        context.commit();
        context.restoreAuthSystemState();
    }

    @RequestMapping(method = { RequestMethod.POST }, value = "/unread")
    public void unread(@RequestParam(required = true) UUID uuid, HttpServletResponse response,
                       HttpServletRequest request) throws SQLException, AuthorizeException {
        Context context = ContextUtil.obtainContext(request);
        Bitstream bitMessage = bitstreamService.find(context, uuid);

        boolean inbound = "inbound".contentEquals(bitMessage.getMetadata("dc.type"));

        Item item = (Item) bitMessage.getParentObject();
        if (inbound) {
            checkIfController(context, item);
        } else {
            checkIfSubmitter(context, item);
        }

        context.turnOffAuthorisationSystem();
        bitstreamService.clearMetadata(context, bitMessage, "dc", "date", "accessioned", null);
        context.commit();
        context.restoreAuthSystemState();
    }

    private void checkIfController(Context context, Item item) throws SQLException, AuthorizeException {
        if (!isController(context, item) && !authorizeService.isAdmin(context)) {
            throw new AuthorizeException("Only controllers or administrators can do that!");
        }
    }

    private void checkIfSubmitter(Context context, Item item) throws AuthorizeException {
        if (!isSubmitter(context, item)) {
            throw new AuthorizeException("Only the submitter can do that!");
        }
    }

    private void checkIfSubmitterOrController(Context context, Item item) throws AuthorizeException, SQLException {
        if (!isSubmitter(context, item) && !isController(context, item)) {
            throw new AuthorizeException("Only the submitter or a controller can add correspondence to an item");
        }
    }

    private boolean isSubmitter(Context context, Item item) {
        if (context.getCurrentUser() == null) {
            return false;
        }
        return item.getSubmitter().equals(context.getCurrentUser());
    }

    private boolean isController(Context context, Item item) throws SQLException {
        if (context.getCurrentUser() == null) {
            return false;
        }
        if (groupService.isMember(context, Group.CONTROLLERS) || authorizeService.isAdmin(context)) {
            return true;
        } else {
            return false;
        }
    }

    private void notify(Context context, Item item, Set<EPerson> epersons, String subject, String text)
        throws SQLException, IOException {
        for (EPerson ep : epersons) {
            try {
                // Get the Locale
                Locale supportedLocale = I18nUtil.getEPersonLocale(ep);
                Email email = Email.getEmail(I18nUtil.getEmailFilename(supportedLocale, "submit_correspondence"));

                // Get title
                String title = item.getName();
                if (StringUtils.isBlank(title)) {
                    try {
                        title = I18nUtil.getMessage("org.dspace.workflow.WorkflowManager.untitled");
                    } catch (MissingResourceException e) {
                        title = "Untitled";
                    }
                }

                email.addRecipient(ep.getEmail());
                email.addArgument(title);
                email.addArgument(subject);
                email.addArgument(text);
                email.send();
            } catch (MessagingException e) {
                log.warn(LogManager.getHeader(context, "notifyOfCorrespondence",
                    "cannot email user=" + ep.getEmail() + "; item_id=" + item.getID() + ":  " + e.getMessage()));
            }
        }
    }

}