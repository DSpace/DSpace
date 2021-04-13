/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.util.UUID;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.discovery.indexobject.IndexableWorkflowItem;
import org.dspace.discovery.indexobject.IndexableWorkspaceItem;

/**
 * Extension of {@link DiscoverResultIterator} that iterate over items.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class DiscoverResultItemIterator extends DiscoverResultIterator<Item, UUID> {

    public DiscoverResultItemIterator(Context context, DiscoverQuery discoverQuery) {
        super(context, discoverQuery);
    }

    public DiscoverResultItemIterator(Context context, IndexableObject<?, ?> scopeObject, DiscoverQuery discoverQuery) {
        super(context, scopeObject, discoverQuery);
    }

    @Override
    public Item next() {
        IndexableObject<?, ?> nextIndexableObject = getNextIndexableObject();
        String objectType = nextIndexableObject.getType();

        if (IndexableItem.TYPE.equals(objectType)) {
            return ((IndexableItem) nextIndexableObject).getIndexedObject();
        }

        if (IndexableWorkflowItem.TYPE.equals(objectType)) {
            return ((IndexableWorkflowItem) nextIndexableObject).getIndexedObject().getItem();
        }

        if (IndexableWorkspaceItem.TYPE.equals(objectType)) {
            return ((IndexableWorkspaceItem) nextIndexableObject).getIndexedObject().getItem();
        }

        throw new IllegalStateException("Invalid object type for discover item iterator:" + objectType);
    }

}
