/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.suggestion.oaire;

import java.util.List;

import org.dspace.content.Item;
import org.dspace.importer.external.datamodel.ImportRecord;

/**
 * This class provide the pipeline of Approver, which is responsible
 * for filtering the ImportRecords from openAire
 * 
 * @author Pasquale Cavallo (pasquale.cavallo at 4science dot it)
 *
 */
public class OAIREPublicationApproverServiceImpl {

    private List<Approver> pipeline;

    /**
     * Set the pipeline of Approver
     * @param pipeline list Approver
     */
    public void setPipeline(List<Approver> pipeline) {
        this.pipeline = pipeline;
    }

    /**
     * This method filter a list of ImportRecords using a pipeline of AuthorNamesApprover
     * and return a filtered list of ImportRecords.
     * 
     * @see org.dspace.app.suggestion.oaire.AuthorNamesApprover
     * @param researcher the researcher Item
     * @param importRecord List of import record
     * @return a list of filtered import records
     */
    public List<ImportRecord> approve(Item researcher, List<ImportRecord> importRecord) {
        List<ImportRecord> filtered = importRecord;
        for (Approver authorNameApprover : pipeline) {
            filtered = authorNameApprover.filter(researcher, importRecord);
        }
        return filtered;
    }
}
