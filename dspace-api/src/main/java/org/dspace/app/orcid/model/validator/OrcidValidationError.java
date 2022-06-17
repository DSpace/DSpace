/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.model.validator;

/**
 * Enum that model all the errors that could occurs during an ORCID object
 * validation.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public enum OrcidValidationError {

    AMOUNT_CURRENCY_REQUIRED("amount-currency.required"),
    EXTERNAL_ID_REQUIRED("external-id.required"),
    TITLE_REQUIRED("title.required"),
    TYPE_REQUIRED("type.required"),
    FUNDER_REQUIRED("funder.required"),
    INVALID_COUNTRY("country.invalid"),
    ORGANIZATION_NAME_REQUIRED("organization.name-required"),
    ORGANIZATION_ADDRESS_REQUIRED("organization.address-required"),
    ORGANIZATION_CITY_REQUIRED("organization.city-required"),
    ORGANIZATION_COUNTRY_REQUIRED("organization.country-required"),
    DISAMBIGUATED_ORGANIZATION_REQUIRED("disambiguated-organization.required"),
    DISAMBIGUATED_ORGANIZATION_VALUE_REQUIRED("disambiguated-organization.value-required"),
    DISAMBIGUATION_SOURCE_REQUIRED("disambiguation-source.required"),
    DISAMBIGUATION_SOURCE_INVALID("disambiguation-source.invalid");

    private final String code;

    private OrcidValidationError(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

}
