/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.deletion.process.strategies;

import java.io.IOException;
import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.BadVirtualMetadataTypeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.core.Context;

/**
 * Deletion strategy for DSpace Item objects.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public class ItemDeletionStrategy implements DSpaceObjectDeletionStrategy {

    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private RelationshipService relationshipService = ContentServiceFactory.getInstance().getRelationshipService();

    @Override
    public void delete(Context context, DSpaceObject dso, String[] copyVirtual)
            throws SQLException, AuthorizeException, IOException {
        Item item = (Item) dso;
        try {
            if (itemService.isInProgressSubmission(context, item)) {
                throw new RuntimeException("The item cannot be deleted. It's part of a in-progress submission.");
            }
            if (item.getTemplateItemOf() != null) {
                throw new RuntimeException("The item cannot be deleted. It's a template for a collection");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        try {
            relationshipService.deleteMultipleRelationshipsCopyVirtualMetadata(context, copyVirtual, item);
            itemService.delete(context, item);
        } catch (SQLException | BadVirtualMetadataTypeException | IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public boolean supports(DSpaceObject dso) {
        return dso instanceof Item;
    }

}