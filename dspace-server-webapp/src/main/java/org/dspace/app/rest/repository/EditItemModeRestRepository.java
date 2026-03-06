/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.UUID;

import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.model.EditItemModeRest;
import org.dspace.content.Item;
import org.dspace.content.edit.EditItemMode;
import org.dspace.content.edit.service.EditItemModeService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
@Component(EditItemModeRest.CATEGORY + "." + EditItemModeRest.PLURAL_NAME)
public class EditItemModeRestRepository
    extends DSpaceRestRepository<EditItemModeRest, String> {

    @Autowired
    private EditItemModeService eimService;

    @Autowired
    ItemService itemService;

    /* (non-Javadoc)
     * @see org.dspace.app.rest.repository.DSpaceRestRepository#findOne(org.dspace.core.Context, java.io.Serializable)
     */
    @Override
    @PreAuthorize("permitAll")
    public EditItemModeRest findOne(Context context, String data) {
        EditItemMode mode = null;
        String uuid = null;
        String modeName = null;
        String[] values = data.split(":");
        if (values != null && values.length == 2) {
            uuid = values[0];
            modeName = values[1];
        } else {
            throw new DSpaceBadRequestException(
                    "Given parameters are incomplete. Expected <UUID-ITEM>:<MODE>, Received: " + data);
        }
        try {
            UUID itemUuid = UUID.fromString(uuid);
            Item item = itemService.find(context, itemUuid);
            if (item == null) {
                throw new ResourceNotFoundException("No such item with uuid : " + itemUuid);
            }
            mode = eimService.findMode(context, item, modeName);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (mode == null) {
            return null;
        }
        return converter.toRest(mode, utils.obtainProjection());
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.repository.DSpaceRestRepository#
     * findAll(org.dspace.core.Context, org.springframework.data.domain.Pageable)
     */
    @Override
    public Page<EditItemModeRest> findAll(Context context, Pageable pageable) {
        throw new RepositoryMethodNotImplementedException("No implementation found; Method not Implemented!", "");
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.repository.DSpaceRestRepository#getDomainClass()
     */
    @Override
    public Class<EditItemModeRest> getDomainClass() {
        return EditItemModeRest.class;
    }

}
