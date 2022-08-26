/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orcid.model.validator;

/**
 * Enum that model all the errors that could occurs during an ORCID object
 * validation. These codes are used by the {@link OrcidValidator} to returns the
 * validation error related to a specific ORCID entity. The values of this enum
 * are returned from the OrcidHistoryRestRepository and can be used to show an
 * error message to the users when they tries to synchronize some data with
 * ORCID.
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
    PUBLICATION_DATE_INVALID("publication.date-invalid"),
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
