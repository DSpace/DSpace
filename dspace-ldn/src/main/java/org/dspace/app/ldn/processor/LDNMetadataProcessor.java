/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.processor;

import static java.lang.String.format;
import static org.dspace.app.ldn.utility.LDNUtils.DATE_PATTERN;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.StringResourceLoader;
import org.dspace.app.ldn.action.ActionStatus;
import org.dspace.app.ldn.action.LDNAction;
import org.dspace.app.ldn.model.Notification;
import org.dspace.app.ldn.utility.LDNUtils;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.service.HandleService;
import org.dspace.services.ConfigurationService;
import org.dspace.web.ContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

/**
 * Linked Data Notification metadata processor for consuming notifications. The
 * storage of notification details are within item metadata.
 * 
 * @author William Welling
 * @author Stefano Maffei (4Science.com)
 * 
 */
public class LDNMetadataProcessor implements LDNProcessor {

    private final static Logger log = LogManager.getLogger(LDNMetadataProcessor.class);

    private final static String LOCATION_HEADER_KEY = "Location";

    private final VelocityEngine velocityEngine;

    private final RestTemplate restTemplate;

    @Autowired
    private ItemService itemService;

    @Autowired
    private HandleService handleService;

    @Autowired
    private ConfigurationService configurationService;

    private LDNContextRepeater repeater = new LDNContextRepeater();

    private List<LDNAction> actions = new ArrayList<>();

    private List<LDNMetadataChange> changes = new ArrayList<>();

    private List<String> allowedExternalResolverUrls = new ArrayList<>();

    private String dspaceUIUrl;

    /**
     * Initialize velocity engine for templating.
     */
    private LDNMetadataProcessor() {
        velocityEngine = new VelocityEngine();
        velocityEngine.setProperty(Velocity.RESOURCE_LOADERS, "string");
        velocityEngine.setProperty("resource.loader.string.class", StringResourceLoader.class.getName());
        velocityEngine.init();
        restTemplate = new RestTemplate();
    }

    @PostConstruct
    public void init() {
        String resolverUrls = configurationService.getProperty("ldn.notify.allowed-external-resolver-urls");
        if (resolverUrls != null) {
            for (String allowedExternalResolverUrl : resolverUrls.split(",")) {
                this.allowedExternalResolverUrls.add(allowedExternalResolverUrl.trim());
            }
        }

        this.dspaceUIUrl = configurationService.getProperty("dspace.ui.url");
    }

    /**
     * Process notification by repeating over context, processing each context
     * notification, and running actions post processing.
     *
     * @param notification received notification
     * @throws Exception something went wrong processing the notification
     */
    @Override
    public void process(Notification notification) throws Exception {
        Iterator<Notification> iterator = repeater.iterator(notification);

        while (iterator.hasNext()) {
            Notification contextNotification = iterator.next();
            Item item = doProcess(contextNotification);
            runActions(contextNotification, item);
        }
    }

    /**
     * Perform the actual notification processing. Applies all defined metadata
     * changes.
     *
     * @param notification current context notification
     * @return Item associated item which persist notification details
     * @throws Exception failed to process notification
     */
    private Item doProcess(Notification notification) throws Exception {
        log.info("Processing notification {} {}", notification.getId(), notification.getType());
        Context context = ContextUtil.obtainCurrentRequestContext();

        VelocityContext velocityContext = prepareTemplateContext(notification);

        Item item = lookupItem(context, notification);

        for (LDNMetadataChange change : changes) {
            String condition = change.renderTemplate(velocityContext, velocityEngine, change.getConditionTemplate());

            boolean proceed = Boolean.parseBoolean(condition);

            if (!proceed) {
                continue;
            }

            change.doAction(velocityContext, velocityEngine, context, item);
        }

        context.turnOffAuthorisationSystem();
        try {
            itemService.update(context, item);
            context.commit();
        } finally {
            context.restoreAuthSystemState();
        }

        return item;
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
    private ActionStatus runActions(Notification notification, Item item) throws Exception {
        ActionStatus operation = ActionStatus.CONTINUE;
        for (LDNAction action : actions) {
            log.info("Running action {} for notification {} {}",
                    action.getClass().getSimpleName(),
                    notification.getId(),
                    notification.getType());

            operation = action.execute(notification, item);
            if (operation == ActionStatus.ABORT) {
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
     * @return List<LDNMetadataChange>
     */
    public List<LDNMetadataChange> getChanges() {
        return changes;
    }

    /**
     * @param changes
     */
    public void setChanges(List<LDNMetadataChange> changes) {
        this.changes = changes;
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
     */
    private Item lookupItem(Context context, Notification notification) throws SQLException {
        Item item = null;

        String url = this.resolveContext(notification);

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

            if (Objects.isNull(handle)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        format("Handle not found for %s", url));
            }

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

    /**
     * Attempts to resolve context id externally if not already a DSpace URL.
     * Context id must resolve with an appropriate DSpace URL in Location header.
     *
     * @param notification current context notification
     * @return external resolved DSpace URL
     */
    private String resolveContext(Notification notification) {
        String url = notification.getContext().getId();
        if (isExternalContextId(url)) {
            try {
                URI uri = new URI(url);
                if (isAllowedExternalContextId(url)) {
                    log.info("Attempting to resolve external context id {}", url);
                    HttpHeaders headers = this.restTemplate.headForHeaders(uri);
                    if (headers.containsKey(LOCATION_HEADER_KEY)) {
                        url = headers.getFirst(LOCATION_HEADER_KEY);
                    } else {
                        log.error("External context id {} HEAD response did not contain Location header", url);
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                format("Invalid context id %s", url));
                    }
                } else {
                    String message = format("Context id %s not allowed for external dereference", url);
                    log.error(message);
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
                }
            } catch (NullPointerException | URISyntaxException | RestClientException e) {
                log.error(format("Failed to resolve context id %s", url), e);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        format("Failed to resolve context id %s: %s", url, e.getMessage()));
            }
        }

        return url;
    }

    /**
     * Determine if the context id is an external context id by checking if it does
     * not start with the DSpace UI URL.
     * 
     * @param url context id
     * @return whether context id is external
     */
    private boolean isExternalContextId(String url) {
        return !url.startsWith(this.dspaceUIUrl);
    }

    /**
     * Determine if external context id is allowed by checking if starts with one of
     * the allowed external resolver urls.
     * 
     * @param url external context id
     * @return whether external context id is allowed
     */
    private boolean isAllowedExternalContextId(String url) {
        boolean allowExternalContextId = false;
        for (String allowedExternalResolverUrl : this.allowedExternalResolverUrls) {
            if (url.startsWith(allowedExternalResolverUrl)) {
                allowExternalContextId = true;
                break;
            }
        }

        return allowExternalContextId;
    }

    /**
     * Prepare velocity template context with notification, timestamp and some
     * static utilities.
     *
     * @param notification current context notification
     * @return VelocityContext prepared velocity context
     */
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


}
