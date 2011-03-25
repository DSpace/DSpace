/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.curate;

import java.io.IOException;
import java.sql.SQLException;

import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;

/**
 * AbstractCurationTask encapsulates a few common patterns of task use,
 * resources, and convenience methods.
 * 
 * @author richardrodgers
 */
public abstract class AbstractCurationTask implements CurationTask
{
    // invoking curator
    protected Curator curator = null;
    // curator-assigned taskId
    protected String taskId = null;

    @Override
    public void init(Curator curator, String taskId) throws IOException
    {
        this.curator = curator;
        this.taskId = taskId;
    }

    @Override
    public abstract int perform(DSpaceObject dso) throws IOException;
    
    /**
     * Distributes a task through a DSpace container - a convenience method
     * for tasks declaring the <code>@Distributive</code> property. Users must
     * override the 'performItem' invoked by this method.
     * 
     * @param dso
     * @throws IOException
     */
    protected void distribute(DSpaceObject dso) throws IOException
    {
        try
        {
            int type = dso.getType();
            if (Constants.ITEM == type)
            {
                performItem((Item)dso);
            }
            else if (Constants.COLLECTION == type)
            {
                ItemIterator iter = ((Collection)dso).getItems();
                while (iter.hasNext())
                {
                    performItem(iter.next());
                }
            }
            else if (Constants.COMMUNITY == type)
            {
                Community comm = (Community)dso;
                for (Community subcomm : comm.getSubcommunities())
                {
                    distribute(subcomm);
                }
                for (Collection coll : comm.getCollections())
                {
                    distribute(coll);
                }
            }
        }
        catch (SQLException sqlE)
        {
            throw new IOException(sqlE.getMessage(), sqlE);
        }       
    }
    
    /**
     * Performs task upon an Item. Must be overridden if <code>distribute</code>
     * method is used.
     * 
     * @param item
     * @throws SQLException
     * @throws IOException
     */
    protected void performItem(Item item) throws SQLException, IOException
    {
        // no-op - override when using 'distribute' method
    }

    @Override
    public int perform(Context ctx, String id) throws IOException
    {
        DSpaceObject dso = dereference(ctx, id);
        return (dso != null) ? perform(dso) : Curator.CURATE_FAIL;
    }
    
    /**
     * Returns a DSpaceObject for passed identifier, if it exists
     * 
     * @param ctx
     *        DSpace context
     * @param id 
     *        canonical id of object
     * @return dso
     *        DSpace object, or null if no object with id exists
     * @throws IOException
     */
    protected DSpaceObject dereference(Context ctx, String id) throws IOException
    {
        try
        {
            return HandleManager.resolveToObject(ctx, id);
        }
        catch (SQLException sqlE)
        {
            throw new IOException(sqlE.getMessage(), sqlE);
        }
    }

    /**
     * Sends message to the reporting stream
     * 
     * @param message
     *        the message to stream
     */
    protected void report(String message)
    {
        curator.report(message);
    }

    /**
     * Assigns the result of the task performance
     * 
     * @param result
     *        the result string
     */
    protected void setResult(String result)
    {
        curator.setResult(taskId, result);
    }
}
