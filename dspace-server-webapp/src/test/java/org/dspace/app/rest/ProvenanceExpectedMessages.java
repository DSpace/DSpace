/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

/**
 * The ProvenanceExpectedMessages enum provides message templates for provenance messages.
 *
 * @author Michaela Paurikova (dspace at dataquest.sk)
 */
public enum ProvenanceExpectedMessages {
    DISCOVERABLE("Item was made discoverable by first (admin) last (admin) (admin@email.com) on \nNo. " +
            "of bitstreams: 0\nItem was in collections:\n"),
    NON_DISCOVERABLE("Item was made non-discoverable by first (admin) last (admin) (admin@email.com) on " +
            "\nNo. of bitstreams: 0\nItem was in collections:\n"),
    MAPPED_COL("was mapped to collection"),
    ADD_ITEM_MTD("Item metadata (dc.title) was added by first (admin) last (admin) (admin@email.com) on"),
    REPLACE_ITEM_MTD("Item metadata (dc.title: Public item 1) was updated by first (admin) last (admin) " +
            "(admin@email.com) on \nNo. of bitstreams: 0"),
    REMOVE_ITEM_MTD("Item metadata (dc.title: Public item 1) was deleted by first (admin) last (admin) " +
            "(admin@email.com) on \nNo. of bitstreams: 0"),
    REMOVE_BITSTREAM_MTD("Item metadata (dc.description) was added by bitstream"),
    REPLACE_BITSTREAM_MTD("metadata (dc.title: test) was updated by first (admin) last (admin) " +
            "(admin@email.com) on \nNo. of bitstreams: 1\n"),
    REMOVE_BITSTREAM("was deleted bitstream"),
    ADD_BITSTREAM("Item was added bitstream to bundle"),
    UPDATE_LICENSE("License (Test 1) was updated by first (admin) last (admin) (admin@email.com) " +
            "on \nNo. of bitstreams: 1\n"),
    ADD_LICENSE("License (empty) was added by first (admin) last (admin) (admin@email.com) on \nNo." +
            " of bitstreams: 0"),
    REMOVE_LICENSE("License (Test) was removed by first (admin) last (admin) (admin@email.com) on " +
            "\nNo. of bitstreams: 1\n"),
    MOVED_ITEM_COL("Item was moved from collection ");

    private final String template;

    // Constructor to initialize enum with the template string
    ProvenanceExpectedMessages(String template) {
        this.template = template;
    }

    // Method to retrieve the template string
    public String getTemplate() {
        return template;
    }
}
