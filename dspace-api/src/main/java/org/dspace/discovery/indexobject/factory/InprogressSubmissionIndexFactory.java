/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.indexobject.factory;

import java.sql.SQLException;

import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.InProgressSubmission;
import org.dspace.core.Context;
import org.dspace.discovery.indexobject.IndexableInProgressSubmission;

/**
 * Factory interface for indexing/retrieving InProgresssSubmission objects in the search core
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public interface InprogressSubmissionIndexFactory<T extends IndexableInProgressSubmission,
        S extends InProgressSubmission>
        extends IndexFactory<T, S> {

    /**
     * Store common fields between workspace / workflow items in the solr document
     * @param context               DSpace context object
     * @param doc                   Solr input document which will be written to our discovery solr core
     * @param inProgressSubmission  the workspace / workflow item
     * @throws SQLException         If database error
     */
    void storeInprogressItemFields(Context context, SolrInputDocument doc, InProgressSubmission inProgressSubmission)
            throws SQLException;
}