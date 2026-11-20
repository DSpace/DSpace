/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.datamodel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Marker list for records from an exact identifier lookup.
 */
public class ExactImportRecords extends ArrayList<ImportRecord> {

    public ExactImportRecords(Collection<? extends ImportRecord> records) {
        super(records == null ? Collections.emptyList() : records);
    }
}
