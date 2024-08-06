/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.builder;

import java.math.BigDecimal;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.ldn.NotifyServiceEntity;
import org.dspace.app.ldn.service.NotifyService;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;

/**
 * Builder for {@link NotifyServiceEntity} entities.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 *
 */
public class NotifyServiceBuilder extends AbstractBuilder<NotifyServiceEntity, NotifyService> {

    /* Log4j logger*/
    private static final Logger log = LogManager.getLogger();

    private NotifyServiceEntity notifyServiceEntity;

    protected NotifyServiceBuilder(Context context) {
        super(context);
    }

    @Override
    protected NotifyService getService() {
        return notifyService;
    }

    @Override
    public void cleanup() throws Exception {
        try (Context c = new Context()) {
            c.setDispatcher("noindex");
            c.turnOffAuthorisationSystem();
            // Ensure object and any related objects are reloaded before checking to see what needs cleanup
            notifyServiceEntity = c.reloadEntity(notifyServiceEntity);
            if (notifyServiceEntity != null) {
                delete(notifyServiceEntity);
            }
            c.complete();
            indexingService.commit();
        }
    }

    @Override
    public void delete(Context c, NotifyServiceEntity notifyServiceEntity) throws Exception {
        if (notifyServiceEntity != null) {
            getService().delete(c, notifyServiceEntity);
        }
    }

    @Override
    public NotifyServiceEntity build() {
        try {

            notifyService.update(context, notifyServiceEntity);
            context.dispatchEvents();

            indexingService.commit();
        } catch (SearchServiceException | SQLException e) {
            log.error(e);
        }
        return notifyServiceEntity;
    }

    public void delete(NotifyServiceEntity notifyServiceEntity) throws Exception {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            NotifyServiceEntity nsEntity = c.reloadEntity(notifyServiceEntity);
            if (nsEntity != null) {
                getService().delete(c, nsEntity);
            }
            c.complete();
        }

        indexingService.commit();
    }

    public static NotifyServiceBuilder createNotifyServiceBuilder(Context context, String name) {
        NotifyServiceBuilder notifyServiceBuilder = new NotifyServiceBuilder(context);
        return notifyServiceBuilder.create(context, name);
    }

    private NotifyServiceBuilder create(Context context, String name) {
        try {

            this.context = context;
            this.notifyServiceEntity = notifyService.create(context, name);

        } catch (SQLException e) {
            log.warn("Failed to create the NotifyService", e);
        }

        return this;
    }

    public NotifyServiceBuilder withDescription(String description) {
        notifyServiceEntity.setDescription(description);
        return this;
    }

    public NotifyServiceBuilder withUrl(String url) {
        notifyServiceEntity.setUrl(url);
        return this;
    }

    public NotifyServiceBuilder withLdnUrl(String ldnUrl) {
        notifyServiceEntity.setLdnUrl(ldnUrl);
        return this;
    }

    public NotifyServiceBuilder withStatus(boolean enabled) {
        notifyServiceEntity.setEnabled(enabled);
        return this;
    }

    public NotifyServiceBuilder withScore(BigDecimal score) {
        notifyServiceEntity.setScore(score);
        return this;
    }

    public NotifyServiceBuilder isEnabled(boolean enabled) {
        notifyServiceEntity.setEnabled(enabled);
        return this;
    }

    public NotifyServiceBuilder withLowerIp(String lowerIp) {
        notifyServiceEntity.setLowerIp(lowerIp);
        return this;
    }

    public NotifyServiceBuilder withUpperIp(String upperIp) {
        notifyServiceEntity.setUpperIp(upperIp);
        return this;
    }

    /**
     * Delete the Test NotifyServiceEntity referred to by the given ID
     * @param id ID of NotifyServiceEntity to delete
     * @throws SQLException if error occurs
     */
    public static void deleteNotifyService(Integer id) throws SQLException {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            NotifyServiceEntity notifyServiceEntity = notifyService.find(c, id);
            if (notifyServiceEntity != null) {
                notifyService.delete(c, notifyServiceEntity);
            }
            c.complete();
        }
    }

}