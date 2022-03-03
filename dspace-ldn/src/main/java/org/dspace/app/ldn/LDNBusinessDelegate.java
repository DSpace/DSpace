/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.removeEnd;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

public class LDNBusinessDelegate {

    private final static Logger log = LogManager.getLogger(LDNBusinessDelegate.class);

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private HandleService handleService;

    public void announceRelease(org.dspace.core.Context ctx, Item item) throws SQLException {
        String dspaceUrl = configurationService.getProperty("dsapce.url");
        String dspaceLdnInboxUrl = format("%s/ldn/inbox", removeEnd(dspaceUrl, "/"));

        log.info("DSpace URL {}", dspaceUrl);
        log.info("DSpace LDN Inbox URL {}", dspaceLdnInboxUrl);

        Notification notification = new Notification();

        notification.setId(format("urn:uuid:%s", UUID.randomUUID()));
        notification.addType("Announce");
        notification.addType("coar-notify:ReleaseAction");

        Actor actor = new Actor();

        actor.setId(configurationService.getProperty("dsapce.url"));
        actor.setName(configurationService.getProperty("dsapce.name"));
        actor.addType("Service");

        Context context = new Context();

        List<MetadataValue> metadata = item.getMetadata();
        for (MetadataValue value : metadata) {
            MetadataField field = value.getMetadataField();
            log.info("Metadata field {} with value {}", field, value.getValue());
        }

        Object object = new Object();

        String itemHandleUrl = handleService.resolveToURL(ctx, item.getHandle());
        String itemUrl = handleService.getCanonicalForm(item.getHandle());

        log.info("Item Handle URL {}", itemHandleUrl);

        log.info("Item URL {}", itemUrl);

        object.setId(itemHandleUrl);
        object.setIetfCiteAs(itemUrl);
        object.setTitle(item.getName());
        object.addType("sorg:ScholarlyArticle");

        Service origin = new Service();
        origin.addType("Service");

        Service target = new Service();
        origin.setId(dspaceLdnInboxUrl);
        origin.setInbox(dspaceLdnInboxUrl);
        origin.addType("Service");

        notification.setActor(actor);
        notification.setContext(context);
        notification.setObject(object);
        notification.setOrigin(origin);
        notification.setTarget(target);
    }

}
