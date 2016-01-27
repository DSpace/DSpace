/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.importer.external.service.other;

import org.dspace.content.Item;
import org.dspace.importer.external.MetadataSourceException;
import org.dspace.importer.external.Query;
import org.dspace.importer.external.datamodel.ImportRecord;

import java.util.Collection;

/**
 * Created by Roeland Dillen (roeland at atmire dot com)
 * Date: 17/09/12
 * Time: 14:08
 */
public interface Imports {
    public int getNbRecords(String query) throws MetadataSourceException;
    public int getNbRecords(Query query) throws MetadataSourceException;
    public Collection<ImportRecord> getRecords(String query, int start, int count)throws MetadataSourceException;
    public Collection<ImportRecord> getRecords(Query q)throws MetadataSourceException;
    public ImportRecord getRecord(String id)throws MetadataSourceException;
    public ImportRecord getRecord(Query q)throws MetadataSourceException;
    public String getImportSource();

    public Collection<ImportRecord> findMatchingRecords(Item item) throws MetadataSourceException;

    public Collection<ImportRecord> findMatchingRecords(Query q) throws MetadataSourceException;
}
