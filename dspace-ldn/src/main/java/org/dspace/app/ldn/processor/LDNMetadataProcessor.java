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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.StringResourceLoader;
import org.apache.velocity.runtime.resource.util.StringResourceRepository;
import org.dspace.app.ldn.action.LDNAction;
import org.dspace.app.ldn.model.Notification;
import org.dspace.app.ldn.utility.LDNUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataValueService;
import org.dspace.core.Context;
import org.dspace.web.ContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;

public class LDNMetadataProcessor implements LDNProcessor {

    private final static Logger log = LogManager.getLogger(LDNMetadataProcessor.class);

    private final static String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    private final VelocityEngine velocityEngine;

    @Autowired
    private ItemService itemService;

    @Autowired
    private MetadataFieldService metadataFieldService;

    @Autowired
    private MetadataValueService metadataValueService;

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
        log.info("Processing notification {} {}", notification.getId(), notification.getType());
        Context context = ContextUtil.obtainCurrentRequestContext();

        VelocityContext velocityContext = prepareTemplateContext(notification);

        UUID uuid = LDNUtils.getUUIDFromURL(notification.getContext().getId());

        Item item = itemService.find(context, uuid);

        if (Objects.isNull(item)) {
            throw new ResourceNotFoundException(format("Item with uuid %s not found", uuid));
        }

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
                    "Adding {}.{}.{} {}",
                    add.getSchema(),
                    add.getElement(),
                    add.getQualifier(),
                    add.getLanguage(),
                    value
                );

                itemService.addMetadata(
                    context,
                    item,
                    add.getSchema(),
                    add.getElement(),
                    add.getQualifier(),
                    add.getLanguage(),
                    value
                );

            } else if (change instanceof LDNMetadataRemove) {
                LDNMetadataRemove remove = (LDNMetadataRemove) change;

                for (String qualifier : remove.getQualifiers()) {
                    MetadataField metadataField = metadataFieldService.findByElement(
                        context,
                        change.getSchema(),
                        change.getElement(),
                        qualifier
                    );

                    for (MetadataValue metadataValue : metadataValueService.findByField(context, metadataField)) {
                        boolean delete = true;
                        for (String valueTemplate : remove.getValueTemplates()) {
                            String value = renderTemplate(velocityContext, valueTemplate);
                            if (!metadataValue.getValue().contains(value)) {
                                delete = false;
                            }
                        }
                        if (delete) {
                            log.info(
                                "Removing {}.{}.{} {}",
                                remove.getSchema(),
                                remove.getElement(),
                                qualifier,
                                remove.getLanguage(),
                                metadataValue.getValue()
                            );

                            metadataValuesToRemove.add(metadataValue);
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

    @Override
    public List<LDNAction> getActions() {
        return actions;
    }

    @Override
    public void setActions(List<LDNAction> actions) {
        this.actions = actions;
    }

    public List<LDNMetadataChange> getChanges() {
        return changes;
    }

    public void setChanges(List<LDNMetadataChange> changes) {
        this.changes = changes;
    }

    private VelocityContext prepareTemplateContext(Notification notification) {
        VelocityContext velocityContext = new VelocityContext();

        String timestamp = new SimpleDateFormat(DATE_PATTERN).format(Calendar.getInstance().getTime());

        velocityContext.put("notification", notification);
        velocityContext.put("timestamp", timestamp);
        velocityContext.put("LDNUtils", LDNUtils.class);
        velocityContext.put("Objects", Objects.class);

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
