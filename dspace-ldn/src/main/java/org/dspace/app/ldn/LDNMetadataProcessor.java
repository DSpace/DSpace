/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.ldn.model.Notification;
import org.dspace.app.ldn.utility.LDNUtils;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.web.ContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;

public class LDNMetadataProcessor implements LDNProcessor {

    private static final Logger log = LogManager.getLogger(LDNProcessor.class);

    @Autowired
    private ItemService itemService;

    private List<LDNAction> actions = new ArrayList<>();

    @Override
    public List<LDNAction> getActions() {
        return actions;
    }

    @Override
    public void setActions(List<LDNAction> actions) {
        this.actions = actions;
    }

    @Override
    public void process(Notification notification) {

        log.info("ItemService {}", itemService);

        Context context = ContextUtil.obtainCurrentRequestContext();

        log.info("Context {}", context);

        log.info("Object id {}", notification.getObject().getId());

        UUID uuid = LDNUtils.getUUIDFromURL(notification.getObject().getId());

        log.info("Item uuid {}", uuid);

        Item item;

        try {
            item = itemService.find(context, uuid);

            log.info("Item {}", item);

        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (Objects.isNull(item)) {
            throw new ResourceNotFoundException("Item with uuid " + uuid + " not found");
        }

    }

}
