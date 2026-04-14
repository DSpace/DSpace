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
import org.dspace.app.rest.model.EditItemRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.edit.EditItem;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * Link resource for {@link EditItemRest#ITEM}
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
@Component(EditItemRest.CATEGORY + "." + EditItemRest.NAME_PLURAL + "." + EditItemRest.ITEM)
public class EditItemItemLinkRestRepository extends AbstractEditItemLinkRestRepository implements LinkRestRepository {

    /**
     * Retrieve the item for an edit item.
     *
     * @param request          - The current request
     * @param data             - The data template that contains both item uuid and mode {uuid:mode}, joined by a column
     * @param optionalPageable - optional pageable object
     * @param projection       - the current projection
     * @return the item for the edit item
     */
    public ItemRest getEditItemItem(
        @Nullable HttpServletRequest request, String data,
        @Nullable Pageable optionalPageable, Projection projection
    ) {
        return getMappedResource(
            getEditItemRestRequest(data),
            EditItem::getItem,
            projection
        );
    }


}
