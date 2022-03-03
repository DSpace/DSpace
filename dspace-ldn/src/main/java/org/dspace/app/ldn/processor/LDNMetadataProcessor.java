/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.processor;

import static java.lang.String.format;

import java.io.StringWriter;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.StringResourceLoader;
import org.apache.velocity.runtime.resource.util.StringResourceRepository;
import org.dspace.app.ldn.action.ActionStatus;
import org.dspace.app.ldn.action.LDNAction;
import org.dspace.app.ldn.model.Notification;
import org.dspace.app.ldn.utility.LDNUtils;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.service.HandleService;
import org.dspace.web.ContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class LDNMetadataProcessor implements LDNProcessor {

    private final static Logger log = LogManager.getLogger(LDNMetadataProcessor.class);

    private final static String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    private final VelocityEngine velocityEngine;

    @Autowired
    private ItemService itemService;

    @Autowired
    private HandleService handleService;

    private LDNContextRepeater repeater = new LDNContextRepeater();

    private List<LDNAction> actions = new ArrayList<>();

    private List<LDNMetadataChange> changes = new ArrayList<>();

    private LDNMetadataProcessor() {
        velocityEngine = new VelocityEngine();
        velocityEngine.setProperty(Velocity.RESOURCE_LOADERS, "string");
        velocityEngine.setProperty("resource.loader.string.class", StringResourceLoader.class.getName());
        velocityEngine.init();
    }

    @Override
    public void process(Notification notification) throws Exception {
        Iterator<Notification> iterator = repeater.iterator(notification);

        while (iterator.hasNext()) {
            doProcess(iterator.next());
        }

        ActionStatus operation;
        while (iterator.hasNext()) {
            operation = doRunActions(iterator.next());
            if (operation == ActionStatus.ABORT) {
                break;
            }
        }
    }

    private void doProcess(Notification notification) throws Exception {
        log.info("Processing notification {} {}", notification.getId(), notification.getType());
        Context context = ContextUtil.obtainCurrentRequestContext();

        VelocityContext velocityContext = prepareTemplateContext(notification);

        Item item = lookupItem(context, notification);

        List<MetadataValue> metadataValuesToRemove = new ArrayList<>();

        for (LDNMetadataChange change : changes) {
            String condition = renderTemplate(velocityContext, change.getConditionTemplate());

            boolean proceed = Boolean.parseBoolean(condition);

            if (!proceed) {
                continue;
            }

            if (change instanceof LDNMetadataAdd) {
                LDNMetadataAdd add = ((LDNMetadataAdd) change);
                String value = renderTemplate(velocityContext, add.getValueTemplate());
                log.info(
                        "Adding {}.{}.{} {} {}",
                        add.getSchema(),
                        add.getElement(),
                        add.getQualifier(),
                        add.getLanguage(),
                        value);

                itemService.addMetadata(
                        context,
                        item,
                        add.getSchema(),
                        add.getElement(),
                        add.getQualifier(),
                        add.getLanguage(),
                        value);

            } else if (change instanceof LDNMetadataRemove) {
                LDNMetadataRemove remove = (LDNMetadataRemove) change;

                for (String qualifier : remove.getQualifiers()) {
                    List<MetadataValue> itemMetadata = itemService.getMetadata(
                            item,
                            change.getSchema(),
                            change.getElement(),
                            qualifier,
                            Item.ANY);

                    for (MetadataValue metadatum : itemMetadata) {
                        log.info("From Item {}.{}.{} {} {}",
                                remove.getSchema(),
                                remove.getElement(),
                                qualifier,
                                remove.getLanguage(),
                                metadatum.getValue());
                        boolean delete = true;
                        for (String valueTemplate : remove.getValueTemplates()) {
                            String value = renderTemplate(velocityContext, valueTemplate);
                            if (!metadatum.getValue().contains(value)) {
                                delete = false;
                            }
                        }
                        if (delete) {
                            log.info("Removing {}.{}.{} {} {}",
                                    remove.getSchema(),
                                    remove.getElement(),
                                    qualifier,
                                    remove.getLanguage(),
                                    metadatum.getValue());

                            metadataValuesToRemove.add(metadatum);
                        }
                    }
                }
            }
        }

        if (!metadataValuesToRemove.isEmpty()) {
            itemService.removeMetadataValues(context, item, metadataValuesToRemove);
        }

        context.turnOffAuthorisationSystem();
        try {
            itemService.update(context, item);
            context.commit();
        } finally {
            context.restoreAuthSystemState();
        }
    }

    private ActionStatus doRunActions(Notification notification) throws Exception {
        ActionStatus operation = ActionStatus.CONTINUE;
        for (LDNAction action : actions) {
            log.info("Running action {} for notification {} {}",
                    action.getClass().getSimpleName(),
                    notification.getId(),
                    notification.getType());

            operation = action.execute(notification);
            if (operation == ActionStatus.ABORT) {
                break;
            }
        }

        return operation;
    }

    public LDNContextRepeater getRepeater() {
        return repeater;
    }

    public void setRepeater(LDNContextRepeater repeater) {
        this.repeater = repeater;
    }

    public List<LDNAction> getActions() {
        return actions;
    }

    public void setActions(List<LDNAction> actions) {
        this.actions = actions;
    }

    public List<LDNMetadataChange> getChanges() {
        return changes;
    }

    public void setChanges(List<LDNMetadataChange> changes) {
        this.changes = changes;
    }

    private Item lookupItem(Context context, Notification notification) throws SQLException {
        Item item = null;

        String url = notification.getContext().getId();

        log.info("Looking up item {}", url);

        if (LDNUtils.hasUUIDInURL(url)) {
            UUID uuid = LDNUtils.getUUIDFromURL(url);

            item = itemService.find(context, uuid);

            if (Objects.isNull(item)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        format("Item with uuid %s not found", uuid));
            }

        } else {
            String handle = handleService.resolveUrlToHandle(context, url);

            DSpaceObject object = handleService.resolveToObject(context, handle);

            if (Objects.isNull(object)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        format("Item with handle %s not found", handle));
            }

            if (object.getType() == Constants.ITEM) {
                item = (Item) object;
            } else {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        format("Handle %s does not resolve to an item", handle));
            }
        }

        return item;
    }

    private VelocityContext prepareTemplateContext(Notification notification) {
        VelocityContext velocityContext = new VelocityContext();

        String timestamp = new SimpleDateFormat(DATE_PATTERN).format(Calendar.getInstance().getTime());

        velocityContext.put("notification", notification);
        velocityContext.put("timestamp", timestamp);
        velocityContext.put("LDNUtils", LDNUtils.class);
        velocityContext.put("Objects", Objects.class);
        velocityContext.put("StringUtils", StringUtils.class);

        return velocityContext;
    }

    private String renderTemplate(VelocityContext context, String template) {
        StringWriter writer = new StringWriter();
        StringResourceRepository repository = StringResourceLoader.getRepository();
        repository.putStringResource("template", template);
        velocityEngine.getTemplate("template").merge(context, writer);

        return writer.toString();
    }

}
