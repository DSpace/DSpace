/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.logic.condition;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.logic.LogicalStatementException;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.handle.service.HandleService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Abstract class for conditions, to implement the basic getter and setter parameters
 *
 * @author Kim Shepherd
 * @version $Revision$
 */
public abstract class AbstractCondition implements Condition {

    // Parameters map (injected, required -- see setter annotation)
    private Map<String, Object> parameters;

    // Declare and instantiate spring services
    @Autowired(required = true)
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    @Autowired(required = true)
    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    @Autowired(required = true)
    protected HandleService handleService;

    // Logging
    Logger log = LogManager.getLogger(AbstractCondition.class);

    /**
     * Get parameters set by spring configuration in item-filters.xml
     * These could be any kind of map that the extending condition class needs for evaluation
     * @return map of parameters
     * @throws LogicalStatementException
     */
    @Override
    public Map<String, Object> getParameters() throws LogicalStatementException {
        return this.parameters;
    }

    /**
     * Set parameters - used by Spring when creating beans from item-filters.xml
     * These could be any kind of map that the extending condition class needs for evaluation
     * @param parameters
     * @throws LogicalStatementException
     */
    @Autowired(required = true)
    @Override
    public void setParameters(Map<String, Object> parameters) throws LogicalStatementException {
        this.parameters = parameters;
    }

    /**
     * Get the result of logical evaluation for an item
     * @param context   DSpace context
     * @param item      Item to evaluate
     * @return boolean
     * @throws LogicalStatementException
     */
    @Override
    public boolean getResult(Context context, Item item) throws LogicalStatementException {
        if (item == null) {
            log.error("Error evaluating item. Passed item is null, returning false");
            return false;
        }
        if (context == null) {
            throw new IllegalStateException("Context is null");
        }
        return true;
    }

    @Override
    public void setItemService(ItemService itemService) {
        this.itemService = itemService;
    }
}
