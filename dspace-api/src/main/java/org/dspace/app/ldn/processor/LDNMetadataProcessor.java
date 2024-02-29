/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.processor;

import static java.lang.String.format;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.ldn.action.LDNAction;
import org.dspace.app.ldn.action.LDNActionStatus;
import org.dspace.app.ldn.model.Notification;
import org.dspace.app.ldn.utility.LDNUtils;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.service.HandleService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Linked Data Notification metadata processor for consuming notifications. The
 * storage of notification details are within item metadata.
 */
public class LDNMetadataProcessor implements LDNProcessor {

    private final static Logger log = LogManager.getLogger(LDNMetadataProcessor.class);

    @Autowired
    private ItemService itemService;

    @Autowired
    private HandleService handleService;

    private LDNContextRepeater repeater = new LDNContextRepeater();

    private List<LDNAction> actions = new ArrayList<>();

    /**
     * Initialize velocity engine for templating.
     */
    private LDNMetadataProcessor() {

    }

    /**
     * Process notification by repeating over context, processing each context
     * notification, and running actions post processing.
     *
     * @param notification received notification
     * @throws Exception something went wrong processing the notification
     */
    @Override
    public void process(Context context, Notification notification) throws Exception {
        Item item = lookupItem(context, notification);
        runActions(context, notification, item);
    }

    /**
     * Run all actions defined for the processor.
     *
     * @param notification current context notification
     * @param item         associated item
     *
     * @return ActionStatus result status of running the action
     *
     * @throws Exception failed execute the action
     */
    private LDNActionStatus runActions(Context context, Notification notification, Item item) throws Exception {
        LDNActionStatus operation = LDNActionStatus.CONTINUE;
        for (LDNAction action : actions) {
            log.info("Running action {} for notification {} {}",
                    action.getClass().getSimpleName(),
                    notification.getId(),
                    notification.getType());

            operation = action.execute(context, notification, item);
            if (operation == LDNActionStatus.ABORT) {
                break;
            }
        }

        return operation;
    }

    /**
     * @return LDNContextRepeater
     */
    public LDNContextRepeater getRepeater() {
        return repeater;
    }

    /**
     * @param repeater
     */
    public void setRepeater(LDNContextRepeater repeater) {
        this.repeater = repeater;
    }

    /**
     * @return List<LDNAction>
     */
    public List<LDNAction> getActions() {
        return actions;
    }

    /**
     * @param actions
     */
    public void setActions(List<LDNAction> actions) {
        this.actions = actions;
    }

    /**
     * Lookup associated item to the notification context. If UUID in URL, lookup bu
     * UUID, else lookup by handle.
     *
     * @param context      current context
     * @param notification current context notification
     *
     * @return Item associated item
     *
     * @throws SQLException failed to lookup item
     * @throws HttpResponseException redirect failure
     */
    private Item lookupItem(Context context, Notification notification) throws SQLException, HttpResponseException {
        Item item = null;

        String url = null;
        if (notification.getContext() != null) {
            url = notification.getContext().getId();
        } else {
            url = notification.getObject().getId();
        }

        log.info("Looking up item {}", url);

        if (LDNUtils.hasUUIDInURL(url)) {
            UUID uuid = LDNUtils.getUUIDFromURL(url);

            item = itemService.find(context, uuid);

            if (Objects.isNull(item)) {
                throw new HttpResponseException(HttpStatus.SC_NOT_FOUND,
                        format("Item with uuid %s not found", uuid));
            }

        } else {
            String handle = handleService.resolveUrlToHandle(context, url);

            if (Objects.isNull(handle)) {
                throw new HttpResponseException(HttpStatus.SC_NOT_FOUND,
                        format("Handle not found for %s", url));
            }

            DSpaceObject object = handleService.resolveToObject(context, handle);

            if (Objects.isNull(object)) {
                throw new HttpResponseException(HttpStatus.SC_NOT_FOUND,
                        format("Item with handle %s not found", handle));
            }

            if (object.getType() == Constants.ITEM) {
                item = (Item) object;
            } else {
                throw new HttpResponseException(HttpStatus.SC_UNPROCESSABLE_ENTITY,
                        format("Handle %s does not resolve to an item", handle));
            }
        }

        return item;
    }

}