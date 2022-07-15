/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Methods of this class are used on PreAuthorize annotations
 * to check security on versioning endpoint
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
@Component(value = "versioningSecurity")
public class VersioningSecurityBean {

    @Autowired
    private ConfigurationService configurationService;

    /**
     * This method checks if the versioning features are enabled
     *
     * @return true if is enabled or unset
     */
    public boolean isEnableVersioning() {
        return configurationService.getBooleanProperty("versioning.enabled", true);
    }

}