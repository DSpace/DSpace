/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority;

import java.util.HashSet;
import java.util.Set;

import org.dspace.authority.service.ItemReferenceResolverService;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.utils.DSpace;

/**
 * Implementation of {@link Consumer} that resolve all the references to the
 * current item.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class ItemReferenceResolverConsumer implements Consumer {

    private ItemReferenceResolverService itemReferenceResolverService;

    private final Set<Item> itemsAlreadyProcessed = new HashSet<Item>();

    @Override
    public void initialize() throws Exception {
        this.itemReferenceResolverService = new DSpace().getSingletonService(ItemReferenceResolverService.class);
    }

    @Override
    public void consume(Context context, Event event) throws Exception {
        Item item = (Item) event.getSubject(context);
        if (item == null || !item.isArchived() || itemsAlreadyProcessed.contains(item)) {
            return;
        }

        itemsAlreadyProcessed.add(item);

        context.turnOffAuthorisationSystem();
        try {
            itemReferenceResolverService.resolveReferences(context, item);
        } finally {
            context.restoreAuthSystemState();
        }
    }

    @Override
    public void end(Context ctx) throws Exception {
        itemsAlreadyProcessed.clear();
        itemReferenceResolverService.clearResolversCache();
    }

    @Override
    public void finish(Context ctx) throws Exception {

    }

}
