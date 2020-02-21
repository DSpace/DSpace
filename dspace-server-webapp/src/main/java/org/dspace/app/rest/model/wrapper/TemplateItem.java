/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.wrapper;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;

/**
 * This class represents a Collection's Item Template. It acts as a wrapper for an {@link Item} object to differentiate
 * between actual Items and TemplateItems
 */
public class TemplateItem {
    private Item item;

    public TemplateItem(Item item) {
        if (item.getTemplateItemOf() == null) {
            throw new IllegalArgumentException("Cannot create a TemplateItem from an item that isn't a template item");
        }

        this.item = item;
    }

    public Item getItem() {
        return this.item;
    }

    public List<MetadataValue> getMetadata() {
        return item.getMetadata();
    }

    public UUID getID() {
        return item.getID();
    }

    public Date getLastModified() {
        return item.getLastModified();
    }

    public Collection getTemplateItemOf() {
        return item.getTemplateItemOf();
    }
}
