/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.model;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.dspace.app.orcid.model.factory.OrcidFactoryUtils.parseConfigurations;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.dspace.app.orcid.model.factory.OrcidFactoryUtils;
import org.dspace.util.SimpleMapConverter;
import org.orcid.jaxb.model.common.FundingContributorRole;

/**
 * Class that contains all the mapping between {@link Funding} and DSpaceCris
 * metadata fields.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidFundingFieldMapping {

    private Map<String, FundingContributorRole> contributorFields;

    private Map<String, String> externalIdentifierFields;

    private String titleField;

    private String typeField;

    private SimpleMapConverter typeConverter;

    private String amountField;

    private String amountCurrencyField;

    private SimpleMapConverter amountCurrencyConverter;

    private String startDateField;

    private String endDateField;

    private String descriptionField;

    private String organizationField;

    private Map<String, FundingContributorRole> parseContributors(String contributors) {
        Map<String, String> contributorsMap = parseConfigurations(contributors);
        return contributorsMap.keySet().stream()
            .collect(toMap(identity(), field -> parseContributorRole(contributorsMap.get(field))));
    }

    private FundingContributorRole parseContributorRole(String contributorRole) {
        try {
            return FundingContributorRole.fromValue(contributorRole);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("The funding contributor role " + contributorRole +
                " is invalid, allowed values are " + getAllowedContributorRoles(), ex);
        }
    }

    private List<String> getAllowedContributorRoles() {
        return Arrays.asList(FundingContributorRole.values()).stream()
            .map(FundingContributorRole::value)
            .collect(Collectors.toList());
    }

    public Map<String, String> getExternalIdentifierFields() {
        return externalIdentifierFields;
    }

    public void setExternalIdentifierFields(String externalIdentifierFields) {
        this.externalIdentifierFields = OrcidFactoryUtils.parseConfigurations(externalIdentifierFields);
    }

    public Map<String, FundingContributorRole> getContributorFields() {
        return contributorFields;
    }

    public void setContributorFields(String contributorFields) {
        this.contributorFields = parseContributors(contributorFields);
    }

    public String getTitleField() {
        return titleField;
    }

    public void setTitleField(String titleField) {
        this.titleField = titleField;
    }

    public String getStartDateField() {
        return startDateField;
    }

    public void setStartDateField(String startDateField) {
        this.startDateField = startDateField;
    }

    public String getEndDateField() {
        return endDateField;
    }

    public void setEndDateField(String endDateField) {
        this.endDateField = endDateField;
    }

    public String getDescriptionField() {
        return descriptionField;
    }

    public void setDescriptionField(String descriptionField) {
        this.descriptionField = descriptionField;
    }

    public String getOrganizationField() {
        return organizationField;
    }

    public void setOrganizationField(String organizationField) {
        this.organizationField = organizationField;
    }

    public String getTypeField() {
        return typeField;
    }

    public void setTypeField(String typeField) {
        this.typeField = typeField;
    }

    public String getAmountField() {
        return amountField;
    }

    public void setAmountField(String amountField) {
        this.amountField = amountField;
    }

    public String getAmountCurrencyField() {
        return amountCurrencyField;
    }

    public void setAmountCurrencyField(String amountCurrencyField) {
        this.amountCurrencyField = amountCurrencyField;
    }

    public String convertAmountCurrency(String currency) {
        return amountCurrencyConverter != null ? amountCurrencyConverter.getValue(currency) : currency;
    }

    public void setAmountCurrencyConverter(SimpleMapConverter amountCurrencyConverter) {
        this.amountCurrencyConverter = amountCurrencyConverter;
    }

    public String convertType(String type) {
        return typeConverter != null ? typeConverter.getValue(type) : type;
    }

    public void setTypeConverter(SimpleMapConverter typeConverter) {
        this.typeConverter = typeConverter;
    }

}
