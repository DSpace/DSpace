/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.net.URI;

import org.apache.logging.log4j.Logger;
import org.dspace.app.ldn.LDNRouter;
import org.dspace.app.ldn.model.Notification;
import org.dspace.app.ldn.service.LDNMessageService;
import org.dspace.core.Context;
import org.dspace.web.ContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
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

    @Lazy
    @Autowired
    private LDNRouter router;

    @Autowired
    private LDNMessageService ldnMessageService;

    /**
     * LDN DSpace inbox.
     *
     * @param notification received notification
     * @return ResponseEntity 400 not stored, 201 stored
     * @throws Exception
     */
    @PostMapping(value = "/inbox", consumes = "application/ld+json")
    public ResponseEntity<Object> inbox(@RequestBody Notification notification) throws Exception {
        Context context = ContextUtil.obtainCurrentRequestContext();

        ldnMessageService.create(context, notification.getId());

        log.info("stored notification {} {}",
                notification.getId(),
                notification.getType());

        URI target = new URI(notification.getTarget().getInbox());

        return ResponseEntity.created(target)
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

}
