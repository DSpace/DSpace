/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orcid.model.factory;

import java.util.Optional;

import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;
import org.dspace.orcid.exception.OrcidValidationException;
import org.orcid.jaxb.model.common.ContributorRole;
import org.orcid.jaxb.model.common.FundingContributorRole;
import org.orcid.jaxb.model.v3.release.common.Contributor;
import org.orcid.jaxb.model.v3.release.common.Country;
import org.orcid.jaxb.model.v3.release.common.FuzzyDate;
import org.orcid.jaxb.model.v3.release.common.Organization;
import org.orcid.jaxb.model.v3.release.common.Url;
import org.orcid.jaxb.model.v3.release.record.FundingContributor;

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
     * Creates an instance of {@link Organization} from the given orgUnit item.
     *
     * @param  context the DSpace context
     * @param  orgUnit the orgUnit item
     * @return         the created Organization's instance, if any
     */
    public Optional<Organization> createOrganization(Context context, Item orgUnit);

    /**
     * Creates an instance of {@link Contributor} from the given metadata value.
     *
     * @param  context       the DSpace context
     * @param  metadataValue the metadata value
     * @param  role          the contributor role
     * @return               the created Contributor instance, if any
     */
    public Optional<Contributor> createContributor(Context context, MetadataValue metadataValue, ContributorRole role);

    /**
     * Creates an instance of {@link FundingContributor} from the given metadata
     * value.
     *
     * @param  context       the DSpace context
     * @param  metadataValue the metadata value
     * @param  role          the contributor role
     * @return               the created FundingContributor instance, if any
     */
    public Optional<FundingContributor> createFundingContributor(Context context, MetadataValue metadataValue,
        FundingContributorRole role);

    /**
     * Creates an instance of {@link Url} from the given item.
     * @param  context the DSpace context
     * @param  item    the item
     * @return         the created Url instance, if any
     */
    public Optional<Url> createUrl(Context context, Item item);

    /**
     * Creates an instance of {@link Country} from the given metadata value.
     *
     * @param  context                  the DSpace context
     * @param  metadataValue            the metadata value
     * @return                          the created Country instance, if any
     * @throws OrcidValidationException if the given metadata value is not a valid
     *                                  ISO 3611 country
     */
    public Optional<Country> createCountry(Context context, MetadataValue metadataValue)
        throws OrcidValidationException;

}
