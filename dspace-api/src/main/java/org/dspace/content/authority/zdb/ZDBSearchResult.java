/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority.zdb;

import java.util.List;

/**
 * Holder for the outcome of a ZDB SRU search: the parsed records for the requested page together
 * with the grand total of matching records as reported by the {@code numberOfRecords} element of
 * the SRU response.
 *
 * <p>The total is required so that authority pagination reflects the full result set rather than
 * the size of the single page returned by the service.</p>
 *
 * @author DSpace
 */
public class ZDBSearchResult {

    private final List<ZDBAuthorityValue> records;
    private final int total;

    /**
     * Create a new result holder.
     *
     * @param records the records parsed for the current page (never {@code null})
     * @param total   the grand total of matching records reported by the SRU service
     */
    public ZDBSearchResult(List<ZDBAuthorityValue> records, int total) {
        this.records = records;
        this.total = total;
    }

    /**
     * Get the records parsed for the current page.
     *
     * @return the list of records (never {@code null})
     */
    public List<ZDBAuthorityValue> getRecords() {
        return records;
    }

    /**
     * Get the grand total of matching records reported by the SRU service.
     *
     * @return the total number of matching records
     */
    public int getTotal() {
        return total;
    }
}
