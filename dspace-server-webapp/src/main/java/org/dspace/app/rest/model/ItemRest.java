/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Item REST Resource
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@LinksRest(links = {
        @LinkRest(
                name = ItemRest.ACCESS_STATUS,
                method = "getAccessStatus"
        ),
        @LinkRest(
                name = ItemRest.BUNDLES,
                method = "getBundles"
        ),
        @LinkRest(
                name = ItemRest.MAPPED_COLLECTIONS,
                method = "getMappedCollections"
        ),
        @LinkRest(
                name = ItemRest.OWNING_COLLECTION,
                method = "getOwningCollection"
        ),
        @LinkRest(
                name = ItemRest.RELATIONSHIPS,
                method = "getRelationships"
        ),
        @LinkRest(
                name = ItemRest.VERSION,
                method = "getItemVersion"
        ),
        @LinkRest(
                name = ItemRest.TEMPLATE_ITEM_OF,
                method = "getTemplateItemOf"
        ),
        @LinkRest(
                name = ItemRest.THUMBNAIL,
                method = "getThumbnail"
        )
})
public class ItemRest extends DSpaceObjectRest {
    public static final String NAME = "item";
    public static final String PLURAL_NAME = "items";
    public static final String CATEGORY = RestAddressableModel.CORE;

    public static final String ACCESS_STATUS = "accessStatus";
    public static final String BUNDLES = "bundles";
    public static final String MAPPED_COLLECTIONS = "mappedCollections";
    public static final String OWNING_COLLECTION = "owningCollection";
    public static final String RELATIONSHIPS = "relationships";
    public static final String VERSION = "version";
    public static final String TEMPLATE_ITEM_OF = "templateItemOf";
    public static final String THUMBNAIL = "thumbnail";

    private boolean inArchive = false;
    private boolean discoverable = false;
    private boolean withdrawn = false;
    private Date lastModified = new Date();
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String entityType = null;

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getType() {
        return NAME;
    }

    public boolean getInArchive() {
        return inArchive;
    }

    public void setInArchive(boolean inArchive) {
        this.inArchive = inArchive;
    }

    public boolean getDiscoverable() {
        return discoverable;
    }

    public void setDiscoverable(boolean discoverable) {
        this.discoverable = discoverable;
    }

    public boolean getWithdrawn() {
        return withdrawn;
    }

    public void setWithdrawn(boolean withdrawn) {
        this.withdrawn = withdrawn;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }
}
