/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn;

import java.sql.SQLException;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.apache.logging.log4j.Logger;

import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.web.ContextUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;

/**
 * Controller for LDN Inbox to support COAR Notify.
 *
 */
@RestController
@RequestMapping("/ldn")
// Only enable this controller if "ldn.enabled=true"
@ConditionalOnProperty("ldn.enabled")
public class LDNController {

    private static final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(LDNController.class);

    @Autowired
    ItemService itemService;

    @PostConstruct
    public void init() {
        log.info("\n\n\nLDN INIT\n\n\n");
    }

    @GetMapping(value = "/{id}")
    public Item inbox(@PathVariable UUID id) throws ResourceNotFoundException {
        log.info("LDN lookup item {}", id);
        Context context = ContextUtil.obtainCurrentRequestContext();
        Item item;
        try {
            item = itemService.find(context, id);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (item == null) {
            log.info("item {} not found", id);
            throw new ResourceNotFoundException("Item with id " + id + " not found");
        }
        return item;
    }

}
