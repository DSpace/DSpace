/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link.harvest;

import org.dspace.app.rest.CollectionHarvestSettingsController;
import org.dspace.app.rest.link.HalLinkFactory;

public abstract class HarvestedCollectionRestHalLinkFactory<T>
    extends HalLinkFactory<T, CollectionHarvestSettingsController> {

    @Override
    protected Class<CollectionHarvestSettingsController> getControllerClass() {
        return CollectionHarvestSettingsController.class;
    }
}
