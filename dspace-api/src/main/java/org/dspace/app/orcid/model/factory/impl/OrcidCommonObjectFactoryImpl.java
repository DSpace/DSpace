/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.model.factory.impl;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.dspace.core.CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.orcid.model.factory.OrcidCommonObjectFactory;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.util.MultiFormatDateParser;
import org.dspace.util.UUIDUtils;
import org.orcid.jaxb.model.common.Iso3166Country;
import org.orcid.jaxb.model.v3.release.common.DisambiguatedOrganization;
import org.orcid.jaxb.model.v3.release.common.FuzzyDate;
import org.orcid.jaxb.model.v3.release.common.Organization;
import org.orcid.jaxb.model.v3.release.common.OrganizationAddress;
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

    private final String organizationCityField;

    private final String organizationCountryField;

    private final Map<String, String> disambiguatedOrganizationIdentifierFields = new HashMap<String, String>();

    public OrcidCommonObjectFactoryImpl(String organizationCityField, String organizationCountryField,
        String disambiguatedOrganizationIdentifierFields, String disambiguatedOrganizationSources) {

        this.organizationCityField = organizationCityField;
        this.organizationCountryField = organizationCountryField;

        String[] identifiers = split(disambiguatedOrganizationIdentifierFields);
        String[] sources = split(disambiguatedOrganizationSources);

        if (identifiers.length != sources.length) {
            throw new IllegalArgumentException("The disambiguated organization ids configuration is not "
                + "compliance with the disambiguated organization sources configuration");
        }

        for (int i = 0; i < identifiers.length; i++) {
            this.disambiguatedOrganizationIdentifierFields.put(sources[i], identifiers[i]);
        }

    }

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
    public Optional<Organization> createOrganization(Context context, MetadataValue metadataValue) {

        if (isUnprocessableValue(metadataValue)) {
            return empty();
        }

        Organization organization = new Organization();

        organization.setName(metadataValue.getValue());

        Item organizationItem = findRelatedItem(context, metadataValue);
        if (organizationItem != null) {
            organization.setAddress(createOrganizationAddress(organizationItem));
            organization.setDisambiguatedOrganization(createDisambiguatedOrganization(organizationItem));
        }

        return of(organization);
    }

    private OrganizationAddress createOrganizationAddress(Item organizationItem) {
        OrganizationAddress address = new OrganizationAddress();
        address.setCity(getMetadataValue(organizationItem, organizationCityField));
        address.setCountry(Iso3166Country.fromValue(getMetadataValue(organizationItem, organizationCountryField)));
        return address;
    }

    private DisambiguatedOrganization createDisambiguatedOrganization(Item organizationItem) {

        for (String source : disambiguatedOrganizationIdentifierFields.keySet()) {

            String identifierField = disambiguatedOrganizationIdentifierFields.get(source);
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

    private Item findRelatedItem(Context context, MetadataValue metadataValue) {
        try {
            return itemService.find(context, UUIDUtils.fromString(metadataValue.getAuthority()));
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    private boolean isUnprocessableValue(MetadataValue value) {
        return value == null || isBlank(value.getValue()) || value.getValue().equals(PLACEHOLDER_PARENT_METADATA_VALUE);
    }

    private String getMetadataValue(Item item, String metadataField) {
        return itemService.getMetadataFirstValue(item, new MetadataFieldName(metadataField), Item.ANY);
    }

    private String[] split(String string) {
        String[] result = StringUtils.split(string, ",");
        return result != null ? result : new String[] {};
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

}
