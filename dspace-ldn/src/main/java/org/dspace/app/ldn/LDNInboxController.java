/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn;

import static org.springframework.http.HttpStatus.CREATED;

import org.dspace.app.ldn.model.Notification;
import org.dspace.app.ldn.processor.LDNProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
 *
 */
@RestController
@RequestMapping("/ldn")
// Only enable this controller if "ldn.enabled=true"
@ConditionalOnProperty("ldn.enabled")
public class LDNInboxController {

    @ResponseStatus(value = CREATED)
    @PostMapping(value = "/inbox", consumes = "application/ld+json", produces = "application/ld+json")
    public Notification inbox(@RequestBody Notification notification, LDNProcessor processor) throws Exception {

        processor.process(notification);

        // TODO: this should become either generic response body with location header or no content with location header
        return notification;
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> handleResponseStatusException(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatus().value())
                .body(e.getMessage());
    }

}
