/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.storedcomponents.service;

import java.sql.SQLException;
import java.util.List;

import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.xmlworkflow.storedcomponents.CollectionRole;

/**
 * Service interface class for the CollectionRole object.
 * The implementation of this class is responsible for all business logic calls for the CollectionRole object and is
 * autowired by spring
 *
 * @author kevinvandevelde at atmire.com
 */
public interface CollectionRoleService {

    /**
     * This is the default name of the role equivalent in the default configuration to the "legacy" workflow step1. Old
     * piece of code will expect to use it in place of the workflow step1
     */
    public final String LEGACY_WORKFLOW_STEP1_NAME = "reviewer";

    /**
     * This is the default name of the role equivalent in the default configuration to the "legacy" workflow step2. Old
     * piece of code will expect to use it in place of the workflow step2
     */
    public final String LEGACY_WORKFLOW_STEP2_NAME = "editor";

    /**
     * This is the default name of the role equivalent in the default configuration to the "legacy" workflow step3. Old
     * piece of code will expect to use it in place of the workflow step3
     */
    public final String LEGACY_WORKFLOW_STEP3_NAME = "finaleditor";

    public CollectionRole find(Context context, int id) throws SQLException;

    public CollectionRole find(Context context, Collection collection, String role) throws SQLException;

    /**
     * 
     * @param context
     *            DSpace context
     * @param group
     *            EPerson Group
     * @return the list of CollectionRole assigned to the specified group
     * @throws SQLException
     */
    public List<CollectionRole> findByGroup(Context context, Group group) throws SQLException;

    public List<CollectionRole> findByCollection(Context context, Collection collection) throws SQLException;

    public CollectionRole create(Context context, Collection collection, String roleId, Group group)
        throws SQLException;

    public void update(Context context, CollectionRole collectionRole) throws SQLException;

    public void delete(Context context, CollectionRole collectionRole) throws SQLException;

    public void deleteByCollection(Context context, Collection collection) throws SQLException;
}
