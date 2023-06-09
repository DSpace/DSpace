/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.builder;

import java.sql.SQLException;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.supervision.SupervisionOrder;
import org.dspace.supervision.service.SupervisionOrderService;

/**
 * Abstract builder to construct SupervisionOrder Objects
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science dot it)
 */
public class SupervisionOrderBuilder
    extends AbstractBuilder<SupervisionOrder, SupervisionOrderService> {

    private static final Logger log = LogManager.getLogger(SupervisionOrderBuilder.class);

    private SupervisionOrder supervisionOrder;

    protected SupervisionOrderBuilder(Context context) {
        super(context);
    }

    public static SupervisionOrderBuilder createSupervisionOrder(Context context, Item item, Group group) {
        SupervisionOrderBuilder builder = new SupervisionOrderBuilder(context);
        return builder.create(context, item, group);
    }

    private SupervisionOrderBuilder create(Context context, Item item, Group group) {
        try {
            this.context = context;
            this.supervisionOrder = getService().create(context, item, group);
        } catch (Exception e) {
            log.error("Error in SupervisionOrderBuilder.create(..), error: ", e);
        }
        return this;
    }

    @Override
    public void cleanup() throws Exception {
        delete(supervisionOrder);
    }

    @Override
    public SupervisionOrder build() throws SQLException, AuthorizeException {
        try {
            getService().update(context, supervisionOrder);
            context.dispatchEvents();
            indexingService.commit();
        } catch (Exception e) {
            log.error("Error in SupervisionOrderBuilder.build(), error: ", e);
        }
        return supervisionOrder;
    }

    @Override
    public void delete(Context context, SupervisionOrder supervisionOrder) throws Exception {
        if (Objects.nonNull(supervisionOrder)) {
            getService().delete(context, supervisionOrder);
        }
    }

    @Override
    protected SupervisionOrderService getService() {
        return supervisionOrderService;
    }

    private void delete(SupervisionOrder supervisionOrder) throws Exception {
        try (Context context = new Context()) {
            context.turnOffAuthorisationSystem();
            context.setDispatcher("noindex");
            SupervisionOrder attached = context.reloadEntity(supervisionOrder);
            if (attached != null) {
                getService().delete(context, attached);
            }
            context.complete();
            indexingService.commit();
        }
    }
}
