/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.util.regex.Pattern;

import org.apache.commons.validator.routines.UrlValidator;
import org.apache.logging.log4j.Logger;
import org.dspace.app.ldn.LDNMessageEntity;
import org.dspace.app.ldn.LDNRouter;
import org.dspace.app.ldn.model.Notification;
import org.dspace.app.ldn.processor.LDNProcessor;
import org.dspace.app.ldn.service.LDNMessageService;
import org.dspace.app.rest.exception.InvalidLDNMessageException;
import org.dspace.core.Context;
import org.dspace.web.ContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/ldn")
@ConditionalOnProperty("ldn.enabled")
public class LDNInboxController {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger();

    @Autowired
    private LDNRouter router;

    @Autowired
    private LDNMessageService ldnMessageService;

    /**
     * LDN DSpace inbox.
     *
     * @param notification received notification
     * @return ResponseEntity 400 not stored, 202 stored
     * @throws Exception
     */
    @PostMapping(value = "/inbox", consumes = "application/ld+json")
    public ResponseEntity<Object> inbox(@RequestBody Notification notification) throws Exception {
        Context context = ContextUtil.obtainCurrentRequestContext();
        validate(notification);

        log.info("stored notification {} {}", notification.getId(), notification.getType());
        context.commit();

        LDNMessageEntity ldnMsgEntity = ldnMessageService.create(context, notification);
        log.info("stored ldn message {}", ldnMsgEntity);
        context.commit();

        if (ldnMsgEntity.getQueueStatus() != LDNMessageEntity.QUEUE_STATUS_UNTRUSTED) {
            LDNProcessor processor = router.route(ldnMsgEntity);
            if (processor == null) {
                log.error(String.format("No processor found for type %s", notification.getType()));
                return ResponseEntity.badRequest()
                    .body(String.format("No processor found for type %s", notification.getType()));
            } else {
                processor.process(notification);
            }
        }
        return ResponseEntity.accepted()
            .body(String.format("Successfully stored notification %s %s",
                notification.getId(), notification.getType()));
    }

    /**
     * LDN DSpace inbox options.
     *
     * @return ResponseEntity 200 with allow and accept-post headers
     */
    @RequestMapping(value = "/inbox", method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> options() {
        return ResponseEntity.ok()
            .allow(HttpMethod.OPTIONS, HttpMethod.POST)
            .header("Accept-Post", "application/ld+json")
            .build();
    }

    /**
     * @param e
     * @return ResponseEntity<String>
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> handleResponseStatusException(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatus().value())
                .body(e.getMessage());
    }

    private void validate(Notification notification) {
        String id = notification.getId();
        Pattern URNRegex =
            Pattern.compile("^urn:uuid:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

        if (!URNRegex.matcher(id).matches() && !new UrlValidator().isValid(id)) {
            throw new InvalidLDNMessageException("Invalid URI format for 'id' field.");
        }

        if (notification.getOrigin() == null || notification.getTarget() == null || notification.getObject() == null) {
            throw new InvalidLDNMessageException("Origin or Target or Object is missing");
        }
    }

}
