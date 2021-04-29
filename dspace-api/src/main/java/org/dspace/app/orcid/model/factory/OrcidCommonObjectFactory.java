/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.model.factory;

import java.util.Optional;

import org.dspace.content.MetadataValue;
import org.dspace.core.Context;
import org.orcid.jaxb.model.v3.release.common.FuzzyDate;
import org.orcid.jaxb.model.v3.release.common.Organization;

/**
 * Interface for factory classes that creates common ORCID objects.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface OrcidCommonObjectFactory {

    /**
     * Creates an instance of {@link FuzzyDate} if the given metadata value
     * represent a date with a supported format.
     *
     * @param  metadataValue the metadata value
     * @return               the FuzzyDate istance, if any
     */
    public Optional<FuzzyDate> createFuzzyDate(MetadataValue metadataValue);

    /**
     * Creates an instance of {@link Organization} from the given metadata value.
     *
     * @param  context       the DSpace context
     * @param  metadataValue the metadata value
     * @return               the created Organization's instance, if any
     */
    public Optional<Organization> createOrganization(Context context, MetadataValue metadataValue);
}
