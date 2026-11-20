/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.datamodel;

/**
 * Count of records from a {@link org.dspace.importer.external.service.components.QuerySource},
 * indicating whether it came from an exact id lookup or a query search.
 */
public class RecordsCount {

    private final int count;
    private final boolean exact;

    private RecordsCount(int count, boolean exact) {
        this.count = count;
        this.exact = exact;
    }

    /**
     * @param count number of matching records from an exact id lookup
     * @return exact count wrapper
     */
    public static RecordsCount exact(int count) {
        return new RecordsCount(count, true);
    }

    /**
     * @param count number of matching records from a query search
     * @return query count wrapper
     */
    public static RecordsCount query(int count) {
        return new RecordsCount(count, false);
    }

    public int getCount() {
        return count;
    }

    public boolean isExact() {
        return exact;
    }
}
