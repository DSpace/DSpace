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
 * Specialization of ItemRest dedicated to the Filtered Items report.
 * This class adds the owning collection property required to properly
 * display search results without compromising the expected behaviour
 * of standard ItemRest instances, in all other contexts, especially
 * when it comes to embedded contents, a criterion that is widely checked
 * against in several integration tests.
 *
 * @author Jean-François Morin (jean-francois.morin@bibl.ulaval.ca)
 */
@LinksRest(links = {
        @LinkRest(
                name = FilteredItemRest.OWNING_COLLECTION,
                method = "getOwningCollection"
        )
})
public class FilteredItemRest extends ItemRest {

    private static final long serialVersionUID = -4743487764046689861L;
    public static final String NAME = "filtered item";
    public static final String PLURAL_NAME = "filtered items";
    public static final String CATEGORY = RestAddressableModel.CONTENT_REPORT;

    public static final String OWNING_COLLECTION = "owningCollection";

    private CollectionRest owningCollection;

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getType() {
        return NAME;
    }

    public CollectionRest getOwningCollection() {
        return owningCollection;
    }

    public void setOwningCollection(CollectionRest owningCollection) {
        this.owningCollection = owningCollection;
    }

}
