/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.info;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.LinkedHashMap;
import java.util.Map;

import org.dspace.app.util.Util;
import org.springframework.boot.actuate.info.Info.Builder;
import org.springframework.boot.actuate.info.InfoContributor;

/**
 * Implementation of {@link InfoContributor} that add the version info.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class VersionInfoContributor implements InfoContributor {

    private static final String APP_INFO = "app";

    @Override
    public void contribute(Builder builder) {
        String sourceVersion = Util.getSourceVersion();
        if (isNotBlank(sourceVersion)) {
            Map<String, Object> appMap = getAppInfoMap(builder);
            builder.withDetails(buildAppWithVersion(appMap, sourceVersion));
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getAppInfoMap(Builder builder) {
        Object app = builder.build().get(APP_INFO);
        return isMap(app) ? (Map<String, Object>) app : Map.of();
    }

    private boolean isMap(Object object) {
        return object != null && object instanceof Map;
    }

    private Map<String, Object> buildAppWithVersion(Map<String, Object> map, String sourceVersion) {
        Map<String, Object> appWithVersion = new LinkedHashMap<String, Object>(map);
        appWithVersion.put("version", sourceVersion);
        return Map.of(APP_INFO, appWithVersion);
    }

}
