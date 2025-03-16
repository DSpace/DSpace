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
import org.dspace.discovery.indexobject.IndexableClaimedTask;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.discovery.indexobject.IndexablePoolTask;
import org.dspace.discovery.indexobject.IndexableWorkflowItem;
import org.dspace.discovery.indexobject.IndexableWorkspaceItem;

/**
 * An iterator for discovering and iterating over DSpace items from the search index.
 * This class extends {@link DiscoverResultIterator} and provides the logic to
 * handle different types of indexable objects and retrieve the corresponding DSpace items.
 *
 * <p>It supports the following indexable object types:</p>
 * <ul>
 *   <li>{@link IndexableItem}</li>
 *   <li>{@link IndexableWorkflowItem}</li>
 *   <li>{@link IndexableWorkspaceItem}</li>
 *   <li>{@link IndexablePoolTask}</li>
 *   <li>{@link IndexableClaimedTask}</li>
 * </ul>
 *
 * <p>Throws an {@link IllegalStateException} if the object type is not recognized.</p>
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class DiscoverResultItemIterator extends DiscoverResultIterator<Item, UUID> {

    /**
     * Constructs an iterator for discovering items based on the given context and query.
     *
     * @param context       the DSpace context
     * @param discoverQuery the discovery query
     */
    public DiscoverResultItemIterator(Context context, DiscoverQuery discoverQuery) {
        super(context, discoverQuery);
    }

    /**
     * Constructs an iterator with the option to uncache entities.
     *
     * @param context the DSpace context
     * @param discoverQuery the discovery query
     * @param uncacheEntities whether to uncache entities after iteration
     */
    public DiscoverResultItemIterator(Context context, DiscoverQuery discoverQuery, boolean uncacheEntities) {
        super(context, discoverQuery, uncacheEntities);
    }

    /**
     * Constructs an iterator within a specific scope object.
     *
     * @param context the DSpace context
     * @param scopeObject the scope object
     * @param discoverQuery the discovery query
     */
    public DiscoverResultItemIterator(Context context, IndexableObject<?, ?> scopeObject, DiscoverQuery discoverQuery) {
        super(context, scopeObject, discoverQuery);
    }

    /**
     * Constructs an iterator with a limit on the maximum number of results.
     *
     * @param context the DSpace context
     * @param discoverQuery the discovery query
     * @param maxResults the maximum number of results to return
     */
    public DiscoverResultItemIterator(Context context, DiscoverQuery discoverQuery, int maxResults) {
        super(context, null, discoverQuery, true, maxResults);
    }

    /**
     * Constructs an iterator with a scope object and a limit on the maximum number of results.
     *
     * @param context the DSpace context
     * @param scopeObject the scope object
     * @param discoverQuery the discovery query
     * @param maxResults the maximum number of results to return
     */
    public DiscoverResultItemIterator(Context context, IndexableObject<?, ?> scopeObject, DiscoverQuery discoverQuery,
                                      int maxResults) {
        super(context, scopeObject, discoverQuery, true, maxResults);
    }

    /**
     * Retrieves the next {@link Item} from the iterator. The item is obtained by
     * determining the type of the next indexable object and extracting the corresponding item.
     *
     * @return the next DSpace item
     * @throws IllegalStateException if the object type is invalid
     */
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

        if (IndexablePoolTask.TYPE.equals(objectType)) {
            return ((IndexablePoolTask) nextIndexableObject).getIndexedObject().getWorkflowItem().getItem();
        }

        if (IndexableClaimedTask.TYPE.equals(objectType)) {
            return ((IndexableClaimedTask) nextIndexableObject).getIndexedObject().getWorkflowItem().getItem();
        }

        throw new IllegalStateException("Invalid object type for discover item iterator: " + objectType);
    }

}
