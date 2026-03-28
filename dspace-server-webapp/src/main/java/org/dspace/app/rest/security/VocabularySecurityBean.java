/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Methods of this class are used on PreAuthorize annotations
 * to check security on vocabulary endpoint
 * 
 * @author Davide Negretti (davide.negretti at 4science.it)
 */
@Component(value = "vocabularySecurity")
public class VocabularySecurityBean {

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private ChoiceAuthorityService choiceAuthorityService;

    /**
     * This method checks if a vocabulary is public.
     *
     * @return the value of `authority.AUTHORITY_NAME.public`, or false if not set
     */
    public boolean isVocabularyPublic(String name) {
        ChoiceAuthority choiceAuthority = choiceAuthorityService.getChoiceAuthorityByAuthorityName(name);
        return choiceAuthority.isPublic();
    }

}
