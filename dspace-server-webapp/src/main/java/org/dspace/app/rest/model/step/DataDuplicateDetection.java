/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.step;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.dspace.app.rest.model.PotentialDuplicateRest;

/**
 * Section data model for potential duplicate items detected during submission
 * 
 * @author Kim Shepherd
 */
public class DataDuplicateDetection implements SectionData {
    public DataDuplicateDetection() {
    }

    /**
     * A list of potential duplicate items found by DuplicateDetectionService, in their REST model form
     */
    @JsonUnwrapped
    private List<PotentialDuplicateRest> potentialDuplicates;

    /**
     * Return the list of detected potential duplicates in REST model form
     * @return list of potential duplicate REST models
     */
    public List<PotentialDuplicateRest> getPotentialDuplicates() {
        return potentialDuplicates;
    }

    /**
     * Set list of potential duplicates.
     * @see org.dspace.app.rest.converter.PotentialDuplicateConverter
     * @param potentialDuplicates list of potential duplicates
     */
    public void setPotentialDuplicates(List<PotentialDuplicateRest> potentialDuplicates) {
        this.potentialDuplicates = potentialDuplicates;
    }
}