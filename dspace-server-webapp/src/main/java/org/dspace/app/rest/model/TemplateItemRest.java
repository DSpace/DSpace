/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.Date;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.dspace.app.rest.RestResourceController;

/**
 * The TemplateItem REST Resource
 */
public class TemplateItemRest extends BaseObjectRest<UUID> {
    private UUID uuid;

    public static final String NAME = "itemtemplate";
    public static final String CATEGORY = RestAddressableModel.CORE;
    @JsonIgnore
    private CollectionRest templateItemOf;
    MetadataRest metadata = new MetadataRest();
    private Date lastModified = new Date();

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public CollectionRest getTemplateItemOf() {
        return templateItemOf;
    }

    public void setTemplateItemOf(CollectionRest templateItemOf) {
        this.templateItemOf = templateItemOf;
    }

    public void setMetadata(MetadataRest metadata) {
        this.metadata = metadata;
    }

    public MetadataRest getMetadata() {
        return this.metadata;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public Class getController() {
        return RestResourceController.class;
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getType() {
        return NAME;
    }

    @Override
    public UUID getId() {
        return uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

}
