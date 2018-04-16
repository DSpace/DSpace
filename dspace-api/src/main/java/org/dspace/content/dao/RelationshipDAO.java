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

import org.dspace.content.Item;
import org.dspace.content.Relationship;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;

public interface RelationshipDAO extends GenericDAO<Relationship> {

    List<Relationship> findByItem(Context context,Item item) throws SQLException;

    int findLeftPlaceByLeftItem(Context context,Item item) throws SQLException;

    int findRightPlaceByRightItem(Context context,Item item) throws SQLException;
}
