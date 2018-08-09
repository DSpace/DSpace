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
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

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
    // logger
    private static Logger log = Logger.getLogger(AbstractCurationTask.class);
    protected CommunityService communityService;
    protected ItemService itemService;
    protected HandleService handleService;
    protected ConfigurationService configurationService;

    @Override
    public void init(Curator curator, String taskId) throws IOException
    {
        this.curator = curator;
        this.taskId = taskId;
        communityService = ContentServiceFactory.getInstance().getCommunityService();
        itemService = ContentServiceFactory.getInstance().getItemService();
        handleService = HandleServiceFactory.getInstance().getHandleService();
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
    }

    @Override
    public abstract int perform(DSpaceObject dso) throws IOException;
    
    /**
     * Distributes a task through a DSpace container - a convenience method
     * for tasks declaring the <code>@Distributive</code> property. 
     * <P>
     * This method invokes the 'performObject()' method on the current DSO, and
     * then recursively invokes the 'performObject()' method on all DSOs contained
     * within the current DSO. For example: if a Community is passed in, then
     * 'performObject()' will be called on that Community object, as well as 
     * on all SubCommunities/Collections/Items contained in that Community.
     * <P>
     * Individual tasks MUST override either the <code>performObject</code> method or
     * the <code>performItem</code> method to ensure the task is run on either all
     * DSOs or just all Items, respectively.
     * 
     * @param dso current DSpaceObject
     * @throws IOException if IO error
     */
    protected void distribute(DSpaceObject dso) throws IOException
    {
        try
        {
            //perform task on this current object
            performObject(dso);
            
            //next, we'll try to distribute to all child objects, based on container type
            int type = dso.getType();
            if (Constants.COLLECTION == type)
            {
                Iterator<Item> iter = itemService.findByCollection(Curator.curationContext(), (Collection) dso);
                while (iter.hasNext())
                {
                    Item item = iter.next();
                    performObject(item);
                    Curator.curationContext().uncacheEntity(item);
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
            else if (Constants.SITE == type)
            {
                List<Community> topComm = communityService.findAllTop(Curator.curationContext());
                for (Community comm : topComm)
                {
                    distribute(comm);
                }
            }
        }
        catch (SQLException sqlE)
        {
            throw new IOException(sqlE.getMessage(), sqlE);
        }       
    }
    
    /**
     * Performs task upon a single DSpaceObject. Used in conjunction with the
     * <code>distribute</code> method to run a single task across multiple DSpaceObjects.
     * <P>
     * By default, this method just wraps a call to <code>performItem</code>
     * for each Item Object. 
     * <P>
     * You should override this method if you want to use
     * <code>distribute</code> to run your task across multiple DSpace Objects.
     * <P>
     * Either this method or <code>performItem</code> should be overridden if 
     * <code>distribute</code> method is used.
     * 
     * @param dso the DSpaceObject
     * @throws SQLException if database error
     * @throws IOException if IO error
     */
    protected void performObject(DSpaceObject dso) throws SQLException, IOException
    {
        // By default this method only performs tasks on Items 
        // (You should override this method if you want to perform task on all objects)
        if(dso.getType()==Constants.ITEM)
        {
            performItem((Item)dso);
        }
        
        //no-op for all other types of DSpace Objects
    }
    
    /**
     * Performs task upon a single DSpace Item. Used in conjunction with the
     * <code>distribute</code> method to run a single task across multiple Items.
     * <P>
     * You should override this method if you want to use
     * <code>distribute</code> to run your task across multiple DSpace Items.
     * <P>
     * Either this method or <code>performObject</code> should be overridden if 
     * <code>distribute</code> method is used.
     * 
     * @param item the DSpace Item
     * @throws SQLException if database error
     * @throws IOException if IO error
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
     * @throws IOException if IO error
     */
    protected DSpaceObject dereference(Context ctx, String id) throws IOException
    {
        try
        {
            return handleService.resolveToObject(ctx, id);
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
    
    /**
     * Returns task configuration property value for passed name, else
     * <code>null</code> if no properties defined or no value for passed key.
     * 
     * @param name
     *        the property name
     * @return value
     *        the property value, or null
     * 
     */
    protected String taskProperty(String name)
    {
        // If a taskID/Name is specified, prepend it on the configuration name
        if(StringUtils.isNotBlank(taskId))
        {
            return configurationService.getProperty(taskId + "." + name);
    	}
        else
        {
            return configurationService.getProperty(name);
        }
    }
    
    /**
     * Returns task configuration integer property value for passed name, else
     * passed default value if no properties defined or no value for passed key.
     * 
     * @param name
     *        the property name
     * @param defaultValue value
     *        the default value
     * @return value
     *        the property value, or default value
     * 
     */
    protected int taskIntProperty(String name, int defaultValue)
    {
        // If a taskID/Name is specified, prepend it on the configuration name
    	if(StringUtils.isNotBlank(taskId))
        {
            return configurationService.getIntProperty(taskId + "." + name, defaultValue);
    	}
        else
        {
            return configurationService.getIntProperty(name, defaultValue);
        }
    } 
    
    /**
     * Returns task configuration long property value for passed name, else
     * passed default value if no properties defined or no value for passed key.
     * 
     * @param name
     *        the property name
     * @param defaultValue value
     *        the default value
     * @return value
     *        the property value, or default
     * 
     */
    protected long taskLongProperty(String name, long defaultValue)
    {
        // If a taskID/Name is specified, prepend it on the configuration name
    	if(StringUtils.isNotBlank(taskId))
        {
            return configurationService.getLongProperty(taskId + "." + name, defaultValue);
    	}
        else
        {
            return configurationService.getLongProperty(name, defaultValue);
        }
    }  
    
    /**
     * Returns task configuration boolean property value for passed name, else
     * passed default value if no properties defined or no value for passed key.
     * 
     * @param name
     *        the property name
     * @param defaultValue value
     *        the default value
     * @return value
     *        the property value, or default
     * 
     */
    protected boolean taskBooleanProperty(String name, boolean defaultValue)
    {
        // If a taskID/Name is specified, prepend it on the configuration name
    	if(StringUtils.isNotBlank(taskId))
        {
            return configurationService.getBooleanProperty(taskId + "." + name, defaultValue);
    	}
        else
        {
            return configurationService.getBooleanProperty(name, defaultValue);
        }
    }
    
    /**
     * Returns task configuration Array property value for passed name, else
     * <code>null</code> if no properties defined or no value for passed key.
     * 
     * @param name
     *        the property name
     * @return value
     *        the property value, or null
     * 
     */
    protected String[] taskArrayProperty(String name)
    {
        // If a taskID/Name is specified, prepend it on the configuration name
        if(StringUtils.isNotBlank(taskId))
        {
            return configurationService.getArrayProperty(taskId + "." + name);
    	}
        else
        {
            return configurationService.getArrayProperty(name);
        }
    }
}
