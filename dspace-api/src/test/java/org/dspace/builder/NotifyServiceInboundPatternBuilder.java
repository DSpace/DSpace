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
import org.dspace.app.ldn.NotifyServiceEntity;
import org.dspace.app.ldn.NotifyServiceInboundPattern;
import org.dspace.app.ldn.service.NotifyServiceInboundPatternService;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;

/**
 * Builder for {@link NotifyServiceInboundPattern} entities.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 *
 */
public class NotifyServiceInboundPatternBuilder
    extends AbstractBuilder<NotifyServiceInboundPattern, NotifyServiceInboundPatternService> {

    /* Log4j logger*/
    private static final Logger log = LogManager.getLogger();

    private NotifyServiceInboundPattern notifyServiceInboundPattern;

    protected NotifyServiceInboundPatternBuilder(Context context) {
        super(context);
    }

    @Override
    protected NotifyServiceInboundPatternService getService() {
        return inboundPatternService;
    }

    @Override
    public void cleanup() throws Exception {
        try (Context c = new Context()) {
            c.setDispatcher("noindex");
            c.turnOffAuthorisationSystem();
            // Ensure object and any related objects are reloaded before checking to see what needs cleanup
            notifyServiceInboundPattern = c.reloadEntity(notifyServiceInboundPattern);
            if (notifyServiceInboundPattern != null) {
                delete(notifyServiceInboundPattern);
            }
            c.complete();
            indexingService.commit();
        }
    }

    @Override
    public void delete(Context c, NotifyServiceInboundPattern notifyServiceInboundPattern) throws Exception {
        if (notifyServiceInboundPattern != null) {
            getService().delete(c, notifyServiceInboundPattern);
        }
    }

    @Override
    public NotifyServiceInboundPattern build() {
        try {

            inboundPatternService.update(context, notifyServiceInboundPattern);
            context.dispatchEvents();

            indexingService.commit();
        } catch (SearchServiceException | SQLException e) {
            log.error(e);
        }
        return notifyServiceInboundPattern;
    }

    public void delete(NotifyServiceInboundPattern notifyServiceInboundPattern) throws Exception {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            NotifyServiceInboundPattern nsEntity = c.reloadEntity(notifyServiceInboundPattern);
            if (nsEntity != null) {
                getService().delete(c, nsEntity);
            }
            c.complete();
        }

        indexingService.commit();
    }

    public static NotifyServiceInboundPatternBuilder createNotifyServiceInboundPatternBuilder(
        Context context, NotifyServiceEntity service) {
        NotifyServiceInboundPatternBuilder notifyServiceBuilder = new NotifyServiceInboundPatternBuilder(context);
        return notifyServiceBuilder.create(context, service);
    }

    private NotifyServiceInboundPatternBuilder create(Context context, NotifyServiceEntity service) {
        try {

            this.context = context;
            this.notifyServiceInboundPattern = inboundPatternService.create(context, service);

        } catch (SQLException e) {
            log.warn("Failed to create the NotifyService", e);
        }

        return this;
    }

    public NotifyServiceInboundPatternBuilder isAutomatic(boolean automatic) {
        notifyServiceInboundPattern.setAutomatic(automatic);
        return this;
    }

    public NotifyServiceInboundPatternBuilder withPattern(String pattern) {
        notifyServiceInboundPattern.setPattern(pattern);
        return this;
    }

    public NotifyServiceInboundPatternBuilder withConstraint(String constraint) {
        notifyServiceInboundPattern.setConstraint(constraint);
        return this;
    }

}