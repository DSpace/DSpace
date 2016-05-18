/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao;

import org.dspace.content.Community;
import org.dspace.content.MetadataField;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

import java.sql.SQLException;
import java.util.List;

/**
 * Database Access Object interface class for the Community object.
 * The implementation of this class is responsible for all database calls for the Community object and is autowired by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface CommunityDAO extends DSpaceObjectLegacySupportDAO<Community> {

    public List<Community> findAll(Context context, MetadataField sortField) throws SQLException;

    public List<Community> findAll(Context context, MetadataField sortField, Integer limit, Integer offset) throws SQLException;

    public Community findByAdminGroup(Context context, Group group) throws SQLException;

    public List<Community> findAllNoParent(Context context, MetadataField sortField) throws SQLException;

    public List<Community> findAuthorized(Context context, EPerson ePerson, List<Integer> actions) throws SQLException;

    public List<Community> findAuthorizedByGroup(Context context, EPerson currentUser, List<Integer> actions) throws SQLException;

    int countRows(Context context) throws SQLException;
}
