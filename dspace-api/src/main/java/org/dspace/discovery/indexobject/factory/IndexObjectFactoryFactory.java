/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.indexobject.factory;

import java.sql.SQLException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.discovery.IndexableObject;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.xmlworkflow.storedcomponents.service.ClaimedTaskService;
import org.dspace.xmlworkflow.storedcomponents.service.PoolTaskService;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Abstract factory to get the IndexFactory objects
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public abstract class IndexObjectFactoryFactory {

    @Autowired
    protected WorkspaceItemService workspaceItemService;
    @Autowired
    protected XmlWorkflowItemService xmlWorkflowItemService;
    @Autowired
    protected ClaimedTaskService claimedTaskService;
    @Autowired
    protected PoolTaskService poolTaskService;

    /**
     * Return the list of all the available implementations of the IndexableObjectService interface
     *
     * @return the list of IndexableObjectService
     */
    public abstract List<IndexFactory> getIndexFactories();

    /**
     * Retrieve the IndexFactory implementation for the given indexable object
     * @param indexableObject the indexable object for which we need our factory
     * @return An IndexFactory implementation
     */
    public IndexFactory getIndexableObjectFactory(IndexableObject indexableObject) {
        return getIndexableObjectFactory(indexableObject.getType());
    }

    /**
     * Retrieve the IndexFactory implementation for the given indexable object unique identifier
      * @param indexableObjectUniqueString the unique identifier of an indexable object
     * @return An IndexFactory implementation
     */
    public IndexFactory getIndexableObjectFactory(String indexableObjectUniqueString) {
        // The unique identifier of an IndexableObject will always be {type}-{identifier}
        return getIndexFactoryByType(StringUtils.substringBefore(indexableObjectUniqueString, "-"));
    }

    /**
     * Retrieve the IndexFactory implementation for the given indexable object type
     * @param indexableFactoryType the object type of the indexable object
     * @return An IndexFactory implementation
     */
    public IndexFactory getIndexFactoryByType(String indexableFactoryType) {
        for (IndexFactory indexableObjectFactory : getIndexFactories()) {
            if (indexableObjectFactory.getType().equals(indexableFactoryType)) {
                return indexableObjectFactory;
            }
        }
        return null;
    }

    /**
     * Retrieve all the indexable objects for the provided object
     * @param context   DSpace context object
     * @param object    The object we want to retrieve our indexable objects for
     * @return          A list of indexable objects
     */
    public List<IndexableObject> getIndexableObjects(Context context, Object object) throws SQLException {
        for (IndexFactory indexableObjectFactory : getIndexFactories()) {
            if (indexableObjectFactory.supports(object)) {
                return indexableObjectFactory.getIndexableObjects(context, object);
            }
        }
        throw new IllegalArgumentException("The object: " + object.getClass().getName()
                + " cannot be indexed");
    }

    /**
     * Retrieve an implementation instance for this factory
     * @return an IndexObjectServiceFactory bean
     */
    public static IndexObjectFactoryFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager()
                                    .getServiceByName("indexObjectFactoryFactory", IndexObjectFactoryFactory.class);
    }
}