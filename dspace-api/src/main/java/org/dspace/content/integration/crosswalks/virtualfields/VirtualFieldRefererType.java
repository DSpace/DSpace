/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.virtualfields;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class VirtualFieldRefererType implements VirtualField {

    private static final String PROPERTY_PREFIX = "crosswalk.virtualname.referer.type.";

    private final ConfigurationService configurationService;

    @Autowired
    public VirtualFieldRefererType(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    public String[] getMetadata(Item item, Map<String, String> fieldCache, String fieldName) {
        String[] virtualFieldName = fieldName.split("\\.");
        String qualifier = virtualFieldName[2];
        String type = configurationService.getProperty(PROPERTY_PREFIX + qualifier + "." + fieldCache.get("formAlias"));
        if (StringUtils.isNotBlank(type)) {
            return new String[] { type };
        }
        return new String[] { configurationService.getProperty(PROPERTY_PREFIX + qualifier) };
    }
}