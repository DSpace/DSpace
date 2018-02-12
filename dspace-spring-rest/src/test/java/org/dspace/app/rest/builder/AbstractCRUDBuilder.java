/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.builder;

import org.dspace.core.Context;
import org.dspace.core.ReloadableEntity;
import org.dspace.service.DSpaceCRUDService;

/**
 * @author Jonas Van Goolen - (jonas@atmire.com)
 */
public abstract class AbstractCRUDBuilder<T extends ReloadableEntity> extends AbstractBuilder<T, DSpaceCRUDService> {

    protected AbstractCRUDBuilder(Context context) {
        super(context);
    }

    protected abstract DSpaceCRUDService getService();

    public abstract T build();

    public void delete(T dso) throws Exception {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            T attachedDso = c.reloadEntity(dso);
            if (attachedDso != null) {
                getService().delete(c, attachedDso);
            }
            c.complete();
        }

        indexingService.commit();
    }

}
