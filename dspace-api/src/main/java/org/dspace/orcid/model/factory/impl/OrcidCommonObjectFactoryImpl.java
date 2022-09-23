/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orcid.model.factory.impl;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.EnumUtils.isValidEnum;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.dspace.orcid.model.factory.OrcidFactoryUtils.parseConfigurations;
import static org.orcid.jaxb.model.common.SequenceType.ADDITIONAL;
import static org.orcid.jaxb.model.common.SequenceType.FIRST;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.handle.service.HandleService;
import org.dspace.orcid.client.OrcidConfiguration;
import org.dspace.orcid.exception.OrcidValidationException;
import org.dspace.orcid.model.factory.OrcidCommonObjectFactory;
import org.dspace.orcid.model.validator.OrcidValidationError;
import org.dspace.util.MultiFormatDateParser;
import org.dspace.util.SimpleMapConverter;
import org.orcid.jaxb.model.common.ContributorRole;
import org.orcid.jaxb.model.common.FundingContributorRole;
import org.orcid.jaxb.model.common.Iso3166Country;
import org.orcid.jaxb.model.v3.release.common.Contributor;
import org.orcid.jaxb.model.v3.release.common.ContributorAttributes;
import org.orcid.jaxb.model.v3.release.common.Country;
import org.orcid.jaxb.model.v3.release.common.CreditName;
import org.orcid.jaxb.model.v3.release.common.DisambiguatedOrganization;
import org.orcid.jaxb.model.v3.release.common.FuzzyDate;
import org.orcid.jaxb.model.v3.release.common.Organization;
import org.orcid.jaxb.model.v3.release.common.OrganizationAddress;
import org.orcid.jaxb.model.v3.release.common.Url;
import org.orcid.jaxb.model.v3.release.record.FundingContributor;
import org.orcid.jaxb.model.v3.release.record.FundingContributorAttributes;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link OrcidCommonObjectFactory}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidCommonObjectFactoryImpl implements OrcidCommonObjectFactory {

    @Autowired
    private ItemService itemService;

    @Autowired
    private OrcidConfiguration orcidConfiguration;

    @Autowired
    private HandleService handleService;

    private SimpleMapConverter countryConverter;

    private String organizationTitleField;

    private String organizationCityField;

    private String organizationCountryField;

    private String contributorEmailField;

    private String contributorOrcidField;

    private Map<String, String> disambiguatedOrganizationIdentifierFields = new HashMap<>();

    @Override
    public Optional<FuzzyDate> createFuzzyDate(MetadataValue metadataValue) {

        if (isUnprocessableValue(metadataValue)) {
            return empty();
        }

        Date date = MultiFormatDateParser.parse(metadataValue.getValue());
        if (date == null) {
            return empty();
        }

        LocalDate localDate = convertToLocalDate(date);
        return of(FuzzyDate.valueOf(localDate.getYear(), localDate.getMonthValue(), localDate.getDayOfMonth()));
    }

    @Override
    public Optional<Organization> createOrganization(Context context, Item orgUnit) {

        if (orgUnit == null) {
            return Optional.empty();
        }

        Organization organization = new Organization();

        organization.setName(getMetadataValue(orgUnit, organizationTitleField));
        organization.setAddress(createOrganizationAddress(orgUnit));
        organization.setDisambiguatedOrganization(createDisambiguatedOrganization(orgUnit));

        return of(organization);
    }

    @Override
    public Optional<Contributor> createContributor(Context context, MetadataValue metadataValue, ContributorRole role) {
        if (isUnprocessableValue(metadataValue)) {
            return empty();
        }

        Contributor contributor = new Contributor();
        contributor.setCreditName(new CreditName(metadataValue.getValue()));
        contributor.setContributorAttributes(getContributorAttributes(metadataValue, role));

        return of(contributor);
    }

    @Override
    public Optional<FundingContributor> createFundingContributor(Context context, MetadataValue metadataValue,
        FundingContributorRole role) {

        if (isUnprocessableValue(metadataValue)) {
            return empty();
        }

        FundingContributor contributor = new FundingContributor();
        contributor.setCreditName(new CreditName(metadataValue.getValue()));
        contributor.setContributorAttributes(getFundingContributorAttributes(metadataValue, role));

        return of(contributor);
    }

    @Override
    public Optional<Url> createUrl(Context context, Item item) {
        String handle = item.getHandle();
        if (StringUtils.isBlank(handle)) {
            return empty();
        }

        return of(new Url(handleService.getCanonicalForm(handle)));
    }

    @Override
    public Optional<Country> createCountry(Context context, MetadataValue metadataValue) {

        if (isUnprocessableValue(metadataValue)) {
            return empty();
        }

        Optional<Iso3166Country> country = convertToIso3166Country(metadataValue.getValue());

        if (country.isEmpty()) {
            throw new OrcidValidationException(OrcidValidationError.INVALID_COUNTRY);
        }

        return country.map(isoCountry -> new Country(isoCountry));
    }

    private ContributorAttributes getContributorAttributes(MetadataValue metadataValue, ContributorRole role) {
        ContributorAttributes attributes = new ContributorAttributes();
        attributes.setContributorRole(role != null ? role : null);
        attributes.setContributorSequence(metadataValue.getPlace() == 0 ? FIRST : ADDITIONAL);
        return attributes;
    }

    private OrganizationAddress createOrganizationAddress(Item organizationItem) {
        OrganizationAddress address = new OrganizationAddress();

        address.setCity(getMetadataValue(organizationItem, organizationCityField));

        convertToIso3166Country(getMetadataValue(organizationItem, organizationCountryField))
            .ifPresent(address::setCountry);

        return address;
    }

    private FundingContributorAttributes getFundingContributorAttributes(MetadataValue metadataValue,
        FundingContributorRole role) {
        FundingContributorAttributes attributes = new FundingContributorAttributes();
        attributes.setContributorRole(role != null ? role : null);
        return attributes;
    }

    private DisambiguatedOrganization createDisambiguatedOrganization(Item organizationItem) {

        for (String identifierField : disambiguatedOrganizationIdentifierFields.keySet()) {

            String source = disambiguatedOrganizationIdentifierFields.get(identifierField);
            String identifier = getMetadataValue(organizationItem, identifierField);

            if (isNotBlank(identifier)) {
                DisambiguatedOrganization disambiguatedOrganization = new DisambiguatedOrganization();
                disambiguatedOrganization.setDisambiguatedOrganizationIdentifier(identifier);
                disambiguatedOrganization.setDisambiguationSource(source);
                return disambiguatedOrganization;
            }

        }

        return null;
    }

    private Optional<Iso3166Country> convertToIso3166Country(String countryValue) {
        return ofNullable(countryValue)
            .map(value -> countryConverter != null ? countryConverter.getValue(value) : value)
            .filter(value -> isValidEnum(Iso3166Country.class, value))
            .map(value -> Iso3166Country.fromValue(value));
    }

    private boolean isUnprocessableValue(MetadataValue value) {
        return value == null || isBlank(value.getValue());
    }

    private String getMetadataValue(Item item, String metadataField) {
        if (StringUtils.isNotBlank(metadataField)) {
            return itemService.getMetadataFirstValue(item, new MetadataFieldName(metadataField), Item.ANY);
        } else {
            return null;
        }
    }

    private LocalDate convertToLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public String getOrganizationCityField() {
        return organizationCityField;
    }

    public String getOrganizationCountryField() {
        return organizationCountryField;
    }

    public Map<String, String> getDisambiguatedOrganizationIdentifierFields() {
        return disambiguatedOrganizationIdentifierFields;
    }

    public String getContributorEmailField() {
        return contributorEmailField;
    }

    public String getContributorOrcidField() {
        return contributorOrcidField;
    }

    public void setItemService(ItemService itemService) {
        this.itemService = itemService;
    }

    public OrcidConfiguration getOrcidConfiguration() {
        return orcidConfiguration;
    }

    public void setOrcidConfiguration(OrcidConfiguration orcidConfiguration) {
        this.orcidConfiguration = orcidConfiguration;
    }

    public void setOrganizationCityField(String organizationCityField) {
        this.organizationCityField = organizationCityField;
    }

    public void setOrganizationCountryField(String organizationCountryField) {
        this.organizationCountryField = organizationCountryField;
    }

    public void setContributorEmailField(String contributorEmailField) {
        this.contributorEmailField = contributorEmailField;
    }

    public void setContributorOrcidField(String contributorOrcidField) {
        this.contributorOrcidField = contributorOrcidField;
    }

    public void setDisambiguatedOrganizationIdentifierFields(String disambiguatedOrganizationIds) {
        this.disambiguatedOrganizationIdentifierFields = parseConfigurations(disambiguatedOrganizationIds);
    }

    public SimpleMapConverter getCountryConverter() {
        return countryConverter;
    }

    public void setCountryConverter(SimpleMapConverter countryConverter) {
        this.countryConverter = countryConverter;
    }

    public String getOrganizationTitleField() {
        return organizationTitleField;
    }

    public void setOrganizationTitleField(String organizationTitleField) {
        this.organizationTitleField = organizationTitleField;
    }

}
