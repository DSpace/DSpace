/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orcid.model.validator;

import java.util.List;

import org.orcid.jaxb.model.v3.release.record.Funding;
import org.orcid.jaxb.model.v3.release.record.Work;

/**
 * Interface for classes that validate the ORCID entity objects.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface OrcidValidator {

    /**
     * Validate the given orcid object and returns the validation errors, if any.
     *
     * @param  object the ORCID object to validate
     * @return        the validation errors, if any
     */
    List<OrcidValidationError> validate(Object object);

    /**
     * Validate the given work and returns the validation errors, if any.
     *
     * @param  work the work to validate
     * @return      the validation errors, if any
     */
    List<OrcidValidationError> validateWork(Work work);

    /**
     * Validate the given funding and returns the validation errors, if any.
     *
     * @param  funding the funding to validate
     * @return         the validation errors, if any
     */
    List<OrcidValidationError> validateFunding(Funding funding);
}
