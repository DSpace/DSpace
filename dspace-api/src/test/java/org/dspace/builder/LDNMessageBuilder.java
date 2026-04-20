/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.builder;

import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.ldn.LDNMessageEntity;
import org.dspace.app.ldn.model.Notification;
import org.dspace.app.ldn.service.LDNMessageService;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;

/**
 * Builder for {@link LDNMessageEntity} entities.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 *
 */
public class LDNMessageBuilder extends AbstractBuilder<LDNMessageEntity, LDNMessageService> {

    /* Log4j logger*/
    private static final Logger log = LogManager.getLogger();

    private LDNMessageEntity ldnMessageEntity;

    protected LDNMessageBuilder(Context context) {
        super(context);
    }

    @Override
    protected LDNMessageService getService() {
        return ldnMessageService;
    }

    @Override
    public void cleanup() throws Exception {
        try (Context c = new Context()) {
            c.setDispatcher("noindex");
            c.turnOffAuthorisationSystem();
            // Ensure object and any related objects are reloaded before checking to see what needs cleanup
            ldnMessageEntity = c.reloadEntity(ldnMessageEntity);
            if (ldnMessageEntity != null) {
                delete(ldnMessageEntity);
            }
            c.complete();
            indexingService.commit();
        }
    }

    @Override
    public void delete(Context c, LDNMessageEntity ldnMessageEntity) throws Exception {
        if (ldnMessageEntity != null) {
            getService().delete(c, ldnMessageEntity);
        }
    }

    @Override
    public LDNMessageEntity build() {
        try {

            ldnMessageService.update(context, ldnMessageEntity);
            context.dispatchEvents();

            indexingService.commit();
        } catch (SearchServiceException | SQLException e) {
            log.error(e);
        }
        return ldnMessageEntity;
    }

    public void delete(LDNMessageEntity ldnMessageEntity) throws Exception {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            LDNMessageEntity nsEntity = c.reloadEntity(ldnMessageEntity);
            if (nsEntity != null) {
                getService().delete(c, nsEntity);
            }
            c.complete();
        }

        indexingService.commit();
    }

    public static LDNMessageBuilder createNotifyServiceBuilder(Context context, String id) {
        LDNMessageBuilder ldnMessageServiceBuilder = new LDNMessageBuilder(context);
        return ldnMessageServiceBuilder.create(context, id);
    }

    public static LDNMessageBuilder createNotifyServiceBuilder(Context context, Notification notification) {
        LDNMessageBuilder ldnMessageServiceBuilder = new LDNMessageBuilder(context);
        return ldnMessageServiceBuilder.create(context, notification);
    }

    private LDNMessageBuilder create(Context context, String id) {
        try {

            this.context = context;
            this.ldnMessageEntity = ldnMessageService.create(context, id);

        } catch (SQLException e) {
            log.warn("Failed to create the NotifyService", e);
        }

        return this;
    }

    private LDNMessageBuilder create(Context context, Notification notification) {
        try {

            this.context = context;
            this.ldnMessageEntity = ldnMessageService.create(context, notification, "127.0.0.1");

        } catch (SQLException e) {
            log.warn("Failed to create the NotifyService", e);
        }

        return this;
    }

}