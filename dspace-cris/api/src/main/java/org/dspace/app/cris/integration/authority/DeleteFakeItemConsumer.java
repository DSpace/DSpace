/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration.authority;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.BooleanUtils;
import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;

/**
 */
public class DeleteFakeItemConsumer implements Consumer
{

    private static final Logger log = Logger.getLogger(DeleteFakeItemConsumer.class);

    private transient Set<String> processedHandles = new HashSet<String>();

    public void initialize() throws Exception
    {
    }

    public void consume(Context ctx, Event event) throws Exception
    {
        DSpaceObject dso = event.getSubject(ctx);
        if (dso instanceof Item)
        {
            Item item = (Item) dso;
            if (item == null || !item.isArchived())
                return;
            if (processedHandles.contains(item.getHandle()))
            {
                return;
            }
            else
            {
                processedHandles.add(item.getHandle());
            }

            ctx.turnOffAuthorisationSystem();
            if (BooleanUtils.toBoolean(item.getMetadata("local.fakeitem"))) {
            	Collection[] collections = item.getCollections();

                // Remove item from all the collections it's in
                for (int i = 0; i < collections.length; i++)
                {
                    collections[i].removeItem(item);
                }
            }
            ctx.restoreAuthSystemState();
        }
    }

    public void end(Context ctx) throws Exception
    {
        // nothing to do
        processedHandles.clear();
    }

    public void finish(Context ctx) throws Exception
    {
        // nothing to do
    }
}