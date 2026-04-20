/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import javax.annotation.Nullable;

import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.EditItemRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.edit.EditItem;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * Link resource for {@link EditItemRest#COLLECTION}
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
@Component(EditItemRest.CATEGORY + "." + EditItemRest.NAME_PLURAL + "." + EditItemRest.COLLECTION)
public class EditItemCollectionLinkRestRepository extends AbstractEditItemLinkRestRepository
    implements LinkRestRepository {

    /**
     * Retrieve the collection for an edit item.
     *
     * @param request          - The current request
     * @param data             - The data template that contains both item uuid and mode {uuid:mode}, joined by a column
     * @param optionalPageable - optional pageable object
     * @param projection       - the current projection
     * @return the item for the edit item
     */
    public CollectionRest getEditItemCollection(
        @Nullable HttpServletRequest request, String data,
        @Nullable Pageable optionalPageable, Projection projection
    ) {
        return getMappedResource(
            getEditItemRestRequest(data),
            EditItem::getCollection,
            projection
        );

    }

}
