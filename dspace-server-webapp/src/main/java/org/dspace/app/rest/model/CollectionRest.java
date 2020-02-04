/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Collection REST Resource
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@LinksRest(links = {
        @LinkRest(
                name = CollectionRest.DEFAULT_ACCESS_CONDITIONS,
                linkClass = ResourcePolicyRest.class,
                method = "getDefaultAccessConditions"
        ),
        @LinkRest(
                name = CollectionRest.LICENSE,
                linkClass = LicenseRest.class,
                method = "getLicense"
        ),
        @LinkRest(
                name = CollectionRest.LOGO,
                linkClass = BitstreamRest.class,
                method = "getLogo"
        ),
        @LinkRest(
                name = CollectionRest.MAPPED_ITEMS,
                linkClass = ItemRest.class,
                method = "getMappedItems"
        )
})
public class CollectionRest extends DSpaceObjectRest {
    public static final String NAME = "collection";
    public static final String PLURAL_NAME = "collections";
    public static final String CATEGORY = RestAddressableModel.CORE;

    public static final String DEFAULT_ACCESS_CONDITIONS = "defaultAccessConditions";
    public static final String HARVEST = "harvester";
    public static final String LICENSE = "license";
    public static final String LOGO = "logo";
    public static final String MAPPED_ITEMS = "mappedItems";

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getType() {
        return NAME;
    }
}
