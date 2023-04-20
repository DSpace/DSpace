/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orcid.model.factory;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.orcid.model.OrcidEntityType;
import org.orcid.jaxb.model.v3.release.record.Activity;

/**
 * Interface to mark factories of Orcid entities.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface OrcidEntityFactory {

    /**
     * Placeholder used to refer the item handle on fields mapping.
     */
    String SIMPLE_HANDLE_PLACEHOLDER = "$simple-handle";

    /**
     * Returns the entity type created from this factory.
     *
     * @return the entity type
     */
    public OrcidEntityType getEntityType();

    /**
     * Creates an ORCID activity from the given object.
     *
     * @param  context the DSpace context
     * @param  item    the item
     * @return         the created activity instance
     */
    public Activity createOrcidObject(Context context, Item item);
}
