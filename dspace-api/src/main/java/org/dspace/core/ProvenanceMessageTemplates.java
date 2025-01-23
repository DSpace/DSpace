/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

/**
 * The ProvenanceMessageTemplates enum provides message templates for provenance messages.
 *
 * @author Michaela Paurikova (dspace at dataquest.sk)
 */
public enum ProvenanceMessageTemplates {
    ACCESS_CONDITION("Access condition (%s) was added to %s (%s)"),
    RESOURCE_POLICIES_REMOVED("Resource policies (%s) of %s (%s) were removed"),
    BUNDLE_ADDED("Item was added bitstream to bundle (%s)"),
    EDIT_LICENSE("License (%s) was %s"),
    MOVE_ITEM("Item was moved from collection (%s) to different collection"),
    MAPPED_ITEM("Item was mapped to collection (%s)"),
    DELETED_ITEM_FROM_MAPPED("Item was deleted from mapped collection (%s)"),
    EDIT_BITSTREAM("Item (%s) was deleted bitstream (%s)"),
    ITEM_METADATA("Item metadata (%s) was %s"),
    BITSTREAM_METADATA("Item metadata (%s) was %s bitstream (%s)"),
    ITEM_REPLACE_SINGLE_METADATA("Item bitstream (%s) metadata (%s) was updated"),
    DISCOVERABLE("Item was made %sdiscoverable");

    private final String template;

    ProvenanceMessageTemplates(String template) {
        this.template = template;
    }

    public String getTemplate() {
        return template;
    }
}
