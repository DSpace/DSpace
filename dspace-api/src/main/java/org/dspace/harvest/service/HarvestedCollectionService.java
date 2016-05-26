/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest.service;

import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.dspace.harvest.HarvestedCollection;

import java.sql.SQLException;
import java.util.List;

/**
 * Service interface class for the HarvestedCollection object.
 * The implementation of this class is responsible for all business logic calls for the HarvestedCollection object and is autowired by spring
 *
 * @author kevinvandevelde at atmire.com
 */
public interface HarvestedCollectionService {

    /**
     * Find the harvest settings corresponding to this collection
     *
     * @return a HarvestInstance object corresponding to this collection's settings, null if not found.
     */
    public HarvestedCollection find(Context context, Collection collection) throws SQLException;

    /**
     * Create a new harvest instance row for a specified collection.
     *
     * @return a new HarvestInstance object
     */
    public HarvestedCollection create(Context context, Collection collection) throws SQLException;

    /** Returns whether the specified collection is harvestable, i.e. whether its harvesting
     * options are set up correctly. This is distinct from "ready", since this collection may
     * be in process of being harvested.
     */
    public boolean isHarvestable(Context context, Collection collection) throws SQLException;

    /** Returns whether this harvest instance is actually harvestable, i.e. whether its settings
     * options are set up correctly. This is distinct from "ready", since this collection may
     * be in process of being harvested.
     */
    public boolean isHarvestable(HarvestedCollection harvestedCollection) throws SQLException;

    /** Returns whether the specified collection is ready for immediate harvest.
     */
    public boolean isReady(Context context, Collection collection) throws SQLException;

    public boolean isReady(HarvestedCollection harvestedCollection) throws SQLException;

    /** Find all collections that are set up for harvesting
     *
     * return: list of collection id's
     * @throws SQLException if database error
     */
    public List<HarvestedCollection> findAll(Context context) throws SQLException;

    /** Find all collections that are ready for harvesting
     *
     * return: list of collection id's
     * @throws SQLException if database error
     */
    public List<HarvestedCollection> findReady(Context context) throws SQLException;

    /**
     * Find all collections with the specified status flag.
     * @param context
     * @param status see HarvestInstance.STATUS_...
     * @throws SQLException if database error
     */
    public List<HarvestedCollection> findByStatus(Context context, int status) throws SQLException;

    /** Find the collection that was harvested the longest time ago.
     * @throws SQLException if database error
     */
    public HarvestedCollection findOldestHarvest (Context context) throws SQLException;


    /** Find the collection that was harvested most recently.
     * @throws SQLException if database error
     */
    public HarvestedCollection findNewestHarvest (Context context) throws SQLException;

    public void delete(Context context, HarvestedCollection harvestedCollection) throws SQLException;

    public void update(Context context, HarvestedCollection harvestedCollection) throws SQLException;

    public boolean exists(Context context) throws SQLException;
}
