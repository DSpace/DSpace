/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao;

import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Database Access Object interface class for the Collection object.
 * The implementation of this class is responsible for all database calls for the Collection object and is autowired by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface CollectionDAO extends DSpaceObjectLegacySupportDAO<Collection> {

    public List<Collection> findAll(Context context, MetadataField order) throws SQLException;

    public List<Collection> findAll(Context context, MetadataField order, Integer limit, Integer offset) throws SQLException;

    public Collection findByTemplateItem(Context context, Item item) throws SQLException;

    public Collection findByGroup(Context context, Group group) throws SQLException;

    public List<Collection> findAuthorized(Context context, EPerson ePerson, List<Integer> actions) throws SQLException;

    List<Collection> findAuthorizedByGroup(Context context, EPerson ePerson, List<Integer> actions) throws SQLException;

    List<Collection> findCollectionsWithSubscribers(Context context) throws SQLException;

    int countRows(Context context) throws SQLException;

    List<Map.Entry<Collection, Long>> getCollectionsWithBitstreamSizesTotal(Context context) throws SQLException;
}
