/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.service;


import java.sql.SQLException;

import org.dspace.content.Relationship;
import org.dspace.core.Context;

/**
 *
 * This service is handles relations between different items.
 *
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 *
 */
public interface RelationshipPlacesIndexingService {

    /**
     * Once a relation is created, left item's references are put into right item's data
     * and the same happens from right to left.
     * If relations has some impacts on items different than two involved
     * (i.e. places should be updated) all impacted
     * items are updated.
     *
     * @param context
     * @param relationship
     * @throws SQLException
     */
    void updateRelationReferences(final Context context, Relationship relationship) throws SQLException;
}
