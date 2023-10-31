/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn;

import static java.lang.String.format;
import static java.lang.String.join;
import static org.dspace.app.ldn.RdfMediaType.APPLICATION_JSON_LD;
import static org.dspace.app.ldn.utility.LDNUtils.processContextResolverId;

import java.net.URI;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.ldn.converter.JsonLdHttpMessageConverter;
import org.dspace.app.ldn.model.Actor;
import org.dspace.app.ldn.model.Context;
import org.dspace.app.ldn.model.Notification;
import org.dspace.app.ldn.model.Object;
import org.dspace.app.ldn.model.Service;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.handle.service.HandleService;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

/**
 * Linked Data Notification business delegate to facilitate sending
 * notification.
 */
public class LDNBusinessDelegate {

    private final static Logger log = LogManager.getLogger(LDNBusinessDelegate.class);

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private HandleService handleService;

    private final RestTemplate restTemplate;

    /**
     * Initialize rest template with appropriate message converters.
     */
    public LDNBusinessDelegate() {
        restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new JsonLdHttpMessageConverter());
    }

    /**
     * Announce item release notification.
     *
     * @param item item released (deposited or updated)
     * @throws SQLException
     */
    public void announceRelease(Item item) {
        String serviceIds = configurationService.getProperty("service.service-id.ldn");

        for (String serviceId : serviceIds.split(",")) {
            doAnnounceRelease(item, serviceId.trim());
        }
    }

    /**
     * Build and POST announce release notification to configured service LDN
     * inboxes.
     *
     * @param item      associated item
     * @param serviceId service id for targer inbox
     */
    public void doAnnounceRelease(Item item, String serviceId) {
        log.info("Announcing release of item {}", item.getID());

        String dspaceServerUrl = configurationService.getProperty("dspace.server.url");
        String dspaceUIUrl = configurationService.getProperty("dspace.ui.url");
        String dspaceName = configurationService.getProperty("dspace.name");
        String dspaceLdnInboxUrl = configurationService.getProperty("ldn.notify.inbox");

        log.info("DSpace Server URL {}", dspaceServerUrl);
        log.info("DSpace UI URL {}", dspaceUIUrl);
        log.info("DSpace Name {}", dspaceName);
        log.info("DSpace LDN Inbox URL {}", dspaceLdnInboxUrl);

        String serviceUrl = configurationService.getProperty(join(".", "service", serviceId, "url"));
        String serviceInboxUrl = configurationService.getProperty(join(".", "service", serviceId, "inbox.url"));
        String serviceResolverUrl = configurationService.getProperty(join(".", "service", serviceId, "resolver.url"));

        log.info("Target URL {}", serviceUrl);
        log.info("Target LDN Inbox URL {}", serviceInboxUrl);

        Notification notification = new Notification();

        notification.setId(format("urn:uuid:%s", UUID.randomUUID()));
        notification.addType("Announce");
        notification.addType("coar-notify:ReleaseAction");

        Actor actor = new Actor();

        actor.setId(dspaceUIUrl);
        actor.setName(dspaceName);
        actor.addType("Service");

        Context context = new Context();

        List<Context> isSupplementedBy = new ArrayList<>();

        List<MetadataValue> metadata = item.getMetadata();
        for (MetadataValue metadatum : metadata) {
            MetadataField field = metadatum.getMetadataField();
            log.info("Metadata field {} with value {}", field, metadatum.getValue());
            if (field.getMetadataSchema().getName().equals("dc") &&
                    field.getElement().equals("data") &&
                    field.getQualifier().equals("uri")) {

                String ietfCiteAs = metadatum.getValue();
                String resolverId = processContextResolverId(ietfCiteAs);
                String id = serviceResolverUrl != null
                        ? format("%s%s", serviceResolverUrl, resolverId)
                        : ietfCiteAs;

                Context supplement = new Context();
                supplement.setId(id);
                supplement.setIetfCiteAs(ietfCiteAs);
                supplement.addType("sorg:Dataset");

                isSupplementedBy.add(supplement);
            }
        }

        context.setIsSupplementedBy(isSupplementedBy);

        Object object = new Object();

        String itemUrl = handleService.getCanonicalForm(item.getHandle());

        log.info("Item Handle URL {}", itemUrl);

        log.info("Item URL {}", itemUrl);

        object.setId(itemUrl);
        object.setIetfCiteAs(itemUrl);
        object.setTitle(item.getName());
        object.addType("sorg:ScholarlyArticle");

        Service origin = new Service();
        origin.setId(dspaceUIUrl);
        origin.setInbox(dspaceLdnInboxUrl);
        origin.addType("Service");

        Service target = new Service();
        target.setId(serviceUrl);
        target.setInbox(serviceInboxUrl);
        target.addType("Service");

        notification.setActor(actor);
        notification.setContext(context);
        notification.setObject(object);
        notification.setOrigin(origin);
        notification.setTarget(target);

        String serviceKey = configurationService.getProperty(join(".", "service", serviceId, "key"));
        String serviceKeyHeader = configurationService.getProperty(join(".", "service", serviceId, "key.header"));

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", APPLICATION_JSON_LD.toString());
        if (serviceKey != null && serviceKeyHeader != null) {
            headers.add(serviceKeyHeader, serviceKey);
        }

        HttpEntity<Notification> request = new HttpEntity<Notification>(notification, headers);

        log.info("Announcing notification {}", request);

        restTemplate.postForLocation(URI.create(target.getInbox()), request);
    }

}