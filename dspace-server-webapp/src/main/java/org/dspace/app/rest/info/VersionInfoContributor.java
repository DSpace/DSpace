/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.info;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import org.dspace.app.util.Util;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.info.Info.Builder;
import org.springframework.boot.actuate.info.InfoContributor;

/**
 * Implementation of {@link InfoContributor} that add the version info.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class VersionInfoContributor implements InfoContributor {

    @Autowired
    private ConfigurationService configurationService;

    @Override
    public void contribute(Builder builder) {
        String sourceVersion = Util.getSourceVersion();
        if (isNotBlank(sourceVersion)) {
            String versionAttribute = configurationService.getProperty("actuator.info.version-attribute", "version");
            builder.withDetail(versionAttribute, sourceVersion);
        }
    }

}
