/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.model.validator.impl;

import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.ArrayUtils.contains;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.dspace.app.orcid.model.validator.OrcidValidationError.AMOUNT_CURRENCY_REQUIRED;
import static org.dspace.app.orcid.model.validator.OrcidValidationError.DISAMBIGUATED_ORGANIZATION_REQUIRED;
import static org.dspace.app.orcid.model.validator.OrcidValidationError.DISAMBIGUATED_ORGANIZATION_VALUE_REQUIRED;
import static org.dspace.app.orcid.model.validator.OrcidValidationError.DISAMBIGUATION_SOURCE_INVALID;
import static org.dspace.app.orcid.model.validator.OrcidValidationError.DISAMBIGUATION_SOURCE_REQUIRED;
import static org.dspace.app.orcid.model.validator.OrcidValidationError.EXTERNAL_ID_REQUIRED;
import static org.dspace.app.orcid.model.validator.OrcidValidationError.FUNDER_REQUIRED;
import static org.dspace.app.orcid.model.validator.OrcidValidationError.ORGANIZATION_ADDRESS_REQUIRED;
import static org.dspace.app.orcid.model.validator.OrcidValidationError.ORGANIZATION_CITY_REQUIRED;
import static org.dspace.app.orcid.model.validator.OrcidValidationError.ORGANIZATION_COUNTRY_REQUIRED;
import static org.dspace.app.orcid.model.validator.OrcidValidationError.ORGANIZATION_NAME_REQUIRED;
import static org.dspace.app.orcid.model.validator.OrcidValidationError.ORGANIZATION_REQUIRED;
import static org.dspace.app.orcid.model.validator.OrcidValidationError.START_DATE_REQUIRED;
import static org.dspace.app.orcid.model.validator.OrcidValidationError.TITLE_REQUIRED;
import static org.dspace.app.orcid.model.validator.OrcidValidationError.TYPE_REQUIRED;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.dspace.app.orcid.model.validator.OrcidValidationError;
import org.dspace.app.orcid.model.validator.OrcidValidator;
import org.dspace.services.ConfigurationService;
import org.orcid.jaxb.model.v3.release.common.DisambiguatedOrganization;
import org.orcid.jaxb.model.v3.release.common.Organization;
import org.orcid.jaxb.model.v3.release.common.OrganizationAddress;
import org.orcid.jaxb.model.v3.release.record.Affiliation;
import org.orcid.jaxb.model.v3.release.record.ExternalIDs;
import org.orcid.jaxb.model.v3.release.record.Funding;
import org.orcid.jaxb.model.v3.release.record.FundingTitle;
import org.orcid.jaxb.model.v3.release.record.Work;
import org.orcid.jaxb.model.v3.release.record.WorkTitle;

/**
 * Implementation of {@link OrcidValidator}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidValidatorImpl implements OrcidValidator {

    private final ConfigurationService configurationService;

    public OrcidValidatorImpl(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @Override
    public List<OrcidValidationError> validate(Object object) {

        if (object instanceof Work && isWorkValidationEnabled()) {
            return validateWork((Work) object);
        }

        if (object instanceof Funding && isFundingValidationEnabled()) {
            return validateFunding((Funding) object);
        }

        if (object instanceof Affiliation && isAffiliationValidationEnabled()) {
            return validateAffiliation((Affiliation) object);
        }

        return Collections.emptyList();
    }

    @Override
    public List<OrcidValidationError> validateWork(Work work) {
        List<OrcidValidationError> errors = new ArrayList<OrcidValidationError>();

        WorkTitle title = work.getWorkTitle();
        if (title == null || title.getTitle() == null || isBlank(title.getTitle().getContent())) {
            errors.add(TITLE_REQUIRED);
        }

        if (work.getWorkType() == null) {
            errors.add(TYPE_REQUIRED);
        }

        ExternalIDs externalIdentifiers = work.getExternalIdentifiers();

        if (externalIdentifiers == null || isEmpty(externalIdentifiers.getExternalIdentifier())) {
            errors.add(EXTERNAL_ID_REQUIRED);
        }

        return errors;
    }

    @Override
    public List<OrcidValidationError> validateFunding(Funding funding) {

        List<OrcidValidationError> errors = new ArrayList<OrcidValidationError>();

        FundingTitle title = funding.getTitle();
        if (title == null || title.getTitle() == null || isBlank(title.getTitle().getContent())) {
            errors.add(TITLE_REQUIRED);
        }

        ExternalIDs externalIdentifiers = funding.getExternalIdentifiers();

        if (externalIdentifiers == null || isEmpty(externalIdentifiers.getExternalIdentifier())) {
            errors.add(EXTERNAL_ID_REQUIRED);
        }

        if (funding.getOrganization() == null) {
            errors.add(FUNDER_REQUIRED);
        } else {
            errors.addAll(validate(funding.getOrganization()));
        }

        if (funding.getAmount() != null && isBlank(funding.getAmount().getCurrencyCode())) {
            errors.add(AMOUNT_CURRENCY_REQUIRED);
        }

        return errors;
    }

    @Override
    public List<OrcidValidationError> validateAffiliation(Affiliation affiliation) {
        List<OrcidValidationError> errors = new ArrayList<OrcidValidationError>();
        if (affiliation.getStartDate() == null) {
            errors.add(START_DATE_REQUIRED);
        }

        if (affiliation.getOrganization() == null) {
            errors.add(ORGANIZATION_REQUIRED);
        } else {
            errors.addAll(validate(affiliation.getOrganization()));
        }

        return errors;
    }

    private List<OrcidValidationError> validate(Organization organization) {
        List<OrcidValidationError> errors = new ArrayList<OrcidValidationError>();
        if (isBlank(organization.getName())) {
            errors.add(ORGANIZATION_NAME_REQUIRED);
        }

        errors.addAll(validate(organization.getAddress()));
        errors.addAll(validate(organization.getDisambiguatedOrganization()));

        return errors;
    }

    private List<OrcidValidationError> validate(DisambiguatedOrganization disambiguatedOrganization) {

        List<OrcidValidationError> errors = new ArrayList<OrcidValidationError>();


        if (disambiguatedOrganization == null) {
            errors.add(DISAMBIGUATED_ORGANIZATION_REQUIRED);
            return errors;
        }

        if (isBlank(disambiguatedOrganization.getDisambiguatedOrganizationIdentifier())) {
            errors.add(DISAMBIGUATED_ORGANIZATION_VALUE_REQUIRED);
        }

        String disambiguationSource = disambiguatedOrganization.getDisambiguationSource();

        if (isBlank(disambiguationSource)) {
            errors.add(DISAMBIGUATION_SOURCE_REQUIRED);
        } else if (isInvalidDisambiguationSource(disambiguationSource)) {
            errors.add(DISAMBIGUATION_SOURCE_INVALID);
        }

        return errors;
    }

    private List<OrcidValidationError> validate(OrganizationAddress address) {
        List<OrcidValidationError> errors = new ArrayList<OrcidValidationError>();

        if (address == null) {
            errors.add(ORGANIZATION_ADDRESS_REQUIRED);
            return errors;
        }

        if (isBlank(address.getCity())) {
            errors.add(ORGANIZATION_CITY_REQUIRED);
        }

        if (address.getCountry() == null) {
            errors.add(ORGANIZATION_COUNTRY_REQUIRED);
        }

        return errors;
    }

    private boolean isInvalidDisambiguationSource(String disambiguationSource) {
        return !contains(getDisambiguedOrganizationSources(), disambiguationSource);
    }

    private String[] getDisambiguedOrganizationSources() {
        return configurationService.getArrayProperty("orcid.validation.organization.identifier-sources");
    }

    private boolean isWorkValidationEnabled() {
        return configurationService.getBooleanProperty("orcid.validation.work.enabled", true);
    }

    private boolean isFundingValidationEnabled() {
        return configurationService.getBooleanProperty("orcid.validation.funding.enabled", true);
    }

    private boolean isAffiliationValidationEnabled() {
        return configurationService.getBooleanProperty("orcid.validation.affiliation.enabled", true);
    }

}
