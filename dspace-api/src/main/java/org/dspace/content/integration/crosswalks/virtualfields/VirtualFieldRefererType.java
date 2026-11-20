/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.virtualfields;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReader;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class VirtualFieldRefererType implements VirtualField {

    private static final String PROPERTY_PREFIX = "crosswalk.virtualfield.referer-type.";

    private final ConfigurationService configurationService;

    @Autowired
    public VirtualFieldRefererType(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    public String[] getMetadata(Context context, Item item, String fieldName) {
        String[] virtualFieldName = fieldName.split("\\.");
        String qualifier = virtualFieldName[2];
        String type = configurationService.getProperty(PROPERTY_PREFIX + qualifier + "." + getAliasForm(item));
        if (StringUtils.isNotBlank(type)) {
            return new String[] { type };
        }
        return new String[] { configurationService.getProperty(PROPERTY_PREFIX + qualifier) };
    }

    private String getAliasForm(Item item) {
        try {
            Collection collection = item.getOwningCollection();
            // Read the input form file for the specific collection
            DCInputsReader inputsReader = new DCInputsReader();

            List<DCInputSet> inputSet = inputsReader.getInputsByCollection(collection);
            DCInputSet dci = inputSet.get(0);
            return dci.getFormName();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}