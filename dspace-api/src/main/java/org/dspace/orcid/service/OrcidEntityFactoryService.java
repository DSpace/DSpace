/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orcid.service;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.orcid.jaxb.model.v3.release.record.Activity;

/**
 * Interface that mark classes that handle the configured instance of
 * {@link OrcidEntityFactory}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface OrcidEntityFactoryService {

    /**
     * Builds an ORCID Activity object starting from the given item. The actual type
     * of Activity constructed depends on the entity type of the input item.
     *
     * @param  context the DSpace context
     * @param  item    the item
     * @return         the created object
     */
    Activity createOrcidObject(Context context, Item item);
}
