/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.service;

import java.util.List;

import org.dspace.content.crosswalk.CrosswalkMode;
import org.dspace.core.Context;

/**
 * Service interface class for the ItemExportFormat object.
 * The implementation of this class is responsible for all business logic calls for the ItemExportFormat object and is
 * autowired by spring
 * 
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 *
 */
public interface ItemExportFormatService {

    /**
     * Get the item export format by id.
     * 
     * @param context
     *            DSpace context object
     * @param id
     *            id of the item export format
     * @return
     */
    ItemExportFormat get(Context context, String id);

    /**
     * 
     * Retrieve all the item export formats.
     * 
     * @param context
     *            DSpace context object
     * @return
     */
    List<ItemExportFormat> getAll(Context context);

    /**
     * Get all the item export formats for the particular entity type and molteplicity
     * 
     * @param context
     *            DSpace context object
     * @param entityTypeId
     *            the entitytype id related to item objects
     * @param molteplicity
     *            the export molteplicity
     * @return
     */
    List<ItemExportFormat> byEntityTypeAndMolteplicity(Context context, String entityTypeId,
            CrosswalkMode molteplicity);

}