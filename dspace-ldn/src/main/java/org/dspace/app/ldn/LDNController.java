/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn;

import static org.springframework.http.HttpStatus.CREATED;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.ldn.model.Notification;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.web.ContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for LDN Inbox to support COAR Notify.
 *
 */
@RestController
@RequestMapping("/ldn")
// Only enable this controller if "ldn.enabled=true"
@ConditionalOnProperty("ldn.enabled")
public class LDNController {

    private static final Logger log = LogManager.getLogger(LDNController.class);

    @Autowired
    ItemService itemService;

    @PostMapping(value = "/inbox", consumes = "application/ld+json", produces = "application/ld+json")
    @ResponseStatus(value = CREATED)
    @PreAuthorize("@LDNAuthorize.isAllowed()")
    public Notification inbox(@RequestBody Notification notification) throws ResourceNotFoundException {
        Context context = ContextUtil.obtainCurrentRequestContext();

        return notification;
    }

}
