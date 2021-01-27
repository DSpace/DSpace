/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest.service;

import org.dspace.harvest.HarvestedCollection;
import org.dspace.harvest.model.OAIHarvesterValidationResult;
import org.jdom.Element;

/**
 * Service to validate a given record during the harvesting process.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface OAIHarvesterValidator {

    /**
     * Validate the given element with the rules related to the harvested
     * collection.
     *
     * @param  record              the element to validate
     * @param  harvestedCollection the harvested collection
     * @return                     the validation result
     */
    OAIHarvesterValidationResult validate(Element record, HarvestedCollection harvestedCollection);
}
