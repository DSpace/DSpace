/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.embargo;

import java.sql.SQLException;
import java.io.IOException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Context;

/**
 * Default plugin implementation of the embargo lifting function.
 *
 * @author Larry Stone
 * @author Richard Rodgers
 */
public class DefaultEmbargoLifter implements EmbargoLifter
{

    public DefaultEmbargoLifter()
    {
        super();
    }

    /**
     * Enforce lifting of embargo by turning read access to bitstreams in
     * this Item back on.
     *
     * @param context the DSpace context
     * @param item    the item to embargo
     */
    @Override
    public void liftEmbargo(Context context, Item item)
            throws SQLException, AuthorizeException, IOException
    {
        // remove the item's policies and replace them with
        // the defaults from the collection
        ContentServiceFactory.getInstance().getItemService().inheritCollectionDefaultPolicies(context, item, item.getOwningCollection());
    }
}
