/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

/**
 * External provider allowed to register e-persons stored with {@link RegistrationData}
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public enum RegistrationTypeEnum {

    ORCID("external-login"),
    VALIDATION_ORCID("review-account"),
    FORGOT("forgot"),
    REGISTER("register"),
    INVITATION("invitation"),
    CHANGE_PASSWORD("change-password");

    private final String link;

    RegistrationTypeEnum(String link) {
        this.link = link;
    }

    public String getLink() {
        return link;
    }
}
