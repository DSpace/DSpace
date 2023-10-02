/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.hibernate.Session;

/**
 * Database Access Object interface class for the Collection object.
 * The implementation of this class is responsible for all database calls for the Collection object and is autowired
 * by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface CollectionDAO extends DSpaceObjectLegacySupportDAO<Collection> {

    public List<Collection> findAll(Session session, MetadataField order) throws SQLException;

    public List<Collection> findAll(Session session, MetadataField order, Integer limit, Integer offset)
        throws SQLException;

    public Collection findByTemplateItem(Session session, Item item) throws SQLException;

    /**
     * Find a Collection having a given submitter or administrator group.
     *
     * @param session current request's database context.
     * @param group EPerson Group
     * @return the collection, if any, that has the specified group as administrators or submitters
     * @throws SQLException
     */
    public Collection findByGroup(Session session, Group group) throws SQLException;

    public List<Collection> findAuthorized(Session session, EPerson ePerson, List<Integer> actions) throws SQLException;

    List<Collection> findAuthorizedByGroup(Session session, EPerson ePerson, List<Integer> actions) throws SQLException;

    List<Collection> findCollectionsWithSubscribers(Session session) throws SQLException;

    int countRows(Session session) throws SQLException;

    List<Map.Entry<Collection, Long>> getCollectionsWithBitstreamSizesTotal(Session session) throws SQLException;
}
