/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn;

import static java.lang.String.format;
import static org.springframework.http.HttpStatus.CREATED;

import java.net.URI;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.ldn.model.Notification;
import org.dspace.app.ldn.processor.LDNProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Controller for LDN Inbox to support COAR Notify.
 */
@RestController
@RequestMapping("/ldn")
// Only enable this controller if "ldn.enabled=true"
@ConditionalOnProperty("ldn.enabled")
public class LDNInboxController {

    private static final Logger log = LogManager.getLogger(LDNInboxController.class);

    @Lazy
    @Autowired
    private LDNRouter router;

    /**
     * LDN DSpace inbox endpoint.
     *
     * @param notification received notification
     * @return ResponseEntity 400 not routable, 201 routed
     * @throws Exception
     */
    @ResponseStatus(value = CREATED)
    @PostMapping(value = "/inbox", consumes = "application/ld+json")
    public ResponseEntity<Object> inbox(@RequestBody Notification notification) throws Exception {

        LDNProcessor processor = router.route(notification);

        if (processor == null) {
            return ResponseEntity.badRequest()
                .body(format("No processor found for type %s", notification.getType()));
        }

        log.info("Routed notification {} {} to {}",
                notification.getId(),
                notification.getType(),
                processor.getClass().getSimpleName());

        processor.process(notification);

        URI target = new URI(notification.getTarget().getInbox());

        return ResponseEntity.created(target)
            .body(format("Successfully routed notification %s %s", notification.getId(), notification.getType()));

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
