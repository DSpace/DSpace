package org.dspace.app.rest.builder;

import java.sql.SQLException;

import org.dspace.content.Collection;
import org.dspace.content.DCDate;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchema;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;

/**
 * TODO TOM UNIT TEST
 */
public class ItemBuilder extends AbstractBuilder {

    private WorkspaceItem workspaceItem;

    public ItemBuilder createItem(final Context context, final Collection col1) {
        this.context = context;

        try {
            workspaceItem = workspaceItemService.create(context, col1, false);
        } catch (Exception e) {
            return handleException(e);
        }

        return this;
    }

    public ItemBuilder withTitle(final String title) {
        try {
            itemService.setMetadataSingleValue(context, workspaceItem.getItem(), MetadataSchema.DC_SCHEMA, "title", null, Item.ANY, title);
        } catch (SQLException e) {
            return handleException(e);
        }

        return this;
    }

    public ItemBuilder withIssueDate(final String issueDate) {
        return addMetadataValueToItem(MetadataSchema.DC_SCHEMA, "date", "issued", new DCDate(issueDate).toString());
    }

    public ItemBuilder withAuthor(final String authorName) {
        return addMetadataValueToItem(MetadataSchema.DC_SCHEMA, "date", "issued", authorName);
    }

    public ItemBuilder withSubject(final String subject) {
        return addMetadataValueToItem(MetadataSchema.DC_SCHEMA, "subject", null, subject);
    }

    public Item build() {
        try {
            Item item = installItemService.installItem(context, workspaceItem);
            context.dispatchEvents();
            indexingService.commit();

            return item;
        } catch (Exception e) {
            return handleException(e);
        }
    }

    private ItemBuilder addMetadataValueToItem(final String schema, final String element, final String qualifier, final String value) {
        try {
            itemService.addMetadata(context, workspaceItem.getItem(), schema, element, qualifier, Item.ANY, value);
        } catch (SQLException e) {
            return handleException(e);
        }

        return this;
    }
}
