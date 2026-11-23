/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import org.dspace.app.rest.projection.Projection;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.edit.EditItem;
import org.dspace.content.edit.service.EditItemService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;

/**
 * Class that contains the basic implementation to retrieve linked {@link EditItem} resources.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class AbstractEditItemLinkRestRepository extends AbstractDSpaceRestRepository {

    private static final Logger log = LoggerFactory.getLogger(AbstractEditItemLinkRestRepository.class);

    @Autowired
    private EditItemService editItemService;
    @Autowired
    private ItemService itemService;

    protected static Optional<EditItemRestRequest> getEditItemRestRequest(String data) {
        if (data == null || data.isEmpty()) {
            return Optional.empty();
        }

        String[] split = data.split(":");

        UUID uuid;
        try {
            uuid = UUID.fromString(split[0]);
        } catch (Exception e) {
            log.error("Cannot convert the following uuid: {}", split[0], e);
            return Optional.empty();
        }
        String mode = split[1];
        return Optional.of(new EditItemRestRequest(uuid, mode));
    }

    protected EditItem findEditItem(EditItemRestRequest requestDetails) {
        try {
            Context context = obtainContext();
            Item item = itemService.find(context, requestDetails.uuid);
            EditItem editItem = editItemService.find(context, item, requestDetails.mode);

            if (editItem == null) {
                throw new ResourceNotFoundException("No such edit item found: " + requestDetails.uuid);
            }
            return editItem;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (AuthorizeException e) {
            throw new AccessDeniedException(
                "The current user does not have rights to edit mode <" + requestDetails.mode + ">"
            );
        }
    }

    protected <T, R> R getMappedResource(
        Optional<EditItemRestRequest> request,
        Function<EditItem, T> mapper,
        Projection projection
    ) {
        return request.map(this::findEditItem)
                      .map(mapper)
                      .<R>map(obj -> converter.toRest(obj, projection))
                      .orElse(null);
    }

    protected static class EditItemRestRequest {
        public final UUID uuid;
        public final String mode;

        public EditItemRestRequest(UUID uuid, String mode) {
            this.uuid = uuid;
            this.mode = mode;
        }
    }
}
