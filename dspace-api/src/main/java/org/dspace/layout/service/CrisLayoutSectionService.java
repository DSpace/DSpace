/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.service;

import java.util.List;

import org.dspace.layout.CrisLayoutSection;

/**
 * The service to manage the {@link CrisLayoutSection}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public interface CrisLayoutSectionService {

    /**
     * Find all the configured CRIS layout sections.
     *
     * @return a list of layout sections
     */
    List<CrisLayoutSection> findAll();


    /**
     * Find all the configured CRIS layout sections
     * marked as visible.
     * 
     * @return a list of visible layout sections
     */
    List<CrisLayoutSection> findAllVisibleSectionsInTopBar();

    /**
     * Find a single CRIS layout section by the given id.
     *
     * @param id the layout section id
     * @return the found layout section
     */
    CrisLayoutSection findOne(String id);

    /**
     * Counts all the configured CRIS layout sections.
     *
     * @return the count result
     */
    int countTotal();

    /**
     * Counts all the visible CRIS layout sections
     * in the top bar
     * 
     * @return the count result
     */
    int countVisibleSectionsInTopBar();
}
