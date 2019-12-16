/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Item REST Resource
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@LinksRest(links = {
    @LinkRest(
            name = ItemRest.RELATIONSHIPS,
            linkClass = RelationshipRest.class,
            method = "getItemRelationships",
            embedOptional = true
    )
})
public class ItemRest extends DSpaceObjectRest {
    public static final String NAME = "item";
    public static final String PLURAL_NAME = "items";
    public static final String CATEGORY = RestAddressableModel.CORE;
    public static final String RELATIONSHIPS = "relationships";
    private boolean inArchive = false;
    private boolean discoverable = false;
    private boolean withdrawn = false;
    private Date lastModified = new Date();
    @JsonIgnore
    private CollectionRest owningCollection;
    @JsonIgnore
    private CollectionRest templateItemOf;

    List<BundleRest> bundles;

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

    public CollectionRest getOwningCollection() {
        return owningCollection;
    }

    public void setOwningCollection(CollectionRest owningCollection) {
        this.owningCollection = owningCollection;
    }

    public CollectionRest getTemplateItemOf() {
        return templateItemOf;
    }

    public void setTemplateItemOf(CollectionRest templateItemOf) {
        this.templateItemOf = templateItemOf;
    }

    @LinkRest(linkClass = BundleRest.class)
    @JsonIgnore
    public List<BundleRest> getBundles() {
        return bundles;
    }

    public void setBundles(List<BundleRest> bundles) {
        this.bundles = bundles;
    }
}
